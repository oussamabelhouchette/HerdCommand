package com.herdcommand.api.api.error;

public final class ErrorCodes {

    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String BREED_CODE_ALREADY_EXISTS = "BREED_CODE_ALREADY_EXISTS";
    public static final String BREED_INACTIVE = "BREED_INACTIVE";
    public static final String INVALID_COLOR_TOKEN = "INVALID_COLOR_TOKEN";
    public static final String STATUS_REQUIRED = "STATUS_REQUIRED";
    public static final String STATUS_INACTIVE = "STATUS_INACTIVE";

    private ErrorCodes() {}
}
