package com.herdcommand.api.api.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ApiError.FieldError(
                        err.getField(),
                        err.getDefaultMessage(),
                        err.getCode()))
                .toList();
        return respond(HttpStatus.BAD_REQUEST, "validation_error", "Request validation failed", request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(
            ConstraintViolationException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "validation_error", "Request validation failed", request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, "unauthorized", "Authentication is required", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException ex, HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "forbidden", "You are not allowed to perform this action", request, List.of());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NoHandlerFoundException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "not_found", "The requested resource was not found", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "An unexpected error occurred", request, List.of());
    }

    private ResponseEntity<ApiError> respond(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<ApiError.FieldError> fields) {
        String correlationId = correlationId(request);
        ApiError body = new ApiError(code, message, correlationId, java.time.Instant.now(), request.getRequestURI(), fields);
        return ResponseEntity.status(status).header(CORRELATION_HEADER, correlationId).body(body);
    }

    private String correlationId(HttpServletRequest request) {
        String existing = request.getHeader(CORRELATION_HEADER);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        String mdc = MDC.get("correlationId");
        if (mdc != null && !mdc.isBlank()) {
            return mdc;
        }
        return UUID.randomUUID().toString();
    }
}
