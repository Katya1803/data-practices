package com.dvdrental.service;

import com.dvdrental.config.CacheConfig;
import com.dvdrental.dto.FilmDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CacheInspectorService {

    private static final List<String> CACHE_NAMES = List.of(
            CacheConfig.CACHE_FILM,
            CacheConfig.CACHE_FILM_AVAILABLE,
            CacheConfig.CACHE_REPORT_TOP_FILMS,
            CacheConfig.CACHE_REPORT_REVENUE,
            CacheConfig.CACHE_REPORT_CUSTOMERS,
            CacheConfig.CACHE_REPORT_CATEGORY
    );

    private final StringRedisTemplate redis;
    private final FilmService filmService;

    public List<Map<String, Object>> getAllEntries() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String cacheName : CACHE_NAMES) {
            Set<String> keys = redis.keys(cacheName + "::*");
            if (keys == null) continue;
            for (String redisKey : keys) {
                Long ttl = redis.getExpire(redisKey, TimeUnit.SECONDS);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("cacheName", cacheName);
                entry.put("key", redisKey.substring(cacheName.length() + 2));
                entry.put("redisKey", redisKey);
                entry.put("ttlSeconds", ttl);
                result.add(entry);
            }
        }
        result.sort(Comparator.comparing(e -> (String) e.get("cacheName")));
        return result;
    }

    /**
     * Read-path: check Redis first, fall back to DB if miss.
     * Returns the data source so the caller can observe consistency behavior.
     */
    public Map<String, Object> probeFilm(int id) {
        String redisKey = CacheConfig.CACHE_FILM + "::" + id;
        boolean inCache = Boolean.TRUE.equals(redis.hasKey(redisKey));

        // @Cacheable returns from Redis on hit; queries DB + writes cache on miss
        FilmDetailDto film = filmService.getFilmById(id);

        Long ttl = inCache ? redis.getExpire(redisKey, TimeUnit.SECONDS) : null;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("filmId", id);
        out.put("source", inCache ? "cache" : "database");
        out.put("ttlSeconds", ttl);
        out.put("film", film);
        return out;
    }

    /**
     * Write-path: evict stale entry from Redis, reload from DB, write back to Redis.
     * Demonstrates explicit cache consistency enforcement.
     */
    public Map<String, Object> warmFilm(int id) {
        String redisKey = CacheConfig.CACHE_FILM + "::" + id;

        // Step 1: remove stale or existing cache entry
        redis.delete(redisKey);

        // Step 2: load from DB — @Cacheable writes the result back to Redis
        FilmDetailDto film = filmService.getFilmById(id);

        Long ttl = redis.getExpire(redisKey, TimeUnit.SECONDS);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("filmId", id);
        out.put("film", film);
        out.put("ttlSeconds", ttl);
        out.put("message", "Read from PostgreSQL, written to Redis (TTL " + ttl + "s)");
        return out;
    }

    /** Delete all keys under a cache namespace. */
    public int evictCache(String cacheName) {
        Set<String> keys = redis.keys(cacheName + "::*");
        if (keys == null || keys.isEmpty()) return 0;
        Long n = redis.delete(keys);
        return n != null ? n.intValue() : 0;
    }

    /** Delete a single Redis key (full key string, e.g. "film::1"). */
    public boolean evictEntry(String redisKey) {
        return Boolean.TRUE.equals(redis.delete(redisKey));
    }
}
