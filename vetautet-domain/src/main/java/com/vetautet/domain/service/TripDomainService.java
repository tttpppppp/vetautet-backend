package com.vetautet.domain.service;

import com.vetautet.domain.model.Trip;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TripDomainService {
    List<Trip> getAllActiveTrips();
    Trip getTripById(Long id);
    Trip getTripByIdFetched(Long id);
    List<Trip> searchTrips(String departure, String arrival, LocalDate date,
                           List<String> trainTypes, String trainCategory,
                           BigDecimal minPrice, BigDecimal maxPrice);
    Trip createTrip(Trip trip, Long departureStationId, Long arrivalStationId, Long trainId);
    Trip updateTrip(Long id, Trip trip, Long departureStationId, Long arrivalStationId, Long trainId);
    void deleteTrip(Long id);
}
