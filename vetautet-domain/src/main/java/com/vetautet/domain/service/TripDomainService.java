package com.vetautet.domain.service;

import com.vetautet.domain.model.Trip;
import com.vetautet.domain.model.TripSummary;
import com.vetautet.domain.model.RouteSummary;
import com.vetautet.domain.model.DestinationSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TripDomainService {
    List<Trip> getAllActiveTrips();
    List<TripSummary> getPopularTripSummaries(int limit);
    List<TripSummary> getUpcomingTripSummaries(int limit);
    List<TripSummary> getScheduleTripSummaries(LocalDate date, String station, int limit);
    List<RouteSummary> getPopularRouteSummaries(int limit);
    List<DestinationSummary> getPopularDestinationSummaries(int limit);
    Trip getTripById(Long id);
    Trip getTripByIdFetched(Long id);
    List<Trip> searchTrips(String departure, String arrival, LocalDate date,
                           List<String> trainTypes, String trainCategory,
                           BigDecimal minPrice, BigDecimal maxPrice);
    Trip createTrip(Trip trip, Long departureStationId, Long arrivalStationId, Long trainId);
    Trip updateTrip(Long id, Trip trip, Long departureStationId, Long arrivalStationId, Long trainId);
    void deleteTrip(Long id);
}
