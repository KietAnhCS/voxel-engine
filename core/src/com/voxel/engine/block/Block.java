package com.voxel.engine.block;

import com.voxel.engine.util.Direction;

public final class Block {

    public static final int MAX_LIGHT = 15;

    private final byte id;
    private final String name;
    private final BlockGeometry geometry;
    private final BlockTint tint;
    private final String topTexture;
    private final String bottomTexture;
    private final String sideTexture;
    private final int attenuation;
    private final int luminance;
    private final boolean opaque;
    private final boolean collidable;
    private final boolean liquid;
    private final boolean windAffected;

    private Block(Builder builder, byte id) {
        this.id = id;
        this.name = builder.name;
        this.geometry = builder.geometry;
        this.tint = builder.tint;
        this.topTexture = builder.topTexture;
        this.bottomTexture = builder.bottomTexture;
        this.sideTexture = builder.sideTexture;
        this.attenuation = builder.attenuation;
        this.luminance = builder.luminance;
        this.opaque = builder.opaque;
        this.collidable = builder.collidable;
        this.liquid = builder.liquid;
        this.windAffected = builder.windAffected;
    }

    public static Builder named(String name) {
        return new Builder(name);
    }

    public byte id() {
        return id;
    }

    public String name() {
        return name;
    }

    public BlockGeometry geometry() {
        return geometry;
    }

    public BlockTint tint() {
        return tint;
    }

    public String textureFor(Direction face) {
        if (face == Direction.UP) {
            return topTexture;
        }
        if (face == Direction.DOWN) {
            return bottomTexture;
        }
        return sideTexture;
    }

    public int attenuation() {
        return attenuation;
    }

    public int luminance() {
        return luminance;
    }

    public boolean isOpaque() {
        return opaque;
    }

    public boolean isCollidable() {
        return collidable;
    }

    public boolean isLiquid() {
        return liquid;
    }

    public boolean isWindAffected() {
        return windAffected;
    }

    public boolean isAir() {
        return id == 0;
    }

    public boolean isLightSource() {
        return luminance > 0;
    }

    @Override
    public String toString() {
        return name;
    }

    public static final class Builder {

        private final String name;
        private BlockGeometry geometry;
        private BlockTint tint = BlockTint.NEUTRAL;
        private String topTexture;
        private String bottomTexture;
        private String sideTexture;
        private int attenuation = 1;
        private int luminance = 0;
        private boolean opaque = true;
        private boolean collidable = true;
        private boolean liquid = false;
        private boolean windAffected = false;

        private Builder(String name) {
            this.name = name;
        }

        public Builder geometry(BlockGeometry geometry) {
            this.geometry = geometry;
            return this;
        }

        public Builder texture(String region) {
            this.topTexture = region;
            this.bottomTexture = region;
            this.sideTexture = region;
            return this;
        }

        public Builder textures(String top, String bottom, String sides) {
            this.topTexture = top;
            this.bottomTexture = bottom;
            this.sideTexture = sides;
            return this;
        }

        public Builder tint(BlockTint tint) {
            this.tint = tint;
            return this;
        }

        public Builder attenuation(int attenuation) {
            this.attenuation = Math.max(1, attenuation);
            return this;
        }

        public Builder luminance(int luminance) {
            this.luminance = Math.min(MAX_LIGHT, Math.max(0, luminance));
            return this;
        }

        public Builder translucent() {
            this.opaque = false;
            return this;
        }

        public Builder passable() {
            this.collidable = false;
            return this;
        }

        public Builder liquid() {
            this.liquid = true;
            this.collidable = false;
            this.opaque = false;
            return this;
        }

        public Builder windAffected() {
            this.windAffected = true;
            return this;
        }

        public Block build(byte id) {
            if (geometry == null) {
                throw new IllegalStateException("block " + name + " has no geometry");
            }
            if (sideTexture == null) {
                throw new IllegalStateException("block " + name + " has no texture");
            }
            return new Block(this, id);
        }
    }
}
