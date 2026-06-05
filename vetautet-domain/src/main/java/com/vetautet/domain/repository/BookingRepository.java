package com.vetautet.domain.repository;

import com.vetautet.domain.model.Booking;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    Booking save(Booking booking);
    Optional<Booking> findById(Long id);
    Optional<Booking> findByOrderNumber(String orderNumber);
    Optional<Booking> findByAsyncRequestId(String asyncRequestId);
    Optional<Booking> findByIdFetched(Long id);
    java.util.List<Booking> findByUserId(Long userId);
    java.util.List<Booking> findExpiredPendingBookings(LocalDateTime now);
    java.util.List<Booking> findAll();
    void deleteById(Long id);
}
