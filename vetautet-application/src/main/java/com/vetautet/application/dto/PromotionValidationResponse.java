package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionValidationResponse {
    private boolean valid;
    private String code;
    private String message;
    private BigDecimal orderAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private PromotionResponse promotion;
}
