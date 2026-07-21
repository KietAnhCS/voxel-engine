package com.voxel.engine.block.geometry;

import com.badlogic.gdx.graphics.Color;
import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockView;
import com.voxel.engine.util.Direction;

public final class FaceLighting {

    private static final float[] LIGHT_CURVE = new float[Block.MAX_LIGHT + 1];
    private static final float[] OCCLUSION_CURVE = {0.42f, 0.60f, 0.79f, 1.00f};
    private static final float SHADER_BIAS = 0.25f;

    static {
        for (int level = 0; level <= Block.MAX_LIGHT; level++) {
            LIGHT_CURVE[level] = Math.max(0.055f, (float) Math.pow(0.82, Block.MAX_LIGHT - level));
        }
    }

    private final Color tintBuffer = new Color();

    public void shade(BlockView view, int x, int y, int z, Block block, Direction face, Color[] out) {
        block.tint().apply(x, y, z, tintBuffer);

        int nx = x + face.dx();
        int ny = y + face.dy();
        int nz = z + face.dz();

        float alpha = block.isWindAffected() ? 0f : 1f;

        for (int corner = 0; corner < 4; corner++) {
            int uSign = (corner == 1 || corner == 2) ? 1 : -1;
            int vSign = (corner == 2 || corner == 3) ? 1 : -1;

            int ax = nx + face.tangentX() * uSign;
            int ay = ny + face.tangentY() * uSign;
            int az = nz + face.tangentZ() * uSign;

            int bx = nx + face.bitangentX() * vSign;
            int by = ny + face.bitangentY() * vSign;
            int bz = nz + face.bitangentZ() * vSign;

            int cx = ax + face.bitangentX() * vSign;
            int cy = ay + face.bitangentY() * vSign;
            int cz = az + face.bitangentZ() * vSign;

            boolean sideA = view.occludes(ax, ay, az, block);
            boolean sideB = view.occludes(bx, by, bz, block);
            boolean diagonal = view.occludes(cx, cy, cz, block);

            int occlusion = sideA && sideB ? 0 : 3 - (count(sideA) + count(sideB) + count(diagonal));

            int lightSum = view.lightAt(nx, ny, nz);
            int lightCount = 1;
            if (!sideA) {
                lightSum += view.lightAt(ax, ay, az);
                lightCount++;
            }
            if (!sideB) {
                lightSum += view.lightAt(bx, by, bz);
                lightCount++;
            }
            if (!diagonal && !(sideA && sideB)) {
                lightSum += view.lightAt(cx, cy, cz);
                lightCount++;
            }

            int level = Math.min(Block.MAX_LIGHT, lightSum / lightCount);
            float brightness = LIGHT_CURVE[level] * OCCLUSION_CURVE[occlusion] * face.shade();

            out[corner].set(
                    tintBuffer.r * brightness + SHADER_BIAS,
                    tintBuffer.g * brightness + SHADER_BIAS,
                    tintBuffer.b * brightness + SHADER_BIAS,
                    alpha);
        }
    }

    public void flat(BlockView view, int x, int y, int z, Block block, float shade, Color out) {
        block.tint().apply(x, y, z, tintBuffer);
        int level = Math.min(Block.MAX_LIGHT, view.lightAt(x, y, z));
        float brightness = LIGHT_CURVE[level] * shade;
        out.set(
                tintBuffer.r * brightness + SHADER_BIAS,
                tintBuffer.g * brightness + SHADER_BIAS,
                tintBuffer.b * brightness + SHADER_BIAS,
                block.isWindAffected() ? 0f : 1f);
    }

    private static int count(boolean flag) {
        return flag ? 1 : 0;
    }
}
