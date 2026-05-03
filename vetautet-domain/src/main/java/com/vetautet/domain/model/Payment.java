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
public class Payment {
    private Long id;
    private Long bookingId;
    private String method;
    private BigDecimal amount;
    private String status;
    private String transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
