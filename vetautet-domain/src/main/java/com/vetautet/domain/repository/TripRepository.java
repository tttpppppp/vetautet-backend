package com.vetautet.domain.repository;

import com.vetautet.domain.model.Trip;
import com.vetautet.domain.model.TripSummary;
import com.vetautet.domain.model.RouteSummary;
import com.vetautet.domain.model.DestinationSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TripRepository {
    List<Trip> findAll();
    List<Trip> findAllActive();
    List<TripSummary> findPopularSummaries(int limit);
    List<TripSummary> findUpcomingSummaries(int limit);
    List<TripSummary> findScheduleSummaries(LocalDate date, String station, int limit);
    List<RouteSummary> findPopularRouteSummaries(int limit);
    List<DestinationSummary> findPopularDestinationSummaries(int limit);
    Optional<Trip> findById(Long id);
    Optional<Trip> findByIdFetched(Long id);
    List<Trip> searchTrips(String departure, String arrival, LocalDate date,
                          List<String> trainTypes, String trainCategory,
                          BigDecimal minPrice, BigDecimal maxPrice);
    Trip save(Trip trip);
    void deleteById(Long id);
}
