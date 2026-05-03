package com.vetautet.domain.gateway;

public interface BookingCacheGateway {
    void removeTripCache();
    void removeBookingCache(Long bookingId);
}
