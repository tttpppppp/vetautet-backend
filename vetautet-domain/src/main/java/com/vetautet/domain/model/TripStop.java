package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripStop {
    private Long id;
    private Long tripId;
    private Station station;
    private Integer stopOrder;
    private LocalDateTime scheduledArrivalTime;
    private LocalDateTime scheduledDepartureTime;
    private LocalDateTime estimatedArrivalTime;
    private LocalDateTime estimatedDepartureTime;
    private LocalDateTime actualArrivalTime;
    private LocalDateTime actualDepartureTime;
    private BigDecimal distanceFromOriginKm;
    private String status;
    private String platform;
    private String note;
}
