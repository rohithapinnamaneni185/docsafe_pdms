package com.pdms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @Size(max = 150, message = "Full name cannot exceed 150 characters")
    private String name;

    @Size(max = 150, message = "Full name cannot exceed 150 characters")
    private String fullName;

    @Email(message = "Please enter a valid email address")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getResolvedName() {
        return fullName != null && !fullName.isBlank() ? fullName : name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
