package com.cloudmedia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String mode = "PUBLIC";
    private int guestTtlMinutes = 20;
    private long guestCleanupIntervalMs = 60000L;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getGuestTtlMinutes() {
        return guestTtlMinutes;
    }

    public void setGuestTtlMinutes(int guestTtlMinutes) {
        this.guestTtlMinutes = guestTtlMinutes;
    }

    public long getGuestCleanupIntervalMs() {
        return guestCleanupIntervalMs;
    }

    public void setGuestCleanupIntervalMs(long guestCleanupIntervalMs) {
        this.guestCleanupIntervalMs = guestCleanupIntervalMs;
    }

    public boolean isPublicMode() {
        return "PUBLIC".equalsIgnoreCase(mode);
    }
}
