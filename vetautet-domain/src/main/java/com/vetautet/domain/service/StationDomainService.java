package com.vetautet.domain.service;

import com.vetautet.domain.model.Station;

import java.util.List;

public interface StationDomainService {
    List<Station> getAllStations();
    Station getStationById(Long id);
    Station createStation(Station station);
    Station updateStation(Long id, Station station);
    void deleteStation(Long id);
}
