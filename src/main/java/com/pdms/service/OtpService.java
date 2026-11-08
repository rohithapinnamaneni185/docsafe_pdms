package com.pdms.service;

import com.pdms.dto.LoginInitiateRequest;
import com.pdms.dto.LoginInitiateResponse;
import com.pdms.entity.User;
import com.pdms.exception.BadRequestException;
import com.pdms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_VALIDITY_MINUTES = 5;

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    // In-memory pending OTP session cache
    private final Map<String, PendingOtpSession> pendingSessions = new ConcurrentHashMap<>();

    public OtpService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Pending OTP Session data class.
     */
    public static class PendingOtpSession {
        private final String tempToken;
        private final Long userId;
        private String otpCode;
        private LocalDateTime expiryTime;
        private final String destination;

        public PendingOtpSession(String tempToken, Long userId, String otpCode, LocalDateTime expiryTime, String destination) {
            this.tempToken = tempToken;
            this.userId = userId;
            this.otpCode = otpCode;
            this.expiryTime = expiryTime;
            this.destination = destination;
        }

        public String getTempToken() {
            return tempToken;
        }

        public Long getUserId() {
            return userId;
        }

        public String getOtpCode() {
            return otpCode;
        }

        public void setOtpCode(String otpCode) {
            this.otpCode = otpCode;
        }

        public LocalDateTime getExpiryTime() {
            return expiryTime;
        }

        public void setExpiryTime(LocalDateTime expiryTime) {
            this.expiryTime = expiryTime;
        }

        public String getDestination() {
            return destination;
        }
    }

    /**
     * Initiates 2-step login: verifies identifier (email or mobile) and password,
     * generates a 6-digit OTP, logs SMS & Email simulation banners, and saves OTP state.
     */
    public LoginInitiateResponse initiateLogin(LoginInitiateRequest request) {
        if (request.getIdentifier() == null || request.getIdentifier().isBlank()) {
            throw new BadRequestException("Please enter your registered email or 10-digit mobile number.");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Please enter your password.");
        }

        String rawIdentifier = request.getIdentifier().trim();
        User user = userRepository.findByIdentifier(rawIdentifier)
                .or(() -> userRepository.findByEmailIgnoreCase(rawIdentifier.toLowerCase()))
                .or(() -> userRepository.findByMobileNumber(rawIdentifier))
                .orElseThrow(() -> new BadRequestException("Invalid credentials. Please verify your email/mobile and password."));

        if (!user.getPassword().equals(request.getPassword().trim())) {
            throw new BadRequestException("Invalid credentials. Please verify your email/mobile and password.");
        }

        // Generate 6-digit OTP
        String otp = generateNumericOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES);
        String tempToken = UUID.randomUUID().toString();

        // Update user record in database
        user.setOtpCode(otp);
        user.setOtpExpiry(expiry);
        userRepository.save(user);

        // Format masked destination
        String maskedDestination = formatMaskedDestination(user);

        // Cache session
        PendingOtpSession session = new PendingOtpSession(tempToken, user.getId(), otp, expiry, maskedDestination);
        pendingSessions.put(tempToken, session);

        // Dispatch simulated SMS and Email
        dispatchOtpSimulation(user, otp);

        return new LoginInitiateResponse("OTP_REQUIRED", tempToken, maskedDestination, otp);
    }

    /**
     * Verifies the 6-digit OTP code against the pending session token.
     */
    public User verifyOtp(String tempToken, String otpCode) {
        if (tempToken == null || tempToken.isBlank()) {
            throw new BadRequestException("Invalid session. Please return to login.");
        }
        if (otpCode == null || otpCode.trim().length() != 6) {
            throw new BadRequestException("Please enter a valid 6-digit verification OTP.");
        }

        PendingOtpSession session = pendingSessions.get(tempToken.trim());
        String cleanCode = otpCode.trim();

        if (session == null) {
            // Check fallback from DB
            User dbUser = userRepository.findAll().stream()
                    .filter(u -> cleanCode.equals(u.getOtpCode()) && u.getOtpExpiry() != null && LocalDateTime.now().isBefore(u.getOtpExpiry()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Invalid or expired OTP. Please request a new code."));

            dbUser.setOtpCode(null);
            dbUser.setOtpExpiry(null);
            return userRepository.save(dbUser);
        }

        if (LocalDateTime.now().isAfter(session.getExpiryTime())) {
            pendingSessions.remove(tempToken);
            throw new BadRequestException("OTP has expired. Please click 'Resend OTP' to receive a fresh code.");
        }

        if (!session.getOtpCode().equals(cleanCode)) {
            throw new BadRequestException("Invalid 6-digit OTP. Please check the code and try again.");
        }

        // Successfully verified
        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new BadRequestException("User record not found."));

        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        pendingSessions.remove(tempToken);
        log.info("Successfully verified 2FA OTP for user: {} ({})", user.getFullName(), user.getEmail());

        return user;
    }

    /**
     * Resends a fresh 6-digit OTP for an active pending session.
     */
    public LoginInitiateResponse resendOtp(String tempToken) {
        if (tempToken == null || tempToken.isBlank()) {
            throw new BadRequestException("Session token is missing. Please return to login.");
        }

        PendingOtpSession session = pendingSessions.get(tempToken.trim());
        if (session == null) {
            throw new BadRequestException("Verification session has expired. Please enter your credentials again.");
        }

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new BadRequestException("User record not found."));

        String newOtp = generateNumericOtp();
        LocalDateTime newExpiry = LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES);

        session.setOtpCode(newOtp);
        session.setExpiryTime(newExpiry);

        user.setOtpCode(newOtp);
        user.setOtpExpiry(newExpiry);
        userRepository.save(user);

        dispatchOtpSimulation(user, newOtp);

        return new LoginInitiateResponse("OTP_REQUIRED", tempToken, session.getDestination(), newOtp);
    }

    private String generateNumericOtp() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    private String formatMaskedDestination(User user) {
        String email = user.getEmail();
        String mobile = user.getMobileNumber();

        String maskedEmail = maskEmail(email);
        String maskedMobile = maskMobile(mobile);

        if (maskedEmail != null && maskedMobile != null) {
            return maskedEmail + " / " + maskedMobile;
        } else if (maskedEmail != null) {
            return maskedEmail;
        } else if (maskedMobile != null) {
            return maskedMobile;
        }
        return "your registered email/mobile";
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + domain;
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) return null;
        String clean = mobile.replaceAll("[^0-9]", "");
        if (clean.length() <= 4) return clean;
        return "******" + clean.substring(clean.length() - 4);
    }

    private void dispatchOtpSimulation(User user, String otp) {
        String mobile = (user.getMobileNumber() != null && !user.getMobileNumber().isBlank())
                ? user.getMobileNumber() : "9876543210";
        String email = user.getEmail();

        System.out.println();
        System.out.println("========================================================================");
        System.out.println("  [SMS GATEWAY SIMULATION] OTP for +91 " + mobile + " is: " + otp);
        System.out.println("  [EMAIL GATEWAY SIMULATION] Sent to " + email + " | 6-Digit OTP: " + otp);
        System.out.println("  [DOCUSAFE 2FA] Validity: 5 Minutes. Do not share this code.");
        System.out.println("========================================================================");
        System.out.println();
    }
}

