package com.vetautet.infrastructure.notification;

import com.vetautet.domain.gateway.BookingNotificationGateway;
import com.vetautet.domain.model.Booking;
import com.vetautet.domain.model.BookingMailEvent;
import com.vetautet.domain.model.Notification;
import org.springframework.stereotype.Component;

@Component
public class BookingNotificationGatewayImpl implements BookingNotificationGateway {

    private final NotificationService notificationService;
    private final MailService mailService;

    public BookingNotificationGatewayImpl(NotificationService notificationService,
                                          MailService mailService) {
        this.notificationService = notificationService;
        this.mailService = mailService;
    }

    @Override
    public void sendBookingConfirmation(Booking booking) {
        Notification notification = notificationService.sendBookingConfirmation(booking);
        sendBookingConfirmationMail(booking, notification);
    }

    private void sendBookingConfirmationMail(Booking booking, Notification notification) {
        if (booking == null || booking.getUser() == null) {
            return;
        }

        BookingMailEvent mailEvent = BookingMailEvent.builder()
                .bookingId(booking.getId())
                .userId(booking.getUser().getId())
                .recipientEmail(booking.getUser().getEmail())
                .subject(notification != null ? notification.getTitle() : "Dat ve thanh cong #" + booking.getId())
                .content(notification != null ? notification.getContent() : "Don hang #" + booking.getId() + " da duoc xac nhan.")
                .build();

        mailService.sendBookingConfirmationMail(mailEvent);
    }
}
