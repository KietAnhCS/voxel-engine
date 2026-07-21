package com.voxel.engine.physics;

import com.badlogic.gdx.math.Vector3;
import com.voxel.engine.block.Block;
import com.voxel.engine.util.Direction;
import com.voxel.engine.world.World;

public final class VoxelRaycaster {

    public boolean cast(World world, Vector3 origin, Vector3 direction, float maxDistance, Hit hit) {
        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        int stepX = signum(direction.x);
        int stepY = signum(direction.y);
        int stepZ = signum(direction.z);

        float deltaX = stepX == 0 ? Float.MAX_VALUE : Math.abs(1f / direction.x);
        float deltaY = stepY == 0 ? Float.MAX_VALUE : Math.abs(1f / direction.y);
        float deltaZ = stepZ == 0 ? Float.MAX_VALUE : Math.abs(1f / direction.z);

        float maxX = boundaryDistance(origin.x, x, stepX, direction.x);
        float maxY = boundaryDistance(origin.y, y, stepY, direction.y);
        float maxZ = boundaryDistance(origin.z, z, stepZ, direction.z);

        Block start = world.blockAt(x, y, z);
        if (isTarget(start)) {
            hit.set(x, y, z, Direction.UP, 0f);
            return true;
        }

        float travelled = 0f;
        while (travelled <= maxDistance) {
            Direction face;
            if (maxX < maxY && maxX < maxZ) {
                x += stepX;
                travelled = maxX;
                maxX += deltaX;
                face = stepX > 0 ? Direction.WEST : Direction.EAST;
            } else if (maxY < maxZ) {
                y += stepY;
                travelled = maxY;
                maxY += deltaY;
                face = stepY > 0 ? Direction.DOWN : Direction.UP;
            } else {
                z += stepZ;
                travelled = maxZ;
                maxZ += deltaZ;
                face = stepZ > 0 ? Direction.NORTH : Direction.SOUTH;
            }

            if (travelled > maxDistance) {
                return false;
            }

            Block block = world.blockAt(x, y, z);
            if (isTarget(block)) {
                hit.set(x, y, z, face, travelled);
                return true;
            }
        }
        return false;
    }

    private static boolean isTarget(Block block) {
        return !block.isAir() && !block.isLiquid();
    }

    private static int signum(float value) {
        if (value > 0f) {
            return 1;
        }
        return value < 0f ? -1 : 0;
    }

    private static float boundaryDistance(float origin, int voxel, int step, float direction) {
        if (step == 0) {
            return Float.MAX_VALUE;
        }
        float boundary = step > 0 ? voxel + 1 - origin : origin - voxel;
        return boundary / Math.abs(direction);
    }

    public static final class Hit {

        private int x;
        private int y;
        private int z;
        private Direction face;
        private float distance;

        void set(int x, int y, int z, Direction face, float distance) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.face = face;
            this.distance = distance;
        }

        public int blockX() {
            return x;
        }

        public int blockY() {
            return y;
        }

        public int blockZ() {
            return z;
        }

        public int adjacentX() {
            return x + face.dx();
        }

        public int adjacentY() {
            return y + face.dy();
        }

        public int adjacentZ() {
            return z + face.dz();
        }

        public Direction face() {
            return face;
        }

        public float distance() {
            return distance;
        }
    }
}
