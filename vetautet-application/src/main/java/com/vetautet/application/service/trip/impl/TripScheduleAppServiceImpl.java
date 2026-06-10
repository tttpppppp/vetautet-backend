package com.vetautet.application.service.trip.impl;

import com.vetautet.application.dto.*;
import com.vetautet.domain.model.PassengerFareRule;
import com.vetautet.application.service.trip.TripScheduleAppService;
import com.vetautet.domain.model.Station;
import com.vetautet.domain.model.Trip;
import com.vetautet.domain.model.TripSegment;
import com.vetautet.domain.model.TripSegmentPrice;
import com.vetautet.domain.model.TripStop;
import com.vetautet.domain.service.PassengerFareRuleDomainService;
import com.vetautet.domain.service.TripDomainService;
import com.vetautet.domain.service.TripScheduleDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TripScheduleAppServiceImpl implements TripScheduleAppService {

    @Autowired
    private TripDomainService tripDomainService;

    @Autowired
    private TripScheduleDomainService tripScheduleDomainService;

    @Autowired
    private PassengerFareRuleDomainService passengerFareRuleDomainService;

    @Override
    public TripItineraryResponse getItinerary(Long tripId) {
        Trip trip = tripDomainService.getTripById(tripId);
        List<TripStop> stops = tripScheduleDomainService.getStops(tripId);
        List<TripSegment> segments = tripScheduleDomainService.getSegments(tripId);
        List<TripSegmentPrice> prices = tripScheduleDomainService.getPrices(tripId);
        Map<Long, Long> availableSeatsBySegment = tripScheduleDomainService.countAvailableSeatsBySegment(tripId);
        return toItineraryResponse(trip, stops, segments, prices, availableSeatsBySegment);
    }

    @Override
    public TripFareQuoteResponse quoteFare(Long tripId,
                                           Long departureStationId,
                                           Long arrivalStationId,
                                           Long carriageTypeId,
                                           String passengerType) {
        if (carriageTypeId == null) {
            throw new RuntimeException("Carriage type id is required to quote fare");
        }

        Trip trip = tripDomainService.getTripById(tripId);
        List<TripStop> stops = tripScheduleDomainService.getStops(tripId);
        List<TripSegment> segments = tripScheduleDomainService.getSegments(tripId);
        TripStop departureStop = findStopByStation(stops, departureStationId, "Departure station is not in this trip");
        TripStop arrivalStop = findStopByStation(stops, arrivalStationId, "Arrival station is not in this trip");

        if (departureStop.getStopOrder() >= arrivalStop.getStopOrder()) {
            throw new RuntimeException("Arrival station must be after departure station in trip schedule");
        }

        List<TripSegment> selectedSegments = segments.stream()
                .filter(segment -> segment.getSegmentOrder() >= departureStop.getStopOrder())
                .filter(segment -> segment.getSegmentOrder() < arrivalStop.getStopOrder())
                .sorted(Comparator.comparing(TripSegment::getSegmentOrder))
                .collect(Collectors.toList());

        if (selectedSegments.isEmpty()) {
            throw new RuntimeException("No segment found for selected route");
        }

        List<Long> segmentIds = selectedSegments.stream().map(TripSegment::getId).collect(Collectors.toList());
        String normalizedPassengerType = normalizePassengerType(passengerType);
        List<TripSegmentPrice> basePrices = tripScheduleDomainService.getPricesForSegments(
                segmentIds,
                carriageTypeId,
                "ADULT"
        );
        boolean usesAdultBase = true;
        if (basePrices.size() != selectedSegments.size() && !"ADULT".equals(normalizedPassengerType)) {
            basePrices = tripScheduleDomainService.getPricesForSegments(
                    segmentIds,
                    carriageTypeId,
                    normalizedPassengerType
            );
            usesAdultBase = false;
        }

        if (basePrices.size() != selectedSegments.size()) {
            throw new RuntimeException("Fare table is not configured for all selected segments");
        }

        List<TripSegmentPrice> prices = usesAdultBase
                ? applyPassengerFareRule(basePrices, normalizedPassengerType)
                : basePrices;
        BigDecimal totalPrice = prices.stream()
                .map(TripSegmentPrice::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        TripSegmentPrice representativePrice = prices.get(0);
        long availableSeats = tripScheduleDomainService.countAvailableSeatsForSegments(tripId, segmentIds, carriageTypeId);
        Map<Long, Long> availableSeatsBySegment = tripScheduleDomainService.countAvailableSeatsBySegment(tripId);
        Map<Long, List<TripSegmentPrice>> pricesBySegment = prices.stream()
                .collect(Collectors.groupingBy(TripSegmentPrice::getSegmentId));

        return TripFareQuoteResponse.builder()
                .tripId(trip.getId())
                .departureStationId(departureStationId)
                .departureStationName(stationName(departureStop.getStation()))
                .arrivalStationId(arrivalStationId)
                .arrivalStationName(stationName(arrivalStop.getStation()))
                .carriageTypeId(carriageTypeId)
                .carriageTypeCode(representativePrice.getCarriageTypeCode())
                .carriageTypeName(representativePrice.getCarriageTypeName())
                .passengerType(normalizedPassengerType)
                .totalPrice(totalPrice)
                .currency(representativePrice.getCurrency())
                .availableSeats(availableSeats)
                .segmentIds(segmentIds)
                .segments(selectedSegments.stream()
                        .map(segment -> toSegmentResponse(segment, pricesBySegment, availableSeatsBySegment))
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public TripItineraryResponse replaceStops(Long tripId, TripStopsUpsertRequest request) {
        List<TripStop> stops = request == null || request.getStops() == null
                ? List.of()
                : request.getStops().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
        tripScheduleDomainService.replaceStops(tripId, stops);
        return getItinerary(tripId);
    }

    @Override
    @Transactional
    public List<TripSegmentPriceResponse> upsertSegmentPrices(Long tripId, TripSegmentPricesUpsertRequest request) {
        List<TripSegmentPrice> prices = request == null || request.getPrices() == null
                ? List.of()
                : request.getPrices().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
        return tripScheduleDomainService.upsertSegmentPrices(tripId, prices).stream()
                .map(this::toPriceResponse)
                .collect(Collectors.toList());
    }

    private TripItineraryResponse toItineraryResponse(Trip trip,
                                                      List<TripStop> stops,
                                                      List<TripSegment> segments,
                                                      List<TripSegmentPrice> prices,
                                                      Map<Long, Long> availableSeatsBySegment) {
        Map<Long, List<TripSegmentPrice>> pricesBySegment = prices.stream()
                .collect(Collectors.groupingBy(TripSegmentPrice::getSegmentId));

        return TripItineraryResponse.builder()
                .tripId(trip.getId())
                .trainCode(trip.getTrain() != null ? trip.getTrain().getCode() : null)
                .trainCategory(trip.getTrain() != null ? trip.getTrain().getCategory() : null)
                .serviceDate(resolveServiceDate(trip, stops))
                .originStationId(trip.getDepartureStation() != null ? trip.getDepartureStation().getId() : null)
                .originStationName(stationName(trip.getDepartureStation()))
                .destinationStationId(trip.getArrivalStation() != null ? trip.getArrivalStation().getId() : null)
                .destinationStationName(stationName(trip.getArrivalStation()))
                .scheduledDepartureTime(trip.getDepartureTime())
                .scheduledArrivalTime(trip.getArrivalTime())
                .status(trip.getStatus())
                .stops(stops.stream().map(this::toStopResponse).collect(Collectors.toList()))
                .segments(segments.stream()
                        .map(segment -> toSegmentResponse(segment, pricesBySegment, availableSeatsBySegment))
                        .collect(Collectors.toList()))
                .build();
    }

    private TripStop toDomain(TripStopRequest request) {
        return TripStop.builder()
                .station(Station.builder().id(request.getStationId()).build())
                .stopOrder(request.getStopOrder())
                .scheduledArrivalTime(request.getScheduledArrivalTime())
                .scheduledDepartureTime(request.getScheduledDepartureTime())
                .estimatedArrivalTime(request.getEstimatedArrivalTime())
                .estimatedDepartureTime(request.getEstimatedDepartureTime())
                .actualArrivalTime(request.getActualArrivalTime())
                .actualDepartureTime(request.getActualDepartureTime())
                .distanceFromOriginKm(request.getDistanceFromOriginKm())
                .status(request.getStatus())
                .platform(request.getPlatform())
                .note(request.getNote())
                .build();
    }

    private TripSegmentPrice toDomain(TripSegmentPriceRequest request) {
        return TripSegmentPrice.builder()
                .segmentId(request.getSegmentId())
                .carriageTypeId(request.getCarriageTypeId())
                .passengerType(request.getPassengerType())
                .price(request.getPrice())
                .currency(request.getCurrency())
                .status(request.getStatus())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .build();
    }

    private TripStopResponse toStopResponse(TripStop stop) {
        Station station = stop.getStation();
        return TripStopResponse.builder()
                .id(stop.getId())
                .stationId(station != null ? station.getId() : null)
                .stationCode(station != null ? station.getCode() : null)
                .stationName(stationName(station))
                .stopOrder(stop.getStopOrder())
                .scheduledArrivalTime(stop.getScheduledArrivalTime())
                .scheduledDepartureTime(stop.getScheduledDepartureTime())
                .estimatedArrivalTime(stop.getEstimatedArrivalTime())
                .estimatedDepartureTime(stop.getEstimatedDepartureTime())
                .actualArrivalTime(stop.getActualArrivalTime())
                .actualDepartureTime(stop.getActualDepartureTime())
                .distanceFromOriginKm(stop.getDistanceFromOriginKm())
                .status(stop.getStatus())
                .platform(stop.getPlatform())
                .note(stop.getNote())
                .build();
    }

    private TripSegmentResponse toSegmentResponse(TripSegment segment,
                                                  Map<Long, List<TripSegmentPrice>> pricesBySegment,
                                                  Map<Long, Long> availableSeatsBySegment) {
        TripStop fromStop = segment.getFromStop();
        TripStop toStop = segment.getToStop();
        Station fromStation = fromStop != null ? fromStop.getStation() : null;
        Station toStation = toStop != null ? toStop.getStation() : null;

        return TripSegmentResponse.builder()
                .id(segment.getId())
                .segmentOrder(segment.getSegmentOrder())
                .fromStopId(fromStop != null ? fromStop.getId() : null)
                .toStopId(toStop != null ? toStop.getId() : null)
                .fromStationId(fromStation != null ? fromStation.getId() : null)
                .fromStationCode(fromStation != null ? fromStation.getCode() : null)
                .fromStationName(stationName(fromStation))
                .toStationId(toStation != null ? toStation.getId() : null)
                .toStationCode(toStation != null ? toStation.getCode() : null)
                .toStationName(stationName(toStation))
                .scheduledDepartureTime(fromStop != null ? fromStop.getScheduledDepartureTime() : null)
                .scheduledArrivalTime(toStop != null ? toStop.getScheduledArrivalTime() : null)
                .distanceKm(segment.getDistanceKm())
                .status(segment.getStatus())
                .availableSeats(availableSeatsBySegment.getOrDefault(segment.getId(), 0L))
                .prices(pricesBySegment.getOrDefault(segment.getId(), List.of()).stream()
                        .map(this::toPriceResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private TripSegmentPriceResponse toPriceResponse(TripSegmentPrice price) {
        return TripSegmentPriceResponse.builder()
                .id(price.getId())
                .segmentId(price.getSegmentId())
                .carriageTypeId(price.getCarriageTypeId())
                .carriageTypeCode(price.getCarriageTypeCode())
                .carriageTypeName(price.getCarriageTypeName())
                .passengerType(price.getPassengerType())
                .price(price.getPrice())
                .currency(price.getCurrency())
                .status(price.getStatus())
                .effectiveFrom(price.getEffectiveFrom())
                .effectiveTo(price.getEffectiveTo())
                .build();
    }

    private TripStop findStopByStation(List<TripStop> stops, Long stationId, String notFoundMessage) {
        if (stationId == null) {
            throw new RuntimeException("Station id is required");
        }
        return stops.stream()
                .filter(stop -> stop.getStation() != null)
                .filter(stop -> stationId.equals(stop.getStation().getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(notFoundMessage));
    }

    private LocalDate resolveServiceDate(Trip trip, List<TripStop> stops) {
        if (trip.getDepartureTime() != null) {
            return trip.getDepartureTime().toLocalDate();
        }
        return stops.stream()
                .map(TripStop::getScheduledDepartureTime)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .map(java.time.LocalDateTime::toLocalDate)
                .orElse(null);
    }

    private String normalizePassengerType(String passengerType) {
        return passengerType == null || passengerType.isBlank()
                ? "ADULT"
                : passengerType.trim().toUpperCase();
    }

    private List<TripSegmentPrice> applyPassengerFareRule(List<TripSegmentPrice> basePrices, String passengerType) {
        return basePrices.stream()
                .map(price -> copyPriceWithPassengerFare(price, passengerType))
                .collect(Collectors.toList());
    }

    private TripSegmentPrice copyPriceWithPassengerFare(TripSegmentPrice price, String passengerType) {
        return TripSegmentPrice.builder()
                .id(price.getId())
                .segmentId(price.getSegmentId())
                .carriageTypeId(price.getCarriageTypeId())
                .carriageTypeCode(price.getCarriageTypeCode())
                .carriageTypeName(price.getCarriageTypeName())
                .passengerType(passengerType)
                .price(applyPassengerFareRule(price.getPrice(), passengerType))
                .currency(price.getCurrency())
                .status(price.getStatus())
                .effectiveFrom(price.getEffectiveFrom())
                .effectiveTo(price.getEffectiveTo())
                .build();
    }

    private BigDecimal applyPassengerFareRule(BigDecimal basePrice, String passengerType) {
        BigDecimal price = basePrice == null ? BigDecimal.ZERO : basePrice;
        if ("ADULT".equals(normalizePassengerType(passengerType))) {
            return price;
        }
        BigDecimal multiplier = passengerFareRuleDomainService.getActiveRule(passengerType)
                .map(PassengerFareRule::getFareMultiplier)
                .filter(Objects::nonNull)
                .orElse(BigDecimal.ONE);
        return price.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
    }

    private String stationName(Station station) {
        return station != null ? station.getName() : null;
    }
}
