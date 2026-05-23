package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.NOT_FOUND)
public class UrlNotFoundException extends RuntimeException {

    private final String shortCode;

    public UrlNotFoundException(String shortCode) {
        super("Short URL not found for code: '" +
              shortCode + "'");
        this.shortCode = shortCode;
    }

    public String getShortCode() {
        return shortCode;
    }
}