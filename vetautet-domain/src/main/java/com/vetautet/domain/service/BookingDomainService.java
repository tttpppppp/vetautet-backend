package com.vetautet.domain.service;

import com.vetautet.domain.model.Booking;

import java.util.List;

public interface BookingDomainService {
    Booking saveBooking(Booking booking);
    Booking getBookingById(Long bookingId);
    Booking getBookingByOrderNumber(String orderNumber);
    Booking findBookingByAsyncRequestIdOrNull(String asyncRequestId);
    Booking getBookingByIdFetched(Long bookingId);
    Booking findBookingByIdOrNull(Long bookingId);
    List<Booking> getBookingsByUserId(Long userId);
    List<Booking> getAllBookings();
    void cancelBooking(Long bookingId);
    void deleteBooking(Long bookingId);
}
