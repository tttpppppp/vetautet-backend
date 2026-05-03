package com.vetautet.domain.repository;

import com.vetautet.domain.model.Payment;

import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findLatestByBookingId(Long bookingId);
}
