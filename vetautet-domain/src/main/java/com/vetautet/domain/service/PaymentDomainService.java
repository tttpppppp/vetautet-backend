package com.vetautet.domain.service;

import com.vetautet.domain.model.Payment;

public interface PaymentDomainService {
    Payment savePayment(Payment payment);
    Payment findLatestByBookingIdOrNull(Long bookingId);
}
