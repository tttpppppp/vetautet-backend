package com.vetautet.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TripSummaryRow {
    Long getId();
    String getTrainCode();
    String getTrainCategory();
    String getDepartureStation();
    String getArrivalStation();
    LocalDateTime getDepartureTime();
    LocalDateTime getArrivalTime();
    Integer getDuration();
    BigDecimal getMinPrice();
    Long getAvailableSeats();
    Long getTotalSeats();
    Long getBookedSeats();
    String getStatus();
}
