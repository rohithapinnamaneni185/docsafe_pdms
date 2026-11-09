package com.pdms.dto;

public class LoginInitiateResponse {

    private String status;
    private String tempToken;
    private String destination;
    private String email;

    public LoginInitiateResponse() {
    }

    public LoginInitiateResponse(String status, String tempToken, String destination) {
        this.status = status;
        this.tempToken = tempToken;
        this.destination = destination;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
