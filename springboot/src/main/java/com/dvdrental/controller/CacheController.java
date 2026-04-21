package com.dvdrental.controller;

import com.dvdrental.dto.ApiResponse;
import com.dvdrental.service.CacheInspectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheInspectorService cacheInspectorService;

    /** List every cached key across all namespaces with remaining TTL. */
    @GetMapping("/entries")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> entries() {
        return ResponseEntity.ok(ApiResponse.ok(cacheInspectorService.getAllEntries()));
    }

    /**
     * Read consistency probe: returns the film and whether it was served
     * from Redis cache or PostgreSQL.
     */
    @GetMapping("/film/{id}/probe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> probe(@PathVariable int id) {
        return ResponseEntity.ok(ApiResponse.ok(cacheInspectorService.probeFilm(id)));
    }

    /**
     * Write consistency: evict stale Redis entry, reload from PostgreSQL,
     * write back to Redis — ensures cache and DB are in sync.
     */
    @PostMapping("/film/{id}/warm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> warm(@PathVariable int id) {
        return ResponseEntity.ok(ApiResponse.ok(cacheInspectorService.warmFilm(id)));
    }

    /**
     * Evict all keys in a cache namespace  → DELETE /api/cache/evict?cacheName=film
     * Evict one specific Redis key         → DELETE /api/cache/evict?key=film::1
     */
    @DeleteMapping("/evict")
    public ResponseEntity<ApiResponse<Map<String, Object>>> evict(
            @RequestParam(required = false) String cacheName,
            @RequestParam(required = false) String key) {

        if (key != null) {
            boolean deleted = cacheInspectorService.evictEntry(key);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("deleted", deleted ? 1 : 0, "key", key)));
        }
        if (cacheName != null) {
            int count = cacheInspectorService.evictCache(cacheName);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("deleted", count, "cacheName", cacheName)));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Provide cacheName or key"));
    }
}
