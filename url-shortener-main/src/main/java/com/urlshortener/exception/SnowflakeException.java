package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SnowflakeException extends RuntimeException {

    public SnowflakeException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}