package com.vetautet.domain.repository;

import com.vetautet.domain.model.TripSegment;
import com.vetautet.domain.model.TripSegmentPrice;
import com.vetautet.domain.model.TripStop;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface TripScheduleRepository {
    List<TripStop> findStopsByTripId(Long tripId);
    List<TripSegment> findSegmentsByTripId(Long tripId);
    List<TripSegmentPrice> findPricesByTripId(Long tripId);
    List<TripSegmentPrice> findPricesBySegmentIds(List<Long> segmentIds, Long carriageTypeId, String passengerType);
    Map<Long, Long> countAvailableSeatsBySegment(Long tripId);
    long countAvailableSeatsForSegments(Long tripId, List<Long> segmentIds, Long carriageTypeId);
    Map<Long, String> findSeatStatusesForSegments(Long tripId, List<Long> segmentIds);
    boolean areSeatSegmentsAvailable(Long tripId, Long seatId, List<Long> segmentIds);
    boolean areSeatSegmentsBookable(Long tripId, Long seatId, List<Long> segmentIds);
    void queueSeatSegments(Long tripId, Long seatId, List<Long> segmentIds, LocalDateTime queueExpiredAt);
    void releaseQueuedSeatSegments(Long tripId, Long seatId, List<Long> segmentIds);
    void holdSeatSegments(Long tripId, Long seatId, List<Long> segmentIds, Long bookingDetailId, LocalDateTime holdExpiredAt);
    void updateSeatSegmentsForBookingDetail(Long bookingDetailId, String status, LocalDateTime holdExpiredAt);
    void replaceStops(Long tripId, List<TripStop> stops);
    List<TripSegmentPrice> upsertSegmentPrices(Long tripId, List<TripSegmentPrice> prices);
}
