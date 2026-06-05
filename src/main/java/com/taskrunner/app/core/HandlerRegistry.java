package com.taskrunner.app.core;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HandlerRegistry {

    private final Map<String, JobHandler> byType = new HashMap<>();
    private final List<JobHandler> discovered;

    public HandlerRegistry(List<JobHandler> discovered) {
        this.discovered = discovered;
    }

    @PostConstruct
    void wire() {
        for (JobHandler h : discovered) {
            if (byType.containsKey(h.type())) {
                throw new IllegalStateException(
                        "Two handlers registered for type: " + h.type());
            }
            byType.put(h.type(), h);
        }
    }

    public JobHandler get(String type) {
        return byType.get(type);
    }

    public boolean knows(String type) {
        return byType.containsKey(type);
    }

    public java.util.Set<String> known() {
        return byType.keySet();
    }
}
