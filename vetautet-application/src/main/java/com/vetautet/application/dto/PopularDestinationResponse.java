package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopularDestinationResponse {
    private Long stationId;
    private String stationName;
    private String stationCode;
    private String location;
    private Integer tripsCount;
    private Integer availableSeats;
    private BigDecimal minPrice;
    private LocalDateTime nextDepartureTime;
    private String imageUrl;
}
