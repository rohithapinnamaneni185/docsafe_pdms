package com.pdms.dto;

import jakarta.validation.constraints.NotBlank;

public class ResendOtpRequest {

    @NotBlank(message = "Session token is required")
    private String tempToken;

    public ResendOtpRequest() {
    }

    public ResendOtpRequest(String tempToken) {
        this.tempToken = tempToken;
    }

    public String getTempToken() {
        return tempToken;
    }

    public void setTempToken(String tempToken) {
        this.tempToken = tempToken;
    }
}

