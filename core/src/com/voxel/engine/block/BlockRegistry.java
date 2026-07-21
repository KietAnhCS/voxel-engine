package com.voxel.engine.block;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class BlockRegistry {

    private static final int CAPACITY = 256;

    private final Block[] byId = new Block[CAPACITY];
    private final Map<String, Block> byName = new HashMap<String, Block>();
    private int next;

    public Block register(Block.Builder builder) {
        if (next >= CAPACITY) {
            throw new IllegalStateException("block id space exhausted");
        }
        Block block = builder.build((byte) next);
        if (byName.containsKey(block.name())) {
            throw new IllegalArgumentException("duplicate block name " + block.name());
        }
        byId[next] = block;
        byName.put(block.name(), block);
        next++;
        return block;
    }

    public Block byId(byte id) {
        Block block = byId[id & 0xFF];
        return block == null ? byId[0] : block;
    }

    public Block byName(String name) {
        Block block = byName.get(name);
        if (block == null) {
            throw new IllegalArgumentException("unknown block " + name);
        }
        return block;
    }

    public Map<String, Block> all() {
        return Collections.unmodifiableMap(byName);
    }

    public int size() {
        return next;
    }
}
