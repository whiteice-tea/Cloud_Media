package com.cloudmedia.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ProgressRequest {

    @NotNull(message = "must not be null")
    @Min(value = 0, message = "must be >= 0")
    @Max(value = 2147483647L, message = "out of int range")
    private Integer progressSeconds;

    public Integer getProgressSeconds() {
        return progressSeconds;
    }

    public void setProgressSeconds(Integer progressSeconds) {
        this.progressSeconds = progressSeconds;
    }
}
