package com.vetautet.domain.repository;

import com.vetautet.domain.model.Trip;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TripRepository {
    List<Trip> findAll();
    List<Trip> findAllActive();
    Optional<Trip> findById(Long id);
    Optional<Trip> findByIdFetched(Long id);
    List<Trip> searchTrips(String departure, String arrival, LocalDate date,
                          List<String> trainTypes, String trainCategory,
                          BigDecimal minPrice, BigDecimal maxPrice);
    Trip save(Trip trip);
    void deleteById(Long id);
}
