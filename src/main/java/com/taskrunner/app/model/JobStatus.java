package com.taskrunner.app.model;

public enum JobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,        // gave up after max retries
    RETRY_PENDING  // failed once but we're going to try again
}
