package com.taskrunner.app.core.handlers;

import com.taskrunner.app.core.JobHandler;
import com.taskrunner.app.model.Job;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Fails ~70% of the time on purpose so you can see the retry logic kick in.
 * payload: { "failRate": 0.7 } (optional)
 */
@Component
public class FlakyHandler implements JobHandler {

    @Override
    public String type() {
        return "flaky";
    }

    @Override
    public Object handle(Job job) {
        double failRate = 0.7;
        Object raw = job.getPayload() == null ? null : job.getPayload().get("failRate");
        if (raw instanceof Number n) failRate = n.doubleValue();

        if (ThreadLocalRandom.current().nextDouble() < failRate) {
            throw new RuntimeException("flaky failure on attempt " + (job.getAttempts() + 1));
        }
        return "made it through on attempt " + (job.getAttempts() + 1);
    }
}
