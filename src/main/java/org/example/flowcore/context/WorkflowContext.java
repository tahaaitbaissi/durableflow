package org.example.flowcore.context;

import org.example.flowcore.config.ActivityOptions;
import org.example.flowcore.value.WorkflowId;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public interface WorkflowContext {
    <T> T newActivityStub(
            Class<T> activityInterface,
            ActivityOptions options
    );
    void sleep(Duration duration);
    Instant now();
    UUID randomUUID();
    double random();
    WorkflowId getWorkflowId();
    int getVersion();
    void complete(Object result);
    void fail(String type, String message);
}
