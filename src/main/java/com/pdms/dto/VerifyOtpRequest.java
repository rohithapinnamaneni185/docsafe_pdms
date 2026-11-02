package com.pdms.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyOtpRequest {

    @NotBlank(message = "Session token is required")
    private String tempToken;

    @NotBlank(message = "OTP code is required")
    private String otp;

    public VerifyOtpRequest() {
    }

    public VerifyOtpRequest(String tempToken, String otp) {
        this.tempToken = tempToken;
        this.otp = otp;
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

