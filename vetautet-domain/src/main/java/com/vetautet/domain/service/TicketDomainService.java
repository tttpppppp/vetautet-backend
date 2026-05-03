package com.vetautet.domain.service;

import com.vetautet.domain.model.Ticket;

import java.math.BigDecimal;
import java.util.List;

public interface TicketDomainService {
    List<Ticket> getTicketsByTrip(Long tripId);
    List<Ticket> getTicketsByUser(Long userId);
    List<Ticket> getTicketsByIds(List<Long> ids);
    long countTicketsByUser(Long userId);
    Ticket saveTicket(Ticket ticket);
    void saveTickets(List<Ticket> tickets);
    Ticket updateTicket(Long id, BigDecimal price, String status);
    void releaseExpiredTickets();
    void deleteTicket(Long id);
}
