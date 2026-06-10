package com.vetautet.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PromotionRequest {
    private String title;
    private String description;
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private LocalDate startsAt;
    private LocalDate endsAt;
    private String conditions;
    private String route;
    private List<String> categories;
    private List<PassengerRuleRequest> passengerRules;
    private Integer usageLimit;
    private Integer usedCount;
    private Integer easeScore;
    private String status;

    @Data
    public static class PassengerRuleRequest {
        private Long id;
        private String passengerType;
        private String label;
        private Integer minAge;
        private Integer maxAge;
        private String discountType;
        private BigDecimal discountValue;
        private BigDecimal maxDiscountAmount;
        private Boolean verificationRequired;
        private String description;
        private String status;
    }
}
