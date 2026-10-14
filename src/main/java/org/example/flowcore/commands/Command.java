package org.example.flowcore.commands;

import org.example.flowcore.value.WorkflowId;

public sealed interface Command permits
        ScheduleActivity,
        StartTimer,
        CompleteWorkflow,
        FailWorkflow,
        CancelWorkflow {

    WorkflowId workflowId();
}
