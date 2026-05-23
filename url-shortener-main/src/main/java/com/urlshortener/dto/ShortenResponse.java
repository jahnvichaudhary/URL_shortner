package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.urlshortener.entity.UrlMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response with shortened URL details")
public record ShortenResponse(

        @Schema(description = "The 7-character short code",
                example = "aB3xZ9k")
        String shortCode,

        @Schema(description = "Full short URL ready to share",
                example = "http://localhost:8080/r/aB3xZ9k")
        String shortUrl,

        @Schema(description = "The original long URL")
        String originalUrl,

        @Schema(description = "Number of times accessed")
        Long clickCount,

        @Schema(description = "Whether this URL is active")
        Boolean isActive,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Expiry datetime, null = never")
        LocalDateTime expiresAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "When this mapping was created")
        LocalDateTime createdAt,

        @Schema(description = "Creator identifier",
                nullable = true)
        String createdBy

) {

        public static ShortenResponse from(
                        UrlMapping entity,
                        String baseUrl) {
                return new ShortenResponse(
                        entity.getShortCode(),
                        baseUrl + "/r/" + entity.getShortCode(),
                        entity.getOriginalUrl(),
                        entity.getClickCount(),
                        entity.getIsActive(),
                        entity.getExpiresAt(),
                        entity.getCreatedAt(),
                        entity.getCreatedBy()
                );
        }
}