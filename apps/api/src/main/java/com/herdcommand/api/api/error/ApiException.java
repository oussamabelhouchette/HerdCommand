package com.herdcommand.api.api.error;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final String messageKey;
    private final List<ApiError.FieldError> fieldErrors;

    public ApiException(String code, HttpStatus status, String messageKey) {
        this(code, status, messageKey, List.of());
    }

    public ApiException(String code, HttpStatus status, String messageKey, List<ApiError.FieldError> fieldErrors) {
        super(messageKey);
        this.code = code;
        this.status = status;
        this.messageKey = messageKey;
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public List<ApiError.FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
