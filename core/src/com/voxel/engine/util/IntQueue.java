package com.voxel.engine.util;

/**
 * Hang doi so nguyen cai bang MANG VONG (circular buffer).
 *
 * Hai chi so head/tail chay vong quanh mang: khi cham cuoi thi quay ve 0. Nho vay
 * lay phan tu ra khong phai don dich toan bo mang nhu ArrayList.
 *
 * Luu int nguyen thuy chu khong phai Integer: BFS anh sang day vao hang doi hang
 * trieu chi so moi lan tinh, dung Queue&lt;Integer&gt; se tao hang trieu object rac.
 *
 * Do phuc tap: enqueue O(1) khau hao (thinh thoang phai nhan doi mang),
 *              dequeue O(1), isEmpty O(1).
 */
public final class IntQueue {

    private int[] items;
    private int head;
    private int tail;
    private int size;

    public IntQueue() {
        this(64);
    }

    public IntQueue(int initialCapacity) {
        items = new int[Math.max(1, initialCapacity)];
    }

    public void enqueue(int value) {
        if (size == items.length) {
            grow();
        }
        items[tail] = value;
        tail = tail + 1 == items.length ? 0 : tail + 1;
        size++;
    }

    public int dequeue() {
        int value = items[head];
        head = head + 1 == items.length ? 0 : head + 1;
        size--;
        return value;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void clear() {
        head = 0;
        tail = 0;
        size = 0;
    }

    /**
     * Nhan doi suc chua va "duoi thang" mang vong lai tu chi so 0.
     * O(n) cho mot lan goi, nhung vi moi lan goi lai nhan doi suc chua nen chia deu
     * ra n phep enqueue thi chi phi khau hao chi con O(1) moi phep.
     */
    private void grow() {
        int[] grown = new int[items.length << 1];
        for (int i = 0; i < size; i++) {
            grown[i] = items[(head + i) % items.length];
        }
        items = grown;
        head = 0;
        tail = size;
    }
}
