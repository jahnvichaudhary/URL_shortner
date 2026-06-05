package com.taskrunner.app.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single unit of work moving through the system.
 *
 * I went with a plain class + getters/setters instead of a record because
 * the status / attempt count get mutated by the workers as they go.
 */
public class Job {

    private final String id;
    private final String type;
    private final Map<String, Object> payload;
    private final Instant createdAt;

    private volatile JobStatus status;
    private volatile int attempts;
    private volatile String lastError;
    private volatile Instant startedAt;
    private volatile Instant finishedAt;
    private volatile Object result;

    public Job(String type, Map<String, Object> payload) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.status = JobStatus.QUEUED;
        this.attempts = 0;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public Map<String, Object> getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public int getAttempts() { return attempts; }
    public void incrementAttempts() { this.attempts++; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }

    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
}
