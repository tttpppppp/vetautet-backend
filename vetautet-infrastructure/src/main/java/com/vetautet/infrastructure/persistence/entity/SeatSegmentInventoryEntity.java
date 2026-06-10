package com.vetautet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "seat_segment_inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_segment_seat", columnNames = {"segment_id", "seat_id"})
        }
)
@Data
@NoArgsConstructor
public class SeatSegmentInventoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id", nullable = false)
    private TripSegmentEntity segment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private SeatEntity seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryStatus status = InventoryStatus.AVAILABLE;

    private LocalDateTime holdExpiredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_detail_id")
    private BookingDetailEntity bookingDetail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum InventoryStatus {
        AVAILABLE, QUEUED, HOLD, BOOKED, BLOCKED
    }
}
