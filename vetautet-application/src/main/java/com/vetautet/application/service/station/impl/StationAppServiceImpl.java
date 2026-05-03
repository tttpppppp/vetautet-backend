package com.vetautet.application.service.station.impl;

import com.vetautet.application.dto.StationRequest;
import com.vetautet.application.dto.StationResponse;
import com.vetautet.application.mapper.UserMapper;
import com.vetautet.application.service.station.StationAppService;
import com.vetautet.domain.model.Station;
import com.vetautet.domain.service.StationDomainService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StationAppServiceImpl implements StationAppService {

    private final StationDomainService stationDomainService;
    private final UserMapper mapper;

    public StationAppServiceImpl(StationDomainService stationDomainService, UserMapper mapper) {
        this.stationDomainService = stationDomainService;
        this.mapper = mapper;
    }

    @Override
    public List<StationResponse> getAllStations() {
        return stationDomainService.getAllStations().stream()
                .map(mapper::toStationResponse)
                .collect(Collectors.toList());
    }

    @Override
    public StationResponse getStationById(Long id) {
        return mapper.toStationResponse(stationDomainService.getStationById(id));
    }

    @Override
    public StationResponse createStation(StationRequest request) {
        Station station = mapper.toStation(request);
        return mapper.toStationResponse(stationDomainService.createStation(station));
    }

    @Override
    public StationResponse updateStation(Long id, StationRequest request) {
        Station station = mapper.toStation(request);
        return mapper.toStationResponse(stationDomainService.updateStation(id, station));
    }

    @Override
    public void deleteStation(Long id) {
        stationDomainService.deleteStation(id);
    }
}
