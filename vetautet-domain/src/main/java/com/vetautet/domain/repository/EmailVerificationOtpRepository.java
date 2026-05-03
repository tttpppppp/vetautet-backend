package com.vetautet.domain.repository;

import com.vetautet.domain.model.EmailVerificationOtp;

import java.util.Optional;

public interface EmailVerificationOtpRepository {
    Optional<EmailVerificationOtp> findLatestUnusedByEmail(String email);
    EmailVerificationOtp save(EmailVerificationOtp otp);
    void markUnusedOtpsUsedByUserId(Long userId);
}
