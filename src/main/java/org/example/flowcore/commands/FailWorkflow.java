package org.example.flowcore.commands;

import org.example.flowcore.value.WorkflowId;

public record FailWorkflow(
        WorkflowId workflowId,
        String failureType,
        String message
) implements Command {}
