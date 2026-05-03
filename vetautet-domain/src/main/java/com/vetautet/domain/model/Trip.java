package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip {
    private Long id;
    private Train train;
    private Station departureStation;
    private Station arrivalStation;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer duration;
    private String status;
    private List<Ticket> tickets;
}
