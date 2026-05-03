package com.vetautet.domain.service.impl;

import com.vetautet.domain.exception.BusinessException;
import com.vetautet.domain.model.EmailVerificationOtp;
import com.vetautet.domain.repository.EmailVerificationOtpRepository;
import com.vetautet.domain.service.EmailVerificationOtpDomainService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EmailVerificationOtpDomainServiceImpl implements EmailVerificationOtpDomainService {

    private final EmailVerificationOtpRepository repository;

    public EmailVerificationOtpDomainServiceImpl(EmailVerificationOtpRepository repository) {
        this.repository = repository;
    }

    @Override
    public EmailVerificationOtp createOtp(Long userId, String email, String otp, LocalDateTime expiresAt) {
        repository.markUnusedOtpsUsedByUserId(userId);

        EmailVerificationOtp verificationOtp = EmailVerificationOtp.builder()
                .userId(userId)
                .email(normalizeEmail(email))
                .otp(otp)
                .expiresAt(expiresAt)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(verificationOtp);
    }

    @Override
    public EmailVerificationOtp verifyOtp(String email, String otp) {
        EmailVerificationOtp verificationOtp = repository.findLatestUnusedByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BusinessException("INVALID_OR_EXPIRED_OTP"));

        if (Boolean.TRUE.equals(verificationOtp.getUsed())) {
            throw new BusinessException("OTP_ALREADY_USED");
        }

        if (verificationOtp.getExpiresAt() == null || verificationOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            verificationOtp.setUsed(true);
            verificationOtp.setUsedAt(LocalDateTime.now());
            repository.save(verificationOtp);
            throw new BusinessException("EXPIRED_OTP");
        }

        if (otp == null || !verificationOtp.getOtp().equals(otp.trim())) {
            throw new BusinessException("INVALID_OTP");
        }

        verificationOtp.setUsed(true);
        verificationOtp.setUsedAt(LocalDateTime.now());
        return repository.save(verificationOtp);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
