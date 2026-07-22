package com.voxel.game.terrain.carve;

import com.voxel.engine.generation.Carver;
import com.voxel.engine.world.ChunkWriter;
import com.voxel.game.Blocks;
import com.voxel.game.terrain.TerrainNoise;

import java.util.Random;

/**
 * PERLIN WORM - hang dong dang duong ham.
 *
 * Y tuong: tha mot "con giun" di trong long dat. Moi buoc no tien ve phia truoc mot khoi
 * va khoet rong xung quanh minh mot qua cau. Huong di khong random hoan toan ma duoc lai
 * boi nhieu Perlin/Simplex, nen duong ham uon luon muot chu khong giat cuc.
 *
 * De hang chay lien tuc qua nhieu chunk, khi sinh chunk (cx, cz) ta duyet tat ca chunk
 * trong ban kinh {@link #CHUNK_RADIUS}, tai tao dung nhung con giun cua chung (cung seed
 * -> cung duong di) va chi khoet phan roi vao chunk hien tai.
 */
public final class PerlinWormCarver implements Carver {

    /** Ban kinh (tinh bang chunk) can quet de bat het cac con giun co the boi toi day. */
    private static final int CHUNK_RADIUS = 6;
    /** Trung binh 1 tren bao nhieu chunk thi sinh mot he thong hang. */
    private static final int SPAWN_RARITY = 5;
    /** So lan mot duong ham duoc phep de nhanh con. */
    private static final int MAX_BRANCH_DEPTH = 2;

    private final TerrainNoise noise;
    private final Blocks blocks;
    private final long seed;

    public PerlinWormCarver(TerrainNoise noise, Blocks blocks, long seed) {
        this.noise = noise;
        this.blocks = blocks;
        this.seed = seed;
    }

    @Override
    public void carve(ChunkWriter writer, int chunkX, int chunkZ, int chunkSize, int worldHeight) {
        CarveScope scope = new CarveScope(writer, blocks,
                writer.originX(), writer.originZ(), chunkSize, worldHeight);

        for (int offsetX = -CHUNK_RADIUS; offsetX <= CHUNK_RADIUS; offsetX++) {
            for (int offsetZ = -CHUNK_RADIUS; offsetZ <= CHUNK_RADIUS; offsetZ++) {
                spawnSystem(scope, chunkX + offsetX, chunkZ + offsetZ, chunkSize);
            }
        }
    }

    private void spawnSystem(CarveScope scope, int sourceChunkX, int sourceChunkZ, int chunkSize) {
        Random random = new Random(CaveSeeds.forChunk(seed, sourceChunkX, sourceChunkZ, 1));
        if (random.nextInt(SPAWN_RARITY) != 0) {
            return;
        }

        int tunnels = 1 + random.nextInt(3);
        for (int i = 0; i < tunnels; i++) {
            double startX = sourceChunkX * chunkSize + random.nextInt(chunkSize);
            double startZ = sourceChunkZ * chunkSize + random.nextInt(chunkSize);
            double startY = 9 + random.nextInt(44);

            double yaw = random.nextDouble() * Math.PI * 2.0;
            double pitch = (random.nextDouble() - 0.5) * 0.4;
            double radius = 1.9 + random.nextDouble() * 1.9;
            int steps = 70 + random.nextInt(110);

            digTunnel(scope, random, startX, startY, startZ, yaw, pitch, radius, steps, 0);
        }
    }

    /**
     * Ham DE QUY: dao mot duong ham; thinh thoang no goi lai chinh no de tao nhanh re.
     *
     * Truong hop co so: depth == MAX_BRANCH_DEPTH -> khong de nhanh nua, chi dao thang.
     * Moi nhanh con ngan bang nua nhanh cha (steps / 2) nen de quy chac chan dung.
     *
     * Do phuc tap: O(steps * r^3) cho mot duong ham; toan bo buoc khoet hang cua mot
     * chunk la O(CHUNK_RADIUS^2 * steps * r^3) vi phai tai tao giun cua cac chunk lan can.
     * Day la ly do CHUNK_RADIUS duoc giu nho (6) - tang len la thoi gian sinh chunk tang binh phuong.
     */
    private void digTunnel(CarveScope scope, Random random,
                           double x, double y, double z,
                           double yaw, double pitch, double radius, int steps, int depth) {
        double wobble = random.nextDouble() * 512.0;
        double cursorX = x;
        double cursorY = y;
        double cursorZ = z;
        double heading = yaw;
        double climb = pitch;

        for (int step = 0; step < steps; step++) {
            double t = step * 0.06;

            // Perlin lai huong: doi huong tu tu, muot ma.
            heading += noise.worm(t, wobble) * 0.35;
            climb = climb * 0.92 + noise.worm(t, wobble + 128.0) * 0.14;
            if (climb > 0.55) {
                climb = 0.55;
            }
            if (climb < -0.55) {
                climb = -0.55;
            }

            cursorX += Math.cos(heading) * Math.cos(climb);
            cursorZ += Math.sin(heading) * Math.cos(climb);
            cursorY += Math.sin(climb) * 0.9;

            if (cursorY < 7.0) {
                cursorY = 7.0;
                climb = Math.abs(climb);
            }
            if (cursorY > 86.0) {
                cursorY = 86.0;
                climb = -Math.abs(climb);
            }

            // Thu nho hai dau ham cho tu nhien.
            double taper = Math.sin(Math.PI * (step + 1.0) / (steps + 1.0));
            double stepRadius = radius * (0.55 + 0.75 * taper);

            if (scope.touches(cursorX, cursorZ, stepRadius)) {
                scope.clearSphere(cursorX, cursorY, cursorZ, stepRadius);
            }

            if (depth < MAX_BRANCH_DEPTH && step > 10 && random.nextInt(110) == 0) {
                digTunnel(scope, random, cursorX, cursorY, cursorZ,
                        heading + (random.nextDouble() - 0.5) * 2.4,
                        climb * 0.5, radius * 0.7, steps / 2, depth + 1);
            }
        }
    }
}
