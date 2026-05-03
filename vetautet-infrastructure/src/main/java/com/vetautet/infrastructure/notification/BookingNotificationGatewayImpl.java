package com.vetautet.infrastructure.notification;

import com.vetautet.domain.gateway.BookingNotificationGateway;
import com.vetautet.domain.model.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingNotificationGatewayImpl implements BookingNotificationGateway {

    private final NotificationService notificationService;

    public BookingNotificationGatewayImpl(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void sendBookingConfirmation(Booking booking) {
        notificationService.sendBookingConfirmation(booking);
    }
}
