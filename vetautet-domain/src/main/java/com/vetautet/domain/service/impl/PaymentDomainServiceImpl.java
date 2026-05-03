package com.vetautet.domain.service.impl;

import com.vetautet.domain.model.Payment;
import com.vetautet.domain.repository.PaymentRepository;
import com.vetautet.domain.service.PaymentDomainService;
import org.springframework.stereotype.Service;

@Service
public class PaymentDomainServiceImpl implements PaymentDomainService {

    private final PaymentRepository paymentRepository;

    public PaymentDomainServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Payment savePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Override
    public Payment findLatestByBookingIdOrNull(Long bookingId) {
        return paymentRepository.findLatestByBookingId(bookingId).orElse(null);
    }
}
