package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findFirstByBooking_IdOrderByCreatedAtDescIdDesc(Long bookingId);
}
