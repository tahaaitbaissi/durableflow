package org.example.flowcore.events;

import org.example.flowcore.value.WorkflowId;

import java.time.Instant;

public record WorkflowFailed(
        WorkflowId workflowId,
        long sequence,
        Instant recordedAt,
        String failureType,
        String message
) implements HistoryEvent {}