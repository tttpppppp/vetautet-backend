package com.vetautet.controller.resource;

import com.vetautet.application.dto.TicketResponse;
import com.vetautet.application.dto.TicketUpdateRequest;
import com.vetautet.application.service.ticket.TicketAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/tickets")
public class AdminTicketController {

    @Autowired
    private TicketAppService ticketAppService;

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<TicketResponse>> getByTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(ticketAppService.getTicketsByTrip(tripId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> update(@PathVariable Long id, @RequestBody TicketUpdateRequest request) {
        return ResponseEntity.ok(ticketAppService.updateTicket(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketAppService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}
