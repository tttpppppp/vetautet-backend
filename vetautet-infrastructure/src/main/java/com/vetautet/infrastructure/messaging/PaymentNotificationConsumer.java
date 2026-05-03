package com.vetautet.infrastructure.messaging;

import com.vetautet.domain.model.Booking;
import com.vetautet.domain.model.BookingMailEvent;
import com.vetautet.domain.model.Notification;
import com.vetautet.domain.repository.BookingRepository;
import com.vetautet.infrastructure.config.KafkaConfig;
import com.vetautet.infrastructure.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class PaymentNotificationConsumer {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaConfig.PAYMENT_CONFIRMED_TOPIC, groupId = "vetautet-group", autoStartup = "${vetautet.kafka.listeners.enabled:true}")
    @Transactional
    public void handlePaymentConfirmed(Long bookingId) {
        System.out.println(">>> [KAFKA] Nhan event payment-confirmed cho Booking: " + bookingId);

        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

            Notification notification = notificationService.sendBookingConfirmation(booking);
            BookingMailEvent mailEvent = BookingMailEvent.builder()
                    .bookingId(booking.getId())
                    .userId(booking.getUser().getId())
                    .recipientEmail(booking.getUser().getEmail())
                    .subject(notification.getTitle())
                    .content(notification.getContent())
                    .build();

            afterCommit(() -> {
                kafkaTemplate.send(KafkaConfig.MAIL_SEND_REQUESTED_TOPIC, bookingId.toString(), mailEvent);
                System.out.println(">>> [KAFKA] Published mail-send-requested for Booking: " + bookingId);
            });

            System.out.println(">>> [NOTI] Da luu notification va push realtime cho Booking: " + bookingId);
        } catch (Exception e) {
            System.err.println(">>> [NOTI ERROR] Loi khi xu ly thong bao cho Booking " + bookingId + ": " + e.getMessage());
        }
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
