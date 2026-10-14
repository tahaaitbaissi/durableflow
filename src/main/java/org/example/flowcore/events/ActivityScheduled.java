package org.example.flowcore.events;

import org.example.flowcore.config.ActivityOptions;
import org.example.flowcore.value.ActivityId;
import org.example.flowcore.value.Payload;
import org.example.flowcore.value.WorkflowId;

import java.time.Instant;

public record ActivityScheduled(
        WorkflowId workflowId,
        long sequence,
        Instant recordedAt,
        ActivityId activityId,
        String activityName,
        Payload input,
        ActivityOptions options
) implements HistoryEvent {}
