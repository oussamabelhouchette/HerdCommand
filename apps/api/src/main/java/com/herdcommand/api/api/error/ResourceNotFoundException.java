package com.herdcommand.api.api.error;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException() {
        super(ErrorCodes.NOT_FOUND, HttpStatus.NOT_FOUND, "error.not_found");
    }
}
