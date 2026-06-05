package com.vetautet.domain.service;

import com.vetautet.domain.model.TripSegment;
import com.vetautet.domain.model.TripSegmentPrice;
import com.vetautet.domain.model.TripStop;

import java.util.List;
import java.util.Map;

public interface TripScheduleDomainService {
    List<TripStop> getStops(Long tripId);
    List<TripSegment> getSegments(Long tripId);
    List<TripSegmentPrice> getPrices(Long tripId);
    List<TripSegmentPrice> getPricesForSegments(List<Long> segmentIds, Long carriageTypeId, String passengerType);
    Map<Long, Long> countAvailableSeatsBySegment(Long tripId);
    long countAvailableSeatsForSegments(Long tripId, List<Long> segmentIds, Long carriageTypeId);
    void replaceStops(Long tripId, List<TripStop> stops);
    List<TripSegmentPrice> upsertSegmentPrices(Long tripId, List<TripSegmentPrice> prices);
}
