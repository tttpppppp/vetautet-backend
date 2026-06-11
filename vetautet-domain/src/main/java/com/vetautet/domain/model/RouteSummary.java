package com.vetautet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RouteSummary(
        Long departureStationId,
        String departureStation,
        String departureStationCode,
        Long arrivalStationId,
        String arrivalStation,
        String arrivalStationCode,
        Long tripsCount,
        Long availableSeats,
        BigDecimal minPrice,
        LocalDateTime nextDepartureTime,
        String trainCategoriesCsv
) {
}
