package com.cloudmedia.util;

public final class ApiCode {

    private ApiCode() {
    }

    public static final int SUCCESS = 0;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int GONE = 410;
    public static final int TOO_MANY_REQUESTS = 429;
    public static final int INTERNAL_ERROR = 500;
    public static final int WORD_CONVERT_FAILED = 600;
    public static final int UNSUPPORTED_FILE_TYPE = 601;
}
