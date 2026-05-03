package com.vetautet.application.service.train;

import com.vetautet.application.dto.TrainRequest;
import com.vetautet.application.dto.TrainResponse;
import java.util.List;

public interface TrainAppService {
    List<TrainResponse> getAllTrains();
    TrainResponse getTrainById(Long id);
    TrainResponse createTrain(TrainRequest request);
    TrainResponse updateTrain(Long id, TrainRequest request);
    void deleteTrain(Long id);
}
