package com.vetautet.controller.resource;

import com.vetautet.application.dto.BookingDetailResponse;
import com.vetautet.application.dto.BookingHistoryResponse;
import com.vetautet.application.dto.BookingRequest;
import com.vetautet.application.dto.BookingResponse;
import com.vetautet.application.service.order.BookingAppService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingAppService bookingAppService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<BookingHistoryResponse>> getMyBookings(
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return ResponseEntity.ok(bookingAppService.getMyBookings(userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingDetailResponse> getMyBookingDetail(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return ResponseEntity.ok(bookingAppService.getMyBookingDetail(userId, id));
    }

    @GetMapping("/order/{orderNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingDetailResponse> getMyBookingDetailByOrderNumber(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return ResponseEntity.ok(bookingAppService.getMyBookingDetailByOrderNumber(
                userId,
                orderNumber
        ));
    }

    @GetMapping("/{id}/invoice.pdf")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<byte[]> downloadMyBookingInvoice(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        byte[] pdf = bookingAppService.generateMyBookingInvoicePdf(
                userId,
                id
        );
        String filename = "vetau-booking-" + id + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(pdf);
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @RateLimiter(name = "bookingWrite")
    public ResponseEntity<BookingResponse> createBooking(@RequestBody BookingRequest request,
                                                         @AuthenticationPrincipal(expression = "userId") Long userId) {
        return ResponseEntity.accepted().body(bookingAppService.enqueueCreateBooking(userId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingResponse> updateBookingDetails(@PathVariable("id") Long id, @RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingAppService.updateBookingDetails(id, request));
    }

    @PostMapping("/{id}/confirm-payment")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingResponse> confirmPayment(@PathVariable("id") Long id) {
        return ResponseEntity.ok(bookingAppService.confirmPayment(id));
    }
}
