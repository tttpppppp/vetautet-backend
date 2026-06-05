package com.vetautet.controller.resource;

import com.vetautet.application.dto.TicketQrVerifyRequest;
import com.vetautet.application.dto.TicketQrVerifyResponse;
import com.vetautet.application.service.ticket.TicketQrService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
public class TicketQrController {

    private final TicketQrService ticketQrService;

    public TicketQrController(TicketQrService ticketQrService) {
        this.ticketQrService = ticketQrService;
    }

    @GetMapping(value = "/{ticketId}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<byte[]> getMyTicketQr(
            @PathVariable Long ticketId,
            @RequestParam Long bookingId,
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        byte[] qr = ticketQrService.generateOwnedTicketQrPng(
                userId,
                bookingId,
                ticketId,
                250
        );
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qr);
    }

    @PostMapping("/verify-qr")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<TicketQrVerifyResponse> verifyQr(@Valid @RequestBody TicketQrVerifyRequest request) {
        return ResponseEntity.ok(ticketQrService.verify(request.getQrToken()));
    }
}
