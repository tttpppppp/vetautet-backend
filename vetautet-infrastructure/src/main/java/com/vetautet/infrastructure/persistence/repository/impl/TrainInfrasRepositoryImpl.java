package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.Train;
import com.vetautet.domain.repository.TrainRepository;
import com.vetautet.infrastructure.persistence.mapper.PersistenceMapper;
import com.vetautet.infrastructure.persistence.repository.TrainJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TrainInfrasRepositoryImpl implements TrainRepository {

    @Autowired
    private TrainJpaRepository jpaRepository;

    @Autowired
    private PersistenceMapper mapper;

    @Override
    public List<Train> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Train> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Train save(Train train) {
        var entity = mapper.toEntity(train);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
