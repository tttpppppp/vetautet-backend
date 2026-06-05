package com.vetautet.application.service.trip;

import com.vetautet.application.dto.TripCreateRequest;
import com.vetautet.application.dto.TripResponse;
import com.vetautet.application.dto.TrainCategoryResponse;
import com.vetautet.application.dto.PopularDestinationResponse;
import com.vetautet.application.dto.PopularRouteResponse;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;

public interface TripAppService {
    List<TripResponse> getAllTrips();
    List<TripResponse> getAllTrips(String promoCode);
    List<TrainCategoryResponse> getTrainCategories();
    List<TripResponse> getPopularTrips(int limit);
    List<PopularRouteResponse> getPopularRoutes(int limit);
    List<PopularDestinationResponse> getPopularDestinations(int limit);
    List<TripResponse> getUpcomingDepartures(int limit);
    List<TripResponse> getSchedules(LocalDate date, String station, int limit);
    TripResponse getTripById(Long id);
    TripResponse getTripById(Long id, Long bookingId);
    TripResponse getTripById(Long id, Long bookingId, Long departureStationId, Long arrivalStationId);
    List<TripResponse> searchTrips(String departure, String arrival, LocalDate date,
                                  List<String> trainTypes, String trainCategory,
                                  BigDecimal minPrice, BigDecimal maxPrice);
    List<TripResponse> searchTrips(String departure, String arrival, LocalDate date,
                                  List<String> trainTypes, String trainCategory,
                                  BigDecimal minPrice, BigDecimal maxPrice,
                                  String promoCode);
    TripResponse createTrip(TripCreateRequest request);
    TripResponse updateTrip(Long id, TripCreateRequest request);
    void deleteTrip(Long id);
}
