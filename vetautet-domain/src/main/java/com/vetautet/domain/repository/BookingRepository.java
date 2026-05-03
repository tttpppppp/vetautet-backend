package com.vetautet.domain.repository;

import com.vetautet.domain.model.Booking;
import java.util.Optional;

public interface BookingRepository {
    Booking save(Booking booking);
    Optional<Booking> findById(Long id);
    Optional<Booking> findByIdFetched(Long id);
    java.util.List<Booking> findByUserId(Long userId);
    java.util.List<Booking> findAll();
    void deleteById(Long id);
}
