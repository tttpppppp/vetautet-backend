package com.vetautet.domain.service.impl;

import com.vetautet.domain.model.Station;
import com.vetautet.domain.repository.StationRepository;
import com.vetautet.domain.service.StationDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationDomainServiceImpl implements StationDomainService {

    @Autowired
    private StationRepository stationRepository;

    @Override
    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    @Override
    public Station getStationById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Station not found"));
    }

    @Override
    public Station createStation(Station station) {
        return stationRepository.save(station);
    }

    @Override
    public Station updateStation(Long id, Station station) {
        Station existing = getStationById(id);
        existing.setName(station.getName());
        existing.setCode(station.getCode());
        existing.setLocation(station.getLocation());
        return stationRepository.save(existing);
    }

    @Override
    public void deleteStation(Long id) {
        stationRepository.deleteById(id);
    }
}
