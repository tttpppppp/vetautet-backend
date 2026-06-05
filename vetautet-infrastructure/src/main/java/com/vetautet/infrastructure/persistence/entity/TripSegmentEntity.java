package com.vetautet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "trip_segments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trip_segment_order", columnNames = {"trip_id", "segment_order"})
        }
)
@Data
@NoArgsConstructor
public class TripSegmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_stop_id", nullable = false)
    private TripStopEntity fromStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_stop_id", nullable = false)
    private TripStopEntity toStop;

    @Column(nullable = false)
    private Integer segmentOrder;

    private BigDecimal distanceKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripSegmentStatus status = TripSegmentStatus.SCHEDULED;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum TripSegmentStatus {
        SCHEDULED, RUNNING, COMPLETED, CANCELLED
    }
}
