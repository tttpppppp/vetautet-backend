package com.vetautet.application.service.payment;

import com.vetautet.application.service.order.BookingAppService;
import com.vetautet.domain.gateway.OutboxEventGateway;
import com.vetautet.domain.model.Booking;
import com.vetautet.domain.model.BookingSagaEvent;
import com.vetautet.domain.model.PaymentFailedEvent;
import com.vetautet.domain.service.BookingDomainService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PaymentFailedSagaConsumer {

    private static final String PAYMENT_FAILED_TOPIC = "payment-failed";
    private static final String BOOKING_SAGA_EVENTS_TOPIC = "booking-saga-events";

    private final BookingDomainService bookingDomainService;
    private final BookingAppService bookingAppService;
    private final OutboxEventGateway outboxEventGateway;
    private final SimpMessagingTemplate messagingTemplate;

    public PaymentFailedSagaConsumer(BookingDomainService bookingDomainService,
                                     BookingAppService bookingAppService,
                                     OutboxEventGateway outboxEventGateway,
                                     SimpMessagingTemplate messagingTemplate) {
        this.bookingDomainService = bookingDomainService;
        this.bookingAppService = bookingAppService;
        this.outboxEventGateway = outboxEventGateway;
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(
            topics = PAYMENT_FAILED_TOPIC,
            groupId = "vetautet-booking-saga-group",
            autoStartup = "${vetautet.kafka.listeners.enabled:true}"
    )
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        Long bookingId = event.getBookingId();
        String step = event.getStep() != null ? event.getStep() : "PAYMENT_FAILED";

        try {
            Booking booking = bookingDomainService.getBookingById(bookingId);
            if ("PENDING".equalsIgnoreCase(booking.getStatus())) {
                bookingAppService.updateBookingStatus(bookingId, "CANCELLED");
                publishBookingSagaEvent(bookingId, step, "COMPENSATED", safeMessage(event.getReason()));
                return;
            }

            publishBookingSagaEvent(bookingId, step, "SKIPPED",
                    "Booking status is " + booking.getStatus() + ", no stock compensation needed");
        } catch (RuntimeException ex) {
            publishBookingSagaEvent(bookingId, step, "COMPENSATION_FAILED", safeMessage(ex));
            throw ex;
        }
    }

    private void publishBookingSagaEvent(Long bookingId, String step, String status, String reason) {
        BookingSagaEvent event = BookingSagaEvent.builder()
                .bookingId(bookingId)
                .ticketIds(List.of())
                .step(step)
                .status(status)
                .reason(reason)
                .occurredAt(LocalDateTime.now())
                .build();

        try {
            messagingTemplate.convertAndSend("/topic/bookings/saga", event);
        } catch (Exception ex) {
            System.err.println(">>> [SAGA WS SKIPPED] " + step + " bookingId=" + bookingId
                    + " error=" + ex.getMessage());
        }

        String key = bookingId != null ? bookingId.toString() : step;
        String eventType = "BOOKING_SAGA_" + step + "_" + status;
        outboxEventGateway.enqueue(
                BOOKING_SAGA_EVENTS_TOPIC,
                key,
                eventType,
                "BOOKING",
                bookingId != null ? bookingId.toString() : null,
                event
        );
        System.out.println(">>> [OUTBOX ENQUEUED] " + eventType + " topic=" + BOOKING_SAGA_EVENTS_TOPIC
                + " bookingId=" + bookingId);
    }

    private String safeMessage(Exception ex) {
        return ex == null || ex.getMessage() == null ? "unknown" : ex.getMessage();
    }

    private String safeMessage(String message) {
        return message == null || message.isBlank() ? "unknown" : message;
    }
}
