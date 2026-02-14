package com.cloudmedia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String mode = "PUBLIC";

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isPublicMode() {
        return "PUBLIC".equalsIgnoreCase(mode);
    }
}
