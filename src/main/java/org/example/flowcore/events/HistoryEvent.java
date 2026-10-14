package org.example.flowcore.events;

import org.example.flowcore.value.WorkflowId;

import java.time.Instant;

public sealed interface HistoryEvent permits
        WorkflowStarted,
        ActivityScheduled,
        ActivityCompleted,
        ActivityFailed,
        TimerStarted,
        TimerFired,
        WorkflowCompleted,
        WorkflowFailed,
        WorkflowCanceled {

    WorkflowId workflowId();
    long sequence();
    Instant recordedAt();
}