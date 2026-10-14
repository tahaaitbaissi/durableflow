package org.example.flowcore.events;

import org.example.flowcore.value.Payload;
import org.example.flowcore.value.WorkflowId;

import java.time.Instant;

public record WorkflowCompleted(
        WorkflowId workflowId,
        long sequence,
        Instant recordedAt,
        Payload result
) implements HistoryEvent {}
