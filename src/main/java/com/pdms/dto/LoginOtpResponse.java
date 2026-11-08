package com.pdms.dto;

public class LoginOtpResponse {

    private boolean success = true;
    private boolean newUser = false;
    private String message;
    private String demoOtp;
    private String email;
    private String tempToken;

    public LoginOtpResponse() {
    }

    public LoginOtpResponse(boolean success, String message, String demoOtp, String email, String tempToken) {
        this.success = success;
        this.message = message;
        this.demoOtp = demoOtp;
        this.email = email;
        this.tempToken = tempToken;
    }

    public LoginOtpResponse(boolean success, boolean newUser, String message, String demoOtp, String email, String tempToken) {
        this.success = success;
        this.newUser = newUser;
        this.message = message;
        this.demoOtp = demoOtp;
        this.email = email;
        this.tempToken = tempToken;
    }

    public static LoginOtpResponse success(String message, String demoOtp, String email, String tempToken) {
        return new LoginOtpResponse(true, message, demoOtp, email, tempToken);
    }

    public static LoginOtpResponse newUser(String message, String email) {
        LoginOtpResponse resp = new LoginOtpResponse(false, message, null, email, null);
        resp.setNewUser(true);
        return resp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isNewUser() {
        return newUser;
    }

    public void setNewUser(boolean newUser) {
        this.newUser = newUser;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDemoOtp() {
        return demoOtp;
    }

    public void setDemoOtp(String demoOtp) {
        this.demoOtp = demoOtp;
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
}
