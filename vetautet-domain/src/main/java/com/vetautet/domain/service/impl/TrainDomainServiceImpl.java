package com.vetautet.domain.service.impl;

import com.vetautet.domain.model.Train;
import com.vetautet.domain.repository.TrainRepository;
import com.vetautet.domain.service.TrainDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainDomainServiceImpl implements TrainDomainService {

    @Autowired
    private TrainRepository trainRepository;

    @Override
    public List<Train> getAllTrains() {
        return trainRepository.findAll();
    }

    @Override
    public Train getTrainById(Long id) {
        return trainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Train not found"));
    }

    @Override
    public Train createTrain(Train train) {
        applyDefaultCategory(train);
        return trainRepository.save(train);
    }

    @Override
    public Train updateTrain(Long id, Train train) {
        Train existing = getTrainById(id);
        existing.setCode(train.getCode());
        existing.setCategory(normalizeCategory(train.getCategory()));
        existing.setDescription(train.getDescription());
        return trainRepository.save(existing);
    }

    @Override
    public void deleteTrain(Long id) {
        trainRepository.deleteById(id);
    }

    private void applyDefaultCategory(Train train) {
        train.setCategory(normalizeCategory(train.getCategory()));
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "SE_TN";
        }
        return category.trim().toUpperCase();
    }
}
