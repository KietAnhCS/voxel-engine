package com.voxel.engine.world;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class WorldEventBus {

    private final List<ChunkListener> listeners = new CopyOnWriteArrayList<ChunkListener>();

    public void subscribe(ChunkListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(ChunkListener listener) {
        listeners.remove(listener);
    }

    public void fireGenerated(Chunk chunk) {
        for (ChunkListener listener : listeners) {
            listener.onChunkGenerated(chunk);
        }
    }

    public void fireLightChanged(Chunk chunk) {
        for (ChunkListener listener : listeners) {
            listener.onChunkLightChanged(chunk);
        }
    }

    public void fireGeometryInvalid(Chunk chunk, boolean urgent) {
        for (ChunkListener listener : listeners) {
            listener.onChunkGeometryInvalid(chunk, urgent);
        }
    }

    public void fireUnloaded(Chunk chunk) {
        for (ChunkListener listener : listeners) {
            listener.onChunkUnloaded(chunk);
        }
    }
}
