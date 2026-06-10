package com.vetautet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "trip_segment_prices",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_segment_price",
                        columnNames = {"segment_id", "carriage_type_id", "passenger_type"}
                )
        }
)
@Data
@NoArgsConstructor
public class TripSegmentPriceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id", nullable = false)
    private TripSegmentEntity segment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carriage_type_id", nullable = false)
    private CarriageTypeEntity carriageType;

    @Column(nullable = false, length = 30)
    private String passengerType = "ADULT";

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriceStatus status = PriceStatus.ACTIVE;

    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum PriceStatus {
        ACTIVE, INACTIVE
    }
}
