package com.taskrunner.app.store;

import com.taskrunner.app.model.Job;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory store. Good enough for a single-node deployment.
 * If you ever need persistence, swap this out for a JPA repo — the rest of
 * the app doesn't care.
 */
@Component
public class JobStore {

    private final ConcurrentHashMap<String, Job> jobs = new ConcurrentHashMap<>();

    public void save(Job job) {
        jobs.put(job.getId(), job);
    }

    public Job find(String id) {
        return jobs.get(id);
    }

    public List<Job> recent(int limit) {
        Collection<Job> all = jobs.values();
        return all.stream()
                .sorted(Comparator.comparing(Job::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public int size() {
        return jobs.size();
    }
}
