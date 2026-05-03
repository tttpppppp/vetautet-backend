package com.vetautet.controller.resource;

import com.vetautet.application.dto.StationRequest;
import com.vetautet.application.dto.StationResponse;
import com.vetautet.application.service.station.StationAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/stations")
public class AdminStationController {

    @Autowired
    private StationAppService stationAppService;

    @GetMapping
    public ResponseEntity<List<StationResponse>> getAll() {
        return ResponseEntity.ok(stationAppService.getAllStations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stationAppService.getStationById(id));
    }

    @PostMapping
    public ResponseEntity<StationResponse> create(@RequestBody StationRequest request) {
        return ResponseEntity.ok(stationAppService.createStation(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StationResponse> update(@PathVariable Long id, @RequestBody StationRequest request) {
        return ResponseEntity.ok(stationAppService.updateStation(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stationAppService.deleteStation(id);
        return ResponseEntity.noContent().build();
    }
}
