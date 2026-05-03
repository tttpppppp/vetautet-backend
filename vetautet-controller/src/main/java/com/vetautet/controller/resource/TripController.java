package com.vetautet.controller.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetautet.application.cache.TripCacheKeys;
import com.vetautet.application.cache.TripJsonCacheService;
import com.vetautet.application.dto.PopularDestinationResponse;
import com.vetautet.application.dto.PopularRouteResponse;
import com.vetautet.application.dto.TrainCategoryResponse;
import com.vetautet.application.dto.TripResponse;
import com.vetautet.application.service.trip.TripAppService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/trips")
public class TripController {

    private static final Duration PUBLIC_TRIP_JSON_TTL = Duration.ofMinutes(10);
    private static final Duration SEARCH_JSON_TTL = Duration.ofSeconds(60);
    private static final Duration LOCAL_PUBLIC_TRIP_JSON_TTL = Duration.ofSeconds(2);
    private static final Duration LOCAL_SEARCH_JSON_TTL = Duration.ofSeconds(2);

    @Autowired
    private TripAppService tripAppService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TripJsonCacheService tripJsonCacheService;

    @GetMapping
    public ResponseEntity<?> getAllTrips(
            @RequestParam(value = "promoCode", required = false) String promoCode) {
        if (!hasText(promoCode)) {
            return cachedJson(
                    TripCacheKeys.HTTP_TRIPS_ALL,
                    PUBLIC_TRIP_JSON_TTL,
                    LOCAL_PUBLIC_TRIP_JSON_TTL,
                    () -> tripAppService.getAllTrips(null)
            );
        }
        return ResponseEntity.ok(tripAppService.getAllTrips(promoCode));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<TrainCategoryResponse>> getTrainCategories() {
        return ResponseEntity.ok(tripAppService.getTrainCategories());
    }

    @GetMapping("/popular")
    public ResponseEntity<List<TripResponse>> getPopularTrips(
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        return ResponseEntity.ok(tripAppService.getPopularTrips(limit));
    }

    @GetMapping("/popular-routes")
    public ResponseEntity<List<PopularRouteResponse>> getPopularRoutes(
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        return ResponseEntity.ok(tripAppService.getPopularRoutes(limit));
    }

    @GetMapping("/popular-destinations")
    public ResponseEntity<List<PopularDestinationResponse>> getPopularDestinations(
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        return ResponseEntity.ok(tripAppService.getPopularDestinations(limit));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<TripResponse>> getUpcomingDepartures(
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        return ResponseEntity.ok(tripAppService.getUpcomingDepartures(limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTripById(
            @PathVariable("id") Long id,
            @RequestParam(value = "bookingId", required = false) Long bookingId) {
        if (bookingId == null) {
            return cachedJson(
                    TripCacheKeys.httpTrip(id),
                    PUBLIC_TRIP_JSON_TTL,
                    LOCAL_PUBLIC_TRIP_JSON_TTL,
                    () -> tripAppService.getTripById(id, null)
            );
        }
        return ResponseEntity.ok(tripAppService.getTripById(id, bookingId));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchTrips(
            @RequestParam(value = "departure", required = false) String departure,
            @RequestParam(value = "arrival", required = false) String arrival,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "trainTypes", required = false) List<String> trainTypes,
            @RequestParam(value = "trainCategory", required = false) String trainCategory,
            @RequestParam(value = "minPrice", required = false) java.math.BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) java.math.BigDecimal maxPrice,
            @RequestParam(value = "promoCode", required = false) String promoCode,
            HttpServletRequest request) {
        String queryString = request.getQueryString() == null ? "" : request.getQueryString();
        return cachedJson(
                TripCacheKeys.httpSearch(cacheKeyHash(queryString)),
                SEARCH_JSON_TTL,
                LOCAL_SEARCH_JSON_TTL,
                () -> tripAppService.searchTrips(departure, arrival, date, trainTypes, trainCategory, minPrice, maxPrice, promoCode)
        );
    }

    private ResponseEntity<String> cachedJson(String cacheKey, Duration redisTtl, Duration localTtl, Supplier<Object> supplier) {
        String json = tripJsonCacheService.get(cacheKey, redisTtl, localTtl, () -> toJson(supplier.get()));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize trip response", e);
        }
    }

    private String cacheKeyHash(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
