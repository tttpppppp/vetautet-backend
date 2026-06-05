package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSegment {
    private Long id;
    private Long tripId;
    private TripStop fromStop;
    private TripStop toStop;
    private Integer segmentOrder;
    private BigDecimal distanceKm;
    private String status;
}
