package com.vetautet.application.service.trip;

import com.vetautet.application.dto.TripFareQuoteResponse;
import com.vetautet.application.dto.TripItineraryResponse;
import com.vetautet.application.dto.TripSegmentPriceResponse;
import com.vetautet.application.dto.TripSegmentPricesUpsertRequest;
import com.vetautet.application.dto.TripStopsUpsertRequest;

import java.util.List;

public interface TripScheduleAppService {
    TripItineraryResponse getItinerary(Long tripId);
    TripFareQuoteResponse quoteFare(Long tripId, Long departureStationId, Long arrivalStationId,
                                    Long carriageTypeId, String passengerType);
    TripItineraryResponse replaceStops(Long tripId, TripStopsUpsertRequest request);
    List<TripSegmentPriceResponse> upsertSegmentPrices(Long tripId, TripSegmentPricesUpsertRequest request);
}
