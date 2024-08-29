package com.opt.githubSearchRepo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class CacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public <T> Flux<T> getFromCacheAsFlux(String key, Class<T> type) {
        return Flux.defer(() -> {
            String data = (String) redisTemplate.opsForValue().get(key);
            if (data != null) {
                try {
                    List<T> cachedList = objectMapper.readValue(data, new TypeReference<List<T>>() {});
                    log.info("Successfully retrieved data from cache for key: {}", key);
                    return Flux.fromIterable(cachedList);
                } catch (JsonProcessingException e) {
                    log.error("Error deserializing data from cache for key {}: {}", key, e.getMessage());
                }
            }
            return Flux.empty();
        });
    }

    public <T> void putInCacheAsFlux(String key, List<T> data) {
        if (!redisTemplate.hasKey(key)) {
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data));
                log.info("Successfully cached data for key: {}", key);
            } catch (JsonProcessingException e) {
                log.error("Error serializing data for caching with key {}: {}", key, e.getMessage());
            }
        } else {
            log.info("Key {} already exists in cache. Skipping cache update.", key);
        }
    }

    @Async
    public <T> CompletableFuture<Void> putInCacheAsync(String key, List<T> data) {
        putInCacheAsFlux(key, data);
        return CompletableFuture.completedFuture(null);
    }
}
