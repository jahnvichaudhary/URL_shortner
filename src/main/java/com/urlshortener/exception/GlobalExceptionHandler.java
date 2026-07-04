package com.urlshortener.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUrlNotFound(
            UrlNotFoundException ex,
            WebRequest request) {

        log.warn("URL not found: {}", ex.getShortCode());

        ProblemDetail problem = ProblemDetail
            .forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage());

        problem.setTitle("Short URL Not Found");
        problem.setType(URI.create(
            "https://errors.urlshortener.com/not-found"));
        problem.setProperty("shortCode", ex.getShortCode());
        problem.setProperty(
            "timestamp", LocalDateTime.now().toString());

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(problem);
    }


    @ExceptionHandler(UrlExpiredException.class)
    public ResponseEntity<ProblemDetail> handleUrlExpired(
            UrlExpiredException ex,
            WebRequest request) {

        log.info("Expired URL accessed: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail
            .forStatusAndDetail(
                HttpStatus.GONE,
                ex.getMessage());

        problem.setTitle("Short URL Expired");
        problem.setType(URI.create(
            "https://errors.urlshortener.com/expired"));
        problem.setProperty(
            "timestamp", LocalDateTime.now().toString());

        return ResponseEntity
            .status(HttpStatus.GONE)
            .body(problem);
    }

    @ExceptionHandler(UrlInactiveException.class)
    public ResponseEntity<ProblemDetail> handleUrlInactive(
            UrlInactiveException ex,
            WebRequest request) {

        log.info("Inactive URL accessed: {}",
            ex.getMessage());

        ProblemDetail problem = ProblemDetail
            .forStatusAndDetail(
                HttpStatus.GONE,
                ex.getMessage());

        problem.setTitle("Short URL Deactivated");
        problem.setType(URI.create(
            "https://errors.urlshortener.com/deactivated"));
        problem.setProperty(
            "timestamp", LocalDateTime.now().toString());

        return ResponseEntity
            .status(HttpStatus.GONE)
            .body(problem);
    }


    @ExceptionHandler(AliasAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleAliasConflict(
            AliasAlreadyExistsException ex,
            WebRequest request) {

        log.info("Alias conflict: {}", ex.getAlias());

        ProblemDetail problem = ProblemDetail
            .forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage());

        problem.setTitle("Alias Already In Use");
        problem.setType(URI.create(
            "https://errors.urlshortener.com/alias-conflict"));
        problem.setProperty("alias", ex.getAlias());
        problem.setProperty(
            "timestamp", LocalDateTime.now().toString());

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(problem);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>>
            handleValidationErrors(
                    MethodArgumentNotValidException ex,
                    WebRequest request) {


        Map<String, String> fieldErrors = ex
            .getBindingResult()
            .getAllErrors()
            .stream()
            .filter(error -> error instanceof FieldError)
            .map(error -> (FieldError) error)
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null
                    ? fe.getDefaultMessage()
                    : "Invalid value",
                // If two constraints fail on same field
                // join their messages
                (msg1, msg2) -> msg1 + "; " + msg2
            ));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 422);
        body.put("error", "Unprocessable Entity");
        body.put("title", "Validation Failed");
        body.put("fieldErrors", fieldErrors);
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("path", request.getDescription(false)
            .replace("uri=", ""));

        log.debug("Validation failed: {}", fieldErrors);

        return ResponseEntity
            .unprocessableEntity()
            .body(body);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericError(
            Exception ex,
            WebRequest request) {

        log.error("Unhandled exception: {}",
            ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail
            .forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. " +
                "Please try again later.");

        problem.setTitle("Internal Server Error");
        problem.setProperty(
            "timestamp", LocalDateTime.now().toString());



        return ResponseEntity
            .internalServerError()
            .body(problem);
    }
}