package com.vetautet.infrastructure.gateway;

import com.vetautet.domain.gateway.SeatCacheGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Component
public class RedisSeatCacheGateway implements SeatCacheGateway {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String SEAT_HOLD_KEY_PREFIX = "seat:";

    @Override
    public void deleteSeatHold(Long tripId, Long ticketId) {
        stringRedisTemplate.delete(buildSeatHoldKey(tripId, ticketId));
    }

    @Override
    public boolean tryHoldSeatsAtomically(List<String> seatKeys, String ownerValue, Duration ttl) {
        if (seatKeys == null || seatKeys.isEmpty()) {
            return true;
        }

        String script = """
                for i = 1, #KEYS do
                  if redis.call('EXISTS', KEYS[i]) == 1 then
                    return 0
                  end
                end

                for i = 1, #KEYS do
                  redis.call('SET', KEYS[i], ARGV[1], 'PX', ARGV[2])
                end

                return 1
                """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = stringRedisTemplate.execute(
                redisScript,
                seatKeys,
                ownerValue,
                String.valueOf(ttl.toMillis())
        );
        return Objects.equals(result, 1L);
    }

    @Override
    public boolean hasSeatHold(Long tripId, Long ticketId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildSeatHoldKey(tripId, ticketId)));
    }

    @Override
    public String buildSeatHoldKey(Long tripId, Long ticketId) {
        return SEAT_HOLD_KEY_PREFIX + tripId + ":" + ticketId;
    }
}
