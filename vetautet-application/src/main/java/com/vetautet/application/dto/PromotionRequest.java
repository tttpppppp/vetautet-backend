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
    private Integer usageLimit;
    private Integer usedCount;
    private Integer easeScore;
    private String status;
}
