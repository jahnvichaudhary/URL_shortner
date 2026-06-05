package com.taskrunner.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskrunnerApplication {

    public static void main(String[] args) {
        // nothing fancy here, just hand off to spring boot
        SpringApplication.run(TaskrunnerApplication.class, args);
    }
}
