package org.example.flowcore.replay;

import org.example.flowcore.commands.Command;
import org.example.flowcore.commands.CompleteWorkflow;
import org.example.flowcore.commands.FailWorkflow;
import org.example.flowcore.context.CommandCollector;
import org.example.flowcore.context.HistoryReader;
import org.example.flowcore.context.NonDeterminismException;
import org.example.flowcore.context.WorkflowContextImpl;
import org.example.flowcore.context.WorkflowSuspended;
import org.example.flowcore.events.HistoryEvent;
import org.example.flowcore.events.WorkflowStarted;
import org.example.flowcore.workflow.Workflow;
import org.example.flowcore.workflow.WorkflowRegistry;
import org.example.flowcore.WorkflowInstance;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class ReplayEngine {

    private final WorkflowRegistry registry;

    public ReplayEngine(WorkflowRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
    }

    public ReplayResult replay(
            WorkflowInstance instance,
            List<HistoryEvent> events) {

        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(events, "events");

        if (events.isEmpty()
                || !(events.get(0) instanceof WorkflowStarted started)) {
            return new ReplayResult.Failed(
                    "InvalidHistory",
                    "History must begin with WorkflowStarted"
            );
        }

        Instant workflowStartTime = started.recordedAt();

        final Workflow workflow;

        try {
            workflow = registry.get(instance.type());
        } catch (RuntimeException e) {
            return new ReplayResult.Failed(
                    e.getClass().getName(),
                    e.getMessage()
            );
        }

        HistoryReader reader = new HistoryReader(events);

        // Consume WorkflowStarted. It initializes the workflow;
        // it is not an activity or timer call.
        reader.next();

        CommandCollector commands = new CommandCollector();

        WorkflowContextImpl context = new WorkflowContextImpl(
                instance.id(),
                instance.version(),
                reader,
                commands,
                workflowStartTime
        );

        try {
            workflow.run(context);

            // A workflow must terminate explicitly or suspend.
            return new ReplayResult.Failed(
                    "WorkflowReturned",
                    "Workflow returned without completing, failing, "
                            + "or suspending"
            );

        } catch (WorkflowSuspended suspended) {
            return classify(commands.snapshot());

        } catch (NonDeterminismException e) {
            return new ReplayResult.Nondeterministic(
                    e.getMessage()
            );

        } catch (Exception e) {
            return new ReplayResult.Failed(
                    e.getClass().getName(),
                    e.getMessage() == null
                            ? "Workflow execution failed"
                            : e.getMessage()
            );
        }
    }


    private ReplayResult classify(List<Command> commands) {
        if (commands.isEmpty()) {
            return new ReplayResult.Failed(
                    "EmptySuspension",
                    "Workflow suspended without emitting a command"
            );
        }

        Command last = commands.get(commands.size() - 1);

        if (last instanceof CompleteWorkflow completed) {
            return new ReplayResult.Completed(completed.result());
        }

        if (last instanceof FailWorkflow failed) {
            return new ReplayResult.Failed(
                    failed.failureType(),
                    failed.message()
            );
        }

        return new ReplayResult.Suspended(commands);
    }

}
