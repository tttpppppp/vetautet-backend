package com.vetautet.infrastructure.messaging;

import com.vetautet.domain.model.BookingSagaEvent;
import com.vetautet.infrastructure.config.KafkaConfig;
import com.vetautet.infrastructure.notification.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BookingSagaNotificationConsumer {

    private final NotificationService notificationService;

    public BookingSagaNotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = KafkaConfig.BOOKING_SAGA_EVENTS_TOPIC,
            groupId = "vetautet-booking-saga-notification-group",
            autoStartup = "${vetautet.kafka.listeners.enabled:true}"
    )
    public void handleBookingSagaEvent(BookingSagaEvent event) {
        if (event.getUserId() == null || !"CREATE_BOOKING".equalsIgnoreCase(event.getStep())) {
            return;
        }
        if (!"FAILED".equalsIgnoreCase(event.getStatus()) && !"COMPENSATED".equalsIgnoreCase(event.getStatus())) {
            return;
        }

        notificationService.sendSystemNotification(
                event.getUserId(),
                "Dat ve khong thanh cong",
                "Yeu cau dat ve khong thanh cong. Ghe da duoc giai phong, vui long thu lai.",
                "BOOKING_FAILED",
                event.getBookingId()
        );
        System.out.println(">>> [NOTI] Saved booking failure notification userId=" + event.getUserId()
                + " bookingId=" + event.getBookingId());
    }
}
