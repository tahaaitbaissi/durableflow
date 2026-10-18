
package org.example.flowcore.replay;

import org.example.flowcore.commands.Command;
import org.example.flowcore.value.Payload;

import java.util.List;

public sealed interface ReplayResult {

    record Suspended(List<Command> commands)
            implements ReplayResult {

        public Suspended {
            commands = List.copyOf(commands);
        }
    }

    record Completed(Payload result)
            implements ReplayResult {
    }

    record Failed(String failureType, String message)
            implements ReplayResult {
    }

    record Nondeterministic(String message)
            implements ReplayResult {
    }
}
