package org.example.flowcore;

import org.example.flowcore.value.Payload;
import org.example.flowcore.value.WorkflowId;
import org.example.flowcore.value.WorkflowType;

import java.time.Instant;

public record WorkflowInstance(
        WorkflowId id,
        WorkflowType type,
        WorkflowStatus status,
        Payload input,
        Payload result,
        int version,
        Instant createdAt,
        Instant updatedAt
) {}
