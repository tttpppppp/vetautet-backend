package com.vetautet.application.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class TripJsonCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> keyLocks = new ConcurrentHashMap<>();

    public TripJsonCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String get(String key, Duration redisTtl, Duration localTtl, Supplier<String> loader) {
        CacheEntry localEntry = localCache.get(key);
        long now = System.currentTimeMillis();
        if (localEntry != null && localEntry.expiresAtMillis() > now) {
            return localEntry.value();
        }

        Object lock = keyLocks.computeIfAbsent(key, ignored -> new Object());
        synchronized (lock) {
            now = System.currentTimeMillis();
            localEntry = localCache.get(key);
            if (localEntry != null && localEntry.expiresAtMillis() > now) {
                return localEntry.value();
            }

            String redisValue = stringRedisTemplate.opsForValue().get(key);
            if (redisValue != null) {
                putLocal(key, redisValue, localTtl, now);
                return redisValue;
            }

            String value = loader.get();
            stringRedisTemplate.opsForValue().set(key, value, redisTtl);
            putLocal(key, value, localTtl, now);
            return value;
        }
    }

    public void evict(String key) {
        localCache.remove(key);
        stringRedisTemplate.delete(key);
    }

    public void evictByPrefix(String keyPrefix) {
        localCache.keySet().removeIf(key -> key.startsWith(keyPrefix));
        keyLocks.keySet().removeIf(key -> key.startsWith(keyPrefix));

        Set<String> redisKeys = stringRedisTemplate.keys(keyPrefix + "*");
        if (redisKeys != null && !redisKeys.isEmpty()) {
            stringRedisTemplate.delete(redisKeys);
        }
    }

    private void putLocal(String key, String value, Duration ttl, long now) {
        localCache.put(key, new CacheEntry(value, now + ttl.toMillis()));
    }

    private record CacheEntry(String value, long expiresAtMillis) {
    }
}
