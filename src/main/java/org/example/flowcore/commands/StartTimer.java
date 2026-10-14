package org.example.flowcore.commands;

import org.example.flowcore.value.TimerId;
import org.example.flowcore.value.WorkflowId;

import java.time.Instant;

public record StartTimer(
        WorkflowId workflowId,
        TimerId timerId,
        Instant fireAt
) implements Command {}
