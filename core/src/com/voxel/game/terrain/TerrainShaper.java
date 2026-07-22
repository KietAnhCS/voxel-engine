package com.voxel.game.terrain;

import com.voxel.engine.block.Block;
import com.voxel.engine.generation.Carver;
import com.voxel.engine.generation.CarverPipeline;
import com.voxel.engine.generation.TerrainPipeline;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.biome.Biome;
import com.voxel.game.terrain.biome.BiomeSource;
import com.voxel.game.terrain.carve.CheeseCaveCarver;
import com.voxel.game.terrain.carve.PerlinWormCarver;
import com.voxel.game.terrain.carve.RavineCarver;

/**
 * Facade cua toan bo qua trinh sinh the gioi. Chunk chi can goi ba viec:
 *  1. {@link #sample} - cot nay cao bao nhieu, thuoc biome nao
 *  2. {@link #resolve} - o do cao y thi la khoi gi
 *  3. {@link #carvers} - khoet hang sau khi da do dat da
 */
public final class TerrainShaper {

    private final TerrainNoise noise;
    private final BiomeSource biomes;
    private final TerrainPipeline<ColumnSample> pipeline;
    private final Carver carvers;
    private final int seaLevel;
    private final int ceiling;
    private final long seed;

    public TerrainShaper(Blocks blocks, BiomeSource biomes, TerrainNoise noise, int worldHeight) {
        this.noise = noise;
        this.biomes = biomes;
        this.seed = noise.seed();
        this.seaLevel = noise.seaLevel();
        this.ceiling = worldHeight - 2;

        this.pipeline = TerrainPipeline.of(
                new BedrockStage(blocks),
                new SurfaceStage(blocks, seaLevel),
                new StoneStage(blocks, noise),
                new OceanStage(blocks, seaLevel),
                new SkyStage(blocks));

        this.carvers = CarverPipeline.of(
                new PerlinWormCarver(noise, blocks, seed),
                new RavineCarver(noise, blocks, seed),
                new CheeseCaveCarver(noise, blocks));
    }

    public void sample(int worldX, int worldZ, ColumnSample out) {
        Biome biome = biomes.pick(worldX, worldZ);
        double height = Math.min(ceiling, biomes.blendedHeight(worldX, worldZ));
        out.prepare(height, noise.temperature(worldX, worldZ), noise.humidity(worldX, worldZ), biome);
    }

    public Block resolve(ColumnSample sample, int worldX, int y, int worldZ) {
        return pipeline.resolve(sample, worldX, y, worldZ);
    }

    public int columnTop(ColumnSample sample) {
        return (int) Math.ceil(Math.max(sample.surfaceHeight(), seaLevel));
    }

    public Carver carvers() {
        return carvers;
    }

    public BiomeSource biomes() {
        return biomes;
    }

    public long seed() {
        return seed;
    }

    public int seaLevel() {
        return seaLevel;
    }
}
