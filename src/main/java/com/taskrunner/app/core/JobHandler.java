package com.taskrunner.app.core;

import com.taskrunner.app.model.Job;

/**
 * Implement this for each kind of work you want the runner to handle.
 * Throw anything to signal failure — the worker will catch it and decide
 * whether to retry.
 */
public interface JobHandler {

    /** Unique handler key. Matches Job.type. */
    String type();

    /** Do the work. Return value is stored on the job as `result`. */
    Object handle(Job job) throws Exception;
}
