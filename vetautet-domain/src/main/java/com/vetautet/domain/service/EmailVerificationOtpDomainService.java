package com.vetautet.domain.service;

import com.vetautet.domain.model.EmailVerificationOtp;

import java.time.LocalDateTime;

public interface EmailVerificationOtpDomainService {
    EmailVerificationOtp createOtp(Long userId, String email, String otp, LocalDateTime expiresAt);
    EmailVerificationOtp verifyOtp(String email, String otp);
}
