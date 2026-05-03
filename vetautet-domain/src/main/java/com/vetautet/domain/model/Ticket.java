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
public class Ticket {
    private Long id;
    private Trip trip;
    private Long tripId;
    private Long seatId;
    private String seatNumber;
    private Long carriageId;
    private String carriageNumber;
    private String carriageTypeName;
    private BigDecimal price;
    private String status;
    private LocalDateTime holdExpiredAt;
}
