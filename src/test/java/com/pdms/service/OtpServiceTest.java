package com.pdms.service;

import com.pdms.dto.LoginInitiateRequest;
import com.pdms.dto.LoginInitiateResponse;
import com.pdms.entity.User;
import com.pdms.exception.BadRequestException;
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

    @InjectMocks
    private OtpService otpService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Rohitha (Self)", "admin@docusafe.local", "admin123", "Self", "ROLE_ADMIN");
        sampleUser.setId(1L);
        sampleUser.setMobileNumber("9876543210");
    }

    @Test
    @DisplayName("initiateLogin generates 6-digit OTP and returns tempToken for valid email")
    void testInitiateLoginByEmail() {
        when(userRepository.findByIdentifier("admin@docusafe.local")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        LoginInitiateRequest request = new LoginInitiateRequest("admin@docusafe.local", "admin123");
        LoginInitiateResponse response = otpService.initiateLogin(request);

        assertNotNull(response);
        assertEquals("OTP_REQUIRED", response.getStatus());
        assertNotNull(response.getTempToken());
        assertNotNull(response.getDebugOtp());
        assertEquals(6, response.getDebugOtp().length());
        assertTrue(response.getDestination().contains("docusafe.local"));
    }

    @Test
    @DisplayName("initiateLogin accepts 10-digit mobile number")
    void testInitiateLoginByMobile() {
        when(userRepository.findByIdentifier("9876543210")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        LoginInitiateRequest request = new LoginInitiateRequest("9876543210", "admin123");
        LoginInitiateResponse response = otpService.initiateLogin(request);

        assertNotNull(response);
        assertEquals("OTP_REQUIRED", response.getStatus());
        assertNotNull(response.getTempToken());
        assertEquals(6, response.getDebugOtp().length());
    }

    @Test
    @DisplayName("initiateLogin throws BadRequestException on invalid password")
    void testInitiateLoginWrongPassword() {
        when(userRepository.findByIdentifier("admin@docusafe.local")).thenReturn(Optional.of(sampleUser));

        LoginInitiateRequest request = new LoginInitiateRequest("admin@docusafe.local", "wrongpassword");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> otpService.initiateLogin(request));
        assertTrue(ex.getMessage().contains("Invalid credentials"));
    }

    @Test
    @DisplayName("verifyOtp returns authenticated user on valid code")
    void testVerifyOtpSuccess() {
        when(userRepository.findByIdentifier("admin@docusafe.local")).thenReturn(Optional.of(sampleUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        LoginInitiateRequest request = new LoginInitiateRequest("admin@docusafe.local", "admin123");
        LoginInitiateResponse initResp = otpService.initiateLogin(request);

        User verifiedUser = otpService.verifyOtp(initResp.getTempToken(), initResp.getDebugOtp());
        assertNotNull(verifiedUser);
        assertEquals("Rohitha (Self)", verifiedUser.getFullName());
    }

    @Test
    @DisplayName("verifyOtp throws on invalid OTP code")
    void testVerifyOtpInvalidCode() {
        when(userRepository.findByIdentifier("admin@docusafe.local")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        LoginInitiateRequest request = new LoginInitiateRequest("admin@docusafe.local", "admin123");
        LoginInitiateResponse initResp = otpService.initiateLogin(request);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                otpService.verifyOtp(initResp.getTempToken(), "000000"));
        assertTrue(ex.getMessage().contains("Invalid 6-digit OTP"));
    }

    @Test
    @DisplayName("resendOtp generates a new OTP")
    void testResendOtp() {
        when(userRepository.findByIdentifier("admin@docusafe.local")).thenReturn(Optional.of(sampleUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        LoginInitiateRequest request = new LoginInitiateRequest("admin@docusafe.local", "admin123");
        LoginInitiateResponse initResp = otpService.initiateLogin(request);

        LoginInitiateResponse resendResp = otpService.resendOtp(initResp.getTempToken());
        assertNotNull(resendResp);
        assertEquals("OTP_REQUIRED", resendResp.getStatus());
        assertEquals(initResp.getTempToken(), resendResp.getTempToken());
        assertEquals(6, resendResp.getDebugOtp().length());
    }
}

