package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.TrainEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainJpaRepository extends JpaRepository<TrainEntity, Long> {
}
