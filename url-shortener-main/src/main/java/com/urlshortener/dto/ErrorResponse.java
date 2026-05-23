package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(

        int status,
        String error,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        String path

) {
        public static ErrorResponse of(
                        int status,
                        String error,
                        String message,
                        String path) {
                return new ErrorResponse(
                        status,
                        error,
                        message,
                        LocalDateTime.now(),
                        path
                );
        }
}