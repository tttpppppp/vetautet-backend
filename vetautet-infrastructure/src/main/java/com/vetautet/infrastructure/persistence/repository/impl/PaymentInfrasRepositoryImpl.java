package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.Payment;
import com.vetautet.domain.repository.PaymentRepository;
import com.vetautet.infrastructure.persistence.entity.BookingEntity;
import com.vetautet.infrastructure.persistence.entity.PaymentEntity;
import com.vetautet.infrastructure.persistence.mapper.PersistenceMapper;
import com.vetautet.infrastructure.persistence.repository.PaymentJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PaymentInfrasRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;
    private final PersistenceMapper mapper;
    private final EntityManager entityManager;

    public PaymentInfrasRepositoryImpl(PaymentJpaRepository jpaRepository,
                                       PersistenceMapper mapper,
                                       EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = mapper.toEntity(payment);
        if (payment.getBookingId() != null) {
            entity.setBooking(entityManager.getReference(BookingEntity.class, payment.getBookingId()));
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Payment> findLatestByBookingId(Long bookingId) {
        return jpaRepository.findFirstByBooking_IdOrderByCreatedAtDescIdDesc(bookingId)
                .map(mapper::toDomain);
    }
}
