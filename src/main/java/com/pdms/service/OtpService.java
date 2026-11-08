package com.pdms.service;

import com.pdms.dto.LoginInitiateRequest;
import com.pdms.dto.LoginInitiateResponse;
import com.pdms.dto.LoginOtpResponse;
import com.pdms.dto.LoginRequest;
import com.pdms.dto.RegisterRequest;
import com.pdms.dto.ResendOtpRequest;
import com.pdms.dto.VerifyOtpRequest;
import com.pdms.entity.OtpVerification;
import com.pdms.entity.User;
import com.pdms.exception.BadRequestException;
import com.pdms.exception.UnauthorizedException;
import com.pdms.repository.OtpVerificationRepository;
import com.pdms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_VALIDITY_MINUTES = 5;

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    // In-memory pending OTP session cache for fast lookup
    private final Map<String, PendingOtpSession> pendingSessions = new ConcurrentHashMap<>();

    @Autowired
    public OtpService(UserRepository userRepository,
                      OtpVerificationRepository otpVerificationRepository,
                      EmailService emailService,
                      @Autowired(required = false) PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.otpVerificationRepository = otpVerificationRepository;
        this.emailService = emailService;
        this.passwordEncoder = (passwordEncoder != null) ? passwordEncoder : new BCryptPasswordEncoder();
    }

    public OtpService(UserRepository userRepository,
                      OtpVerificationRepository otpVerificationRepository,
                      EmailService emailService) {
        this(userRepository, otpVerificationRepository, emailService, new BCryptPasswordEncoder());
    }

    public OtpService(UserRepository userRepository) {
        this(userRepository, null, null, new BCryptPasswordEncoder());
    }

    /**
     * Session cache model for pending OTP verification.
     */
    public static class PendingOtpSession {
        private final String tempToken;
        private final Long userId;
        private final String email;
        private String otpCode;
        private LocalDateTime expiryTime;

        public PendingOtpSession(String tempToken, Long userId, String email, String otpCode, LocalDateTime expiryTime) {
            this.tempToken = tempToken;
            this.userId = userId;
            this.email = email;
            this.otpCode = otpCode;
            this.expiryTime = expiryTime;
        }

        public String getTempToken() {
            return tempToken;
        }

        public Long getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
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
            return email;
        }
    }

    /**
     * Securely checks a password against the stored password (BCrypt or legacy plaintext).
     * If legacy plaintext matches, auto-upgrades the record to BCrypt.
     */
    public boolean verifyPassword(String rawPassword, String storedPassword, User user) {
        if (storedPassword == null || rawPassword == null) {
            return false;
        }
        // Check BCrypt hash ($2a$, $2b$, or $2y$)
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        // Check legacy plaintext password
        if (storedPassword.equals(rawPassword)) {
            try {
                user.setPassword(passwordEncoder.encode(rawPassword));
                userRepository.save(user);
                log.info("Auto-upgraded user {} password to BCrypt hash.", user.getEmail());
            } catch (Exception ex) {
                log.warn("Could not auto-upgrade password for {}: {}", user.getEmail(), ex.getMessage());
            }
            return true;
        }
        return false;
    }

    /**
     * POST /api/auth/login-request
     * Validates email & password. If invalid, throws UnauthorizedException (401).
     * If valid, generates a 6-digit OTP, saves in database, dispatches email/console, and returns response.
     */
    public LoginOtpResponse loginRequest(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!verifyPassword(request.getPassword().trim(), user.getPassword(), user)) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Credentials match: generate 6-digit OTP
        String otp = generateNumericOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES);
        String tempToken = UUID.randomUUID().toString();

        // 1. Save in dedicated otp_verifications table
        if (otpVerificationRepository != null) {
            OtpVerification verification = new OtpVerification(user.getEmail(), otp, expiry);
            otpVerificationRepository.save(verification);
        }

        // 2. Persist in user entity
        user.setOtpCode(otp);
        user.setOtpExpiry(expiry);
        userRepository.save(user);

        // 3. Cache session
        PendingOtpSession session = new PendingOtpSession(tempToken, user.getId(), user.getEmail(), otp, expiry);
        pendingSessions.put(tempToken, session);

        // 4. Dispatch Email / console simulation
        if (emailService != null) {
            emailService.sendOtpEmail(user.getEmail(), otp);
        } else {
            System.out.println("========================================================================");
            System.out.println("  [DOCUSAFE OTP] Verification code for " + user.getEmail() + ": " + otp);
            System.out.println("========================================================================");
        }

        return LoginOtpResponse.success("OTP sent to your email", otp, user.getEmail(), tempToken);
    }

    /**
     * POST /api/auth/register-request
     * Registers a new user account with BCrypt password hashing and sends an OTP verification email.
     */
    public LoginOtpResponse registerRequest(RegisterRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new BadRequestException("Email address is required.");
        }
        if (req.getFullName() == null || req.getFullName().isBlank()) {
            throw new BadRequestException("Full name is required.");
        }
        if (req.getPassword() == null || req.getPassword().trim().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters.");
        }

        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new BadRequestException("An account with this email already exists: " + email + ". Please sign in.");
        }

        // Hash password with BCrypt
        String encodedPassword = passwordEncoder.encode(req.getPassword().trim());

        // Determine role: First user is admin, otherwise member
        String role = (userRepository.count() == 0) ? "ROLE_ADMIN" : "ROLE_MEMBER";

        User user = new User();
        user.setFullName(req.getFullName().trim());
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setRole(role);
        user.setRelationship("Self");
        user.setTheme("theme-default");

        String otp = generateNumericOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES);
        String tempToken = UUID.randomUUID().toString();

        user.setOtpCode(otp);
        user.setOtpExpiry(expiry);
        user = userRepository.save(user);

        if (otpVerificationRepository != null) {
            OtpVerification verification = new OtpVerification(user.getEmail(), otp, expiry);
            otpVerificationRepository.save(verification);
        }

        pendingSessions.put(tempToken, new PendingOtpSession(tempToken, user.getId(), user.getEmail(), otp, expiry));

        if (emailService != null) {
            emailService.sendOtpEmail(user.getEmail(), otp);
        } else {
            System.out.println("========================================================================");
            System.out.println("  [DOCUSAFE OTP] Verification code for " + user.getEmail() + ": " + otp);
            System.out.println("========================================================================");
        }

        log.info("New vault account registered for {} with BCrypt password hashing.", email);
        return LoginOtpResponse.success("Account created successfully! Verification code sent to your email.", otp, user.getEmail(), tempToken);
    }

    /**
     * Handles Google Sign-In: finds or creates a user account for the Google identity.
     */
    public User googleLogin(String email, String fullName) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Google email address is required.");
        }
        String cleanEmail = email.trim().toLowerCase();
        String displayName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : cleanEmail.split("@")[0];

        Optional<User> existing = userRepository.findByEmailIgnoreCase(cleanEmail);
        if (existing.isPresent()) {
            User user = existing.get();
            log.info("Google OAuth login for existing user: {}", cleanEmail);
            return user;
        }

        // New Google account registration
        User newUser = new User();
        newUser.setFullName(displayName);
        newUser.setEmail(cleanEmail);
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        newUser.setRelationship("Self");
        newUser.setRole(userRepository.count() == 0 ? "ROLE_ADMIN" : "ROLE_MEMBER");
        newUser.setTheme("theme-default");
        User saved = userRepository.save(newUser);
        log.info("Google OAuth auto-provisioned new vault account for: {}", cleanEmail);
        return saved;
    }

    /**
     * Backward-compatible initiateLogin for LoginInitiateRequest.
     */
    public LoginInitiateResponse initiateLogin(LoginInitiateRequest request) {
        String identifier = request.getIdentifier() != null ? request.getIdentifier().trim() : "";
        LoginOtpResponse res = loginRequest(new LoginRequest(identifier, request.getPassword()));
        return new LoginInitiateResponse("OTP_REQUIRED", res.getTempToken(), res.getEmail(), res.getDemoOtp());
    }

    /**
     * Verifies the 6-digit OTP by email (or fallback to session token).
     */
    public User verifyOtpByEmail(String email, String otpCode) {
        if (otpCode == null || otpCode.trim().length() != 6) {
            throw new BadRequestException("Please enter a valid 6-digit verification OTP.");
        }
        String cleanCode = otpCode.trim();

        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email address is required for OTP verification.");
        }

        String cleanEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new BadRequestException("No registered account found with email: " + cleanEmail));

        // 1. Check otp_verifications repository if available
        boolean verified = false;
        if (otpVerificationRepository != null) {
            var verificationOpt = otpVerificationRepository
                    .findTopByEmailIgnoreCaseAndOtpCodeAndVerifiedFalseOrderByCreatedAtDesc(cleanEmail, cleanCode);

            if (verificationOpt.isPresent()) {
                OtpVerification record = verificationOpt.get();
                if (LocalDateTime.now().isBefore(record.getExpiresAt())) {
                    record.setVerified(true);
                    otpVerificationRepository.save(record);
                    verified = true;
                } else {
                    throw new BadRequestException("OTP has expired. Please request a new code.");
                }
            }
        }

        // 2. Fallback to user entity check if not verified yet
        if (!verified) {
            if (cleanCode.equals(user.getOtpCode())) {
                if (user.getOtpExpiry() != null && LocalDateTime.now().isBefore(user.getOtpExpiry())) {
                    verified = true;
                } else {
                    throw new BadRequestException("OTP has expired. Please request a new code.");
                }
            }
        }

        // 3. Fallback to pending sessions cache check
        if (!verified) {
            for (Map.Entry<String, PendingOtpSession> entry : pendingSessions.entrySet()) {
                PendingOtpSession session = entry.getValue();
                if (session.getEmail().equalsIgnoreCase(cleanEmail) && session.getOtpCode().equals(cleanCode)) {
                    if (LocalDateTime.now().isBefore(session.getExpiryTime())) {
                        verified = true;
                        pendingSessions.remove(entry.getKey());
                        break;
                    } else {
                        throw new BadRequestException("OTP has expired. Please request a new code.");
                    }
                }
            }
        }

        if (!verified) {
            throw new BadRequestException("Invalid 6-digit OTP. Please check the code and try again.");
        }

        // Clear OTP from user entity
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        log.info("Successfully verified Email 2FA OTP for user: {} ({})", user.getFullName(), user.getEmail());
        return user;
    }

    /**
     * Backward-compatible verifyOtp by tempToken and OTP code.
     */
    public User verifyOtp(String tempToken, String otpCode) {
        if (otpCode == null || otpCode.trim().length() != 6) {
            throw new BadRequestException("Please enter a valid 6-digit verification OTP.");
        }

        if (tempToken != null && pendingSessions.containsKey(tempToken.trim())) {
            PendingOtpSession session = pendingSessions.get(tempToken.trim());
            return verifyOtpByEmail(session.getEmail(), otpCode);
        }

        // Fallback: check all users for matching unexpired code
        String cleanCode = otpCode.trim();
        User dbUser = userRepository.findAll().stream()
                .filter(u -> cleanCode.equals(u.getOtpCode()) && u.getOtpExpiry() != null && LocalDateTime.now().isBefore(u.getOtpExpiry()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP. Please request a new code."));

        dbUser.setOtpCode(null);
        dbUser.setOtpExpiry(null);
        return userRepository.save(dbUser);
    }

    /**
     * Verifies OTP using VerifyOtpRequest DTO.
     */
    public User verifyOtp(VerifyOtpRequest req) {
        if (req == null) {
            throw new BadRequestException("Verification request cannot be null.");
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            return verifyOtpByEmail(req.getEmail(), req.getOtp());
        }
        return verifyOtp(req.getTempToken(), req.getOtp());
    }

    /**
     * Resends a fresh 6-digit OTP to the specified email.
     */
    public LoginOtpResponse resendOtpByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required to resend OTP.");
        }

        String cleanEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new BadRequestException("No registered account found with email: " + cleanEmail));

        String newOtp = generateNumericOtp();
        LocalDateTime newExpiry = LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES);
        String tempToken = UUID.randomUUID().toString();

        if (otpVerificationRepository != null) {
            OtpVerification verification = new OtpVerification(user.getEmail(), newOtp, newExpiry);
            otpVerificationRepository.save(verification);
        }

        user.setOtpCode(newOtp);
        user.setOtpExpiry(newExpiry);
        userRepository.save(user);

        pendingSessions.put(tempToken, new PendingOtpSession(tempToken, user.getId(), user.getEmail(), newOtp, newExpiry));

        if (emailService != null) {
            emailService.sendOtpEmail(user.getEmail(), newOtp);
        } else {
            System.out.println("========================================================================");
            System.out.println("  [DOCUSAFE OTP] Verification code for " + user.getEmail() + ": " + newOtp);
            System.out.println("========================================================================");
        }

        return LoginOtpResponse.success("OTP sent to your email", newOtp, user.getEmail(), tempToken);
    }

    /**
     * Backward-compatible resendOtp using tempToken.
     */
    public LoginInitiateResponse resendOtp(String tempToken) {
        if (tempToken == null || tempToken.isBlank()) {
            throw new BadRequestException("Session token is missing. Please return to login.");
        }

        PendingOtpSession session = pendingSessions.get(tempToken.trim());
        if (session == null) {
            throw new BadRequestException("Verification session has expired. Please enter your credentials again.");
        }

        LoginOtpResponse res = resendOtpByEmail(session.getEmail());
        return new LoginInitiateResponse("OTP_REQUIRED", res.getTempToken(), res.getEmail(), res.getDemoOtp());
    }

    /**
     * Resends OTP using ResendOtpRequest DTO.
     */
    public LoginInitiateResponse resendOtp(ResendOtpRequest req) {
        if (req == null) {
            throw new BadRequestException("Resend request cannot be null.");
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            LoginOtpResponse resp = resendOtpByEmail(req.getEmail());
            return new LoginInitiateResponse("OTP_REQUIRED", resp.getTempToken(), resp.getEmail(), resp.getDemoOtp());
        }
        return resendOtp(req.getTempToken());
    }

    private String generateNumericOtp() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }
}
