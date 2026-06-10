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
import java.time.LocalDateTime;

@Entity
@Table(name = "passenger_fare_rules")
@Data
@NoArgsConstructor
public class PassengerFareRuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "passenger_type", nullable = false, unique = true, length = 30)
    private String passengerType;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "fare_multiplier", nullable = false, precision = 8, scale = 4)
    private BigDecimal fareMultiplier;

    @Column(name = "verification_required", nullable = false)
    private Boolean verificationRequired;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false, length = 30)
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (discountPercent == null) {
            discountPercent = BigDecimal.ZERO;
        }
        if (fareMultiplier == null) {
            fareMultiplier = BigDecimal.ONE;
        }
        if (verificationRequired == null) {
            verificationRequired = false;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
