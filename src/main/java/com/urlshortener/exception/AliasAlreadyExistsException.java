package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.CONFLICT)
public class AliasAlreadyExistsException
        extends RuntimeException {

    private final String alias;

    public AliasAlreadyExistsException(String alias) {
        super("Custom alias '" + alias +
              "' is already in use");
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }
}