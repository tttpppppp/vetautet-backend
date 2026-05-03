package com.vetautet.domain.repository;

import com.vetautet.domain.model.Station;
import java.util.List;
import java.util.Optional;

public interface StationRepository {
    List<Station> findAll();
    Optional<Station> findById(Long id);
    Station save(Station station);
    void deleteById(Long id);
}
