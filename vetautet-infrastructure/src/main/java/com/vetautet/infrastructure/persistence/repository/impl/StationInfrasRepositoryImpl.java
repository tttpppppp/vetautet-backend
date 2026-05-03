package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.Station;
import com.vetautet.domain.repository.StationRepository;
import com.vetautet.infrastructure.persistence.entity.StationEntity;
import com.vetautet.infrastructure.persistence.mapper.PersistenceMapper;
import com.vetautet.infrastructure.persistence.repository.StationJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class StationInfrasRepositoryImpl implements StationRepository {

    @Autowired
    private StationJpaRepository jpaRepository;

    @Autowired
    private PersistenceMapper mapper;

    @Override
    public List<Station> findAll() {
        return jpaRepository.findAllActive().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Station> findById(Long id) {
        return jpaRepository.findById(id)
                .filter(s -> s.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public Station save(Station station) {
        StationEntity entity = mapper.toEntity(station);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.findById(id).ifPresent(s -> {
            s.setDeletedAt(LocalDateTime.now());
            jpaRepository.save(s);
        });
    }
}
