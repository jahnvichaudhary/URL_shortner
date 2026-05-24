package com.urlshortener.service;

import com.urlshortener.config.RedisConfig;
import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.entity.UrlMapping;
import com.urlshortener.exception.AliasAlreadyExistsException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlInactiveException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.repository.UrlMappingRepository;
import com.urlshortener.util.Base62Encoder;
import com.urlshortener.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlService {


    private final UrlMappingRepository repository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final Base62Encoder base62Encoder;


    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.default-expiry-days:30}")
    private int defaultExpiryDays;



    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request) {
        log.info("Shortening URL: {} alias={}",
            request.originalUrl(), request.customAlias());


        String shortCode;
        if (request.customAlias() != null
                && !request.customAlias().isBlank()) {


            if (repository.existsByShortCode(
                    request.customAlias())) {
                throw new AliasAlreadyExistsException(
                    request.customAlias());
            }
            shortCode = request.customAlias();

        } else {

            long snowflakeId = snowflakeIdGenerator.nextId();
            shortCode = base62Encoder.encode(snowflakeId);
            log.debug("Generated shortCode={} from id={}",
                shortCode, snowflakeId);
        }


        LocalDateTime expiresAt = null;
        int expiryDays = (request.expiryDays() != null)
            ? request.expiryDays()
            : defaultExpiryDays;

        if (expiryDays > 0) {
            expiresAt = LocalDateTime.now()
                .plusDays(expiryDays);
        }

        long entityId = snowflakeIdGenerator.nextId();

        UrlMapping mapping = UrlMapping.builder()
            .id(entityId)
            .shortCode(shortCode)
            .originalUrl(request.originalUrl())
            .customAlias(request.customAlias())
            .expiresAt(expiresAt)
            .createdBy(request.createdBy())
            .isActive(true)
            .clickCount(0L)
            .build();

        UrlMapping saved = repository.save(mapping);
        log.info("Saved shortCode={} → {}",
            shortCode, request.originalUrl());


        return ShortenResponse.from(saved, baseUrl);
    }



@Cacheable(
    value = RedisConfig.CACHE_URLS,
    key = "#shortCode",
    condition = "!#shortCode.isEmpty()",
    cacheManager = "cacheManager"
)
@Transactional(readOnly = true)
public UrlMapping resolveUrl(String shortCode) {
    log.debug("Cache MISS — querying DB for {}",
        shortCode);

    UrlMapping mapping = repository
        .findByShortCode(shortCode)
        .orElseThrow(() ->
            new UrlNotFoundException(shortCode));

    if (!mapping.getIsActive()) {
        throw new UrlInactiveException(shortCode);
    }
    if (mapping.isExpired()) {
        throw new UrlExpiredException(shortCode);
    }

    return mapping;
}


    @Transactional
    public void recordClick(String shortCode) {
        try {
            repository.incrementClickCount(shortCode);
        } catch (Exception ex) {
            // Click counting is non-critical
            // Never let it break the redirect flow
            log.error("Failed to increment click for {}: {}",
                shortCode, ex.getMessage());
        }
    }




    @Transactional(readOnly = true)
    public ShortenResponse getUrlInfo(String shortCode) {
        UrlMapping mapping = resolveUrl(shortCode);
        return ShortenResponse.from(mapping, baseUrl);
    }



    @Transactional
    @CacheEvict(
        value = RedisConfig.CACHE_URLS,
        key = "#shortCode"
    )
    public void deactivateUrl(String shortCode) {
        UrlMapping mapping = repository
            .findByShortCode(shortCode)
            .orElseThrow(() ->
                new UrlNotFoundException(shortCode));

        mapping.setIsActive(false);
        repository.save(mapping);

        log.info("Deactivated shortCode={}", shortCode);
    }




    @Transactional(readOnly = true)
    public List<ShortenResponse> getUrlsByUser(
            String createdBy) {
        return repository
            .findByCreatedByOrderByCreatedAtDesc(createdBy)
            .stream()
            .map(m -> ShortenResponse.from(m, baseUrl))
            .toList();
    }



    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredUrls() {
        log.info("Running expired URL cleanup...");


        int count = repository.deactivateExpiredUrls(
            LocalDateTime.now());

        if (count > 0) {
            log.info("Deactivated {} expired URL(s)", count);
        }
    }




    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        long totalUrls = repository.count();
        long urlsToday = repository
            .countActiveUrlsCreatedAfter(
                LocalDateTime.now().minusDays(1));
        List<UrlMapping> topUrls = repository
            .findTopByClickCount(5);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUrls", totalUrls);
        stats.put("urlsLast24h", urlsToday);
        stats.put("topUrls", topUrls.stream()
            .map(m -> Map.of(
                "shortCode", m.getShortCode(),
                "clickCount", m.getClickCount()
            )).toList());

        return stats;
    }
    @Cacheable(
    value = "urls",
    key = "#shortCode",
    unless = "#result == null"
)
@Transactional(readOnly = true)
public String findLongUrlByShortCode(String shortCode) {
    return repository.findByShortCode(shortCode)
        .map(UrlMapping::getOriginalUrl)
        .orElse(null);
}
}