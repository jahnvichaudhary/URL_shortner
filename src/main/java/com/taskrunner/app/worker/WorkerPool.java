package com.taskrunner.app.worker;

import com.taskrunner.app.core.HandlerRegistry;
import com.taskrunner.app.core.JobHandler;
import com.taskrunner.app.model.Job;
import com.taskrunner.app.model.JobStatus;
import com.taskrunner.app.store.JobStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Spins up N worker threads that pull from the queue and run handlers.
 *
 * Retry strategy: on failure we bump the attempt counter; if we still have
 * room we schedule the job back onto the queue with exponential backoff.
 * Otherwise we mark it FAILED.
 */
@Component
public class WorkerPool {

    private static final Logger log = LoggerFactory.getLogger(WorkerPool.class);

    private final JobQueue queue;
    private final JobStore store;
    private final HandlerRegistry registry;

    @Value("${taskrunner.workers:4}")
    private int workerCount;

    @Value("${taskrunner.max-retries:3}")
    private int maxRetries;

    @Value("${taskrunner.retry-backoff-ms:500}")
    private long backoffMs;

    private final List<Thread> workers = new ArrayList<>();
    private final ScheduledExecutorService retryScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "retry-scheduler");
                t.setDaemon(true);
                return t;
            });

    private volatile boolean running = true;

    public WorkerPool(JobQueue queue, JobStore store, HandlerRegistry registry) {
        this.queue = queue;
        this.store = store;
        this.registry = registry;
    }

    @PostConstruct
    void start() {
        log.info("starting {} worker thread(s)", workerCount);
        for (int i = 0; i < workerCount; i++) {
            Thread t = new Thread(this::loop, "worker-" + i);
            t.setDaemon(true);
            t.start();
            workers.add(t);
        }
    }

    @PreDestroy
    void stop() {
        running = false;
        workers.forEach(Thread::interrupt);
        retryScheduler.shutdownNow();
    }

    private void loop() {
        while (running) {
            Job job;
            try {
                job = queue.poll(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
            if (job == null) continue;
            runOne(job);
        }
    }

    private void runOne(Job job) {
        JobHandler handler = registry.get(job.getType());
        if (handler == null) {
            // Misconfigured / unknown type — no point retrying.
            job.setStatus(JobStatus.FAILED);
            job.setLastError("no handler registered for type: " + job.getType());
            job.setFinishedAt(Instant.now());
            return;
        }

        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job.incrementAttempts();

        try {
            Object out = handler.handle(job);
            job.setResult(out);
            job.setStatus(JobStatus.SUCCEEDED);
            job.setFinishedAt(Instant.now());
            log.info("job {} ok on attempt {}", job.getId(), job.getAttempts());
        } catch (Throwable t) {
            job.setLastError(t.getClass().getSimpleName() + ": " + t.getMessage());
            log.warn("job {} failed on attempt {} ({})",
                    job.getId(), job.getAttempts(), job.getLastError());

            if (job.getAttempts() < maxRetries) {
                scheduleRetry(job);
            } else {
                job.setStatus(JobStatus.FAILED);
                job.setFinishedAt(Instant.now());
            }
        }
    }

    private void scheduleRetry(Job job) {
        job.setStatus(JobStatus.RETRY_PENDING);
        long delay = backoffMs * (1L << (job.getAttempts() - 1)); // 500, 1000, 2000, ...
        retryScheduler.schedule(() -> {
            job.setStatus(JobStatus.QUEUED);
            queue.enqueue(job);
        }, delay, TimeUnit.MILLISECONDS);
    }
}
