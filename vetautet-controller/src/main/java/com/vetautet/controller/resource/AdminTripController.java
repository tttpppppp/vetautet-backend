package com.vetautet.controller.resource;

import com.vetautet.application.dto.TripCreateRequest;
import com.vetautet.application.dto.TripResponse;
import com.vetautet.application.dto.TripItineraryResponse;
import com.vetautet.application.dto.TripSegmentPriceResponse;
import com.vetautet.application.dto.TripSegmentPricesUpsertRequest;
import com.vetautet.application.dto.TripStopsUpsertRequest;
import com.vetautet.application.service.trip.TripAppService;
import com.vetautet.application.service.trip.TripScheduleAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/trips")
public class AdminTripController {

    @Autowired
    private TripAppService tripAppService;

    @Autowired
    private TripScheduleAppService tripScheduleAppService;

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

    @PutMapping("/{id}/stops")
    public ResponseEntity<TripItineraryResponse> replaceStops(
            @PathVariable Long id,
            @RequestBody TripStopsUpsertRequest request) {
        return ResponseEntity.ok(tripScheduleAppService.replaceStops(id, request));
    }

    @PutMapping("/{id}/segment-prices")
    public ResponseEntity<List<TripSegmentPriceResponse>> upsertSegmentPrices(
            @PathVariable Long id,
            @RequestBody TripSegmentPricesUpsertRequest request) {
        return ResponseEntity.ok(tripScheduleAppService.upsertSegmentPrices(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tripAppService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }
}
