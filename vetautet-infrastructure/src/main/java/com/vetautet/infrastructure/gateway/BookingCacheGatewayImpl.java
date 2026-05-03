package com.vetautet.infrastructure.gateway;

import com.vetautet.domain.gateway.BookingCacheGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class BookingCacheGatewayImpl implements BookingCacheGateway {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TRIP_CACHE_KEY = "trips:all";

    @Override
    public void removeTripCache() {
        redisTemplate.delete(TRIP_CACHE_KEY);
        // Có thể mở rộng để xóa chi tiết từng trip nếu cần
    }

    @Override
    public void removeBookingCache(Long bookingId) {
        redisTemplate.delete("booking:" + bookingId);
    }
}
