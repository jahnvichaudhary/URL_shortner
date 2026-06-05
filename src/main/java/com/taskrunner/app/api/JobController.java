package com.taskrunner.app.api;

import com.taskrunner.app.core.HandlerRegistry;
import com.taskrunner.app.model.Job;
import com.taskrunner.app.store.JobStore;
import com.taskrunner.app.worker.JobQueue;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobQueue queue;
    private final JobStore store;
    private final HandlerRegistry registry;

    public JobController(JobQueue queue, JobStore store, HandlerRegistry registry) {
        this.queue = queue;
        this.store = store;
        this.registry = registry;
    }

    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody SubmitJobRequest req) {
        if (!registry.knows(req.getType())) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "unknown job type: " + req.getType());
            err.put("known", registry.known());
            return ResponseEntity.badRequest().body(err);
        }

        Job job = new Job(req.getType(), req.getPayload());
        store.save(job);
        queue.enqueue(job);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", job.getId());
        body.put("status", job.getStatus());
        body.put("statusUrl", "http://localhost:8080/api/jobs/" + job.getId());
        return ResponseEntity.accepted().body(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> status(@PathVariable String id) {
        Job job = store.find(id);
        if (job == null) {
            return ResponseEntity.status(404).body(Map.of("error", "no job with id " + id));
        }
        return ResponseEntity.ok(view(job));
    }

    @GetMapping
    public ResponseEntity<?> recent(@RequestParam(defaultValue = "25") int limit) {
        List<Map<String, Object>> rows = store.recent(limit).stream().map(this::view).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("count", rows.size());
        body.put("queueDepth", queue.depth());
        body.put("total", store.size());
        body.put("jobs", rows);
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> view(Job j) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", j.getId());
        m.put("type", j.getType());
        m.put("status", j.getStatus());
        m.put("attempts", j.getAttempts());
        m.put("createdAt", j.getCreatedAt());
        m.put("startedAt", j.getStartedAt());
        m.put("finishedAt", j.getFinishedAt());
        m.put("result", j.getResult());
        m.put("lastError", j.getLastError());
        return m;
    }
}
