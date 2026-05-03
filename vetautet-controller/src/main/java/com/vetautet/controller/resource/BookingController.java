package com.vetautet.controller.resource;

import com.vetautet.application.dto.BookingDetailResponse;
import com.vetautet.application.dto.BookingHistoryResponse;
import com.vetautet.application.dto.BookingRequest;
import com.vetautet.application.dto.BookingResponse;
import com.vetautet.application.service.order.BookingAppService;
import com.vetautet.domain.security.AuthenticatedUser;
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
    public ResponseEntity<List<BookingHistoryResponse>> getMyBookings(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(bookingAppService.getMyBookings(authenticatedUser.getDomainUser().getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingDetailResponse> getMyBookingDetail(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(bookingAppService.getMyBookingDetail(authenticatedUser.getDomainUser().getId(), id));
    }

    @GetMapping("/{id}/invoice.pdf")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<byte[]> downloadMyBookingInvoice(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        byte[] pdf = bookingAppService.generateMyBookingInvoicePdf(
                authenticatedUser.getDomainUser().getId(),
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
    public ResponseEntity<BookingResponse> createBooking(@RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingAppService.createBooking(request));
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
