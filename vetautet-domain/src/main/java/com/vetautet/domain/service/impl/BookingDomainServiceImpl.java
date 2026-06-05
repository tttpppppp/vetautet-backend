package com.vetautet.domain.service.impl;

import com.vetautet.domain.gateway.BookingCacheGateway;
import com.vetautet.domain.model.Booking;
import com.vetautet.domain.model.BookingDetail;
import com.vetautet.domain.model.Ticket;
import com.vetautet.domain.repository.BookingRepository;
import com.vetautet.domain.repository.TicketRepository;
import com.vetautet.domain.service.BookingDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingDomainServiceImpl implements BookingDomainService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private BookingCacheGateway bookingCacheGateway;

    @Override
    public Booking saveBooking(Booking booking) {
        return bookingRepository.save(booking);
    }

    @Override
    public Booking getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
    }

    @Override
    public Booking getBookingByOrderNumber(String orderNumber) {
        return bookingRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + orderNumber));
    }

    @Override
    public Booking findBookingByAsyncRequestIdOrNull(String asyncRequestId) {
        return bookingRepository.findByAsyncRequestId(asyncRequestId).orElse(null);
    }

    @Override
    public Booking getBookingByIdFetched(Long bookingId) {
        return bookingRepository.findByIdFetched(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
    }

    @Override
    public Booking findBookingByIdOrNull(Long bookingId) {
        return bookingRepository.findById(bookingId).orElse(null);
    }

    @Override
    public List<Booking> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        // 1. Tìm đơn hàng
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        // 2. Chỉ xử lý nếu đơn hàng đang ở trạng thái PENDING
        if (!"PENDING".equals(booking.getStatus())) {
            System.out.println(">>> Booking " + bookingId + " is already " + booking.getStatus() + ". Skip cancellation.");
            return;
        }

        // 3. Cập nhật trạng thái Booking -> CANCELLED
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

        // 4. Cập nhật trạng thái các vé -> AVAILABLE
        List<Ticket> tickets = booking.getDetails().stream()
                .map(BookingDetail::getTicket)
                .peek(ticket -> {
                    ticket.setStatus("AVAILABLE");
                    ticket.setHoldExpiredAt(null);
                })
                .collect(Collectors.toList());
        ticketRepository.saveAll(tickets);

        // 5. Cập nhật Redis Cache (Xóa cache trip để cập nhật trạng thái ghế mới)
        bookingCacheGateway.removeTripCache();
        bookingCacheGateway.removeBookingCache(bookingId);

        System.out.println(">>> Successfully CANCELLED booking " + bookingId + " and released seats.");
    }

    @Override
    public void deleteBooking(Long bookingId) {
        bookingRepository.deleteById(bookingId);
    }
}
