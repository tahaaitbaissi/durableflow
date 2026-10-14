package org.example.flowcore.events;

import org.example.flowcore.value.ActivityId;
import org.example.flowcore.value.Payload;
import org.example.flowcore.value.WorkflowId;

import java.time.Instant;

public record ActivityCompleted(
        WorkflowId workflowId,
        long sequence,
        Instant recordedAt,
        ActivityId activityId,
        Payload result
) implements HistoryEvent {}
