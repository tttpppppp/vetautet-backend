package com.vetautet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
public class SeatEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carriage_id", nullable = false)
    private CarriageEntity carriage;

    @Column(nullable = false)
    private String seatNumber;

    private String seatType;

    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
}
