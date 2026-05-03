package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.PromotionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PromotionJpaRepository extends JpaRepository<PromotionEntity, Long> {

    @Query("SELECT p FROM PromotionEntity p " +
            "WHERE p.deletedAt IS NULL " +
            "AND (:query IS NULL OR " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.code) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.conditions) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.route) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND (:discountType IS NULL OR p.discountType = :discountType) " +
            "AND (:category IS NULL OR CONCAT(',', p.categories, ',') LIKE CONCAT('%,', :category, ',%')) " +
            "AND (:route IS NULL OR p.route = :route)")
    List<PromotionEntity> search(@Param("query") String query,
                                 @Param("discountType") String discountType,
                                 @Param("category") String category,
                                 @Param("route") String route);

    Optional<PromotionEntity> findByCodeAndDeletedAtIsNull(String code);

    @Query("SELECT p FROM PromotionEntity p " +
            "WHERE p.deletedAt IS NULL " +
            "AND p.code = :code " +
            "AND p.status = 'ACTIVE' " +
            "AND p.startsAt <= :date " +
            "AND p.endsAt >= :date")
    Optional<PromotionEntity> findActiveByCode(@Param("code") String code, @Param("date") LocalDate date);
}
