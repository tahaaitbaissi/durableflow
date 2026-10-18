
package org.example.flowcore.workflow;

import org.example.flowcore.value.WorkflowType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class WorkflowRegistry {

    private final Map<WorkflowType, Workflow> workflows =
            new HashMap<>();

    public void register(WorkflowType type, Workflow workflow) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(workflow, "workflow");

        if (workflows.containsKey(type)) {
            throw new IllegalArgumentException(
                    "Workflow already registered: " + type
            );
        }

        workflows.put(type, workflow);
    }

    public Workflow get(WorkflowType type) {
        Workflow workflow = workflows.get(type);

        if (workflow == null) {
            throw new IllegalArgumentException(
                    "No workflow registered for type: " + type
            );
        }

        return workflow;
    }
}
