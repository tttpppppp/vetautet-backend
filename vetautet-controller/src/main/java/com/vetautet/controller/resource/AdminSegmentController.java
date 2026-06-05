package com.vetautet.controller.resource;

import com.vetautet.application.dto.TripItineraryResponse;
import com.vetautet.application.dto.TripSegmentPriceResponse;
import com.vetautet.application.dto.TripSegmentPricesUpsertRequest;
import com.vetautet.application.dto.TripStopsUpsertRequest;
import com.vetautet.application.service.trip.TripScheduleAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/segments")
@RequiredArgsConstructor
public class AdminSegmentController {

    private final TripScheduleAppService tripScheduleAppService;

    @GetMapping("/trips/{tripId}")
    public ResponseEntity<TripItineraryResponse> getItinerary(@PathVariable Long tripId) {
        return ResponseEntity.ok(tripScheduleAppService.getItinerary(tripId));
    }

    @PutMapping("/trips/{tripId}/stops")
    public ResponseEntity<TripItineraryResponse> replaceStops(
            @PathVariable Long tripId,
            @RequestBody TripStopsUpsertRequest request) {
        return ResponseEntity.ok(tripScheduleAppService.replaceStops(tripId, request));
    }

    @PutMapping("/trips/{tripId}/prices")
    public ResponseEntity<List<TripSegmentPriceResponse>> upsertSegmentPrices(
            @PathVariable Long tripId,
            @RequestBody TripSegmentPricesUpsertRequest request) {
        return ResponseEntity.ok(tripScheduleAppService.upsertSegmentPrices(tripId, request));
    }
}
