package com.pdms.service;

import com.pdms.dto.LoginInitiateResponse;
import com.pdms.entity.OtpVerification;
import com.pdms.entity.User;
import com.pdms.exception.BadRequestException;
import com.pdms.exception.TooManyRequestsException;
import com.pdms.exception.UnauthorizedException;
import com.pdms.repository.OtpVerificationRepository;
import com.pdms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpVerificationRepository otpRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OtpService otpService;

    private final Map<String, OtpVerification> savedTokens = new HashMap<>();
    private User sampleUser;

    @BeforeEach
    void setUp() {
        savedTokens.clear();
        sampleUser = new User("Rohitha", "admin@example.com", "password-hash", "Self", "ROLE_ADMIN");
        sampleUser.setId(1L);
        lenient().when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(sampleUser));
        lenient().when(passwordEncoder.matches("admin123", "password-hash")).thenReturn(true);
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(invocation ->
                "hash-" + invocation.getArgument(0));
        lenient().when(emailService.sendOtpEmail(anyString(), anyString(), anyString())).thenReturn(true);
        lenient().when(otpRepository.findAllByEmailIgnoreCaseAndUsedFalse(anyString()))
                .thenReturn(new ArrayList<>());
        lenient().when(otpRepository.save(any(OtpVerification.class))).thenAnswer(invocation -> {
            OtpVerification verification = invocation.getArgument(0);
            if (verification.getVerificationToken() != null) {
                savedTokens.put(verification.getVerificationToken(), verification);
            }
            return verification;
        });
        lenient().when(otpRepository.findByVerificationToken(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(savedTokens.get(invocation.getArgument(0))));
    }

    @Test
    @DisplayName("An existing account receives a secure six-digit code without returning it")
    void existingUserReceivesOtp() {
        LoginInitiateResponse response = otpService.initiateLogin("admin@example.com", "admin123");

        assertEquals("OTP_REQUIRED", response.getStatus());
        assertNotNull(response.getTempToken());
        assertEquals("a***@example.com", response.getDestination());
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("admin@example.com"), otp.capture(), eq("Rohitha"));
        assertTrue(otp.getValue().matches("[1-9]\\d{5}"));
        assertThrows(NoSuchMethodException.class, () -> LoginInitiateResponse.class.getMethod("getOtp"));
    }

    @Test
    @DisplayName("An incorrect password is rejected without changing the saved password")
    void incorrectPasswordDoesNotChangeStoredPassword() {
        when(passwordEncoder.matches("wrong-password", "password-hash")).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> otpService.initiateLogin("admin@example.com", "wrong-password"));

        assertEquals("password-hash", sampleUser.getPassword());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Registration hashes the password and sends an OTP")
    void registrationCreatesAccountAndSendsOtp() {
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        LoginInitiateResponse response = otpService.register("New User", "new@example.com", "safe-password");

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(user.capture());
        assertEquals("hash-safe-password", user.getValue().getPassword());
        assertNotNull(response.getTempToken());
        verify(emailService).sendOtpEmail(eq("new@example.com"), matches("[1-9]\\d{5}"), eq("New User"));
    }

    @Test
    @DisplayName("A valid emailed code authenticates once and then becomes unusable")
    void verifyOtpIsSingleUse() {
        LoginInitiateResponse response = otpService.initiateLogin("admin@example.com", "admin123");
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("admin@example.com"), otp.capture(), anyString());
        when(passwordEncoder.matches(otp.getValue(), "hash-" + otp.getValue())).thenReturn(true);

        assertSame(sampleUser, otpService.verifyOtp(response.getTempToken(), otp.getValue()));
        assertThrows(BadRequestException.class,
                () -> otpService.verifyOtp(response.getTempToken(), otp.getValue()));
    }

    @Test
    @DisplayName("Three incorrect codes invalidate the OTP and return HTTP 429")
    void threeIncorrectCodesInvalidateOtp() {
        LoginInitiateResponse response = otpService.initiateLogin("admin@example.com", "admin123");
        OtpVerification saved = savedTokens.get(response.getTempToken());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("admin@example.com"), code.capture(), anyString());
        String wrongCode = "111111".equals(code.getValue()) ? "111112" : "111111";

        assertThrows(BadRequestException.class, () -> otpService.verifyOtp(response.getTempToken(), wrongCode));
        assertEquals(1, saved.getAttempts());
        assertThrows(BadRequestException.class, () -> otpService.verifyOtp(response.getTempToken(), wrongCode));
        assertEquals(2, saved.getAttempts());
        assertThrows(TooManyRequestsException.class,
                () -> otpService.verifyOtp(response.getTempToken(), wrongCode));
        assertEquals(3, saved.getAttempts());
        assertTrue(saved.isUsed());
    }

    @Test
    @DisplayName("Resending is blocked during cooldown and sends a fresh code after 60 seconds")
    void resendOtpEnforcesCooldown() {
        LoginInitiateResponse initial = otpService.initiateLogin("admin@example.com", "admin123");
        OtpVerification verification = savedTokens.get(initial.getTempToken());

        assertThrows(TooManyRequestsException.class, () -> otpService.resendOtp(initial.getTempToken()));
        verification.setCreatedAt(Instant.now().minusSeconds(61));
        LoginInitiateResponse resent = otpService.resendOtp(initial.getTempToken());

        assertEquals(initial.getTempToken(), resent.getTempToken());
        verify(emailService, times(2)).sendOtpEmail(eq("admin@example.com"), matches("[1-9]\\d{5}"), anyString());
        assertEquals(0, verification.getAttempts());
        assertFalse(verification.isUsed());
    }

    @Test
    @DisplayName("Expired OTPs cannot authenticate")
    void expiredOtpCannotAuthenticate() {
        LoginInitiateResponse response = otpService.initiateLogin("admin@example.com", "admin123");
        OtpVerification verification = savedTokens.get(response.getTempToken());
        verification.setExpiresAt(Instant.now().minusSeconds(1));

        assertThrows(BadRequestException.class,
                () -> otpService.verifyOtp(response.getTempToken(), "123456"));
        assertTrue(verification.isUsed());
    }
}
