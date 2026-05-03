package com.vetautet.domain.repository;

import com.vetautet.domain.model.Ticket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    Optional<Ticket> findById(Long id);
    List<Ticket> findByUserId(Long userId);
    List<Ticket> findByTripId(Long tripId);
    Ticket save(Ticket ticket);
    List<Ticket> findAllById(List<Long> ids);
    void saveAll(List<Ticket> tickets);
    List<Ticket> findExpiredHoldTickets(LocalDateTime now);
    void deleteById(Long id);
    long countByUserId(Long userId);
}
