package com.vetautet.application.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class TripJsonCacheService {

    private static final Logger log = LoggerFactory.getLogger(TripJsonCacheService.class);
    private static final int LOCAL_CACHE_INITIAL_CAPACITY = 10;
    private static final int LOCAL_CACHE_CONCURRENCY_LEVEL = 12;
    private static final long LOCAL_CACHE_MAX_SIZE = 10_000;
    private static final long LOCAL_CACHE_EXPIRE_MINUTES = 10;
    private static final String LOCK_PREFIX = "lock:trip-json:";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    @Value("${vetautet.cache.trip.log-local-cache:true}")
    private boolean logLocalCache;

    @Value("${vetautet.cache.trip.lock-wait-seconds:30}")
    private long lockWaitSeconds;

    @Value("${vetautet.cache.trip.lock-lease-seconds:60}")
    private long lockLeaseSeconds;

    private final Cache<String, CacheEntry> localCache = CacheBuilder.newBuilder()
            .initialCapacity(LOCAL_CACHE_INITIAL_CAPACITY)
            .concurrencyLevel(LOCAL_CACHE_CONCURRENCY_LEVEL)
            .maximumSize(LOCAL_CACHE_MAX_SIZE)
            .expireAfterWrite(LOCAL_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .recordStats()
            .build();
    private final Cache<String, ByteCacheEntry> localByteCache = CacheBuilder.newBuilder()
            .initialCapacity(LOCAL_CACHE_INITIAL_CAPACITY)
            .concurrencyLevel(LOCAL_CACHE_CONCURRENCY_LEVEL)
            .maximumSize(LOCAL_CACHE_MAX_SIZE)
            .expireAfterWrite(LOCAL_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .recordStats()
            .build();

    public TripJsonCacheService(StringRedisTemplate stringRedisTemplate, RedissonClient redissonClient) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
    }

    public String get(String key, Duration redisTtl, Duration localTtl, Supplier<String> loader) {
        long now = System.currentTimeMillis();
        CacheEntry localEntry = getFreshLocalEntry(key, now);
        if (localEntry != null) {
            logLocalHit("LOCAL_HIT", key);
            return localEntry.value();
        }

        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        try {
            if (!lock.tryLock(lockWaitSeconds, lockLeaseSeconds, TimeUnit.SECONDS)) {
                String redisValue = stringRedisTemplate.opsForValue().get(key);
                if (redisValue != null) {
                    log.debug("[TRIP JSON CACHE] REDIS_HIT_AFTER_LOCK_TIMEOUT key={}", key);
                    putLocal(key, redisValue, localTtl, System.currentTimeMillis());
                    return redisValue;
                }
                log.warn("[TRIP JSON CACHE] LOCK_TIMEOUT key={} waitSeconds={} -> loading without distributed lock",
                        key, lockWaitSeconds);
                String value = loader.get();
                stringRedisTemplate.opsForValue().set(key, value, redisTtl);
                putLocal(key, value, localTtl, System.currentTimeMillis());
                return value;
            }

            now = System.currentTimeMillis();
            localEntry = getFreshLocalEntry(key, now);
            if (localEntry != null) {
                logLocalHit("LOCAL_HIT_AFTER_LOCK", key);
                return localEntry.value();
            }

            String redisValue = stringRedisTemplate.opsForValue().get(key);
            if (redisValue != null) {
                log.debug("[TRIP JSON CACHE] REDIS_HIT key={}", key);
                putLocal(key, redisValue, localTtl, now);
                return redisValue;
            }

            log.debug("[TRIP JSON CACHE] MISS key={} -> loading from application service", key);
            String value = loader.get();
            stringRedisTemplate.opsForValue().set(key, value, redisTtl);
            putLocal(key, value, localTtl, now);
            log.debug("[TRIP JSON CACHE] STORED key={} redisTtl={} localTtl={}", key, redisTtl, localTtl);
            return value;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring distributed cache lock for key: " + key, e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public byte[] getBytes(String key, Duration redisTtl, Duration localTtl, Supplier<byte[]> loader) {
        long now = System.currentTimeMillis();
        ByteCacheEntry localEntry = getFreshLocalByteEntry(key, now);
        if (localEntry != null) {
            logLocalHit("LOCAL_BYTES_HIT", key, localByteCache.stats());
            return localEntry.value();
        }

        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        try {
            if (!lock.tryLock(lockWaitSeconds, lockLeaseSeconds, TimeUnit.SECONDS)) {
                String redisValue = stringRedisTemplate.opsForValue().get(key);
                if (redisValue != null) {
                    log.info("[TRIP JSON CACHE] REDIS_HIT_AFTER_LOCK_TIMEOUT key={} bytes={}",
                            key, redisValue.getBytes(StandardCharsets.UTF_8).length);
                    byte[] bytes = redisValue.getBytes(StandardCharsets.UTF_8);
                    putLocalBytes(key, bytes, localTtl, System.currentTimeMillis());
                    return bytes;
                }
                log.warn("[TRIP JSON CACHE] LOCK_TIMEOUT key={} waitSeconds={} -> loading without distributed lock",
                        key, lockWaitSeconds);
                byte[] value = loader.get();
                stringRedisTemplate.opsForValue().set(key, new String(value, StandardCharsets.UTF_8), redisTtl);
                putLocalBytes(key, value, localTtl, System.currentTimeMillis());
                return value;
            }

            now = System.currentTimeMillis();
            localEntry = getFreshLocalByteEntry(key, now);
            if (localEntry != null) {
                logLocalHit("LOCAL_BYTES_HIT_AFTER_LOCK", key, localByteCache.stats());
                return localEntry.value();
            }

            String redisValue = stringRedisTemplate.opsForValue().get(key);
            if (redisValue != null) {
                log.info("[TRIP JSON CACHE] REDIS_HIT key={} bytes={}", key, redisValue.getBytes(StandardCharsets.UTF_8).length);
                byte[] bytes = redisValue.getBytes(StandardCharsets.UTF_8);
                putLocalBytes(key, bytes, localTtl, now);
                return bytes;
            }

            log.info("[TRIP JSON CACHE] MISS key={} -> loading from application service", key);
            byte[] value = loader.get();
            stringRedisTemplate.opsForValue().set(key, new String(value, StandardCharsets.UTF_8), redisTtl);
            Boolean redisHasKey = stringRedisTemplate.hasKey(key);
            Long redisTtlSeconds = stringRedisTemplate.getExpire(key);
            putLocalBytes(key, value, localTtl, now);
            log.info("[TRIP JSON CACHE] STORED key={} redisTtl={} localTtl={} bytes={} redisHasKey={} redisTtlSeconds={}",
                    key, redisTtl, localTtl, value.length, redisHasKey, redisTtlSeconds);
            return value;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring distributed cache lock for key: " + key, e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void evict(String key) {
        localCache.invalidate(key);
        localByteCache.invalidate(key);
        stringRedisTemplate.delete(key);
        log.debug("[TRIP JSON CACHE] EVICT key={}", key);
    }

    public void evictByPrefix(String keyPrefix) {
        Set<String> localKeys = localCache.asMap().keySet().stream()
                .filter(key -> key.startsWith(keyPrefix))
                .collect(Collectors.toSet());
        localCache.invalidateAll(localKeys);
        Set<String> localByteKeys = localByteCache.asMap().keySet().stream()
                .filter(key -> key.startsWith(keyPrefix))
                .collect(Collectors.toSet());
        localByteCache.invalidateAll(localByteKeys);

        Set<String> redisKeys = stringRedisTemplate.keys(keyPrefix + "*");
        if (redisKeys != null && !redisKeys.isEmpty()) {
            stringRedisTemplate.delete(redisKeys);
        }
        log.debug("[TRIP JSON CACHE] EVICT_PREFIX prefix={} redisKeyCount={}", keyPrefix, redisKeys == null ? 0 : redisKeys.size());
    }

    private void putLocal(String key, String value, Duration ttl, long now) {
        localCache.put(key, new CacheEntry(value, now + ttl.toMillis()));
    }

    private void putLocalBytes(String key, byte[] value, Duration ttl, long now) {
        localByteCache.put(key, new ByteCacheEntry(value, now + ttl.toMillis()));
    }

    private CacheEntry getFreshLocalEntry(String key, long now) {
        CacheEntry localEntry = localCache.getIfPresent(key);
        if (localEntry == null) {
            return null;
        }
        if (localEntry.expiresAtMillis() > now) {
            return localEntry;
        }
        localCache.invalidate(key);
        log.debug("[TRIP JSON CACHE] LOCAL_EXPIRED key={}", key);
        return null;
    }

    private ByteCacheEntry getFreshLocalByteEntry(String key, long now) {
        ByteCacheEntry localEntry = localByteCache.getIfPresent(key);
        if (localEntry == null) {
            return null;
        }
        if (localEntry.expiresAtMillis() > now) {
            return localEntry;
        }
        localByteCache.invalidate(key);
        log.debug("[TRIP JSON CACHE] LOCAL_BYTES_EXPIRED key={}", key);
        return null;
    }

    private void logLocalHit(String event, String key) {
        logLocalHit(event, key, localCache.stats());
    }

    private void logLocalHit(String event, String key, Object stats) {
        if (logLocalCache) {
            log.info("[TRIP JSON LOCAL CACHE] {} key={} stats={}", event, key, stats);
            return;
        }
        log.debug("[TRIP JSON LOCAL CACHE] {} key={}", event, key);
    }

    private record CacheEntry(String value, long expiresAtMillis) {
    }

    private record ByteCacheEntry(byte[] value, long expiresAtMillis) {
    }
}
