package com.vetautet.domain.gateway;

public interface EmailVerificationMailGateway {
    void sendVerificationOtp(String recipientEmail, String recipientName, String otp, int expiresInMinutes);
    void sendPasswordResetOtp(String recipientEmail, String recipientName, String otp, int expiresInMinutes);
}
