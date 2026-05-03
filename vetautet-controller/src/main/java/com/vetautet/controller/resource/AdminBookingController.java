package com.vetautet.controller.resource;

import com.vetautet.application.dto.BookingResponse;
import com.vetautet.application.service.order.BookingAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    @Autowired
    private BookingAppService bookingAppService;

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getAll() {
        return ResponseEntity.ok(bookingAppService.getAllBookings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingAppService.getBookingById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<BookingResponse> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String status = request.get("status");
        return ResponseEntity.ok(bookingAppService.updateBookingStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingAppService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }
}
