package com.herdcommand.api.api.error;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(String code, String messageKey) {
        super(code, HttpStatus.BAD_REQUEST, messageKey);
    }

    public BadRequestException(String code, String messageKey, java.util.List<ApiError.FieldError> fieldErrors) {
        super(code, HttpStatus.BAD_REQUEST, messageKey, fieldErrors);
    }
}
