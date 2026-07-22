package com.voxel.engine.generation;

import com.voxel.engine.world.ChunkWriter;

/**
 * Composite: gom nhieu carver nho thanh mot carver duy nhat, chay theo thu tu.
 */
public final class CarverPipeline implements Carver {

    private final Carver[] carvers;

    private CarverPipeline(Carver[] carvers) {
        this.carvers = carvers;
    }

    public static CarverPipeline of(Carver... carvers) {
        return new CarverPipeline(carvers.clone());
    }

    @Override
    public void carve(ChunkWriter writer, int chunkX, int chunkZ, int chunkSize, int worldHeight) {
        for (Carver carver : carvers) {
            carver.carve(writer, chunkX, chunkZ, chunkSize, worldHeight);
        }
    }
}
