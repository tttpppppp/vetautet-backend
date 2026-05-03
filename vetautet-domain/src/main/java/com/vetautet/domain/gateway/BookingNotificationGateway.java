package com.vetautet.domain.gateway;

import com.vetautet.domain.model.Booking;

public interface BookingNotificationGateway {
    void sendBookingConfirmation(Booking booking);
}
