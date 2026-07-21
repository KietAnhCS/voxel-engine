package com.voxel.engine.render;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class RenderCommandQueue {

    private final Queue<RenderCommand> commands = new ConcurrentLinkedQueue<RenderCommand>();

    public void submit(RenderCommand command) {
        commands.add(command);
    }

    public int drain(int budget) {
        int executed = 0;
        while (executed < budget) {
            RenderCommand command = commands.poll();
            if (command == null) {
                break;
            }
            command.execute();
            executed++;
        }
        return executed;
    }

    public int pending() {
        return commands.size();
    }

    public void clear() {
        commands.clear();
    }
}
