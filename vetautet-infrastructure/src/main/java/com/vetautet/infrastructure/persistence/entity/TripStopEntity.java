package com.vetautet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "trip_stops",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trip_stop_order", columnNames = {"trip_id", "stop_order"}),
                @UniqueConstraint(name = "uk_trip_station", columnNames = {"trip_id", "station_id"})
        }
)
@Data
@NoArgsConstructor
public class TripStopEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private StationEntity station;

    @Column(nullable = false)
    private Integer stopOrder;

    private LocalDateTime scheduledArrivalTime;
    private LocalDateTime scheduledDepartureTime;
    private LocalDateTime estimatedArrivalTime;
    private LocalDateTime estimatedDepartureTime;
    private LocalDateTime actualArrivalTime;
    private LocalDateTime actualDepartureTime;
    private BigDecimal distanceFromOriginKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStopStatus status = TripStopStatus.SCHEDULED;

    private String platform;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum TripStopStatus {
        SCHEDULED, ARRIVING, ARRIVED, DEPARTED, DELAYED, SKIPPED, CANCELLED
    }
}
