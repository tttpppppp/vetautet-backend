package com.vetautet.controller.resource;

import com.vetautet.application.dto.PopularDestinationResponse;
import com.vetautet.application.dto.StationResponse;
import com.vetautet.application.service.station.StationAppService;
import com.vetautet.application.service.trip.TripAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/stations")
public class StationController {

    @Autowired
    private StationAppService stationAppService;

    @Autowired
    private TripAppService tripAppService;

    @GetMapping
    public ResponseEntity<List<StationResponse>> getAllStations() {
        return ResponseEntity.ok(stationAppService.getAllStations());
    }

    @GetMapping("/popular")
    public ResponseEntity<List<PopularDestinationResponse>> getPopularStations(
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        return ResponseEntity.ok(tripAppService.getPopularDestinations(limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StationResponse> getStationById(@PathVariable Long id) {
        return ResponseEntity.ok(stationAppService.getStationById(id));
    }
}
