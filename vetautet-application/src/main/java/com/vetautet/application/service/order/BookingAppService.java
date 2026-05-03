package com.vetautet.application.service.order;

import com.vetautet.application.dto.BookingRequest;
import com.vetautet.application.dto.BookingDetailResponse;
import com.vetautet.application.dto.BookingHistoryResponse;
import com.vetautet.application.dto.BookingResponse;

public interface BookingAppService {
    BookingResponse createBooking(BookingRequest request);
    BookingResponse updateBookingDetails(Long bookingId, BookingRequest request);
    BookingResponse confirmPayment(Long bookingId);
    java.util.List<BookingHistoryResponse> getMyBookings(Long userId);
    BookingDetailResponse getMyBookingDetail(Long userId, Long bookingId);
    byte[] generateMyBookingInvoicePdf(Long userId, Long bookingId);
    
    // Admin methods
    java.util.List<BookingResponse> getAllBookings();
    BookingResponse getBookingById(Long id);
    BookingResponse updateBookingStatus(Long id, String status);
    void deleteBooking(Long id);
}
