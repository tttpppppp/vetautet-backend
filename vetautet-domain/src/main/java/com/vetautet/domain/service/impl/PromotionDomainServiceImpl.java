package com.vetautet.domain.service.impl;

import com.vetautet.domain.model.Promotion;
import com.vetautet.domain.repository.PromotionRepository;
import com.vetautet.domain.service.PromotionDomainService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PromotionDomainServiceImpl implements PromotionDomainService {

    private final PromotionRepository promotionRepository;

    public PromotionDomainServiceImpl(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Override
    public List<Promotion> searchPromotions(String query, String discountType, String category, String route) {
        return promotionRepository.search(query, discountType, category, route);
    }

    @Override
    public Promotion getPromotionById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));
    }

    @Override
    public Promotion getPromotionByCode(String code) {
        return promotionRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));
    }

    @Override
    public Promotion getActivePromotionByCode(String code, LocalDate date) {
        return promotionRepository.findActiveByCode(code, date)
                .orElseThrow(() -> new RuntimeException("Promotion is not valid"));
    }

    @Override
    public Promotion createPromotion(Promotion promotion) {
        normalizePromotion(promotion);
        return promotionRepository.save(promotion);
    }

    @Override
    public Promotion updatePromotion(Long id, Promotion promotion) {
        Promotion existing = getPromotionById(id);
        existing.setTitle(promotion.getTitle());
        existing.setDescription(promotion.getDescription());
        existing.setCode(promotion.getCode());
        existing.setDiscountType(promotion.getDiscountType());
        existing.setDiscountValue(promotion.getDiscountValue());
        existing.setMaxDiscountAmount(promotion.getMaxDiscountAmount());
        existing.setMinOrderAmount(promotion.getMinOrderAmount());
        existing.setStartsAt(promotion.getStartsAt());
        existing.setEndsAt(promotion.getEndsAt());
        existing.setConditions(promotion.getConditions());
        existing.setRoute(promotion.getRoute());
        existing.setCategories(promotion.getCategories());
        existing.setUsageLimit(promotion.getUsageLimit());
        existing.setUsedCount(promotion.getUsedCount());
        existing.setEaseScore(promotion.getEaseScore());
        existing.setStatus(promotion.getStatus());
        normalizePromotion(existing);
        return promotionRepository.save(existing);
    }

    @Override
    public void deletePromotion(Long id) {
        promotionRepository.deleteById(id);
    }

    private void normalizePromotion(Promotion promotion) {
        if (promotion.getCode() != null) {
            promotion.setCode(promotion.getCode().trim().toUpperCase());
        }
        if (promotion.getStatus() == null || promotion.getStatus().isBlank()) {
            promotion.setStatus("ACTIVE");
        }
        if (promotion.getUsedCount() == null) {
            promotion.setUsedCount(0);
        }
        if (promotion.getEaseScore() == null) {
            promotion.setEaseScore(70);
        }
    }
}
