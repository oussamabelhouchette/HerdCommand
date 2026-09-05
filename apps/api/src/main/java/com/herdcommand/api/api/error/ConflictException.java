package com.herdcommand.api.api.error;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String code, String messageKey) {
        super(code, HttpStatus.CONFLICT, messageKey);
    }

    public ConflictException(String code, String messageKey, java.util.List<ApiError.FieldError> fieldErrors) {
        super(code, HttpStatus.CONFLICT, messageKey, fieldErrors);
    }
}
