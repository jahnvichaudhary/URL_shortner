package com.urlshortener.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    public static final String CACHE_URLS       = "urls";
    public static final String CACHE_STATS      = "url-stats";
    public static final String CACHE_ACTIVE_URL = "active-url";


    private ObjectMapper buildRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(
            PropertyAccessor.ALL,
            JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    private RedisCacheConfiguration defaultCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(24))
            .disableCachingNullValues()
            .serializeKeysWith(
                SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer(
                        buildRedisObjectMapper())));
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory) {

        Map<String, RedisCacheConfiguration> cacheConfigs =
            new HashMap<>();

        cacheConfigs.put(CACHE_URLS,
            defaultCacheConfig()
                .entryTtl(Duration.ofHours(24)));

        cacheConfigs.put(CACHE_STATS,
            defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)));

        cacheConfigs.put(CACHE_ACTIVE_URL,
            defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)));

        return RedisCacheManager
            .builder(connectionFactory)
            .cacheDefaults(defaultCacheConfig())
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}