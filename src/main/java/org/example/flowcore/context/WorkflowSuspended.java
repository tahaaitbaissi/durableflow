package org.example.flowcore.context;

public final class WorkflowSuspended extends Error {

    public WorkflowSuspended() {
        super("Workflow suspended", null, false, false);
    }
}
