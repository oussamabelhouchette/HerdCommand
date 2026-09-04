package com.herdcommand.api.api.error;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String code, String messageKey) {
        super(code, HttpStatus.CONFLICT, messageKey);
    }
}
