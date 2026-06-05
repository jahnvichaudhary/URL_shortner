package com.taskrunner.app.core.handlers;

import com.taskrunner.app.core.JobHandler;
import com.taskrunner.app.model.Job;
import org.springframework.stereotype.Component;

/**
 * Pretends to do slow work. Useful for watching the queue drain.
 * payload: { "ms": 1500 }
 */
@Component
public class SleepHandler implements JobHandler {

    @Override
    public String type() {
        return "sleep";
    }

    @Override
    public Object handle(Job job) throws InterruptedException {
        long ms = 1000;
        Object raw = job.getPayload() == null ? null : job.getPayload().get("ms");
        if (raw instanceof Number n) ms = n.longValue();

        Thread.sleep(ms);
        return "slept " + ms + "ms";
    }
}
