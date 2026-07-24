package com.voxel.game.mob;

import com.badlogic.gdx.math.Vector3;
import com.voxel.engine.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Tim duong cho quai vat bang thuat toan A* (A-sao) tren luoi khoi vuong.
 *
 * <p>DSA duoc dung o day:
 * <ul>
 *   <li><b>Do thi</b> an: moi o dat co the dung duoc la mot DINH, buoc sang o ke la mot CANH.</li>
 *   <li><b>Hang doi uu tien</b> ({@link PriorityQueue} = dong nhi phan / binary heap) luon lay ra
 *       o co f = g + h nho nhat de mo rong truoc.</li>
 *   <li><b>Bang bam</b> ({@link HashMap}) luu chi phi g tot nhat va o cha de lan nguoc ra duong.</li>
 *   <li><b>Heuristic</b> h = khoang cach Manhattan - khong bao gio uoc luong VUOT qua chi phi that,
 *       nen A* dam bao tra ve duong ngan nhat (tinh chat "admissible").</li>
 * </ul>
 *
 * <p>So o duoc mo rong bi chan boi {@link #MAX_EXPANSIONS} de mot lan tim duong khong lam khung
 * hinh giat; neu khong toi dich trong gioi han do, tra ve duong toi o GAN dich nhat da tim thay.
 */
public final class AStarPathFinder {

    /** Bon huong di ngang: dong, tay, nam, bac. */
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    /** Chan tren so o duyet moi lan tim duong -> gioi han chi phi, tranh lag. */
    private static final int MAX_EXPANSIONS = 500;

    private final World world;
    private final int worldHeight;

    public AStarPathFinder(World world) {
        this.world = world;
        this.worldHeight = world.config().worldHeight();
    }

    /**
     * Tim duong tu {@code from} toi {@code to} (toa do the gioi cua ban chan).
     *
     * @return danh sach diem giua (tam khoi) tu o ke tiep den dich; rong neu khong the di dau ca.
     */
    public List<Vector3> findPath(Vector3 from, Vector3 to) {
        int sx = floor(from.x), sz = floor(from.z);
        int gx = floor(to.x), gz = floor(to.z);
        int sy = standY(sx, floor(from.y), sz);
        int gy = standY(gx, floor(to.y), gz);
        if (sy < 0 || gy < 0) {
            return new ArrayList<Vector3>();
        }

        // Hang doi uu tien sap theo f; bang g tot nhat; tap dong (da xet xong).
        PriorityQueue<Node> open = new PriorityQueue<Node>(Comparator.comparingInt(n -> n.f));
        HashMap<Long, Integer> bestG = new HashMap<Long, Integer>();
        HashSet<Long> closed = new HashSet<Long>();

        Node start = new Node(sx, sy, sz, 0, heuristic(sx, sy, sz, gx, gy, gz), null);
        open.add(start);
        bestG.put(key(sx, sy, sz), 0);

        Node closest = start;         // o gan dich nhat tim duoc (de co duong lui neu ket dich)
        int closestH = heuristic(sx, sy, sz, gx, gy, gz);
        int expansions = 0;

        while (!open.isEmpty() && expansions < MAX_EXPANSIONS) {
            Node cur = open.poll();
            long ck = key(cur.x, cur.y, cur.z);
            if (!closed.add(ck)) {
                continue;                 // da xu ly o nay roi (ban sao cu trong hang doi)
            }
            expansions++;

            if (cur.x == gx && cur.z == gz) {
                return reconstruct(cur);  // toi cung cot voi nguoi choi -> xong
            }

            int h = heuristic(cur.x, cur.y, cur.z, gx, gy, gz);
            if (h < closestH) {
                closestH = h;
                closest = cur;
            }

            for (int[] d : DIRS) {
                int nx = cur.x + d[0];
                int nz = cur.z + d[1];
                int ny = stepY(cur.x, cur.y, cur.z, nx, nz);
                if (ny < 0) {
                    continue;             // khong buoc sang o do duoc (tuong, vuc, tran thap)
                }
                long nk = key(nx, ny, nz);
                if (closed.contains(nk)) {
                    continue;
                }
                int tentativeG = cur.g + 1 + Math.abs(ny - cur.y); // leo doc thi ton hon
                Integer prev = bestG.get(nk);
                if (prev == null || tentativeG < prev) {
                    bestG.put(nk, tentativeG);
                    int f = tentativeG + heuristic(nx, ny, nz, gx, gy, gz);
                    open.add(new Node(nx, ny, nz, tentativeG, f, cur));
                }
            }
        }

        // Het gio / bi tuong chan: di ve phia o gan nguoi choi nhat da tim ra.
        return reconstruct(closest);
    }

    /** Lan nguoc chuoi o cha thanh danh sach diem giua, bo o dang dung. */
    private List<Vector3> reconstruct(Node node) {
        ArrayList<Vector3> path = new ArrayList<Vector3>();
        for (Node n = node; n != null; n = n.parent) {
            path.add(new Vector3(n.x + 0.5f, n.y, n.z + 0.5f));
        }
        Collections.reverse(path);
        if (!path.isEmpty()) {
            path.remove(0);
        }
        return path;
    }

    /** Do dung duoc cua o (x,y,z): chan trong khong khi, dau trong khong khi, duoi la khoi ran. */
    private boolean walkable(int x, int y, int z) {
        if (y < 1 || y >= worldHeight - 1) {
            return false;
        }
        return world.blockAt(x, y, z).isAir()
                && world.blockAt(x, y + 1, z).isAir()
                && world.blockAt(x, y - 1, z).isCollidable();
    }

    /** Tim o dung duoc gan {@code y} nhat trong cot (x,z); -1 neu khong co. */
    private int standY(int x, int y, int z) {
        int hi = Math.min(worldHeight - 2, y + 2);
        int lo = Math.max(1, y - 3);
        for (int yy = hi; yy >= lo; yy--) {
            if (walkable(x, yy, z)) {
                return yy;
            }
        }
        return -1;
    }

    /**
     * Cao do dap xuong khi buoc tu (cx,cy,cz) sang cot ke (nx,nz): uu tien cung cao do,
     * roi buoc len 1 (can du tran de nhay), roi buoc xuong 1. -1 neu khong buoc duoc.
     */
    private int stepY(int cx, int cy, int cz, int nx, int nz) {
        if (walkable(nx, cy, nz)) {
            return cy;
        }
        if (walkable(nx, cy + 1, nz) && world.blockAt(cx, cy + 2, cz).isAir()) {
            return cy + 1;
        }
        if (walkable(nx, cy - 1, nz)) {
            return cy - 1;
        }
        return -1;
    }

    /** Khoang cach Manhattan lam heuristic - khong bao gio doan qua that. */
    private static int heuristic(int x, int y, int z, int gx, int gy, int gz) {
        return Math.abs(x - gx) + Math.abs(y - gy) + Math.abs(z - gz);
    }

    /** Goi ba toa do khoi thanh mot khoa long duy nhat cho HashMap / HashSet. */
    private static long key(int x, int y, int z) {
        return ((long) (x + 0x80000) << 40) | ((long) (z + 0x80000) << 20) | (long) (y & 0xFFFFF);
    }

    private static int floor(float value) {
        return (int) Math.floor(value);
    }

    /** Mot dinh trong cay tim kiem A*: toa do khoi, chi phi g, uu tien f va o cha. */
    private static final class Node {
        final int x;
        final int y;
        final int z;
        final int g;
        final int f;
        final Node parent;

        Node(int x, int y, int z, int g, int f, Node parent) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.g = g;
            this.f = f;
            this.parent = parent;
        }
    }
}
