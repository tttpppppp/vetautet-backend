package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.DestinationSummary;
import com.vetautet.domain.model.RouteSummary;
import com.vetautet.domain.model.Trip;
import com.vetautet.domain.model.TripSummary;
import com.vetautet.domain.repository.TripRepository;
import com.vetautet.infrastructure.persistence.mapper.PersistenceMapper;
import com.vetautet.infrastructure.persistence.projection.DestinationSummaryRow;
import com.vetautet.infrastructure.persistence.projection.RouteSummaryRow;
import com.vetautet.infrastructure.persistence.projection.TripSummaryRow;
import com.vetautet.infrastructure.persistence.repository.TripJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class TripInfrasRepositoryImpl implements TripRepository {

    private static final Logger log = LoggerFactory.getLogger(TripInfrasRepositoryImpl.class);

    @Autowired
    private TripJpaRepository jpaRepository;

    @Autowired
    private PersistenceMapper mapper;

    @Override
    public List<Trip> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Trip> findAllActive() {
        return jpaRepository.findAllActive().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TripSummary> findPopularSummaries(int limit) {
        return jpaRepository.findPopularSummaries(page(limit)).stream()
                .map(this::toTripSummary)
                .collect(Collectors.toList());
    }

    @Override
    public List<TripSummary> findUpcomingSummaries(int limit) {
        return jpaRepository.findUpcomingSummaries(LocalDateTime.now(), page(limit)).stream()
                .map(this::toTripSummary)
                .collect(Collectors.toList());
    }

    @Override
    public List<TripSummary> findScheduleSummaries(LocalDate date, String station, int limit) {
        LocalDate scheduleDate = date != null ? date : LocalDate.now();
        LocalDateTime startOfDay = scheduleDate.atStartOfDay();
        LocalDateTime endOfDay = scheduleDate.atTime(LocalTime.MAX);
        String stationFilter = station == null || station.isBlank() ? null : station.trim().toLowerCase();

        return jpaRepository.findScheduleSummaries(startOfDay, endOfDay, stationFilter, page(limit)).stream()
                .map(this::toTripSummary)
                .collect(Collectors.toList());
    }

    @Override
    public List<RouteSummary> findPopularRouteSummaries(int limit) {
        return jpaRepository.findPopularRouteSummaries(LocalDateTime.now(), page(limit)).stream()
                .map(this::toRouteSummary)
                .collect(Collectors.toList());
    }

    @Override
    public List<DestinationSummary> findPopularDestinationSummaries(int limit) {
        return jpaRepository.findPopularDestinationSummaries(LocalDateTime.now(), page(limit)).stream()
                .map(this::toDestinationSummary)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Trip> findById(Long id) {
        log.info("[TRIP DB QUERY] findById tripId={}", id);
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Trip> findByIdFetched(Long id) {
        log.info("[TRIP DB QUERY] findByIdFetched tripId={}", id);
        return jpaRepository.findByIdFetched(id).map(mapper::toDomain);
    }

    @Override
    public List<Trip> searchTrips(String departure, String arrival, LocalDate date,
                                  List<String> trainTypes, String trainCategory,
                                  BigDecimal minPrice, BigDecimal maxPrice) {
        LocalDateTime startOfDay = (date != null) ? date.atStartOfDay() : null;
        LocalDateTime endOfDay = (date != null) ? date.atTime(LocalTime.MAX) : null;

        return jpaRepository.searchTripsAdvanced(
                        departure,
                        arrival,
                        startOfDay,
                        endOfDay,
                        trainTypes,
                        trainCategory,
                        minPrice,
                        maxPrice
                ).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Trip save(Trip trip) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(trip)));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.findById(id).ifPresent(trip -> {
            trip.setDeletedAt(LocalDateTime.now());
            jpaRepository.save(trip);
        });
    }

    private PageRequest page(int limit) {
        int size = limit <= 0 ? 6 : Math.min(limit, 12);
        return PageRequest.of(0, size);
    }

    private TripSummary toTripSummary(TripSummaryRow row) {
        return new TripSummary(
                row.getId(),
                row.getTrainCode(),
                row.getTrainCategory(),
                row.getDepartureStation(),
                row.getArrivalStation(),
                row.getDepartureTime(),
                row.getArrivalTime(),
                row.getDuration(),
                row.getMinPrice(),
                row.getAvailableSeats(),
                row.getTotalSeats(),
                row.getBookedSeats(),
                row.getStatus()
        );
    }

    private RouteSummary toRouteSummary(RouteSummaryRow row) {
        return new RouteSummary(
                row.getDepartureStationId(),
                row.getDepartureStation(),
                row.getDepartureStationCode(),
                row.getArrivalStationId(),
                row.getArrivalStation(),
                row.getArrivalStationCode(),
                row.getTripsCount(),
                row.getAvailableSeats(),
                row.getMinPrice(),
                row.getNextDepartureTime(),
                row.getTrainCategoriesCsv()
        );
    }

    private DestinationSummary toDestinationSummary(DestinationSummaryRow row) {
        return new DestinationSummary(
                row.getStationId(),
                row.getStationName(),
                row.getStationCode(),
                row.getLocation(),
                row.getTripsCount(),
                row.getAvailableSeats(),
                row.getMinPrice(),
                row.getNextDepartureTime()
        );
    }
}
