package com.vetautet.domain.repository;

import com.vetautet.domain.model.Train;
import java.util.List;
import java.util.Optional;

public interface TrainRepository {
    List<Train> findAll();
    Optional<Train> findById(Long id);
    Train save(Train train);
    void deleteById(Long id);
}
