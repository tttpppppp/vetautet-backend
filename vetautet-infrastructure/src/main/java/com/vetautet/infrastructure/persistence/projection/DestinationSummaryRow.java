package com.vetautet.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface DestinationSummaryRow {
    Long getStationId();
    String getStationName();
    String getStationCode();
    String getLocation();
    Long getTripsCount();
    Long getAvailableSeats();
    BigDecimal getMinPrice();
    LocalDateTime getNextDepartureTime();
}
