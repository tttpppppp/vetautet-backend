package com.vetautet.domain.service.impl;

import com.vetautet.domain.gateway.RealtimeGateway;
import com.vetautet.domain.gateway.SeatCacheGateway;
import com.vetautet.domain.model.Ticket;
import com.vetautet.domain.repository.TicketRepository;
import com.vetautet.domain.service.TicketDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketDomainServiceImpl implements TicketDomainService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private SeatCacheGateway seatCacheGateway;

    @Autowired
    private RealtimeGateway realtimeGateway;

    @Override
    public List<Ticket> getTicketsByTrip(Long tripId) {
        return ticketRepository.findByTripId(tripId);
    }

    @Override
    public List<Ticket> getTicketsByUser(Long userId) {
        return ticketRepository.findByUserId(userId);
    }

    @Override
    public List<Ticket> getTicketsByIds(List<Long> ids) {
        return ticketRepository.findAllById(ids);
    }

    @Override
    public long countTicketsByUser(Long userId) {
        return ticketRepository.countByUserId(userId);
    }

    @Override
    public Ticket saveTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    @Override
    public void saveTickets(List<Ticket> tickets) {
        ticketRepository.saveAll(tickets);
    }

    @Override
    public Ticket updateTicket(Long id, BigDecimal price, String status) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (price != null) {
            ticket.setPrice(price);
        }
        if (status != null) {
            ticket.setStatus(status);
        }

        return ticketRepository.save(ticket);
    }

    @Override
    @Transactional
    public void releaseExpiredTickets() {
        LocalDateTime now = LocalDateTime.now();
        List<Ticket> expiredTickets = ticketRepository.findExpiredHoldTickets(now);

        if (expiredTickets.isEmpty()) {
            return;
        }

        System.out.println(">>> [SCHEDULER] Releasing " + expiredTickets.size() + " expired tickets");
        for (Ticket ticket : expiredTickets) {
            ticket.setStatus("AVAILABLE");
            ticket.setHoldExpiredAt(null);

            seatCacheGateway.deleteSeatHold(ticket.getTripId(), ticket.getId());
            realtimeGateway.broadcastSeatStatus(
                    ticket.getTripId(),
                    ticket.getId(),
                    ticket.getSeatNumber(),
                    "AVAILABLE"
            );
        }
        ticketRepository.saveAll(expiredTickets);
    }

    @Override
    public void deleteTicket(Long id) {
        ticketRepository.deleteById(id);
    }
}
