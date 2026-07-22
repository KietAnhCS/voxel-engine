package com.voxel.engine.world;

import com.voxel.engine.block.Block;
import com.voxel.engine.block.BlockRegistry;
import com.voxel.engine.util.Direction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Nuoc chay kieu Minecraft.
 *
 * Moi o nuoc co mot MUC tu 1 den 8: 8 la khoi nguon (day o), so cang nho o cang can.
 * Toan bo hanh vi nam trong DUY NHAT mot cong thuc, ap cho o khong khi va o nuoc
 * khong phai nguon:
 *
 *     muc moi = 8                              neu co tu 2 khoi nguon ke ben (bien lan ra)
 *             = 7                              neu ngay tren dau co nuoc (nuoc do xuong)
 *             = max(muc 4 o ben canh) - 1      nguoc lai, toi thieu la 0
 *
 * Muc 0 nghia la o tro lai thanh khong khi. Mot cong thuc lo ca hai chieu:
 *  - Dat nguon: cac o quanh nguon nhan 7, roi 6, 5... nen nuoc loang ra dung 7 o.
 *  - Mat nguon: muc lon nhat con lai cua vung phai giam it nhat 1 sau moi luot
 *    (mot o chi giu duoc muc M khi co hang xom muc M+1), nen nuoc tu rut can het.
 *    Khong can viet luat rieng cho viec rut.
 *
 * Nuoc bat dau chay tu hai nguon: nguoi choi dat/pha khoi ({@link #schedule}), va chinh
 * dia hinh vua sinh ra ({@link #seed}) - nho the bien va ho tu do vao hang ngam ma khong
 * can ai cham vao.
 *
 * Cach chay: giu mot HANG DOI cac o can tinh lai. Moi luot lay ra TOAN BO hang doi
 * hien tai roi tinh; o nao thay doi thi {@link World#setBlock} lai day 6 o ke vao
 * hang doi cho luot sau. Nho vay moi luot dong nuoc chi lan them dung mot o - dong
 * chay bo dan ra chu khong hien ra tuc thi.
 *
 * Do phuc tap: O(k) moi luot voi k = so o dang cho. Mot lan dat nuoc chi cham toi
 * cac o trong ban kinh lan toa (7 o) nen k bi chan, khong phu thuoc kich thuoc the gioi.
 */
public final class FluidSimulator {

    /** Muc cua nuoc dang roi tu tren xuong: gan day o nhung khong phai khoi nguon. */
    private static final int FALLING_LEVEL = 7;
    /** Cu bay nhieu buoc vat ly thi nuoc lan them mot o: 4 luot/giay, dung nhip Minecraft. */
    private static final int STEPS_PER_SPREAD = 15;
    /** Chan tren so o xu ly moi luot de mot vu no nuoc khong lam khung hinh khung lai. */
    private static final int MAX_CELLS_PER_SPREAD = 4096;

    private static final Direction[] SIDES = {
            Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};

    private final World world;
    /** byLevel[0] = khong khi, byLevel[1..7] = nuoc dang chay, byLevel[8] = khoi nguon. */
    private final Block[] byLevel;
    /**
     * Hop thu cac o vua duoc ghi ten. Chunk duoc sinh o luong rieng nen {@link #seed}
     * chay ngoai luong chinh - hop thu la cho duy nhat hai luong gap nhau.
     */
    private final Queue<Long> inbox = new ConcurrentLinkedQueue<Long>();
    private final Set<Long> pending = new LinkedHashSet<Long>();
    private final List<Long> batch = new ArrayList<Long>();
    /** Cac chunk da doi trong luot nay, de chi tinh lai anh sang MOT lan cho moi chunk. */
    private final Set<Chunk> dirty = new LinkedHashSet<Chunk>();
    private int steps;

    public FluidSimulator(World world, Block[] byLevel) {
        if (byLevel.length != Block.MAX_FLUID_LEVEL + 1) {
            throw new IllegalArgumentException("can du " + (Block.MAX_FLUID_LEVEL + 1) + " muc nuoc");
        }
        this.world = world;
        this.byLevel = byLevel.clone();
    }

    /** Bao rang o nay va 6 o ke can duoc tinh lai o luot sau. */
    public void schedule(int x, int y, int z) {
        inbox.add(pack(x, y, z));
        for (int i = 0; i < Direction.ALL.length; i++) {
            Direction side = Direction.ALL[i];
            inbox.add(pack(x + side.dx(), y + side.dy(), z + side.dz()));
        }
    }

    /**
     * Ghi ten nhung o nuoc co the chay ngay khi chunk vua sinh xong, de bien va ho tu
     * do vao cac hang ngam bi khoet trung vao thanh - khong can nguoi choi dao nhat nao.
     *
     * Chi ghi ten o nuoc CO LOI THOAT: ben duoi hoac mot trong 4 ben la o trong. Mat
     * bien phang li khong o nao thoa dieu kien nen ca dai duong khong bi danh thuc oan.
     * O ke nam ngoai chunk thi bo qua - chunk ben canh se tu quet phan cua no.
     *
     * Do phuc tap: O(size^2 * height) mot lan cho moi chunk, va chi doc mang trong chunk.
     */
    public void seed(Chunk chunk) {
        ChunkStorage storage = chunk.storage();
        BlockRegistry registry = chunk.registry();
        int size = storage.size();

        for (int y = 1; y < storage.height(); y++) {
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    if (!registry.byId(storage.blockId(x, y, z)).isLiquid()) {
                        continue;
                    }
                    if (hasOutlet(storage, registry, x, y, z)) {
                        schedule(chunk.originX() + x, y, chunk.originZ() + z);
                    }
                }
            }
        }
    }

    private boolean hasOutlet(ChunkStorage storage, BlockRegistry registry, int x, int y, int z) {
        if (isOpen(storage, registry, x, y - 1, z)) {
            return true;
        }
        for (int i = 0; i < SIDES.length; i++) {
            if (isOpen(storage, registry, x + SIDES[i].dx(), y, z + SIDES[i].dz())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOpen(ChunkStorage storage, BlockRegistry registry, int x, int y, int z) {
        return storage.contains(x, y, z) && registry.byId(storage.blockId(x, y, z)).isAir();
    }

    /** Goi moi buoc vat ly. */
    public void tick() {
        if (++steps < STEPS_PER_SPREAD) {
            return;
        }
        steps = 0;

        for (Long cell = inbox.poll(); cell != null; cell = inbox.poll()) {
            pending.add(cell);
        }
        if (pending.isEmpty()) {
            return;
        }

        // Chep hang doi ra roi xoa: nhung o duoc them trong luc tinh se thuoc ve luot sau.
        batch.clear();
        batch.addAll(pending);
        pending.clear();

        int handled = Math.min(batch.size(), MAX_CELLS_PER_SPREAD);
        for (int i = 0; i < handled; i++) {
            update(batch.get(i));
        }
        for (int i = handled; i < batch.size(); i++) {
            pending.add(batch.get(i));
        }

        // Ca luot chi tinh lai anh sang va dung lai hinh MOT lan cho moi chunk, thay vi
        // mot lan cho moi o vua doi. Day la cho quyet dinh muot hay giat.
        for (Chunk chunk : dirty) {
            world.relightAsync(chunk, true);
        }
        dirty.clear();
    }

    private void update(long cell) {
        int x = unpackX(cell);
        int y = unpackY(cell);
        int z = unpackZ(cell);

        Block current = world.blockAt(x, y, z);
        if (current.fluidLevel() == Block.MAX_FLUID_LEVEL) {
            return;                                     // khoi nguon khong bao gio can di
        }
        if (!current.isAir() && !current.isLiquid()) {
            return;                                     // dat da: nuoc khong vao duoc
        }

        int target = inflow(x, y, z);
        if (target != current.fluidLevel() && world.setBlockDeferred(x, y, z, byLevel[target])) {
            markDirty(x, z);
        }
    }

    /**
     * Ghi nho chunk chua o vua doi, va ca cac chunk ke ben: o nam sat bien thi hinh va
     * anh sang cua chunk ben canh cung phai dung lai.
     */
    private void markDirty(int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Chunk chunk = world.chunkContaining(x + dx, z + dz);
                if (chunk != null) {
                    dirty.add(chunk);
                }
            }
        }
    }

    /** Muc nuoc ma o nay se co o luot sau - chinh la cong thuc mo ta trong javadoc lop. */
    private int inflow(int x, int y, int z) {
        int best = 0;
        int sources = 0;
        for (int i = 0; i < SIDES.length; i++) {
            int nx = x + SIDES[i].dx();
            int nz = z + SIDES[i].dz();
            int level = world.blockAt(nx, y, nz).fluidLevel();
            if (level == Block.MAX_FLUID_LEVEL) {
                sources++;
            }
            if (level - 1 > best && !spillsDown(nx, y, nz)) {
                best = level - 1;
            }
        }

        // Nam giua hai khoi nguon thi chinh minh thanh nguon. Nho luat nay mot ranh dao
        // thong ra bien se day len thanh bien that chu khong chi la mot vet nuoc can, va
        // nuoc trong bien khong bao gio "chay het" ra ngoai.
        if (sources >= 2) {
            return Block.MAX_FLUID_LEVEL;
        }
        if (world.blockAt(x, y + 1, z).isLiquid()) {
            return FALLING_LEVEL;
        }
        return best;
    }

    /**
     * O nuoc nay co cho trong ben duoi khong? Neu co thi no do thang xuong chu khong
     * loang sang ngang - nho the nuoc roi tu tren cao thanh mot cot thay vi mot dam
     * bet ra giua khong trung, dung nhu Minecraft.
     */
    private boolean spillsDown(int x, int y, int z) {
        Block below = world.blockAt(x, y - 1, z);
        return below.isAir() || (below.isLiquid() && below.fluidLevel() < Block.MAX_FLUID_LEVEL);
    }

    /**
     * Nhet toa do the gioi vao mot long de lam khoa cho hang doi: 26 bit cho x, 12 bit
     * cho y, 26 bit cho z. Nho vay khong phai cap phat doi tuong toa do cho tung o.
     */
    private static long pack(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFFL) << 26 | ((long) z & 0x3FFFFFFL);
    }

    private static int unpackX(long cell) {
        return (int) (cell >> 38);
    }

    private static int unpackY(long cell) {
        return (int) (cell >> 26) & 0xFFF;
    }

    private static int unpackZ(long cell) {
        return (int) (cell << 38 >> 38);
    }
}
