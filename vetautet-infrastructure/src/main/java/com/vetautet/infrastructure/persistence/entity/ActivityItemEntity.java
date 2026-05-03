package com.vetautet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_items")
@Data
@NoArgsConstructor
public class ActivityItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private ActivityEntity activity;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Integer stockInitial = 0;

    @Column(nullable = false)
    private Integer stockAvailable = 0;

    @Column(nullable = false)
    private Boolean isStockPrepared = false;

    @Column(nullable = false)
    private BigDecimal priceOriginal;

    @Column(nullable = false)
    private BigDecimal priceFlash;

    @Column(nullable = false)
    private LocalDateTime saleStartTime;

    @Column(nullable = false)
    private LocalDateTime saleEndTime;

    @Column(nullable = false)
    private Integer status = 0;

    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
}
