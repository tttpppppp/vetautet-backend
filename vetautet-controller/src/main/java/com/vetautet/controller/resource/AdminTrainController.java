package com.vetautet.controller.resource;

import com.vetautet.application.dto.TrainRequest;
import com.vetautet.application.dto.TrainResponse;
import com.vetautet.application.service.train.TrainAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/trains")
public class AdminTrainController {

    @Autowired
    private TrainAppService trainAppService;

    @GetMapping
    public ResponseEntity<List<TrainResponse>> getAll() {
        return ResponseEntity.ok(trainAppService.getAllTrains());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(trainAppService.getTrainById(id));
    }

    @PostMapping
    public ResponseEntity<TrainResponse> create(@RequestBody TrainRequest request) {
        return ResponseEntity.ok(trainAppService.createTrain(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrainResponse> update(@PathVariable Long id, @RequestBody TrainRequest request) {
        return ResponseEntity.ok(trainAppService.updateTrain(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        trainAppService.deleteTrain(id);
        return ResponseEntity.noContent().build();
    }
}
