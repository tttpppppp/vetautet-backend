package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface TicketJpaRepository extends JpaRepository<TicketEntity, Long> {
    
    @Query("SELECT bd.ticket FROM BookingDetailEntity bd WHERE bd.booking.user.id = :userId")
    List<TicketEntity> findAllByUserId(@Param("userId") Long userId);

    List<TicketEntity> findByTripId(Long tripId);

    List<TicketEntity> findByStatusAndHoldExpiredAtBefore(TicketEntity.TicketStatus status, LocalDateTime now);

    @Query("SELECT COUNT(bd) FROM BookingDetailEntity bd WHERE bd.booking.user.id = :userId AND bd.booking.status = 'CONFIRMED'")
    long countByUserId(@Param("userId") Long userId);
}
