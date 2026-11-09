package com.pdms.controller;

import com.pdms.config.SecurityConfig;
import com.pdms.dto.LoginInitiateResponse;
import com.pdms.exception.EmailDispatchException;
import com.pdms.exception.UnauthorizedException;
import com.pdms.service.GoogleOAuth2SuccessHandler;
import com.pdms.service.OtpService;
import com.pdms.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, CsrfController.class})
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private OtpService otpService;

    @MockBean
    private GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void csrfEndpointIsOutsideAuthenticationRoutes() throws Exception {
        mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    if (result.getResponse().getContentAsString().isBlank()) {
                        throw new AssertionError("Expected a CSRF token for non-authentication API requests.");
                    }
                });
    }

    @Test
    void loginInitReturnsPendingSessionWithoutOtp() throws Exception {
        when(otpService.initiateLogin("new@example.com", "password"))
                .thenReturn(new LoginInitiateResponse("OTP_REQUIRED", "pending-token", "n***@example.com"));

        mockMvc.perform(post("/api/auth/login-init")
                        .contentType("application/json")
                        .content("{\"email\":\"new@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Verification code sent to your email"))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.token").value("pending-token"))
                .andExpect(jsonPath("$.data.destination").value("n***@example.com"))
                .andExpect(jsonPath("$.data.debugOtp").doesNotExist());
    }

    @Test
    void loginInitReportsSmtpConfigurationFailures() throws Exception {
        when(otpService.initiateLogin("new@example.com", "password"))
                .thenThrow(new EmailDispatchException(
                        "Failed to dispatch email. Please verify SMTP configuration."));

        mockMvc.perform(post("/api/auth/login-init")
                        .contentType("application/json")
                        .content("{\"email\":\"new@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value(
                        "Failed to dispatch email. Please verify SMTP configuration."));
    }

    @Test
    void loginRequestRejectsInvalidCredentialsWithUnauthorized() throws Exception {
        when(otpService.initiateLogin("new@example.com", "wrong-password"))
                .thenThrow(new UnauthorizedException("Invalid email or password. Please try again."));

        mockMvc.perform(post("/api/auth/login-request")
                        .contentType("application/json")
                        .content("{\"email\":\"new@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "Invalid email or password. Please try again."));
    }

    @Test
    void registrationStartsOtpVerificationWithoutReturningOtp() throws Exception {
        when(otpService.register("New User", "new@example.com", "password123"))
                .thenReturn(new LoginInitiateResponse("OTP_REQUIRED", "pending-token", "n***@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"name\":\"New User\",\"email\":\"new@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Verification OTP sent to your email"))
                .andExpect(jsonPath("$.token").value("pending-token"))
                .andExpect(jsonPath("$.otp").doesNotExist())
                .andExpect(jsonPath("$.data.otp").doesNotExist());
    }
}
