package com.pdms.service;

import com.pdms.dto.CreateMemberRequest;
import com.pdms.dto.AddFamilyMemberRequest;
import com.pdms.dto.LoginInitiateResponse;
import com.pdms.dto.UserResponse;
import com.pdms.dto.VerifyFamilyMemberOtpRequest;
import com.pdms.entity.OtpVerification;
import com.pdms.entity.User;
import com.pdms.exception.BadRequestException;
import com.pdms.exception.EmailDispatchException;
import com.pdms.exception.ForbiddenException;
import com.pdms.exception.TooManyRequestsException;
import com.pdms.repository.OtpVerificationRepository;
import com.pdms.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FamilyMemberOtpService {

    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, PendingMember> pendingMembers = new ConcurrentHashMap<>();

    public FamilyMemberOtpService(
            UserRepository userRepository,
            OtpVerificationRepository otpRepository,
            UserService userService,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.userService = userService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginInitiateResponse initiate(CreateMemberRequest request, User administrator) {
        if (!administrator.isAdmin()) {
            throw new ForbiddenException("Only the primary administrator can add family members.");
        }
        if (userRepository.countNonSelfMembers() >= 3) {
            throw new ForbiddenException("Family member limit reached. Please upgrade to DocuSafe Premium to add more members.");
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("A family member with this email already exists: " + email);
        }

        String otp = generateNumericOtp();
        String encodedPassword = passwordEncoder.encode(request.getPassword().trim());
        String token = UUID.randomUUID().toString();
        if (!emailService.sendOtpEmail(email, otp, request.getFullName())) {
            throw new EmailDispatchException(
                    "Failed to dispatch email. Please verify SMTP configuration.");
        }
        Instant now = Instant.now();
        OtpVerification verification = new OtpVerification(email.toLowerCase(),
                passwordEncoder.encode(otp),
                now.plus(OTP_VALIDITY_MINUTES, ChronoUnit.MINUTES), token);
        verification.setCreatedAt(now);
        otpRepository.save(verification);

        pendingMembers.put(token, new PendingMember(
                administrator.getId(),
                request.getFullName().trim(),
                email,
                encodedPassword,
                request.getRelationship().trim()));
        LoginInitiateResponse response = new LoginInitiateResponse("OTP_REQUIRED", token, maskEmail(email));
        response.setEmail(email);
        return response;
    }

    @Transactional
    public LoginInitiateResponse initiate(AddFamilyMemberRequest request, User administrator) {
        CreateMemberRequest legacyRequest = new CreateMemberRequest();
        legacyRequest.setFullName(request.name());
        legacyRequest.setRelationship(request.relationship());
        legacyRequest.setEmail(request.email());
        legacyRequest.setPassword(request.password());
        return initiate(legacyRequest, administrator);
    }

    @Transactional(noRollbackFor = {BadRequestException.class, TooManyRequestsException.class})
    public UserResponse verifyAndCreate(VerifyFamilyMemberOtpRequest request, User administrator) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        OtpVerification verification = otpRepository
                .findTopByEmailIgnoreCaseOrderByCreatedAtDescIdDesc(email)
                .orElseThrow(() -> new BadRequestException(
                        "Verification session has expired. Please request a new code."));
        String token = verification.getVerificationToken();
        PendingMember pending = token == null ? null : pendingMembers.get(token);
        if (pending == null
                || !pending.administratorId.equals(administrator.getId())
                || !pending.email.equals(email)
                || !pending.fullName.equals(request.name().trim())
                || !pending.relationship.equals(request.relationship().trim())
                || !passwordEncoder.matches(request.password(), pending.encodedPassword)) {
            throw new BadRequestException(
                    "Family member details do not match the verification request. Please start again.");
        }
        return verify(token, request.otp(), administrator);
    }

    @Transactional(noRollbackFor = {BadRequestException.class, TooManyRequestsException.class})
    public UserResponse verify(String token, String otp, User administrator) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Verification session is missing. Please try adding the family member again.");
        }
        OtpVerification verification = otpRepository.findByVerificationToken(token.trim())
                .orElseThrow(() -> new BadRequestException(
                        "Verification session has expired. Please request a new code."));
        if (verification.getAttempts() >= MAX_OTP_ATTEMPTS) {
            throw new TooManyRequestsException("Too many failed attempts. Request a new OTP.");
        }
        PendingMember pending = pendingMembers.get(token.trim());
        if (pending == null || !pending.administratorId.equals(administrator.getId())) {
            throw new BadRequestException("Verification session has expired. Please request a new code.");
        }
        if (verification.isUsed()) {
            throw new BadRequestException("Verification session has expired. Please request a new code.");
        }
        if (!Instant.now().isBefore(verification.getExpiresAt())) {
            verification.setUsed(true);
            otpRepository.save(verification);
            pendingMembers.remove(token.trim(), pending);
            throw new BadRequestException("Verification code has expired. Please request a new one.");
        }
        if (otp == null || !otp.trim().matches("\\d{6}")
                || !passwordEncoder.matches(otp.trim(), verification.getOtpHash())) {
            int attempts = verification.getAttempts() + 1;
            verification.setAttempts(attempts);
            if (attempts >= MAX_OTP_ATTEMPTS) {
                verification.setUsed(true);
                otpRepository.save(verification);
                pendingMembers.remove(token.trim(), pending);
                throw new TooManyRequestsException("Too many failed attempts. Request a new OTP.");
            }
            otpRepository.save(verification);
            throw new BadRequestException("Invalid verification code. "
                    + (MAX_OTP_ATTEMPTS - attempts) + " attempt(s) remaining.");
        }

        UserResponse created = userService.createFamilyMemberWithEncodedPassword(
                pending.fullName, pending.email, pending.encodedPassword, pending.relationship, administrator);
        verification.setUsed(true);
        verification.setVerificationToken(null);
        otpRepository.save(verification);
        pendingMembers.remove(token.trim(), pending);
        return created;
    }

    private String generateNumericOtp() {
        return Integer.toString(secureRandom.nextInt(900_000) + 100_000);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        String localPart = email.substring(0, at);
        return localPart.substring(0, 1) + "***@" + email.substring(at + 1);
    }

    private static final class PendingMember {
        private final Long administratorId;
        private final String fullName;
        private final String email;
        private final String encodedPassword;
        private final String relationship;

        private PendingMember(
                Long administratorId,
                String fullName,
                String email,
                String encodedPassword,
                String relationship) {
            this.administratorId = administratorId;
            this.fullName = fullName;
            this.email = email;
            this.encodedPassword = encodedPassword;
            this.relationship = relationship;
        }
    }
}
