package com.vetautet.application.service.train.impl;

import com.vetautet.application.dto.TrainRequest;
import com.vetautet.application.dto.TrainResponse;
import com.vetautet.application.mapper.UserMapper;
import com.vetautet.application.service.train.TrainAppService;
import com.vetautet.domain.model.Train;
import com.vetautet.domain.service.TrainDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrainAppServiceImpl implements TrainAppService {

    @Autowired
    private TrainDomainService trainDomainService;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<TrainResponse> getAllTrains() {
        return trainDomainService.getAllTrains().stream()
                .map(userMapper::toTrainResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TrainResponse getTrainById(Long id) {
        return userMapper.toTrainResponse(trainDomainService.getTrainById(id));
    }

    @Override
    public TrainResponse createTrain(TrainRequest request) {
        Train train = userMapper.toTrain(request);
        Train saved = trainDomainService.createTrain(train);
        return userMapper.toTrainResponse(saved);
    }

    @Override
    public TrainResponse updateTrain(Long id, TrainRequest request) {
        Train train = userMapper.toTrain(request);
        Train saved = trainDomainService.updateTrain(id, train);
        return userMapper.toTrainResponse(saved);
    }

    @Override
    public void deleteTrain(Long id) {
        trainDomainService.deleteTrain(id);
    }
}
