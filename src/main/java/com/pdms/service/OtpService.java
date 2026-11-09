package com.pdms.service;

import com.pdms.dto.LoginInitiateRequest;
import com.pdms.dto.LoginInitiateResponse;
import com.pdms.entity.OtpVerification;
import com.pdms.entity.User;
import com.pdms.exception.BadRequestException;
import com.pdms.exception.EmailDispatchException;
import com.pdms.exception.TooManyRequestsException;
import com.pdms.exception.UnauthorizedException;
import com.pdms.repository.OtpVerificationRepository;
import com.pdms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional
public class OtpService {

    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final String SMTP_ERROR_MESSAGE =
            "Failed to dispatch email. Please verify SMTP configuration.";

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public OtpService(UserRepository userRepository, OtpVerificationRepository otpRepository,
                      EmailService emailService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginInitiateResponse initiateLogin(String email, String password) {
        if (email == null || email.isBlank() || !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new BadRequestException("Please enter a valid email address.");
        }
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .filter(existing -> password != null && existing.getPassword() != null
                        && passwordEncoder.matches(password, existing.getPassword()))
                .orElseThrow(() -> new UnauthorizedException(
                        "Invalid email or password. Please try again."));
        return startVerification(user);
    }

    public LoginInitiateResponse initiateLogin(LoginInitiateRequest request) {
        if (request == null || request.getIdentifier() == null || request.getIdentifier().isBlank()) {
            throw new BadRequestException("Please enter a valid email address.");
        }
        User user = userRepository.findByIdentifier(request.getIdentifier().trim())
                .filter(existing -> request.getPassword() != null && existing.getPassword() != null
                        && passwordEncoder.matches(request.getPassword(), existing.getPassword()))
                .orElseThrow(() -> new UnauthorizedException(
                        "Invalid email or password. Please try again."));
        return startVerification(user);
    }

    public LoginInitiateResponse register(String fullName, String email, String password) {
        if (fullName == null || fullName.isBlank()) {
            throw new BadRequestException("Full name is required.");
        }
        if (email == null || email.isBlank() || !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new BadRequestException("Please enter a valid email address.");
        }
        if (password == null || password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BadRequestException("An account with this email already exists.");
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRelationship("Self");
        user.setRole("ROLE_MEMBER");
        user.setTheme("theme-default");
        return startVerification(userRepository.save(user));
    }

    @Transactional(noRollbackFor = {BadRequestException.class, TooManyRequestsException.class})
    public User verifyOtp(String emailOrToken, String otpCode) {
        if (emailOrToken == null || emailOrToken.isBlank()) {
            throw new BadRequestException("Email address or verification token is required.");
        }
        if (otpCode == null || !otpCode.trim().matches("\\d{6}")) {
            throw new BadRequestException("Please enter a valid 6-digit verification code.");
        }

        OtpVerification verification = findVerification(emailOrToken.trim())
                .orElseThrow(() -> new BadRequestException(
                        "Verification code is invalid or expired. Please request a new code."));
        Instant now = Instant.now();
        if (verification.isUsed() && verification.getAttempts() < MAX_OTP_ATTEMPTS) {
            throw new BadRequestException("Verification code is invalid or expired. Please request a new code.");
        }
        if (!now.isBefore(verification.getExpiresAt())) {
            verification.setUsed(true);
            otpRepository.save(verification);
            throw new BadRequestException("Verification code has expired. Please request a new code.");
        }
        if (verification.getAttempts() >= MAX_OTP_ATTEMPTS) {
            verification.setUsed(true);
            otpRepository.save(verification);
            throw new TooManyRequestsException("Too many failed attempts. Request a new OTP.");
        }
        if (!passwordEncoder.matches(otpCode.trim(), verification.getOtpHash())) {
            int attempts = verification.getAttempts() + 1;
            verification.setAttempts(attempts);
            if (attempts >= MAX_OTP_ATTEMPTS) {
                verification.setUsed(true);
                otpRepository.save(verification);
                throw new TooManyRequestsException("Too many failed attempts. Request a new OTP.");
            }
            otpRepository.save(verification);
            throw new BadRequestException("Invalid verification code. "
                    + (MAX_OTP_ATTEMPTS - attempts) + " attempt(s) remaining.");
        }

        User user = userRepository.findByEmailIgnoreCase(verification.getEmail())
                .orElseThrow(() -> new BadRequestException("User account no longer exists."));
        verification.setUsed(true);
        verification.setVerificationToken(null);
        otpRepository.save(verification);
        return user;
    }

    public LoginInitiateResponse resendOtp(String tokenOrEmail) {
        if (tokenOrEmail == null || tokenOrEmail.isBlank()) {
            throw new BadRequestException("Email address or verification token is required.");
        }
        OtpVerification verification = findVerification(tokenOrEmail.trim())
                .orElseThrow(() -> new BadRequestException(
                        "Verification session has expired. Please sign in again."));
        if (verification.getVerificationToken() == null) {
            throw new BadRequestException("Verification session has expired. Please sign in again.");
        }

        Instant now = Instant.now();
        Instant nextAllowedAt = verification.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS);
        if (now.isBefore(nextAllowedAt)) {
            long secondsRemaining = Math.max(1, (Duration.between(now, nextAllowedAt).toMillis() + 999) / 1000);
            throw new TooManyRequestsException(
                    "Please wait " + secondsRemaining + " seconds before requesting another code.");
        }

        User user = userRepository.findByEmailIgnoreCase(verification.getEmail())
                .orElseThrow(() -> new BadRequestException("User account no longer exists."));
        String otp = generateNumericOtp();
        sendOtp(verification.getEmail(), otp, user.getFullName());

        verification.setOtpHash(passwordEncoder.encode(otp));
        verification.setExpiresAt(now.plus(OTP_VALIDITY_MINUTES, ChronoUnit.MINUTES));
        verification.setAttempts(0);
        verification.setUsed(false);
        verification.setCreatedAt(now);
        otpRepository.save(verification);

        LoginInitiateResponse response = new LoginInitiateResponse(
                "OTP_REQUIRED", verification.getVerificationToken(), maskEmail(verification.getEmail()));
        response.setEmail(verification.getEmail());
        return response;
    }

    private LoginInitiateResponse startVerification(User user) {
        String email = user.getEmail().trim().toLowerCase();
        otpRepository.findAllByEmailIgnoreCaseAndUsedFalse(email).forEach(existing -> {
            existing.setUsed(true);
            existing.setVerificationToken(null);
        });

        String otp = generateNumericOtp();
        String token = UUID.randomUUID().toString();
        sendOtp(email, otp, user.getFullName());

        Instant now = Instant.now();
        OtpVerification verification = new OtpVerification(
                email,
                passwordEncoder.encode(otp),
                now.plus(OTP_VALIDITY_MINUTES, ChronoUnit.MINUTES),
                token);
        verification.setCreatedAt(now);
        otpRepository.save(verification);

        LoginInitiateResponse response = new LoginInitiateResponse("OTP_REQUIRED", token, maskEmail(email));
        response.setEmail(email);
        return response;
    }

    private void sendOtp(String email, String otp, String fullName) {
        if (emailService == null || !emailService.sendOtpEmail(email, otp, fullName)) {
            throw new EmailDispatchException(SMTP_ERROR_MESSAGE);
        }
    }

    private java.util.Optional<OtpVerification> findVerification(String identifier) {
        if (identifier.contains("@")) {
            return otpRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDescIdDesc(identifier);
        }
        return otpRepository.findByVerificationToken(identifier);
    }

    private String generateNumericOtp() {
        return Integer.toString(secureRandom.nextInt(900_000) + 100_000);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        String localPart = email.substring(0, at);
        return localPart.substring(0, 1) + "***" + email.substring(at);
    }
}
