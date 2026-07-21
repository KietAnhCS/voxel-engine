package com.voxel.game.terrain;

import com.voxel.engine.block.Block;
import com.voxel.engine.generation.SimplexNoise;
import com.voxel.engine.generation.TerrainPipeline;
import com.voxel.engine.generation.TerrainSample;
import com.voxel.game.Blocks;

public final class TerrainShaper {

    private final SimplexNoise continents;
    private final SimplexNoise hills;
    private final SimplexNoise ridges;
    private final SimplexNoise humidity;
    private final TerrainPipeline pipeline;
    private final int seaLevel;
    private final int ceiling;
    private final long seed;

    public TerrainShaper(Blocks blocks, long seed, int seaLevel, int worldHeight) {
        this.seed = seed;
        this.seaLevel = seaLevel;
        this.ceiling = worldHeight - 2;
        this.continents = new SimplexNoise(seed);
        this.hills = new SimplexNoise(seed ^ 0x5DEECE66DL);
        this.ridges = new SimplexNoise(seed * 31L + 17L);
        this.humidity = new SimplexNoise(seed * 131L + 7919L);

        SimplexNoise strata = new SimplexNoise(seed * 7L + 104729L);
        this.pipeline = TerrainPipeline.of(
                new BedrockStage(blocks),
                new MountainStage(blocks, strata),
                new SurfaceStage(blocks, strata, seaLevel),
                new OceanStage(blocks, seaLevel),
                new SkyStage(blocks));
    }

    public void sample(int worldX, int worldZ, TerrainSample out) {
        double continent = continents.fractal2d(worldX, worldZ, 3, 0.0016, 2.0, 0.5);
        double hill = hills.fractal2d(worldX, worldZ, 4, 0.0090, 2.0, 0.5);
        double ridge = 1.0 - Math.abs(ridges.fractal2d(worldX, worldZ, 4, 0.0042, 2.0, 0.5));
        double wet = humidity.fractal2d(worldX, worldZ, 2, 0.0025, 2.0, 0.5);

        double surface = seaLevel + 6.0 + continent * 26.0 + hill * 8.0;
        double mountainMask = Math.max(0.0, continent);
        double ridgeHeight = surface + Math.pow(ridge, 3.0) * 46.0 * mountainMask;

        out.prepare(Math.min(surface, ceiling), Math.min(ridgeHeight, ceiling), wet);
    }

    public Block resolve(TerrainSample sample, int worldX, int y, int worldZ) {
        return pipeline.resolve(sample, worldX, y, worldZ);
    }

    public int columnTop(TerrainSample sample) {
        return (int) Math.ceil(Math.max(Math.max(sample.surfaceHeight(), sample.ridgeHeight()), seaLevel));
    }

    public long seed() {
        return seed;
    }

    public int seaLevel() {
        return seaLevel;
    }
}
