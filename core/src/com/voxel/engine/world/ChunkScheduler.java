package com.voxel.engine.world;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Quyet dinh chunk nao duoc dung mesh truoc. Ba cau truc du lieu, moi cai mot viec:
 *
 *  - NGAN XEP (stack) cho viec gap: nguoi choi vua dap/pha mot khoi. Vao sau ra truoc,
 *    vi thao tac vua lam moi la thu nguoi choi dang nhin -> phai thay ngay.
 *
 *  - HANG DOI UU TIEN ({@link ChunkHeap}) cho viec nen: chunk moi sinh ra.
 *    Chunk gan nguoi choi nhat ra truoc, bat ke no vao hang luc nao.
 *
 *  - TAP HOP BAM (hash set) de kiem tra "chunk nay da nam trong hang chua?" trong O(1).
 *    Neu khong co no thi moi lan submit phai quet ca hang -> O(n).
 *
 * Tat ca phuong thuc deu synchronized vi luong sinh chunk, luong mesh va luong ve
 * hinh cung goi vao day.
 */
public final class ChunkScheduler {

    private final Deque<Chunk> urgent = new ArrayDeque<Chunk>();
    private final ChunkHeap background = new ChunkHeap(256);
    private final Set<Chunk> queued = new HashSet<Chunk>();

    /**
     * Do phuc tap: O(1) cho viec gap, O(log n) cho viec nen.
     *
     * @param isUrgent true = nguoi choi vua sua khoi, day vao ngan xep
     * @param priority khoang cach toi nguoi choi (cang nho cang duoc lam truoc)
     */
    public synchronized void submit(Chunk chunk, boolean isUrgent, int priority) {
        if (!queued.add(chunk)) {
            return;
        }
        if (isUrgent) {
            urgent.push(chunk);
        } else {
            background.push(chunk, priority);
        }
    }

    /**
     * Viec gap luon duoc uu tien hon toan bo hang nen.
     *
     * Do phuc tap: O(log n).
     */
    public synchronized Chunk poll() {
        Chunk chunk = urgent.isEmpty() ? background.pop() : urgent.pop();
        if (chunk != null) {
            queued.remove(chunk);
        }
        return chunk;
    }

    /** Do phuc tap: O(n) - phai tim chunk trong ngan xep va trong heap. */
    public synchronized void forget(Chunk chunk) {
        if (queued.remove(chunk)) {
            urgent.remove(chunk);
            background.remove(chunk);
        }
    }

    public synchronized int pending() {
        return queued.size();
    }

    public synchronized void clear() {
        urgent.clear();
        background.clear();
        queued.clear();
    }
}
