package com.taskrunner.app.core.handlers;

import com.taskrunner.app.core.JobHandler;
import com.taskrunner.app.model.Job;
import org.springframework.stereotype.Component;

/**
 * Sanity-check handler. Submit { "type": "echo", "payload": {"msg": "..."} }
 * and you'll get the same message back as the result.
 */
@Component
public class EchoHandler implements JobHandler {

    @Override
    public String type() {
        return "echo";
    }

    @Override
    public Object handle(Job job) {
        Object msg = job.getPayload() == null ? null : job.getPayload().get("msg");
        return msg == null ? "(empty)" : msg;
    }
}
