package com.vetautet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_details")
@Data
@NoArgsConstructor
public class BookingDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingEntity booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketEntity ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Direction direction = Direction.OUTBOUND;

    private Long departureStationId;

    private Long arrivalStationId;

    @Column(length = 255)
    private String segmentIds;

    private BigDecimal segmentPrice;

    @Column(nullable = false)
    private String passengerName;

    @Column(nullable = false, length = 512)
    private String passengerIdCard;

    @Column(length = 30, nullable = false)
    private String passengerType = "ADULT";

    private LocalDateTime createdAt;

    public enum Direction {
        OUTBOUND, RETURN
    }
}
