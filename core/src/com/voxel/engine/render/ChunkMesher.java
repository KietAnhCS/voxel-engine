package com.voxel.engine.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.voxel.engine.EngineConfig;
import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.block.BlockView;
import com.voxel.engine.block.QuadEmitter;
import com.voxel.engine.util.Direction;
import com.voxel.engine.world.Chunk;
import com.voxel.engine.world.ChunkStorage;
import com.voxel.engine.world.World;

import java.util.HashMap;
import java.util.Map;

public final class ChunkMesher implements BlockView, QuadEmitter {

    public static final int SECTION_HEIGHT = 16;

    private final EngineConfig config;
    private final BlockRegistry registry;
    private final Map<String, float[]> uvCache = new HashMap<String, float[]>();
    private final Block air;

    private final MeshBuffer solid = new MeshBuffer();
    private final MeshBuffer fluid = new MeshBuffer();
    private final Color[] corners = {new Color(), new Color(), new Color(), new Color()};
    private final float[] positions = new float[12];
    private final float[] uvs = new float[8];

    private World world;
    private Chunk chunk;
    private ChunkStorage storage;
    private int originX;
    private int originZ;
    private MeshBuffer target;

    public ChunkMesher(EngineConfig config, BlockRegistry registry, TextureAtlas atlas) {
        this.config = config;
        this.registry = registry;
        this.air = registry.byId((byte) 0);
        for (Map.Entry<String, Block> entry : registry.all().entrySet()) {
            cacheRegion(atlas, entry.getValue().textureFor(Direction.UP));
            cacheRegion(atlas, entry.getValue().textureFor(Direction.DOWN));
            cacheRegion(atlas, entry.getValue().textureFor(Direction.NORTH));
        }
    }

    private void cacheRegion(TextureAtlas atlas, String name) {
        if (name == null || uvCache.containsKey(name)) {
            return;
        }
        TextureAtlas.AtlasRegion region = atlas.findRegion(name);
        if (region == null) {
            throw new IllegalArgumentException("missing atlas region " + name);
        }
        float u0 = region.getU();
        float v0 = region.getV();
        float u1 = region.getU2();
        float v1 = region.getV2();
        uvCache.put(name, new float[]{u0, v1, u1, v1, u1, v0, u0, v0});
    }

    public ChunkGeometryData build(World world, Chunk chunk) {
        this.world = world;
        this.chunk = chunk;
        this.storage = chunk.storage();
        this.originX = chunk.originX();
        this.originZ = chunk.originZ();

        int size = config.chunkSize();
        int sections = config.worldHeight() / SECTION_HEIGHT;
        ChunkGeometryData data = new ChunkGeometryData(sections);

        for (int section = 0; section < sections; section++) {
            solid.reset();
            fluid.reset();

            int baseY = section * SECTION_HEIGHT;
            for (int y = baseY; y < baseY + SECTION_HEIGHT; y++) {
                for (int x = 0; x < size; x++) {
                    for (int z = 0; z < size; z++) {
                        Block block = registry.byId(storage.blockId(x, y, z));
                        if (block.isAir()) {
                            continue;
                        }
                        target = block.isLiquid() ? fluid : solid;
                        block.geometry().emit(this, x, y, z, block, this);
                    }
                }
            }

            if (!solid.isEmpty()) {
                data.setSolid(section, solid.vertexData(), solid.indexData());
            }
            if (!fluid.isEmpty()) {
                data.setTranslucent(section, fluid.vertexData(), fluid.indexData());
            }
        }

        this.world = null;
        this.chunk = null;
        this.storage = null;
        return data;
    }

    @Override
    public Block blockAt(int x, int y, int z) {
        if (y < 0 || y >= config.worldHeight()) {
            return air;
        }
        if (storage.contains(x, y, z)) {
            return registry.byId(storage.blockId(x, y, z));
        }
        return world.blockAt(originX + x, y, originZ + z);
    }

    @Override
    public boolean occludes(int x, int y, int z, Block source) {
        if (y < 0) {
            return true;
        }
        if (y >= config.worldHeight()) {
            return false;
        }
        Block block = blockAt(x, y, z);
        if (block.isAir()) {
            return false;
        }
        if (block.geometry().occludesNeighbours()) {
            return true;
        }
        return block == source;
    }

    @Override
    public int lightAt(int x, int y, int z) {
        if (y >= config.worldHeight()) {
            return Block.MAX_LIGHT;
        }
        if (y < 0) {
            return 0;
        }
        if (storage.contains(x, y, z)) {
            return storage.combinedLight(storage.index(x, y, z));
        }
        return world.lightAt(originX + x, y, originZ + z);
    }

    @Override
    public Color[] cornerBuffer() {
        return corners;
    }

    @Override
    public void quad(float x, float y, float z, Direction face, float width, float height,
                     String textureRegion, Color[] cornerColors) {
        float px = x + face.originX();
        float py = y + face.originY();
        float pz = z + face.originZ();

        float ux = face.tangentX() * width;
        float uy = face.tangentY() * width;
        float uz = face.tangentZ() * width;

        float vx = face.bitangentX() * height;
        float vy = face.bitangentY() * height;
        float vz = face.bitangentZ() * height;

        positions[0] = px;
        positions[1] = py;
        positions[2] = pz;
        positions[3] = px + ux;
        positions[4] = py + uy;
        positions[5] = pz + uz;
        positions[6] = px + ux + vx;
        positions[7] = py + uy + vy;
        positions[8] = pz + uz + vz;
        positions[9] = px + vx;
        positions[10] = py + vy;
        positions[11] = pz + vz;

        System.arraycopy(uvCache.get(textureRegion), 0, uvs, 0, 8);
        target.addQuad(positions, uvs, face.dx(), face.dy(), face.dz(), cornerColors);
    }

    @Override
    public void billboard(float x, float y, float z, float offsetX, float offsetZ, boolean alongX,
                          String textureRegion, Color tint) {
        float x0 = x + (alongX ? 1f : 0f) + offsetX;
        float z0 = z + offsetZ;
        float x1 = x + (alongX ? 0f : 1f) + offsetX;
        float z1 = z + 1f + offsetZ;

        for (int corner = 0; corner < 4; corner++) {
            corners[corner].set(tint);
        }

        positions[0] = x0;
        positions[1] = y;
        positions[2] = z0;
        positions[3] = x1;
        positions[4] = y;
        positions[5] = z1;
        positions[6] = x1;
        positions[7] = y + 1f;
        positions[8] = z1;
        positions[9] = x0;
        positions[10] = y + 1f;
        positions[11] = z0;

        System.arraycopy(uvCache.get(textureRegion), 0, uvs, 0, 8);
        target.addQuad(positions, uvs, 0f, 1f, 0f, corners);

        positions[0] = x1;
        positions[1] = y;
        positions[2] = z1;
        positions[3] = x0;
        positions[4] = y;
        positions[5] = z0;
        positions[6] = x0;
        positions[7] = y + 1f;
        positions[8] = z0;
        positions[9] = x1;
        positions[10] = y + 1f;
        positions[11] = z1;

        System.arraycopy(uvCache.get(textureRegion), 0, uvs, 0, 8);
        target.addQuad(positions, uvs, 0f, 1f, 0f, corners);
    }
}
