package com.taskrunner.app.worker;

import com.taskrunner.app.model.Job;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Thin wrapper around LinkedBlockingQueue. Reason for the wrapper:
 * it lets me swap in a priority queue or a Redis-backed queue later
 * without touching the workers.
 */
@Component
public class JobQueue {

    private final LinkedBlockingQueue<Job> q = new LinkedBlockingQueue<>();

    public void enqueue(Job job) {
        q.offer(job);
    }

    public Job take() throws InterruptedException {
        return q.take();
    }

    public Job poll(long timeoutMs) throws InterruptedException {
        return q.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public int depth() {
        return q.size();
    }
}
