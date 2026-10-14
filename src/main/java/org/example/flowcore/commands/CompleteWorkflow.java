package org.example.flowcore.commands;

import org.example.flowcore.value.Payload;
import org.example.flowcore.value.WorkflowId;

public record CompleteWorkflow(
        WorkflowId workflowId,
        Payload result
) implements Command {}
