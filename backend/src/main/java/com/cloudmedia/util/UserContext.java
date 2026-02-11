package com.cloudmedia.util;

public final class UserContext {

    private static final ThreadLocal<JwtUserClaims> CONTEXT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(JwtUserClaims claims) {
        CONTEXT.set(claims);
    }

    public static JwtUserClaims get() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        JwtUserClaims claims = CONTEXT.get();
        return claims == null ? null : claims.getUserId();
    }

    public static String getUsername() {
        JwtUserClaims claims = CONTEXT.get();
        return claims == null ? null : claims.getUsername();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
