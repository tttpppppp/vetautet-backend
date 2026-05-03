package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.EmailVerificationOtp;
import com.vetautet.domain.repository.EmailVerificationOtpRepository;
import com.vetautet.infrastructure.persistence.mapper.PersistenceMapper;
import com.vetautet.infrastructure.persistence.repository.EmailVerificationOtpJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class EmailVerificationOtpInfrasRepositoryImpl implements EmailVerificationOtpRepository {

    private final EmailVerificationOtpJpaRepository jpaRepository;
    private final PersistenceMapper mapper;

    public EmailVerificationOtpInfrasRepositoryImpl(
            EmailVerificationOtpJpaRepository jpaRepository,
            PersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<EmailVerificationOtp> findLatestUnusedByEmail(String email) {
        return jpaRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .map(mapper::toDomain);
    }

    @Override
    public EmailVerificationOtp save(EmailVerificationOtp otp) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(otp)));
    }

    @Override
    @Transactional
    public void markUnusedOtpsUsedByUserId(Long userId) {
        jpaRepository.markUnusedOtpsUsedByUserId(userId, LocalDateTime.now());
    }
}
