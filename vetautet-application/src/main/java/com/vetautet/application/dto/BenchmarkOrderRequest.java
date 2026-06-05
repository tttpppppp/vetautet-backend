package com.vetautet.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BenchmarkOrderRequest {
    private String requestId;
    private Long userRef;
    private Long ticketRef;
    private Integer quantity = 1;
    private BigDecimal amount = BigDecimal.valueOf(100000);
    private String note;
}
