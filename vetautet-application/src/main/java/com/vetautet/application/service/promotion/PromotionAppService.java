package com.vetautet.application.service.promotion;

import com.vetautet.application.dto.PromotionRequest;
import com.vetautet.application.dto.PromotionResponse;
import com.vetautet.application.dto.PromotionValidationResponse;

import java.math.BigDecimal;
import java.util.List;

public interface PromotionAppService {
    List<PromotionResponse> getPromotions(String query,
                                          String minDiscount,
                                          String discountType,
                                          String category,
                                          String status,
                                          String route,
                                          String sort);

    PromotionResponse getPromotionByCode(String code);

    PromotionValidationResponse validatePromotion(String code, BigDecimal orderAmount);

    PromotionResponse createPromotion(PromotionRequest request);

    PromotionResponse updatePromotion(Long id, PromotionRequest request);

    void deletePromotion(Long id);
}
