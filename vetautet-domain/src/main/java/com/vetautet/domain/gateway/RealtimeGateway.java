package com.vetautet.domain.gateway;

public interface RealtimeGateway {
    void broadcastSeatStatus(Long tripId, Long ticketId, String seatNumber, String status);
}
