package com.vetautet.domain.service;

import com.vetautet.domain.model.Train;

import java.util.List;

public interface TrainDomainService {
    List<Train> getAllTrains();
    Train getTrainById(Long id);
    Train createTrain(Train train);
    Train updateTrain(Long id, Train train);
    void deleteTrain(Long id);
}
