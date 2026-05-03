package com.vetautet.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Data
@NoArgsConstructor
public class PromotionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 30)
    private String discountType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Column(precision = 15, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(nullable = false)
    private LocalDate startsAt;

    @Column(nullable = false)
    private LocalDate endsAt;

    @Column(columnDefinition = "TEXT")
    private String conditions;

    private String route;

    @Column(length = 255)
    private String categories;

    private Integer usageLimit;

    private Integer usedCount;

    private Integer easeScore;

    @Column(nullable = false, length = 30)
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = "ACTIVE";
        }
        if (usedCount == null) {
            usedCount = 0;
        }
        if (easeScore == null) {
            easeScore = 70;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
