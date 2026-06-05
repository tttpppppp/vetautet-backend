package com.vetautet.infrastructure.persistence.order;

import com.vetautet.infrastructure.persistence.repository.BookingJpaRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BookingMonthlyMirrorBootstrap {

    private final BookingJpaRepository bookingJpaRepository;
    private final BookingMonthlyMirrorRepository monthlyMirrorRepository;

    public BookingMonthlyMirrorBootstrap(BookingJpaRepository bookingJpaRepository,
                                         BookingMonthlyMirrorRepository monthlyMirrorRepository) {
        this.bookingJpaRepository = bookingJpaRepository;
        this.monthlyMirrorRepository = monthlyMirrorRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfillExistingBookings() {
        bookingJpaRepository.findAll().forEach(monthlyMirrorRepository::upsert);
    }
}
