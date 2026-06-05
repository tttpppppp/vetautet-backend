package com.vetautet.domain.gateway;

import java.time.Duration;
import java.util.List;

public interface SeatCacheGateway {
    void deleteSeatHold(Long tripId, Long ticketId);
    boolean tryHoldSeatsAtomically(List<String> seatKeys, String ownerValue, Duration ttl);
    boolean hasSeatHold(Long tripId, Long ticketId);
    String buildSeatHoldKey(Long tripId, Long ticketId);
}
