package com.cloudmedia.model.vo;

public class ProgressVO {

    private Integer progressSeconds;

    public ProgressVO() {
    }

    public ProgressVO(Integer progressSeconds) {
        this.progressSeconds = progressSeconds;
    }

    public Integer getProgressSeconds() {
        return progressSeconds;
    }

    public void setProgressSeconds(Integer progressSeconds) {
        this.progressSeconds = progressSeconds;
    }
}
