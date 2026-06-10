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
public class PassengerFareRuleRequest {
    private Long id;
    private String passengerType;
    private String label;
    private Integer minAge;
    private Integer maxAge;
    private BigDecimal discountPercent;
    private BigDecimal fareMultiplier;
    private Boolean verificationRequired;
    private String description;
    private Integer sortOrder;
    private String status;
}
