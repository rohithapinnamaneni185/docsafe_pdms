package com.pdms.dto;

import jakarta.validation.constraints.NotBlank;

public class OtpVerifyRequest {

    private String email;

    private String tempToken;

    @NotBlank(message = "OTP code is required")
    private String otp;

    public OtpVerifyRequest() {
    }

    public OtpVerifyRequest(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTempToken() {
        return tempToken;
    }

    public void setTempToken(String tempToken) {
        this.tempToken = tempToken;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}

