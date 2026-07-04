package com.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Request payload to create a short URL")
public record ShortenRequest(


        @NotBlank(message = "Original URL is required")
        @Size(
            max = 2048,
            message = "URL must not exceed 2048 characters"
        )
        @Pattern(
            regexp = "^https?://.*",
            message = "URL must start with http:// or https://"
        )
        @Schema(
            description = "The long URL to shorten",
            example = "https://www.example.com/very/long/path"
        )
        String originalUrl,


        @Size(
            min = 3,
            max = 50,
            message = "Alias must be between 3 and 50 characters"
        )
        @Pattern(
            regexp = "^[a-zA-Z0-9-]*$",
            message = "Alias can only contain letters, " +
                      "numbers, and hyphens"
        )
        @Schema(
            description = "Optional custom alias",
            example = "my-blog",
            nullable = true
        )
        String customAlias,

        @Min(
            value = 0,
            message = "Expiry days must be 0 or more"
        )
        @Max(
            value = 3650,
            message = "Expiry days cannot exceed 3650"
        )
        @Schema(
            description = "Days until expiry. 0 = never expires",
            example = "30",
            nullable = true
        )
        Integer expiryDays,


        @Size(max = 100)
        @Schema(
            description = "Creator identifier",
            nullable = true
        )
        String createdBy

) {}