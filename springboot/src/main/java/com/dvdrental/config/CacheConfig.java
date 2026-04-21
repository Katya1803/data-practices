package com.dvdrental.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_FILM             = "film";
    public static final String CACHE_FILM_AVAILABLE   = "film:available";
    public static final String CACHE_REPORT_TOP_FILMS = "report:top-films";
    public static final String CACHE_REPORT_REVENUE   = "report:revenue:monthly";
    public static final String CACHE_REPORT_CUSTOMERS = "report:top-customers";
    public static final String CACHE_REPORT_CATEGORY  = "report:category";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.WRAPPER_ARRAY
        );

        var jsonSerializer = RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer(mapper));

        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(jsonSerializer)
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> perCacheConfig = Map.of(
                CACHE_FILM,             defaultConfig.entryTtl(Duration.ofMinutes(10)),
                CACHE_FILM_AVAILABLE,   defaultConfig.entryTtl(Duration.ofMinutes(2)),
                CACHE_REPORT_TOP_FILMS, defaultConfig.entryTtl(Duration.ofMinutes(30)),
                CACHE_REPORT_REVENUE,   defaultConfig.entryTtl(Duration.ofMinutes(30)),
                CACHE_REPORT_CUSTOMERS, defaultConfig.entryTtl(Duration.ofMinutes(15)),
                CACHE_REPORT_CATEGORY,  defaultConfig.entryTtl(Duration.ofMinutes(5))
        );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }
}
