package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.GONE)
public class UrlInactiveException extends RuntimeException {

    public UrlInactiveException(String shortCode) {
        super("Short URL '" + shortCode +
              "' has been deactivated");
    }
}