package com.pdms.service;

import com.pdms.dto.CreateMemberRequest;
import com.pdms.dto.AddFamilyMemberRequest;
import com.pdms.dto.LoginInitiateResponse;
import com.pdms.dto.UserResponse;
import com.pdms.dto.VerifyFamilyMemberOtpRequest;
import com.pdms.entity.OtpVerification;
import com.pdms.entity.User;
import com.pdms.exception.BadRequestException;
import com.pdms.exception.TooManyRequestsException;
import com.pdms.repository.OtpVerificationRepository;
import com.pdms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamilyMemberOtpServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpVerificationRepository otpRepository;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private FamilyMemberOtpService familyMemberOtpService;
    private final Map<String, OtpVerification> savedTokens = new HashMap<>();

    @Test
    void familyAccountIsCreatedOnlyAfterOtpSentToRequestedAddressIsVerified() {
        User administrator = new User("Admin", "admin@example.com", "password", "Self", "ROLE_ADMIN");
        administrator.setId(1L);
        CreateMemberRequest request = new CreateMemberRequest();
        request.setFullName("Family Member");
        request.setEmail("member@example.com");
        request.setPassword("password");
        request.setRelationship("Child");
        when(userRepository.countNonSelfMembers()).thenReturn(0L);
        when(userRepository.existsByEmailIgnoreCase("member@example.com")).thenReturn(false);
        when(emailService.sendOtpEmail(anyString(), anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> {
            String value = invocation.getArgument(0);
            return "encoded-" + value;
        });
        when(passwordEncoder.matches(anyString(), anyString())).thenAnswer(invocation ->
                invocation.getArgument(1).equals("encoded-" + invocation.getArgument(0)));
        when(otpRepository.save(any(OtpVerification.class))).thenAnswer(invocation -> {
            OtpVerification verification = invocation.getArgument(0);
            savedTokens.put(verification.getVerificationToken(), verification);
            return verification;
        });
        when(otpRepository.findByVerificationToken(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(savedTokens.get(invocation.getArgument(0))));
        when(userService.createFamilyMemberWithEncodedPassword(
                anyString(), anyString(), anyString(), anyString(), eq(administrator)))
                .thenReturn(new UserResponse());

        LoginInitiateResponse response = familyMemberOtpService.initiate(request, administrator);

        assertEquals("member@example.com", response.getEmail());
        assertNotNull(response.getTempToken());
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("member@example.com"), otp.capture(), eq("Family Member"));
        assertTrue(otp.getValue().matches("[1-9]\\d{5}"));
        verifyNoInteractions(userService);

        UserResponse created = familyMemberOtpService.verify(response.getTempToken(), otp.getValue(), administrator);

        assertNotNull(created);
        verify(userService).createFamilyMemberWithEncodedPassword(
                "Family Member", "member@example.com", "encoded-password", "Child", administrator);
    }

    @Test
    void requestedFamilyEndpointsNormalizeEmailAndCreateAfterVerification() {
        User administrator = new User("Admin", "admin@example.com", "password", "Self", "ROLE_ADMIN");
        administrator.setId(1L);
        AddFamilyMemberRequest addRequest =
                new AddFamilyMemberRequest("Family Member", "Child", " Member@Example.com ", "password");
        when(userRepository.countNonSelfMembers()).thenReturn(0L);
        when(userRepository.existsByEmailIgnoreCase("member@example.com")).thenReturn(false);
        when(emailService.sendOtpEmail(anyString(), anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation ->
                "encoded-" + invocation.getArgument(0));
        when(passwordEncoder.matches(anyString(), anyString())).thenAnswer(invocation ->
                invocation.getArgument(1).equals("encoded-" + invocation.getArgument(0)));
        when(otpRepository.save(any(OtpVerification.class))).thenAnswer(invocation -> {
            OtpVerification verification = invocation.getArgument(0);
            savedTokens.put(verification.getVerificationToken(), verification);
            return verification;
        });
        when(otpRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDescIdDesc("member@example.com"))
                .thenAnswer(invocation -> savedTokens.values().stream().findFirst());
        when(otpRepository.findByVerificationToken(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(savedTokens.get(invocation.getArgument(0))));
        when(userService.createFamilyMemberWithEncodedPassword(
                anyString(), anyString(), anyString(), anyString(), eq(administrator)))
                .thenReturn(new UserResponse());

        familyMemberOtpService.initiate(addRequest, administrator);
        OtpVerification verification = savedTokens.values().iterator().next();
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("member@example.com"), otp.capture(), eq("Family Member"));

        UserResponse created = familyMemberOtpService.verifyAndCreate(
                new VerifyFamilyMemberOtpRequest(
                        "MEMBER@example.com", otp.getValue(), "Family Member", "Child", "password"),
                administrator);

        assertNotNull(created);
        verify(userService).createFamilyMemberWithEncodedPassword(
                "Family Member", "member@example.com", "encoded-password", "Child", administrator);
        assertTrue(verification.isUsed());
    }

    @Test
    void familyVerificationLocksAfterThreeIncorrectCodes() {
        User administrator = new User("Admin", "admin@example.com", "password", "Self", "ROLE_ADMIN");
        administrator.setId(1L);
        CreateMemberRequest request = new CreateMemberRequest();
        request.setFullName("Family Member");
        request.setEmail("member@example.com");
        request.setPassword("password");
        request.setRelationship("Child");
        when(userRepository.countNonSelfMembers()).thenReturn(0L);
        when(userRepository.existsByEmailIgnoreCase("member@example.com")).thenReturn(false);
        when(emailService.sendOtpEmail(anyString(), anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation ->
                "encoded-" + invocation.getArgument(0));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(otpRepository.save(any(OtpVerification.class))).thenAnswer(invocation -> {
            OtpVerification verification = invocation.getArgument(0);
            savedTokens.put(verification.getVerificationToken(), verification);
            return verification;
        });
        when(otpRepository.findByVerificationToken(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(savedTokens.get(invocation.getArgument(0))));

        LoginInitiateResponse response = familyMemberOtpService.initiate(request, administrator);

        assertThrows(BadRequestException.class,
                () -> familyMemberOtpService.verify(response.getTempToken(), "000000", administrator));
        assertThrows(BadRequestException.class,
                () -> familyMemberOtpService.verify(response.getTempToken(), "000000", administrator));
        assertThrows(TooManyRequestsException.class,
                () -> familyMemberOtpService.verify(response.getTempToken(), "000000", administrator));
        assertThrows(TooManyRequestsException.class,
                () -> familyMemberOtpService.verify(response.getTempToken(), "000000", administrator));
    }
}
