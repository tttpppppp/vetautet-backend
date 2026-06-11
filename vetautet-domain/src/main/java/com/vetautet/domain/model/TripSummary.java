package com.vetautet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripSummary(
        Long id,
        String trainCode,
        String trainCategory,
        String departureStation,
        String arrivalStation,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        Integer duration,
        BigDecimal minPrice,
        Long availableSeats,
        Long totalSeats,
        Long bookedSeats,
        String status
) {
}
