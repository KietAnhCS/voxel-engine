package com.voxel.engine.world;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public final class ChunkScheduler {

    private final Deque<Chunk> urgent = new ArrayDeque<Chunk>();
    private final Deque<Chunk> background = new ArrayDeque<Chunk>();
    private final Set<Chunk> queued = new HashSet<Chunk>();

    public synchronized void submit(Chunk chunk, boolean isUrgent) {
        if (!queued.add(chunk)) {
            return;
        }
        if (isUrgent) {
            urgent.push(chunk);
        } else {
            background.addLast(chunk);
        }
    }

    public synchronized Chunk poll() {
        Chunk chunk = urgent.isEmpty() ? background.pollFirst() : urgent.pop();
        if (chunk != null) {
            queued.remove(chunk);
        }
        return chunk;
    }

    public synchronized void forget(Chunk chunk) {
        if (queued.remove(chunk)) {
            urgent.remove(chunk);
            background.remove(chunk);
        }
    }

    public synchronized int pending() {
        return queued.size();
    }

    public synchronized void clear() {
        urgent.clear();
        background.clear();
        queued.clear();
    }
}
