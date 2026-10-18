package org.example.flowcore.context;

import org.example.flowcore.commands.Command;

import java.util.ArrayList;
import java.util.List;

public final class CommandCollector {

    private final List<Command> commands = new ArrayList<>();

    public void add(Command command) {
        commands.add(command);
    }

    public List<Command> snapshot() {
        return List.copyOf(commands);
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }
}
