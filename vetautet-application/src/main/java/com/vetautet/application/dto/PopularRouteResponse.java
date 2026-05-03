package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopularRouteResponse {
    private Long departureStationId;
    private String departureStation;
    private String departureStationCode;
    private Long arrivalStationId;
    private String arrivalStation;
    private String arrivalStationCode;
    private Integer tripsCount;
    private Integer availableSeats;
    private BigDecimal minPrice;
    private LocalDateTime nextDepartureTime;
    private List<String> trainCategories;
}
