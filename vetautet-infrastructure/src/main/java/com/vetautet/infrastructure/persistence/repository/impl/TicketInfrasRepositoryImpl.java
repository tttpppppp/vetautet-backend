package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.Ticket;
import com.vetautet.domain.repository.TicketRepository;
import com.vetautet.infrastructure.persistence.mapper.PersistenceMapper;
import com.vetautet.infrastructure.persistence.repository.TicketJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TicketInfrasRepositoryImpl implements TicketRepository {

    @Autowired
    private TicketJpaRepository jpaRepository;

    @Autowired
    private PersistenceMapper mapper;

    @Override
    public Optional<Ticket> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Ticket> findByUserId(Long userId) {
        return jpaRepository.findAllByUserId(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Ticket> findByTripId(Long tripId) {
        return jpaRepository.findByTripId(tripId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Ticket save(Ticket ticket) {
        com.vetautet.infrastructure.persistence.entity.TicketEntity entity = jpaRepository.findById(ticket.getId())
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticket.getId()));

        entity.setStatus(
                com.vetautet.infrastructure.persistence.entity.TicketEntity.TicketStatus.valueOf(ticket.getStatus()));
        entity.setHoldExpiredAt(ticket.getHoldExpiredAt());
        entity.setUpdatedAt(LocalDateTime.now());

        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<Ticket> findAllById(List<Long> ids) {
        return jpaRepository.findAllById(ids).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void saveAll(List<Ticket> tickets) {
        List<Long> ids = tickets.stream().map(Ticket::getId).collect(Collectors.toList());
        List<com.vetautet.infrastructure.persistence.entity.TicketEntity> entities = jpaRepository.findAllById(ids);

        for (com.vetautet.infrastructure.persistence.entity.TicketEntity entity : entities) {
            tickets.stream()
                    .filter(t -> t.getId().equals(entity.getId()))
                    .findFirst()
                    .ifPresent(t -> {
                        entity.setStatus(com.vetautet.infrastructure.persistence.entity.TicketEntity.TicketStatus
                                .valueOf(t.getStatus()));
                        entity.setHoldExpiredAt(t.getHoldExpiredAt());
                        entity.setUpdatedAt(LocalDateTime.now());
                    });
        }

        jpaRepository.saveAll(entities);
    }

    @Override
    public List<Ticket> findExpiredHoldTickets(LocalDateTime now) {
        return jpaRepository
                .findByStatusInAndHoldExpiredAtBefore(
                        java.util.List.of(
                                com.vetautet.infrastructure.persistence.entity.TicketEntity.TicketStatus.HOLD,
                                com.vetautet.infrastructure.persistence.entity.TicketEntity.TicketStatus.QUEUED
                        ),
                        now)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public long countByUserId(Long userId) {
        return jpaRepository.countByUserId(userId);
    }
}
