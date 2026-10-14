package org.example.flowcore.events;

import org.example.flowcore.value.Payload;
import org.example.flowcore.value.WorkflowId;
import org.example.flowcore.value.WorkflowType;

import java.time.Instant;

public record WorkflowStarted(
        WorkflowId workflowId,
        long sequence,
        Instant recordedAt,
        WorkflowType workflowType,
        Payload input,
        int version
) implements HistoryEvent {}
