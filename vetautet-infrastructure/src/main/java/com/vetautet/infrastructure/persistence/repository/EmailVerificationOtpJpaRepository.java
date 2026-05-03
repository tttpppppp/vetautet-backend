package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.EmailVerificationOtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationOtpJpaRepository extends JpaRepository<EmailVerificationOtpEntity, Long> {
    Optional<EmailVerificationOtpEntity> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("UPDATE EmailVerificationOtpEntity e SET e.used = true, e.usedAt = :usedAt WHERE e.userId = :userId AND e.used = false")
    void markUnusedOtpsUsedByUserId(@Param("userId") Long userId, @Param("usedAt") LocalDateTime usedAt);
}
