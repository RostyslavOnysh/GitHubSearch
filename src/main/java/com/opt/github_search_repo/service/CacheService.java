package com.opt.github_search_repo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public <T> void putInCache(String key, T data) {
        try {
            String serializedData = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, serializedData);
            log.info("Serialized data for caching: {}", serializedData);
        } catch (JsonProcessingException e) {
            log.error("Error serializing data for caching: {}", e.getMessage());
        }
    }

    public <T> T getFromCache(String key, TypeReference<T> typeReference) {
        try {
            String data = (String) redisTemplate.opsForValue().get(key);
            T result = objectMapper.readValue(data, typeReference);
            log.info("Deserialized data from cache: {}", result);
            return result;
        } catch (Exception e) {
            log.warn("Error deserializing data from cache: {}", e.getMessage());
            return null;
        }
    }
}
