package com.vetautet.application.service.promotion.impl;

import com.vetautet.application.dto.PromotionRequest;
import com.vetautet.application.dto.PromotionResponse;
import com.vetautet.application.dto.PromotionValidationResponse;
import com.vetautet.application.service.promotion.PromotionAppService;
import com.vetautet.domain.model.Promotion;
import com.vetautet.domain.service.PromotionDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PromotionAppServiceImpl implements PromotionAppService {

    private static final int EXPIRING_SOON_DAYS = 7;

    private final PromotionDomainService promotionDomainService;

    public PromotionAppServiceImpl(PromotionDomainService promotionDomainService) {
        this.promotionDomainService = promotionDomainService;
    }

    @Override
    public List<PromotionResponse> getPromotions(String query,
                                                 String minDiscount,
                                                 String discountType,
                                                 String category,
                                                 String status,
                                                 String route,
                                                 String sort) {
        BigDecimal normalizedMinDiscount = parseMinDiscount(minDiscount);
        String normalizedType = normalizeBlank(discountType);
        String normalizedCategory = normalizeBlank(category);
        String normalizedStatus = normalizeBlank(status);
        String normalizedRoute = normalizeBlank(route);

        return promotionDomainService.searchPromotions(
                        normalizeBlank(query),
                        normalizedType,
                        normalizedCategory,
                        normalizedRoute
                ).stream()
                .filter(promotion -> filterByMinDiscount(promotion, normalizedMinDiscount))
                .filter(promotion -> filterByStatus(promotion, normalizedStatus))
                .sorted(sortComparator(sort))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PromotionResponse getPromotionByCode(String code) {
        return toResponse(promotionDomainService.getPromotionByCode(normalizeCode(code)));
    }

    @Override
    public PromotionValidationResponse validatePromotion(String code, BigDecimal orderAmount) {
        BigDecimal amount = orderAmount == null ? BigDecimal.ZERO : orderAmount.max(BigDecimal.ZERO);
        try {
            Promotion promotion = promotionDomainService.getActivePromotionByCode(normalizeCode(code), LocalDate.now());
            if (promotion.getMinOrderAmount() != null && amount.compareTo(promotion.getMinOrderAmount()) < 0) {
                return invalid(code, amount, "Order amount does not meet promotion minimum");
            }
            if (promotion.getUsageLimit() != null
                    && promotion.getUsedCount() != null
                    && promotion.getUsedCount() >= promotion.getUsageLimit()) {
                return invalid(code, amount, "Promotion usage limit reached");
            }

            BigDecimal discountAmount = calculateDiscountAmount(promotion, amount);
            BigDecimal finalAmount = amount.subtract(discountAmount).max(BigDecimal.ZERO);
            return PromotionValidationResponse.builder()
                    .valid(true)
                    .code(promotion.getCode())
                    .message("Promotion is valid")
                    .orderAmount(amount)
                    .discountAmount(discountAmount)
                    .finalAmount(finalAmount)
                    .promotion(toResponse(promotion))
                    .build();
        } catch (RuntimeException ex) {
            return invalid(code, amount, ex.getMessage());
        }
    }

    @Override
    @Transactional
    public PromotionResponse createPromotion(PromotionRequest request) {
        return toResponse(promotionDomainService.createPromotion(toDomain(request)));
    }

    @Override
    @Transactional
    public PromotionResponse updatePromotion(Long id, PromotionRequest request) {
        return toResponse(promotionDomainService.updatePromotion(id, toDomain(request)));
    }

    @Override
    @Transactional
    public void deletePromotion(Long id) {
        promotionDomainService.deletePromotion(id);
    }

    private Promotion toDomain(PromotionRequest request) {
        return Promotion.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .code(normalizeCode(request.getCode()))
                .discountType(normalizeBlank(request.getDiscountType()))
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderAmount(request.getMinOrderAmount())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .conditions(request.getConditions())
                .route(normalizeBlank(request.getRoute()))
                .categories(request.getCategories())
                .usageLimit(request.getUsageLimit())
                .usedCount(request.getUsedCount())
                .easeScore(request.getEaseScore())
                .status(normalizeBlank(request.getStatus()))
                .build();
    }

    private PromotionResponse toResponse(Promotion promotion) {
        long daysLeft = daysLeft(promotion);
        return PromotionResponse.builder()
                .id(promotion.getId() != null ? promotion.getId().toString() : null)
                .title(promotion.getTitle())
                .description(promotion.getDescription())
                .code(promotion.getCode())
                .discountType(promotion.getDiscountType())
                .discountLabel(discountLabel(promotion))
                .discountValue(promotion.getDiscountValue())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .minOrderAmount(promotion.getMinOrderAmount())
                .startsAt(promotion.getStartsAt())
                .endsAt(promotion.getEndsAt())
                .conditions(promotion.getConditions())
                .route(promotion.getRoute())
                .categories(promotion.getCategories() == null ? List.of() : promotion.getCategories())
                .usageLimit(promotion.getUsageLimit())
                .usedCount(promotion.getUsedCount())
                .easeScore(promotion.getEaseScore())
                .status(promotion.getStatus())
                .createdAt(promotion.getCreatedAt())
                .active(isActive(promotion))
                .expiringSoon(isExpiringSoon(promotion))
                .daysLeft(daysLeft)
                .build();
    }

    private PromotionValidationResponse invalid(String code, BigDecimal amount, String message) {
        return PromotionValidationResponse.builder()
                .valid(false)
                .code(normalizeCode(code))
                .message(message)
                .orderAmount(amount)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(amount)
                .build();
    }

    private BigDecimal calculateDiscountAmount(Promotion promotion, BigDecimal orderAmount) {
        BigDecimal discountValue = promotion.getDiscountValue() == null ? BigDecimal.ZERO : promotion.getDiscountValue();
        BigDecimal discountAmount;
        if ("percent".equalsIgnoreCase(promotion.getDiscountType())) {
            discountAmount = orderAmount.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            discountAmount = discountValue;
        }

        if (promotion.getMaxDiscountAmount() != null) {
            discountAmount = discountAmount.min(promotion.getMaxDiscountAmount());
        }
        return discountAmount.min(orderAmount).max(BigDecimal.ZERO);
    }

    private String discountLabel(Promotion promotion) {
        BigDecimal value = promotion.getDiscountValue();
        if ("percent".equalsIgnoreCase(promotion.getDiscountType())) {
            return "Giam " + formatNumber(value) + "%";
        }
        if ("serviceFee".equalsIgnoreCase(promotion.getDiscountType())) {
            return "Mien phi dich vu";
        }
        return "Giam " + formatNumber(value) + "d";
    }

    private String formatNumber(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private boolean filterByMinDiscount(Promotion promotion, BigDecimal minDiscount) {
        if (minDiscount == null) {
            return true;
        }
        return "percent".equalsIgnoreCase(promotion.getDiscountType())
                && promotion.getDiscountValue() != null
                && promotion.getDiscountValue().compareTo(minDiscount) >= 0;
    }

    private boolean filterByStatus(Promotion promotion, String status) {
        if (status == null) {
            return true;
        }
        if ("active".equalsIgnoreCase(status)) {
            return isActive(promotion);
        }
        if ("expiring".equalsIgnoreCase(status)) {
            return isExpiringSoon(promotion);
        }
        return true;
    }

    private Comparator<Promotion> sortComparator(String sort) {
        if ("expiring".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Promotion::getEndsAt, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if ("discount".equalsIgnoreCase(sort)) {
            return Comparator.comparing(this::discountScore, Comparator.nullsLast(BigDecimal::compareTo)).reversed();
        }
        if ("easy".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Promotion::getEaseScore, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        }
        return Comparator.comparing(Promotion::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
    }

    private BigDecimal discountScore(Promotion promotion) {
        if (promotion.getDiscountValue() == null) {
            return BigDecimal.ZERO;
        }
        if ("percent".equalsIgnoreCase(promotion.getDiscountType())) {
            return promotion.getDiscountValue().multiply(BigDecimal.valueOf(10000));
        }
        return promotion.getDiscountValue();
    }

    private boolean isActive(Promotion promotion) {
        LocalDate today = LocalDate.now();
        return "ACTIVE".equalsIgnoreCase(promotion.getStatus())
                && (promotion.getStartsAt() == null || !promotion.getStartsAt().isAfter(today))
                && (promotion.getEndsAt() == null || !promotion.getEndsAt().isBefore(today));
    }

    private boolean isExpiringSoon(Promotion promotion) {
        long daysLeft = daysLeft(promotion);
        return daysLeft >= 0 && daysLeft <= EXPIRING_SOON_DAYS;
    }

    private long daysLeft(Promotion promotion) {
        if (promotion.getEndsAt() == null) {
            return Long.MAX_VALUE;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), promotion.getEndsAt());
    }

    private BigDecimal parseMinDiscount(String minDiscount) {
        String normalized = normalizeBlank(minDiscount);
        if (normalized == null) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeCode(String value) {
        String normalized = normalizeBlank(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
