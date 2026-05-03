package com.vetautet.controller.resource;

import com.vetautet.application.dto.TripCreateRequest;
import com.vetautet.application.dto.TripResponse;
import com.vetautet.application.service.trip.TripAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/trips")
public class AdminTripController {

    @Autowired
    private TripAppService tripAppService;

    @GetMapping
    public ResponseEntity<List<TripResponse>> getAll() {
        return ResponseEntity.ok(tripAppService.getAllTrips());
    }

    @PostMapping
    public ResponseEntity<TripResponse> create(@RequestBody TripCreateRequest request) {
        return ResponseEntity.ok(tripAppService.createTrip(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripResponse> update(@PathVariable Long id, @RequestBody TripCreateRequest request) {
        return ResponseEntity.ok(tripAppService.updateTrip(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tripAppService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }
}
