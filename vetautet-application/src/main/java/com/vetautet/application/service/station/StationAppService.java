package com.vetautet.application.service.station;

import com.vetautet.application.dto.StationRequest;
import com.vetautet.application.dto.StationResponse;
import java.util.List;

public interface StationAppService {
    List<StationResponse> getAllStations();
    StationResponse getStationById(Long id);
    StationResponse createStation(StationRequest request);
    StationResponse updateStation(Long id, StationRequest request);
    void deleteStation(Long id);
}
