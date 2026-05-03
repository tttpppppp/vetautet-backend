package com.vetautet.application.dto;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String email;
    private Boolean isEmailVerified;
    private Boolean requiresEmailVerification;
    private Boolean emailAlreadyRegistered;
    private String code;
    private String message;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String refreshToken, String email) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.email = email;
    }

    public AuthResponse(String accessToken, String refreshToken, String email, Boolean isEmailVerified, Boolean requiresEmailVerification) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.email = email;
        this.isEmailVerified = isEmailVerified;
        this.requiresEmailVerification = requiresEmailVerification;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Boolean getIsEmailVerified() { return isEmailVerified; }
    public void setIsEmailVerified(Boolean isEmailVerified) { this.isEmailVerified = isEmailVerified; }
    public Boolean getRequiresEmailVerification() { return requiresEmailVerification; }
    public void setRequiresEmailVerification(Boolean requiresEmailVerification) { this.requiresEmailVerification = requiresEmailVerification; }
    public Boolean getEmailAlreadyRegistered() { return emailAlreadyRegistered; }
    public void setEmailAlreadyRegistered(Boolean emailAlreadyRegistered) { this.emailAlreadyRegistered = emailAlreadyRegistered; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
