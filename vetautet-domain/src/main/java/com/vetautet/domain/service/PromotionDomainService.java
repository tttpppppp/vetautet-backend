package com.vetautet.domain.service;

import com.vetautet.domain.model.Promotion;

import java.time.LocalDate;
import java.util.List;

public interface PromotionDomainService {
    List<Promotion> searchPromotions(String query, String discountType, String category, String route);
    Promotion getPromotionById(Long id);
    Promotion getPromotionByCode(String code);
    Promotion getActivePromotionByCode(String code, LocalDate date);
    Promotion createPromotion(Promotion promotion);
    Promotion updatePromotion(Long id, Promotion promotion);
    void deletePromotion(Long id);
}
