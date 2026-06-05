package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripStopRequest {
    private Long stationId;
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
