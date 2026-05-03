package com.vetautet.domain.repository;

import com.vetautet.domain.model.Promotion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository {
    List<Promotion> search(String query, String discountType, String category, String route);
    Optional<Promotion> findById(Long id);
    Optional<Promotion> findByCode(String code);
    Optional<Promotion> findActiveByCode(String code, LocalDate date);
    Promotion save(Promotion promotion);
    void deleteById(Long id);
}
