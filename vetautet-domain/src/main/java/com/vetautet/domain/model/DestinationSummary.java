package com.vetautet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DestinationSummary(
        Long stationId,
        String stationName,
        String stationCode,
        String location,
        Long tripsCount,
        Long availableSeats,
        BigDecimal minPrice,
        LocalDateTime nextDepartureTime
) {
}
