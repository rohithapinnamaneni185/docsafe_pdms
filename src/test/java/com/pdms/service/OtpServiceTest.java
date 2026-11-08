package com.pdms.service;

import com.pdms.dto.LoginOtpResponse;
import com.pdms.dto.LoginRequest;
import com.pdms.entity.OtpVerification;
import com.pdms.entity.User;
import com.pdms.exception.BadRequestException;
import com.pdms.exception.UnauthorizedException;
import com.pdms.repository.OtpVerificationRepository;
import com.pdms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OtpService otpService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Rohitha (Self)", "admin@docusafe.local", "admin123", "Self", "ROLE_ADMIN");
        sampleUser.setId(1L);
    }

    @Test
    @DisplayName("loginRequest generates 6-digit OTP and returns response for valid email + password")
    void testLoginRequestSuccess() {
        when(userRepository.findByEmailIgnoreCase("admin@docusafe.local")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        if (otpVerificationRepository != null) {
            when(otpVerificationRepository.save(any(OtpVerification.class))).thenReturn(new OtpVerification());
        }

        LoginRequest request = new LoginRequest("admin@docusafe.local", "admin123");
        LoginOtpResponse response = otpService.loginRequest(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("admin@docusafe.local", response.getEmail());
        assertNotNull(response.getDemoOtp());
        assertEquals(6, response.getDemoOtp().length());
        assertNotNull(response.getTempToken());
    }

    @Test
    @DisplayName("loginRequest throws 401 UnauthorizedException on incorrect password")
    void testLoginRequestWrongPassword() {
        when(userRepository.findByEmailIgnoreCase("admin@docusafe.local")).thenReturn(Optional.of(sampleUser));

        LoginRequest request = new LoginRequest("admin@docusafe.local", "wrongpassword");

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> otpService.loginRequest(request));
        assertEquals("Invalid email or password", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("loginRequest throws 401 UnauthorizedException when email is not found")
    void testLoginRequestEmailNotFound() {
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("unknown@example.com", "admin123");

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> otpService.loginRequest(request));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    @DisplayName("verifyOtpByEmail returns authenticated user on valid code")
    void testVerifyOtpByEmailSuccess() {
        when(userRepository.findByEmailIgnoreCase("admin@docusafe.local")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        LoginRequest request = new LoginRequest("admin@docusafe.local", "admin123");
        LoginOtpResponse initResp = otpService.loginRequest(request);

        User verifiedUser = otpService.verifyOtpByEmail("admin@docusafe.local", initResp.getDemoOtp());
        assertNotNull(verifiedUser);
        assertEquals("Rohitha (Self)", verifiedUser.getFullName());
    }

    @Test
    @DisplayName("verifyOtpByEmail throws BadRequestException on invalid code")
    void testVerifyOtpByEmailInvalidCode() {
        when(userRepository.findByEmailIgnoreCase("admin@docusafe.local")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        LoginRequest request = new LoginRequest("admin@docusafe.local", "admin123");
        otpService.loginRequest(request);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                otpService.verifyOtpByEmail("admin@docusafe.local", "000000"));
        assertTrue(ex.getMessage().contains("Invalid 6-digit OTP"));
    }

    @Test
    @DisplayName("resendOtpByEmail dispatches a fresh OTP")
    void testResendOtpByEmail() {
        when(userRepository.findByEmailIgnoreCase("admin@docusafe.local")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        LoginOtpResponse resendResp = otpService.resendOtpByEmail("admin@docusafe.local");
        assertNotNull(resendResp);
        assertTrue(resendResp.isSuccess());
        assertEquals("admin@docusafe.local", resendResp.getEmail());
        assertEquals(6, resendResp.getDemoOtp().length());
    }
}
