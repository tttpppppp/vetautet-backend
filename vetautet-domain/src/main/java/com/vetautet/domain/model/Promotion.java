package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {
    private Long id;
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
    private List<PromotionPassengerRule> passengerRules;
    private Integer usageLimit;
    private Integer usedCount;
    private Integer easeScore;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
