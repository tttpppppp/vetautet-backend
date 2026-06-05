package com.vetautet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private Long referenceId; // bookingId

    @Column(nullable = false)
    private boolean isRead = false;

    private LocalDateTime createdAt;

    public enum NotificationType {
        BOOKING_CONFIRMED,
        BOOKING_CANCELLED,
        BOOKING_EXPIRED,
        BOOKING_FAILED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        SYSTEM
    }
}
