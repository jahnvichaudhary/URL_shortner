package com.urlshortener;

import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.entity.UrlMapping;
import com.urlshortener.exception.AliasAlreadyExistsException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.repository.UrlMappingRepository;
import com.urlshortener.service.UrlService;
import com.urlshortener.util.Base62Encoder;
import com.urlshortener.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ══════════════════════════════════════════════════════════════
//  SNOWFLAKE ID GENERATOR TESTS
// ══════════════════════════════════════════════════════════════

class SnowflakeIdGeneratorTest {

    private SnowflakeIdGenerator generator;

    @BeforeEach
    void setUp() {
        // Real instance — no mocks needed
        // SnowflakeIdGenerator has no external dependencies
        generator = new SnowflakeIdGenerator(1L, 1L);
    }

    @Test
    @DisplayName("Should generate a positive ID")
    void shouldGeneratePositiveId() {
        long id = generator.nextId();
        assertThat(id).isPositive();
    }

    @Test
    @DisplayName("Should generate unique IDs on consecutive calls")
    void shouldGenerateUniqueIds() {
        long id1 = generator.nextId();
        long id2 = generator.nextId();
        long id3 = generator.nextId();

        assertThat(id1).isNotEqualTo(id2);
        assertThat(id2).isNotEqualTo(id3);
        assertThat(id1).isNotEqualTo(id3);
    }

    @Test
    @DisplayName("Should generate monotonically increasing IDs")
    void shouldGenerateIncreasingIds() {
        long id1 = generator.nextId();
        long id2 = generator.nextId();

        // Each successive ID must be larger
        // Because timestamp component increases
        assertThat(id2).isGreaterThan(id1);
    }

