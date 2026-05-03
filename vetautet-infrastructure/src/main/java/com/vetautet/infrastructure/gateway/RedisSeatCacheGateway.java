package com.vetautet.infrastructure.gateway;

import com.vetautet.domain.gateway.SeatCacheGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisSeatCacheGateway implements SeatCacheGateway {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SEAT_HOLD_KEY_PREFIX = "seat:";

    @Override
    public void deleteSeatHold(Long tripId, Long ticketId) {
        String redisKey = SEAT_HOLD_KEY_PREFIX + tripId + ":" + ticketId;
        redisTemplate.delete(redisKey);
    }
}
