package com.pdms.dto;

public class VerifyOtpResponse {

    private boolean success = true;
    private String message;
    private String redirectUrl;
    private UserResponse data;

    public VerifyOtpResponse() {
    }

    public VerifyOtpResponse(boolean success, String message, String redirectUrl, UserResponse data) {
        this.success = success;
        this.message = message;
        this.redirectUrl = redirectUrl;
        this.data = data;
    }

    public static VerifyOtpResponse success(String message, String redirectUrl, UserResponse data) {
        return new VerifyOtpResponse(true, message, redirectUrl, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public UserResponse getData() {
        return data;
    }

    public void setData(UserResponse data) {
        this.data = data;
    }
}