    @Test
    @DisplayName("Should generate 1000 unique IDs without collision")
    void shouldGenerate1000UniqueIds() {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(generator.nextId());
        }
        // If all 1000 are unique the set size is 1000
        // Any duplicate would reduce the size
        assertThat(ids).hasSize(1000);
    }

    @Test
    @DisplayName("Should throw on invalid datacenter ID")
    void shouldThrowOnInvalidDatacenterId() {
        // Max datacenter ID is 31 (5 bits)
        // 32 is illegal
        assertThatThrownBy(() ->
            new SnowflakeIdGenerator(32L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Datacenter ID");
    }

    @Test
    @DisplayName("Should throw on invalid machine ID")
    void shouldThrowOnInvalidMachineId() {
        assertThatThrownBy(() ->
            new SnowflakeIdGenerator(1L, 32L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Machine ID");
    }
}


// ══════════════════════════════════════════════════════════════
//  BASE62 ENCODER TESTS
// ══════════════════════════════════════════════════════════════

class Base62EncoderTest {

    private final Base62Encoder encoder = new Base62Encoder();

    @Test
    @DisplayName("Should encode to exactly 7 characters")
    void shouldEncodeToSevenChars() {
        String code = encoder.encode(
            7_204_620_543_851_921_408L);
        assertThat(code).hasSize(7);
    }

    @Test
    @DisplayName("Should only contain alphanumeric characters")
    void shouldOnlyContainAlphanumericChars() {
        String code = encoder.encode(
            9_876_543_210_123_456L);
        // Regex: only 0-9 a-z A-Z allowed
        assertThat(code).matches("[0-9a-zA-Z]+");
    }

    @Test
    @DisplayName("Same input should always produce same output")
    void shouldBeDeterministic() {
        long id = 1_234_567_890_123_456L;
        String code1 = encoder.encode(id);
        String code2 = encoder.encode(id);
        assertThat(code1).isEqualTo(code2);
    }

    @Test
    @DisplayName("Different inputs should produce different codes")
    void shouldProduceDifferentCodesForDifferentIds() {
        String code1 = encoder.encode(1_000_000_000_000L);
        String code2 = encoder.encode(2_000_000_000_000L);
        assertThat(code1).isNotEqualTo(code2);
    }

    @Test
    @DisplayName("Should throw on zero ID")
    void shouldThrowOnZeroId() {
        assertThatThrownBy(() -> encoder.encode(0L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw on negative ID")
    void shouldThrowOnNegativeId() {
        assertThatThrownBy(() -> encoder.encode(-1L))
            .isInstanceOf(IllegalArgumentException.class);
    }
}


// ══════════════════════════════════════════════════════════════
//  URL SERVICE TESTS
// ══════════════════════════════════════════════════════════════

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlService Tests")
class UrlServiceTest {

    // @Mock creates fake instances
    // Spring does NOT create these — Mockito does
    @Mock
    private UrlMappingRepository repository;

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Mock
    private Base62Encoder base62Encoder;

    // @InjectMocks creates real UrlService
    // and injects the mocks above into it
    @InjectMocks
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        // @Value fields are not injected by Mockito
        // ReflectionTestUtils sets them manually
        // bypassing access modifiers
        ReflectionTestUtils.setField(
            urlService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(
            urlService, "defaultExpiryDays", 30);
    }

    // ── CREATE TESTS ──────────────────────────────────────────────

    @Test
    @DisplayName("Should create short URL with auto-generated code")
    void shouldCreateShortUrl() {
        // ARRANGE — teach mocks what to return
        long fakeId = 7_204_620_543_851_921_408L;
        String fakeCode = "aB3xZ9k";

        when(snowflakeIdGenerator.nextId())
            .thenReturn(fakeId);
        when(base62Encoder.encode(fakeId))
            .thenReturn(fakeCode);

        UrlMapping savedMapping = UrlMapping.builder()
            .id(fakeId)
            .shortCode(fakeCode)
            .originalUrl("https://example.com/long/path")
            .isActive(true)
            .clickCount(0L)
            .build();

        when(repository.save(any(UrlMapping.class)))
            .thenReturn(savedMapping);

        // ACT — call the real method
        ShortenRequest request = new ShortenRequest(
            "https://example.com/long/path",
            null,
            30,
            "user1"
        );
        ShortenResponse response =
            urlService.shortenUrl(request);

        // ASSERT — verify the result
        assertThat(response).isNotNull();
        assertThat(response.shortCode())
            .isEqualTo(fakeCode);
        assertThat(response.shortUrl())
            .isEqualTo("http://localhost:8080/r/" + fakeCode);
        assertThat(response.originalUrl())
            .isEqualTo("https://example.com/long/path");

        // VERIFY — ensure save was called once
        verify(repository, times(1))
            .save(any(UrlMapping.class));
    }

    @Test
    @DisplayName("Should use custom alias when provided")
    void shouldUseCustomAlias() {
        // ARRANGE
        when(repository.existsByShortCode("my-blog"))
            .thenReturn(false); // alias is available

        long fakeId = 1_234_567_890L;
        when(snowflakeIdGenerator.nextId())
            .thenReturn(fakeId);

        UrlMapping savedMapping = UrlMapping.builder()
            .id(fakeId)
            .shortCode("my-blog")
            .originalUrl("https://example.com")
            .isActive(true)
            .clickCount(0L)
            .build();
        when(repository.save(any()))
            .thenReturn(savedMapping);

        // ACT
        ShortenRequest request = new ShortenRequest(
            "https://example.com",
            "my-blog",
            30,
            "user1"
        );
        ShortenResponse response =
            urlService.shortenUrl(request);

        // ASSERT
        assertThat(response.shortCode())
            .isEqualTo("my-blog");
        assertThat(response.shortUrl())
            .contains("my-blog");

        // VERIFY alias was checked
        verify(repository).existsByShortCode("my-blog");

        // VERIFY encoder was NOT called
        // We used the alias not a generated code
        verify(base62Encoder, never()).encode(anyLong());
    }

    @Test
    @DisplayName("Should throw when alias is already taken")
    void shouldThrowWhenAliasTaken() {
        // ARRANGE — alias already exists
        when(repository.existsByShortCode("taken"))
            .thenReturn(true);

        ShortenRequest request = new ShortenRequest(
            "https://example.com",
            "taken",
            30,
            "user1"
        );

        // ASSERT — calling shortenUrl should throw
        assertThatThrownBy(() ->
            urlService.shortenUrl(request))
            .isInstanceOf(AliasAlreadyExistsException.class)
            .hasMessageContaining("taken");

        // VERIFY save was never called
        verify(repository, never()).save(any());
    }

    // ── RESOLVE TESTS ─────────────────────────────────────────────

    @Test
    @DisplayName("Should throw for unknown short code")
    void shouldThrowForUnknownShortCode() {
        // ARRANGE — DB returns empty
        when(repository.findByShortCode("notexist"))
            .thenReturn(Optional.empty());

        // ASSERT
        assertThatThrownBy(() ->
            urlService.resolveUrl("notexist"))
            .isInstanceOf(UrlNotFoundException.class)
            .hasMessageContaining("notexist");
    }

    @Test
    @DisplayName("Should return mapping for valid short code")
    void shouldResolveValidShortCode() {
        // ARRANGE
        UrlMapping mapping = UrlMapping.builder()
            .id(999L)
            .shortCode("valid7c")
            .originalUrl("https://example.com")
            .isActive(true)
            .clickCount(5L)
            .build();

        when(repository.findByShortCode("valid7c"))
            .thenReturn(Optional.of(mapping));

        // ACT
        UrlMapping result =
            urlService.resolveUrl("valid7c");

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getOriginalUrl())
            .isEqualTo("https://example.com");
        assertThat(result.getClickCount())
            .isEqualTo(5L);
    }
}