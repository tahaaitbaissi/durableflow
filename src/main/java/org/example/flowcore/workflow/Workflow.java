package org.example.flowcore.workflow;

import org.example.flowcore.context.WorkflowContext;

public interface Workflow {
    void run(WorkflowContext ctx) throws Exception;
}