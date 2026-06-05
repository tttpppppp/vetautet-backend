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
public class TripSegmentPrice {
    private Long id;
    private Long segmentId;
    private Long carriageTypeId;
    private String carriageTypeCode;
    private String carriageTypeName;
    private String passengerType;
    private BigDecimal price;
    private String currency;
    private String status;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
}
