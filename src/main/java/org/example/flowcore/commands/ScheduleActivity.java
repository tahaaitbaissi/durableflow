package org.example.flowcore.commands;

import org.example.flowcore.config.ActivityOptions;
import org.example.flowcore.value.ActivityId;
import org.example.flowcore.value.Payload;
import org.example.flowcore.value.WorkflowId;

public record ScheduleActivity(
        WorkflowId workflowId,
        ActivityId activityId,
        String activityName,
        Payload input,
        ActivityOptions options
) implements Command {}
