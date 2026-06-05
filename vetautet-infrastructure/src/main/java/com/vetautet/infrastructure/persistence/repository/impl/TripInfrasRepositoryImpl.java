package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.Trip;
import com.vetautet.domain.repository.TripRepository;
import com.vetautet.infrastructure.persistence.mapper.PersistenceMapper;
import com.vetautet.infrastructure.persistence.repository.TripJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
}
