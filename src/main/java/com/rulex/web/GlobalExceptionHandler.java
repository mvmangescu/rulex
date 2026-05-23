package com.rulex.web;

import java.time.Instant;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.rulex.exception.RuleEvaluationException;
import com.rulex.exception.RuleParseException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorResponse(String error, String message, Instant timestamp, String requestId) {}

    @ExceptionHandler(RuleParseException.class)
    public ResponseEntity<ErrorResponse> handleRuleParseException(RuleParseException ex) {
        log.warn("Rule parse error [requestId={}]: {}", requestId(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error("PARSE_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(RuleEvaluationException.class)
    public ResponseEntity<ErrorResponse> handleRuleEvaluationException(RuleEvaluationException ex) {
        log.warn("Rule evaluation error [requestId={}]: {}", requestId(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(error("EVALUATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(NamedRuleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNamedRuleNotFound(NamedRuleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body [requestId={}]: {}", requestId(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error("BAD_REQUEST", "Malformed or missing request body"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation error [requestId={}]: {}", requestId(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error("VALIDATION_ERROR", message));
    }

    // Handles @Validated path variable and method parameter constraint violations
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return field + ": " + v.getMessage();
                })
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation [requestId={}]: {}", requestId(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error [requestId={}]", requestId(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("INTERNAL_ERROR", "An unexpected error occurred. Reference: " + requestId()));
    }

    private ErrorResponse error(String code, String message) {
        return new ErrorResponse(code, message, Instant.now(), requestId());
    }

    private static String requestId() {
        String id = MDC.get(RequestIdFilter.MDC_KEY);
        return id != null ? id : "unknown";
    }
}
