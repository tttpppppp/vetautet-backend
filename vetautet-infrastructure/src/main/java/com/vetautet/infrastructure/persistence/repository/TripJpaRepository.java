package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.TripEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripJpaRepository extends JpaRepository<TripEntity, Long> {
    
    @Query("SELECT DISTINCT t FROM TripEntity t " +
           "JOIN FETCH t.train " +
           "JOIN FETCH t.departureStation " +
           "JOIN FETCH t.arrivalStation " +
           "LEFT JOIN FETCH t.tickets tk " +
           "LEFT JOIN FETCH tk.seat s " +
           "LEFT JOIN FETCH s.carriage c " +
           "LEFT JOIN FETCH c.type " +
           "WHERE t.id = :id AND t.deletedAt IS NULL")
    Optional<TripEntity> findByIdFetched(@org.springframework.data.repository.query.Param("id") Long id);

    @Query("SELECT DISTINCT t FROM TripEntity t " +
           "JOIN FETCH t.train " +
           "JOIN FETCH t.departureStation " +
           "JOIN FETCH t.arrivalStation " +
           "LEFT JOIN FETCH t.tickets " +
           "WHERE t.deletedAt IS NULL ORDER BY t.departureTime ASC")
    List<TripEntity> findAllActive();

    @Query("SELECT DISTINCT t FROM TripEntity t " +
           "LEFT JOIN FETCH t.tickets tk " +
           "WHERE (:departure IS NULL OR t.departureStation.name = :departure) " +
           "AND (:arrival IS NULL OR t.arrivalStation.name = :arrival) " +
           "AND (:startTime IS NULL OR t.departureTime >= :startTime) " +
           "AND (:endTime IS NULL OR t.departureTime <= :endTime) " +
           "AND (:trainTypes IS NULL OR t.train.code IN :trainTypes) " +
           "AND (:trainCategory IS NULL OR t.train.category = :trainCategory) " +
           "AND (:minPrice IS NULL OR tk.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR tk.price <= :maxPrice) " +
           "AND t.deletedAt IS NULL")
    List<TripEntity> searchTripsAdvanced(@org.springframework.data.repository.query.Param("departure") String departure, 
                                        @org.springframework.data.repository.query.Param("arrival") String arrival, 
                                        @org.springframework.data.repository.query.Param("startTime") LocalDateTime startTime, 
                                        @org.springframework.data.repository.query.Param("endTime") LocalDateTime endTime,
                                        @org.springframework.data.repository.query.Param("trainTypes") List<String> trainTypes, 
                                        @org.springframework.data.repository.query.Param("trainCategory") String trainCategory,
                                        @org.springframework.data.repository.query.Param("minPrice") BigDecimal minPrice, 
                                        @org.springframework.data.repository.query.Param("maxPrice") BigDecimal maxPrice);
}
