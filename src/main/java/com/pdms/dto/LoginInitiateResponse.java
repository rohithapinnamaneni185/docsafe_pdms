package com.pdms.dto;

public class LoginInitiateResponse {

    private String status;
    private String tempToken;
    private String destination;
    private String debugOtp;

    public LoginInitiateResponse() {
    }

    public LoginInitiateResponse(String status, String tempToken, String destination, String debugOtp) {
        this.status = status;
        this.tempToken = tempToken;
        this.destination = destination;
        this.debugOtp = debugOtp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTempToken() {
        return tempToken;
    }

    public void setTempToken(String tempToken) {
        this.tempToken = tempToken;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDebugOtp() {
        return debugOtp;
    }

    public void setDebugOtp(String debugOtp) {
        this.debugOtp = debugOtp;
    }
}

