package com.pdms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record VerifyFamilyMemberOtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,
        @NotBlank(message = "OTP code is required")
        @Pattern(regexp = "\\d{6}", message = "OTP must be a 6-digit code")
        String otp,
        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name cannot exceed 150 characters")
        String name,
        @NotBlank(message = "Relationship is required")
        @Size(max = 50, message = "Relationship cannot exceed 50 characters")
        String relationship,
        @NotBlank(message = "Password is required")
        @Size(min = 4, max = 100, message = "Password must be at least 4 characters")
        String password) {

    public VerifyFamilyMemberOtpRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
