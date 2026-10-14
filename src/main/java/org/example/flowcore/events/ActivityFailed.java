package org.example.flowcore.events;

import org.example.flowcore.value.ActivityId;
import org.example.flowcore.value.WorkflowId;

import java.time.Instant;

public record ActivityFailed(
        WorkflowId workflowId,
        long sequence,
        Instant recordedAt,
        ActivityId activityId,
        String failureType,
        String message,
        boolean retryable
) implements HistoryEvent {}
