package com.vetautet.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface RouteSummaryRow {
    Long getDepartureStationId();
    String getDepartureStation();
    String getDepartureStationCode();
    Long getArrivalStationId();
    String getArrivalStation();
    String getArrivalStationCode();
    Long getTripsCount();
    Long getAvailableSeats();
    BigDecimal getMinPrice();
    LocalDateTime getNextDepartureTime();
    String getTrainCategoriesCsv();
}
