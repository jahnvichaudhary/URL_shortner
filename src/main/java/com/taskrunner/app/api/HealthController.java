package com.taskrunner.app.api;

import com.taskrunner.app.core.HandlerRegistry;
import com.taskrunner.app.store.JobStore;
import com.taskrunner.app.worker.JobQueue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final JobQueue queue;
    private final JobStore store;
    private final HandlerRegistry registry;

    public HealthController(JobQueue queue, JobStore store, HandlerRegistry registry) {
        this.queue = queue;
        this.store = store;
        this.registry = registry;
    }

    @GetMapping("/")
    public Map<String, Object> root() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("service", "taskrunner");
        m.put("ok", true);
        m.put("endpoints", Map.of(
                "submit",   "POST   http://localhost:8080/api/jobs",
                "status",   "GET    http://localhost:8080/api/jobs/{id}",
                "list",     "GET    http://localhost:8080/api/jobs",
                "health",   "GET    http://localhost:8080/health"
        ));
        m.put("handlers", registry.known());
        return m;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "UP");
        m.put("queueDepth", queue.depth());
        m.put("jobsTracked", store.size());
        return m;
    }
}
