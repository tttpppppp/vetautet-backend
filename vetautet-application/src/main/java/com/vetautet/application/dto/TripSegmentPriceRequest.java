package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripSegmentPriceRequest {
    private Long segmentId;
    private Long carriageTypeId;
    private String passengerType;
    private BigDecimal price;
    private String currency;
    private String status;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
}
