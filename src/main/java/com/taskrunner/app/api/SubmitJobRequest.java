package com.taskrunner.app.api;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public class SubmitJobRequest {

    @NotBlank
    private String type;

    private Map<String, Object> payload;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
}
