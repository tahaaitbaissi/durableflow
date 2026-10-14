package org.example.flowcore.events;

import org.example.flowcore.value.TimerId;
import org.example.flowcore.value.WorkflowId;

import java.time.Instant;

public record TimerStarted(
        WorkflowId workflowId,
        long sequence,
        Instant recordedAt,
        TimerId timerId,
        Instant fireAt
) implements HistoryEvent {}
