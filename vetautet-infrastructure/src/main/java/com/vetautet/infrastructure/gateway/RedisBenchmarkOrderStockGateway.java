package com.vetautet.infrastructure.gateway;

import com.vetautet.domain.gateway.BenchmarkOrderStockGateway;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class RedisBenchmarkOrderStockGateway implements BenchmarkOrderStockGateway {

    private static final String STOCK_KEY_PREFIX = "benchmark:order:stock:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisBenchmarkOrderStockGateway(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void prepareStock(Long ticketRef, long stock) {
        stringRedisTemplate.opsForValue().set(stockKey(ticketRef), String.valueOf(Math.max(stock, 0)));
    }

    @Override
    public long getStock(Long ticketRef) {
        String value = stringRedisTemplate.opsForValue().get(stockKey(ticketRef));
        if (value == null || value.isBlank()) {
            return -1L;
        }
        return Long.parseLong(value);
    }

    @Override
    public void clearStock(Long ticketRef) {
        stringRedisTemplate.delete(stockKey(ticketRef));
    }

    @Override
    public int decreaseStockByLua(Long ticketRef, int quantity) {
        String script =
                "local stock = redis.call('GET', KEYS[1]); " +
                        "if stock == false then return -1 end; " +
                        "stock = tonumber(stock); " +
                        "local qty = tonumber(ARGV[1]); " +
                        "if stock >= qty then " +
                        "  redis.call('SET', KEYS[1], stock - qty); " +
                        "  return 1; " +
                        "end; " +
                        "return 0;";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(stockKey(ticketRef)), String.valueOf(quantity));
        return result == null ? -1 : result.intValue();
    }

    @Override
    public boolean increaseStock(Long ticketRef, int quantity) {
        String script =
                "local stock = redis.call('GET', KEYS[1]); " +
                        "if stock == false then return 0 end; " +
                        "redis.call('SET', KEYS[1], tonumber(stock) + tonumber(ARGV[1])); " +
                        "return 1;";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(stockKey(ticketRef)), String.valueOf(quantity));
        return result != null && result == 1L;
    }

    private String stockKey(Long ticketRef) {
        return STOCK_KEY_PREFIX + ticketRef;
    }
}
