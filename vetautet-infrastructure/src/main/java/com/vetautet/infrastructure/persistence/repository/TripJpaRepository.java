package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.TripEntity;
import com.vetautet.infrastructure.persistence.projection.DestinationSummaryRow;
import com.vetautet.infrastructure.persistence.projection.RouteSummaryRow;
import com.vetautet.infrastructure.persistence.projection.TripSummaryRow;
import org.springframework.data.domain.Pageable;
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

    @Query(value = """
            SELECT
                t.id AS id,
                tr.code AS trainCode,
                tr.category AS trainCategory,
                ds.name AS departureStation,
                ars.name AS arrivalStation,
                t.departure_time AS departureTime,
                t.arrival_time AS arrivalTime,
                t.duration AS duration,
                COALESCE(MIN(CASE WHEN tk.status = 'AVAILABLE' THEN tk.price END), MIN(tk.price)) AS minPrice,
                COUNT(CASE WHEN tk.status = 'AVAILABLE' THEN tk.id END) AS availableSeats,
                COUNT(tk.id) AS totalSeats,
                COUNT(CASE WHEN tk.status = 'BOOKED' THEN tk.id END) AS bookedSeats,
                CAST(t.status AS CHAR) AS status
            FROM trips t
            JOIN trains tr ON tr.id = t.train_id
            JOIN stations ds ON ds.id = t.departure_station_id
            JOIN stations ars ON ars.id = t.arrival_station_id
            LEFT JOIN tickets tk ON tk.trip_id = t.id
            WHERE t.deleted_at IS NULL
            GROUP BY t.id, tr.code, tr.category, ds.name, ars.name,
                     t.departure_time, t.arrival_time, t.duration, t.status
            ORDER BY bookedSeats DESC, availableSeats DESC, minPrice ASC, t.departure_time ASC
            """, nativeQuery = true)
    List<TripSummaryRow> findPopularSummaries(Pageable pageable);

    @Query(value = """
            SELECT
                t.id AS id,
                tr.code AS trainCode,
                tr.category AS trainCategory,
                ds.name AS departureStation,
                ars.name AS arrivalStation,
                t.departure_time AS departureTime,
                t.arrival_time AS arrivalTime,
                t.duration AS duration,
                COALESCE(MIN(CASE WHEN tk.status = 'AVAILABLE' THEN tk.price END), MIN(tk.price)) AS minPrice,
                COUNT(CASE WHEN tk.status = 'AVAILABLE' THEN tk.id END) AS availableSeats,
                COUNT(tk.id) AS totalSeats,
                COUNT(CASE WHEN tk.status = 'BOOKED' THEN tk.id END) AS bookedSeats,
                CAST(t.status AS CHAR) AS status
            FROM trips t
            JOIN trains tr ON tr.id = t.train_id
            JOIN stations ds ON ds.id = t.departure_station_id
            JOIN stations ars ON ars.id = t.arrival_station_id
            LEFT JOIN tickets tk ON tk.trip_id = t.id
            WHERE t.deleted_at IS NULL
              AND t.departure_time >= :now
            GROUP BY t.id, tr.code, tr.category, ds.name, ars.name,
                     t.departure_time, t.arrival_time, t.duration, t.status
            ORDER BY t.departure_time ASC
            """, nativeQuery = true)
    List<TripSummaryRow> findUpcomingSummaries(@org.springframework.data.repository.query.Param("now") LocalDateTime now,
                                               Pageable pageable);

    @Query(value = """
            SELECT
                t.id AS id,
                tr.code AS trainCode,
                tr.category AS trainCategory,
                ds.name AS departureStation,
                ars.name AS arrivalStation,
                t.departure_time AS departureTime,
                t.arrival_time AS arrivalTime,
                t.duration AS duration,
                COALESCE(MIN(CASE WHEN tk.status = 'AVAILABLE' THEN tk.price END), MIN(tk.price)) AS minPrice,
                COUNT(CASE WHEN tk.status = 'AVAILABLE' THEN tk.id END) AS availableSeats,
                COUNT(tk.id) AS totalSeats,
                COUNT(CASE WHEN tk.status = 'BOOKED' THEN tk.id END) AS bookedSeats,
                CAST(t.status AS CHAR) AS status
            FROM trips t
            JOIN trains tr ON tr.id = t.train_id
            JOIN stations ds ON ds.id = t.departure_station_id
            JOIN stations ars ON ars.id = t.arrival_station_id
            LEFT JOIN tickets tk ON tk.trip_id = t.id
            WHERE t.deleted_at IS NULL
              AND t.departure_time >= :startTime
              AND t.departure_time <= :endTime
              AND (:station IS NULL
                   OR LOWER(ds.name) LIKE CONCAT('%', :station, '%')
                   OR LOWER(ars.name) LIKE CONCAT('%', :station, '%')
                   OR LOWER(ds.code) LIKE CONCAT('%', :station, '%')
                   OR LOWER(ars.code) LIKE CONCAT('%', :station, '%'))
            GROUP BY t.id, tr.code, tr.category, ds.name, ars.name,
                     t.departure_time, t.arrival_time, t.duration, t.status
            ORDER BY t.departure_time ASC
            """, nativeQuery = true)
    List<TripSummaryRow> findScheduleSummaries(@org.springframework.data.repository.query.Param("startTime") LocalDateTime startTime,
                                               @org.springframework.data.repository.query.Param("endTime") LocalDateTime endTime,
                                               @org.springframework.data.repository.query.Param("station") String station,
                                               Pageable pageable);

    @Query(value = """
            SELECT
                ds.id AS departureStationId,
                ds.name AS departureStation,
                ds.code AS departureStationCode,
                ars.id AS arrivalStationId,
                ars.name AS arrivalStation,
                ars.code AS arrivalStationCode,
                COUNT(DISTINCT t.id) AS tripsCount,
                COUNT(CASE WHEN tk.status = 'AVAILABLE' THEN tk.id END) AS availableSeats,
                COALESCE(MIN(CASE WHEN tk.status = 'AVAILABLE' THEN tk.price END), MIN(tk.price)) AS minPrice,
                MIN(CASE WHEN t.departure_time >= :now THEN t.departure_time END) AS nextDepartureTime,
                GROUP_CONCAT(DISTINCT tr.category ORDER BY tr.category SEPARATOR ',') AS trainCategoriesCsv
            FROM trips t
            JOIN trains tr ON tr.id = t.train_id
            JOIN stations ds ON ds.id = t.departure_station_id
            JOIN stations ars ON ars.id = t.arrival_station_id
            LEFT JOIN tickets tk ON tk.trip_id = t.id
            WHERE t.deleted_at IS NULL
            GROUP BY ds.id, ds.name, ds.code, ars.id, ars.name, ars.code
            ORDER BY tripsCount DESC, minPrice ASC
            """, nativeQuery = true)
    List<RouteSummaryRow> findPopularRouteSummaries(@org.springframework.data.repository.query.Param("now") LocalDateTime now,
                                                    Pageable pageable);

    @Query(value = """
            SELECT
                ars.id AS stationId,
                ars.name AS stationName,
                ars.code AS stationCode,
                ars.location AS location,
                COUNT(DISTINCT t.id) AS tripsCount,
                COUNT(CASE WHEN tk.status = 'AVAILABLE' THEN tk.id END) AS availableSeats,
                COALESCE(MIN(CASE WHEN tk.status = 'AVAILABLE' THEN tk.price END), MIN(tk.price)) AS minPrice,
                MIN(CASE WHEN t.departure_time >= :now THEN t.departure_time END) AS nextDepartureTime
            FROM trips t
            JOIN stations ars ON ars.id = t.arrival_station_id
            LEFT JOIN tickets tk ON tk.trip_id = t.id
            WHERE t.deleted_at IS NULL
            GROUP BY ars.id, ars.name, ars.code, ars.location
            ORDER BY tripsCount DESC, minPrice ASC
            """, nativeQuery = true)
    List<DestinationSummaryRow> findPopularDestinationSummaries(@org.springframework.data.repository.query.Param("now") LocalDateTime now,
                                                                Pageable pageable);

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
