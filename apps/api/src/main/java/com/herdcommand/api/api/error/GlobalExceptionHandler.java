package com.herdcommand.api.api.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ApiError.FieldError(err.getField(), err.getDefaultMessage()))
                .toList();
        return respond(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "error.validation", request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fields = ex.getConstraintViolations().stream()
                .map(violation -> new ApiError.FieldError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();
        return respond(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "error.validation", request, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "error.validation", request, List.of());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
        return respond(ex.getStatus(), ex.getCode(), ex.getMessageKey(), request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "error.unauthorized", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException ex, HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, "error.forbidden", request, List.of());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(Exception ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "error.not_found", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled API error on {}", request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "error.internal", request, List.of());
    }

    private ResponseEntity<ApiError> respond(
            HttpStatus status,
            String code,
            String messageKey,
            HttpServletRequest request,
            List<ApiError.FieldError> fields) {
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(messageKey, null, messageKey, locale);
        ApiError body = ApiError.of(code, message, request.getRequestURI(), fields);
        return ResponseEntity.status(status)
                .header(CORRELATION_HEADER, correlationId(request))
                .body(body);
    }

    private String correlationId(HttpServletRequest request) {
        String existing = request.getHeader(CORRELATION_HEADER);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        String mdc = org.slf4j.MDC.get("correlationId");
        if (mdc != null && !mdc.isBlank()) {
            return mdc;
        }
        return java.util.UUID.randomUUID().toString();
    }
}
