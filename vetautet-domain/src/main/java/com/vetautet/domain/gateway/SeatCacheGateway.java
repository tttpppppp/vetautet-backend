package com.vetautet.domain.gateway;

public interface SeatCacheGateway {
    void deleteSeatHold(Long tripId, Long ticketId);
}
