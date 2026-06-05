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
public class BenchmarkOrder {
    private Long id;
    private String requestId;
    private Long userRef;
    private Long ticketRef;
    private Integer quantity;
    private BigDecimal amount;
    private String status;
    private String source;
    private String note;
    private String kafkaKey;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
