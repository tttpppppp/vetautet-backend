package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingJpaRepository extends JpaRepository<BookingEntity, Long> {
    java.util.Optional<BookingEntity> findByAsyncRequestId(String asyncRequestId);

    @Query("SELECT b FROM BookingEntity b " +
            "JOIN FETCH b.user u " +
            "WHERE b.orderNumber = :orderNumber")
    java.util.Optional<BookingEntity> findByOrderNumber(@Param("orderNumber") String orderNumber);

    @Query("SELECT DISTINCT b FROM BookingEntity b " +
            "JOIN FETCH b.user u " +
            "LEFT JOIN FETCH b.details d " +
            "LEFT JOIN FETCH d.ticket tk " +
            "LEFT JOIN FETCH tk.trip tr " +
            "LEFT JOIN FETCH tr.train " +
            "LEFT JOIN FETCH tr.departureStation " +
            "LEFT JOIN FETCH tr.arrivalStation " +
            "LEFT JOIN FETCH tk.seat s " +
            "LEFT JOIN FETCH s.carriage c " +
            "LEFT JOIN FETCH c.type " +
            "WHERE b.id = :id")
    java.util.Optional<BookingEntity> findByIdFetched(@Param("id") Long id);

    @Query("SELECT DISTINCT b FROM BookingEntity b " +
            "JOIN FETCH b.user u " +
            "LEFT JOIN FETCH b.details d " +
            "LEFT JOIN FETCH d.ticket tk " +
            "LEFT JOIN FETCH tk.seat s " +
            "LEFT JOIN FETCH s.carriage c " +
            "LEFT JOIN FETCH c.type " +
            "WHERE u.id = :userId " +
            "ORDER BY b.createdAt DESC")
    List<BookingEntity> findByUserIdFetched(@Param("userId") Long userId);

    @Query("SELECT DISTINCT b FROM BookingEntity b " +
            "JOIN FETCH b.user u " +
            "LEFT JOIN FETCH b.details d " +
            "LEFT JOIN FETCH d.ticket tk " +
            "LEFT JOIN FETCH tk.trip tr " +
            "LEFT JOIN FETCH tr.train " +
            "LEFT JOIN FETCH tr.departureStation " +
            "LEFT JOIN FETCH tr.arrivalStation " +
            "LEFT JOIN FETCH tk.seat s " +
            "LEFT JOIN FETCH s.carriage c " +
            "LEFT JOIN FETCH c.type " +
            "WHERE b.id IN :ids")
    List<BookingEntity> findByIdsFetched(@Param("ids") List<Long> ids);

    @Query("SELECT b.id FROM BookingEntity b " +
            "WHERE b.status = :status " +
            "AND b.expiredAt <= :now")
    List<Long> findIdsByStatusAndExpiredAtBefore(@Param("status") BookingEntity.BookingStatus status,
                                                 @Param("now") LocalDateTime now);
}
