package com.vetautet.domain.service.impl;

import com.vetautet.domain.model.TripSegment;
import com.vetautet.domain.model.TripSegmentPrice;
import com.vetautet.domain.model.TripStop;
import com.vetautet.domain.repository.TripRepository;
import com.vetautet.domain.repository.TripScheduleRepository;
import com.vetautet.domain.service.TripScheduleDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TripScheduleDomainServiceImpl implements TripScheduleDomainService {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripScheduleRepository tripScheduleRepository;

    @Override
    public List<TripStop> getStops(Long tripId) {
        ensureTripExists(tripId);
        return tripScheduleRepository.findStopsByTripId(tripId);
    }

    @Override
    public List<TripSegment> getSegments(Long tripId) {
        ensureTripExists(tripId);
        return tripScheduleRepository.findSegmentsByTripId(tripId);
    }

    @Override
    public List<TripSegmentPrice> getPrices(Long tripId) {
        ensureTripExists(tripId);
        return tripScheduleRepository.findPricesByTripId(tripId);
    }

    @Override
    public List<TripSegmentPrice> getPricesForSegments(List<Long> segmentIds, Long carriageTypeId, String passengerType) {
        if (segmentIds == null || segmentIds.isEmpty()) {
            throw new RuntimeException("Segment list is required");
        }
        return tripScheduleRepository.findPricesBySegmentIds(segmentIds, carriageTypeId, passengerType);
    }

    @Override
    public Map<Long, Long> countAvailableSeatsBySegment(Long tripId) {
        ensureTripExists(tripId);
        return tripScheduleRepository.countAvailableSeatsBySegment(tripId);
    }

    @Override
    public long countAvailableSeatsForSegments(Long tripId, List<Long> segmentIds, Long carriageTypeId) {
        ensureTripExists(tripId);
        if (segmentIds == null || segmentIds.isEmpty()) {
            return 0;
        }
        return tripScheduleRepository.countAvailableSeatsForSegments(tripId, segmentIds, carriageTypeId);
    }

    @Override
    public void replaceStops(Long tripId, List<TripStop> stops) {
        ensureTripExists(tripId);
        validateStops(stops);
        tripScheduleRepository.replaceStops(tripId, stops);
    }

    @Override
    public List<TripSegmentPrice> upsertSegmentPrices(Long tripId, List<TripSegmentPrice> prices) {
        ensureTripExists(tripId);
        validatePrices(prices);
        return tripScheduleRepository.upsertSegmentPrices(tripId, prices);
    }

    private void ensureTripExists(Long tripId) {
        if (tripId == null) {
            throw new RuntimeException("Trip id is required");
        }
        tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));
    }

    private void validateStops(List<TripStop> stops) {
        if (stops == null || stops.size() < 2) {
            throw new RuntimeException("Trip schedule must have at least 2 stops");
        }

        Set<Integer> orders = new HashSet<>();
        Set<Long> stationIds = new HashSet<>();
        for (TripStop stop : stops) {
            if (stop.getStation() == null || stop.getStation().getId() == null) {
                throw new RuntimeException("Station id is required for each stop");
            }
            if (stop.getStopOrder() == null || stop.getStopOrder() <= 0) {
                throw new RuntimeException("Stop order must be positive");
            }
            if (!orders.add(stop.getStopOrder())) {
                throw new RuntimeException("Stop order is duplicated: " + stop.getStopOrder());
            }
            if (!stationIds.add(stop.getStation().getId())) {
                throw new RuntimeException("Station is duplicated in this trip: " + stop.getStation().getId());
            }
        }

        for (int order = 1; order <= orders.size(); order++) {
            if (!orders.contains(order)) {
                throw new RuntimeException("Stop order must be continuous from 1");
            }
        }
    }

    private void validatePrices(List<TripSegmentPrice> prices) {
        if (prices == null || prices.isEmpty()) {
            throw new RuntimeException("At least one segment price is required");
        }
        for (TripSegmentPrice price : prices) {
            if (price.getSegmentId() == null) {
                throw new RuntimeException("Segment id is required");
            }
            if (price.getCarriageTypeId() == null) {
                throw new RuntimeException("Carriage type id is required");
            }
            if (price.getPrice() == null || price.getPrice().signum() < 0) {
                throw new RuntimeException("Price must be zero or positive");
            }
        }
    }
}
