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
    public static final String GROUP_CODE_ALREADY_EXISTS = "GROUP_CODE_ALREADY_EXISTS";
    public static final String FARM_CODE_ALREADY_EXISTS = "FARM_CODE_ALREADY_EXISTS";
    public static final String FARM_OWNER_REQUIRED = "FARM_OWNER_REQUIRED";
    public static final String FARM_VERSION_CONFLICT = "FARM_VERSION_CONFLICT";
    public static final String OWNER_ALREADY_ASSIGNED = "OWNER_ALREADY_ASSIGNED";
    public static final String FEATURE_NOT_FOUND = "FEATURE_NOT_FOUND";
    public static final String FEATURE_NOT_AVAILABLE = "FEATURE_NOT_AVAILABLE";
    public static final String FEATURE_ASSIGNMENT_INVALID = "FEATURE_ASSIGNMENT_INVALID";

    private ErrorCodes() {}
}
