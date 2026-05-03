package com.vetautet.domain.service.impl;

import com.vetautet.domain.model.Station;
import com.vetautet.domain.model.Train;
import com.vetautet.domain.model.Trip;
import com.vetautet.domain.repository.StationRepository;
import com.vetautet.domain.repository.TrainRepository;
import com.vetautet.domain.repository.TripRepository;
import com.vetautet.domain.service.TripDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
public class TripDomainServiceImpl implements TripDomainService {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Override
    public List<Trip> getAllActiveTrips() {
        return tripRepository.findAllActive();
    }

    @Override
    public Trip getTripById(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found"));
    }

    @Override
    public Trip getTripByIdFetched(Long id) {
        return tripRepository.findByIdFetched(id)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + id));
    }

    @Override
    public List<Trip> searchTrips(String departure, String arrival, LocalDate date,
                                  List<String> trainTypes, String trainCategory,
                                  BigDecimal minPrice, BigDecimal maxPrice) {
        return tripRepository.searchTrips(departure, arrival, date, trainTypes, trainCategory, minPrice, maxPrice);
    }

    @Override
    public Trip createTrip(Trip trip, Long departureStationId, Long arrivalStationId, Long trainId) {
        applyTripReferences(trip, departureStationId, arrivalStationId, trainId);
        updateDuration(trip);
        return tripRepository.save(trip);
    }

    @Override
    public Trip updateTrip(Long id, Trip trip, Long departureStationId, Long arrivalStationId, Long trainId) {
        Trip existing = getTripById(id);
        existing.setDepartureTime(trip.getDepartureTime());
        existing.setArrivalTime(trip.getArrivalTime());
        existing.setStatus(trip.getStatus());
        applyTripReferences(existing, departureStationId, arrivalStationId, trainId);
        updateDuration(existing);
        return tripRepository.save(existing);
    }

    @Override
    public void deleteTrip(Long id) {
        tripRepository.deleteById(id);
    }

    private void applyTripReferences(Trip trip, Long departureStationId, Long arrivalStationId, Long trainId) {
        Station departureStation = stationRepository.findById(departureStationId)
                .orElseThrow(() -> new RuntimeException("Departure station not found"));
        Station arrivalStation = stationRepository.findById(arrivalStationId)
                .orElseThrow(() -> new RuntimeException("Arrival station not found"));
        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new RuntimeException("Train not found"));

        trip.setDepartureStation(departureStation);
        trip.setArrivalStation(arrivalStation);
        trip.setTrain(train);
    }

    private void updateDuration(Trip trip) {
        if (trip.getDepartureTime() == null || trip.getArrivalTime() == null) {
            throw new RuntimeException("Trip time is required");
        }
        long duration = Duration.between(trip.getDepartureTime(), trip.getArrivalTime()).toMinutes();
        trip.setDuration((int) duration);
    }
}
