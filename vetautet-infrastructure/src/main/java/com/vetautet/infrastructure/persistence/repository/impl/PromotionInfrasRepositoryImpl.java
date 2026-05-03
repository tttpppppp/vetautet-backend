package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.Promotion;
import com.vetautet.domain.repository.PromotionRepository;
import com.vetautet.infrastructure.persistence.entity.PromotionEntity;
import com.vetautet.infrastructure.persistence.repository.PromotionJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class PromotionInfrasRepositoryImpl implements PromotionRepository {

    private final PromotionJpaRepository jpaRepository;

    public PromotionInfrasRepositoryImpl(PromotionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Promotion> search(String query, String discountType, String category, String route) {
        return jpaRepository.search(query, discountType, category, route).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Promotion> findById(Long id) {
        return jpaRepository.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    public Optional<Promotion> findByCode(String code) {
        return jpaRepository.findByCodeAndDeletedAtIsNull(code).map(this::toDomain);
    }

    @Override
    public Optional<Promotion> findActiveByCode(String code, LocalDate date) {
        return jpaRepository.findActiveByCode(code, date).map(this::toDomain);
    }

    @Override
    @Transactional
    public Promotion save(Promotion promotion) {
        PromotionEntity entity = promotion.getId() == null
                ? new PromotionEntity()
                : jpaRepository.findById(promotion.getId()).orElseGet(PromotionEntity::new);
        applyToEntity(promotion, entity);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setDeletedAt(LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }

    private Promotion toDomain(PromotionEntity entity) {
        return Promotion.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .code(entity.getCode())
                .discountType(entity.getDiscountType())
                .discountValue(entity.getDiscountValue())
                .maxDiscountAmount(entity.getMaxDiscountAmount())
                .minOrderAmount(entity.getMinOrderAmount())
                .startsAt(entity.getStartsAt())
                .endsAt(entity.getEndsAt())
                .conditions(entity.getConditions())
                .route(entity.getRoute())
                .categories(parseCategories(entity.getCategories()))
                .usageLimit(entity.getUsageLimit())
                .usedCount(entity.getUsedCount())
                .easeScore(entity.getEaseScore())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void applyToEntity(Promotion promotion, PromotionEntity entity) {
        entity.setId(promotion.getId());
        entity.setTitle(promotion.getTitle());
        entity.setDescription(promotion.getDescription());
        entity.setCode(promotion.getCode());
        entity.setDiscountType(promotion.getDiscountType());
        entity.setDiscountValue(promotion.getDiscountValue());
        entity.setMaxDiscountAmount(promotion.getMaxDiscountAmount());
        entity.setMinOrderAmount(promotion.getMinOrderAmount());
        entity.setStartsAt(promotion.getStartsAt());
        entity.setEndsAt(promotion.getEndsAt());
        entity.setConditions(promotion.getConditions());
        entity.setRoute(promotion.getRoute());
        entity.setCategories(joinCategories(promotion.getCategories()));
        entity.setUsageLimit(promotion.getUsageLimit());
        entity.setUsedCount(promotion.getUsedCount());
        entity.setEaseScore(promotion.getEaseScore());
        entity.setStatus(promotion.getStatus());
    }

    private List<String> parseCategories(String categories) {
        if (categories == null || categories.isBlank()) {
            return List.of();
        }
        return Arrays.stream(categories.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
    }

    private String joinCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return "";
        }
        return categories.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(","));
    }
}
