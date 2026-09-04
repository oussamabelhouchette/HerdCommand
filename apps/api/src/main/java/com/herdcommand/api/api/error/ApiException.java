package com.herdcommand.api.api.error;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final String messageKey;

    public ApiException(String code, HttpStatus status, String messageKey) {
        super(messageKey);
        this.code = code;
        this.status = status;
        this.messageKey = messageKey;
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
}
