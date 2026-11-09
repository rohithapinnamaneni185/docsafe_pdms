package com.pdms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private boolean success;
    private String message;
    private String token;
    private String email;
    private String redirectUrl;
    private Object user;
    private Object data;

    public AuthResponse() {
        this.success = true;
    }

    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AuthResponse(boolean success, String message, String token) {
        this.success = success;
        this.message = message;
        this.token = token;
    }

    public AuthResponse(boolean success, String message, String token, Object user) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.user = user;
        this.data = user;
    }

    public static AuthResponse ok(String message) {
        return new AuthResponse(true, message);
    }

    public static AuthResponse ok(String message, String token) {
        return new AuthResponse(true, message, token);
    }

    public static AuthResponse ok(String message, String token, Object user) {
        return new AuthResponse(true, message, token, user);
    }

    public static AuthResponse error(String message) {
        return new AuthResponse(false, message);
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public Object getUser() {
        return user;
    }

    public void setUser(Object user) {
        this.user = user;
        if (this.data == null) {
            this.data = user;
        }
    }

    public Object getData() {
        return data != null ? data : user;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
