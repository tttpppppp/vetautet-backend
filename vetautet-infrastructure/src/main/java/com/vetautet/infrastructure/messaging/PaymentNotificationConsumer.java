package com.vetautet.infrastructure.messaging;

import com.vetautet.domain.model.Booking;
import com.vetautet.domain.model.BookingMailEvent;
import com.vetautet.domain.model.Notification;
import com.vetautet.domain.model.PaymentFailedEvent;
import com.vetautet.domain.gateway.OutboxEventGateway;
import com.vetautet.domain.repository.BookingRepository;
import com.vetautet.infrastructure.config.KafkaConfig;
import com.vetautet.infrastructure.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentNotificationConsumer {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OutboxEventGateway outboxEventGateway;

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

            outboxEventGateway.enqueue(
                    KafkaConfig.MAIL_SEND_REQUESTED_TOPIC,
                    bookingId.toString(),
                    "MAIL_SEND_REQUESTED",
                    "BOOKING",
                    bookingId.toString(),
                    mailEvent
            );
            System.out.println(">>> [OUTBOX ENQUEUED] mail-send-requested for Booking: " + bookingId);

            System.out.println(">>> [NOTI] Da luu notification va push realtime cho Booking: " + bookingId);
        } catch (Exception e) {
            System.err.println(">>> [NOTI ERROR] Loi khi xu ly thong bao cho Booking " + bookingId + ": " + e.getMessage());
        }
    }

    @KafkaListener(
            topics = KafkaConfig.PAYMENT_FAILED_TOPIC,
            groupId = "vetautet-payment-failed-notification-group",
            autoStartup = "${vetautet.kafka.listeners.enabled:true}"
    )
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        Long bookingId = event != null ? event.getBookingId() : null;
        if (bookingId == null) {
            return;
        }

        System.out.println(">>> [KAFKA] Nhan event payment-failed cho Booking: " + bookingId);

        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

            notificationService.sendPaymentFailure(booking, event.getReason());
        } catch (Exception e) {
            System.err.println(">>> [NOTI ERROR] Loi khi xu ly thong bao payment fail cho Booking "
                    + bookingId + ": " + e.getMessage());
        }
    }
}
