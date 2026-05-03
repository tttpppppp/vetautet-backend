package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.Booking;
import com.vetautet.domain.repository.BookingRepository;
import com.vetautet.infrastructure.persistence.entity.BookingEntity;
import com.vetautet.infrastructure.persistence.mapper.PersistenceMapper;
import com.vetautet.infrastructure.persistence.repository.BookingJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class BookingInfrasRepositoryImpl implements BookingRepository {

    @Autowired
    private BookingJpaRepository jpaRepository;

    @Autowired
    private PersistenceMapper mapper;

    @Autowired
    private EntityManager entityManager;

    @Override
    public Booking save(Booking booking) {
        BookingEntity entity = mapper.toEntity(booking);
        // Fix for detached entity
        if (booking.getUser() != null && booking.getUser().getId() != null) {
            entity.setUser(entityManager.getReference(com.vetautet.infrastructure.persistence.entity.UserEntity.class, booking.getUser().getId()));
        }

        // MapStruct might not map the bidirectional relationship correctly if not
        // configured
        if (entity.getDetails() != null) {
            entity.getDetails().forEach(detail -> {
                detail.setBooking(entity);
                if (detail.getTicket() != null && detail.getTicket().getId() != null) {
                    detail.setTicket(entityManager.getReference(com.vetautet.infrastructure.persistence.entity.TicketEntity.class, detail.getTicket().getId()));
                }
            });
        }
        BookingEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Booking> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Booking> findByIdFetched(Long id) {
        return jpaRepository.findByIdFetched(id).map(mapper::toDomain);
    }

    @Override
    public List<Booking> findByUserId(Long userId) {
        return jpaRepository.findByUserIdFetched(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
