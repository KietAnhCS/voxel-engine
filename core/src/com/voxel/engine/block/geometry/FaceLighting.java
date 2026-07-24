package com.voxel.engine.block.geometry;

import com.badlogic.gdx.graphics.Color;
import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockView;
import com.voxel.engine.util.Direction;

/**
 * Tinh mau cho bon goc cua mot mat khoi: do sang, bong o goc (ambient occlusion) va sac
 * mau cua khoi.
 *
 * <p>Kenh ALPHA khong dung de trong suot ma cho hai thong tin ma shader can:
 * <pre>
 *   alpha &lt; 0.5  : khoi dua theo gio (co, hoa) - shader lam no lac lu
 *   phan le      : ANH SANG NAY DEN TU BAU TROI bao nhieu (0 = toan do duoc, 1 = toan do troi)
 * </pre>
 * Nho phan le do ma khi mat troi lan, shader chi lam toi phan anh sang troi va giu nguyen
 * quang duoc/den - dung nhu Minecraft, khong phai bam lai luoi ca the gioi moi lan troi toi.
 */
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

        boolean windy = block.isWindAffected();

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

            // Lay rieng anh sang TROI va anh sang DUOC cua cac o quanh goc nay.
            int skySum = view.skyLightAt(nx, ny, nz);
            int torchSum = view.blockLightAt(nx, ny, nz);
            int lightCount = 1;
            if (!sideA) {
                skySum += view.skyLightAt(ax, ay, az);
                torchSum += view.blockLightAt(ax, ay, az);
                lightCount++;
            }
            if (!sideB) {
                skySum += view.skyLightAt(bx, by, bz);
                torchSum += view.blockLightAt(bx, by, bz);
                lightCount++;
            }
            if (!diagonal && !(sideA && sideB)) {
                skySum += view.skyLightAt(cx, cy, cz);
                torchSum += view.blockLightAt(cx, cy, cz);
                lightCount++;
            }

            int skyLevel = Math.min(Block.MAX_LIGHT, skySum / lightCount);
            int torchLevel = Math.min(Block.MAX_LIGHT, torchSum / lightCount);
            int level = Math.max(skyLevel, torchLevel);
            float brightness = LIGHT_CURVE[level] * OCCLUSION_CURVE[occlusion] * face.shade();

            out[corner].set(
                    tintBuffer.r * brightness + SHADER_BIAS,
                    tintBuffer.g * brightness + SHADER_BIAS,
                    tintBuffer.b * brightness + SHADER_BIAS,
                    packSkyShare(skyShare(skyLevel, torchLevel), windy));
        }
    }

    public void flat(BlockView view, int x, int y, int z, Block block, float shade, Color out) {
        block.tint().apply(x, y, z, tintBuffer);
        int skyLevel = Math.min(Block.MAX_LIGHT, view.skyLightAt(x, y, z));
        int torchLevel = Math.min(Block.MAX_LIGHT, view.blockLightAt(x, y, z));
        int level = Math.max(skyLevel, torchLevel);
        float brightness = LIGHT_CURVE[level] * shade;
        out.set(
                tintBuffer.r * brightness + SHADER_BIAS,
                tintBuffer.g * brightness + SHADER_BIAS,
                tintBuffer.b * brightness + SHADER_BIAS,
                packSkyShare(skyShare(skyLevel, torchLevel), block.isWindAffected()));
    }

    /**
     * Bao nhieu phan do sang o day la CUA BAU TROI (0..1).
     *
     * Cho nao duoc sang bang hoac hon anh sang troi thi tra 0 (troi toi cung khong anh
     * huong), cho nao chi co anh sang troi thi tra 1 (troi toi la toi theo).
     */
    private static float skyShare(int skyLevel, int torchLevel) {
        float sky = LIGHT_CURVE[skyLevel];
        float torch = LIGHT_CURVE[torchLevel];
        return sky <= torch ? 0f : (sky - torch) / sky;
    }

    /** Nhet ti le anh sang troi vao nua tren/nua duoi cua alpha, tuy khoi co lac theo gio khong. */
    private static float packSkyShare(float share, boolean windy) {
        float clamped = Math.max(0f, Math.min(0.999f, share));
        return (windy ? 0f : 0.5f) + clamped * 0.5f;
    }

    private static int count(boolean flag) {
        return flag ? 1 : 0;
    }
}
