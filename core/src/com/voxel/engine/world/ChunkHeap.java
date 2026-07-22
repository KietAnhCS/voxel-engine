package com.voxel.engine.world;

/**
 * Hang doi uu tien cai bang BINARY HEAP (heap nhi phan).
 *
 * Heap la mot CAY NHI PHAN HOAN CHINH nhung khong luu bang node va con tro - no duoc
 * "trai phang" ra mang, nen quan he cha con chi la phep tinh chi so:
 *
 *        cha cua i     = (i - 1) / 2
 *        con trai cua i = 2i + 1
 *        con phai cua i = 2i + 2
 *
 *              0                     mang: [0][1][2][3][4][5][6]
 *            /   \
 *           1     2                  Cach luu nay khong ton bo nho cho con tro
 *          / \   / \                 va cac node nam lien nhau trong RAM nen
 *         3   4 5   6                duyet rat nhanh.
 *
 * Tinh chat MIN-HEAP: moi node luon co do uu tien <= hai con cua no. Suy ra goc
 * (chi so 0) luon la phan tu nho nhat -> lay ra trong O(1).
 *
 * Do phuc tap:
 *   push()   O(log n)   di nguoc len tu la toi goc
 *   pop()    O(log n)   dua node cuoi len goc roi chim xuong
 *   peek()   O(1)
 *   remove() O(n)       phai quet tim vi tri truoc, sau do sua lai heap trong O(log n)
 */
public final class ChunkHeap {

    private Chunk[] items;
    private int[] priorities;
    private int size;

    public ChunkHeap(int initialCapacity) {
        int capacity = Math.max(1, initialCapacity);
        this.items = new Chunk[capacity];
        this.priorities = new int[capacity];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** Them mot chunk vao cay roi day no LEN toi dung cho. */
    public void push(Chunk chunk, int priority) {
        if (size == items.length) {
            grow();
        }
        items[size] = chunk;
        priorities[size] = priority;
        siftUp(size);
        size++;
    }

    /** Lay ra chunk co do uu tien nho nhat (gan nguoi choi nhat). */
    public Chunk pop() {
        if (size == 0) {
            return null;
        }

        Chunk top = items[0];
        size--;

        if (size > 0) {
            // Node cuoi cung nhay len lam goc de cay van "hoan chinh", roi cho no CHIM xuong.
            items[0] = items[size];
            priorities[0] = priorities[size];
            siftDown(0);
        }

        items[size] = null;
        return top;
    }

    /** Bo mot chunk cu the ra khoi cay (vi du khi no vua bi go khoi the gioi). */
    public boolean remove(Chunk chunk) {
        for (int i = 0; i < size; i++) {
            if (items[i] != chunk) {
                continue;
            }

            size--;
            if (i == size) {
                items[size] = null;
                return true;
            }

            items[i] = items[size];
            priorities[i] = priorities[size];
            items[size] = null;

            // Phan tu chuyen toi day co the qua lon hoac qua nho, thu ca hai chieu.
            siftDown(i);
            siftUp(i);
            return true;
        }
        return false;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            items[i] = null;
        }
        size = 0;
    }

    /** Doi node len phia goc chung nao no con nho hon cha. */
    private void siftUp(int index) {
        Chunk chunk = items[index];
        int priority = priorities[index];

        while (index > 0) {
            int parent = (index - 1) / 2;
            if (priorities[parent] <= priority) {
                break;
            }
            items[index] = items[parent];
            priorities[index] = priorities[parent];
            index = parent;
        }

        items[index] = chunk;
        priorities[index] = priority;
    }

    /** Doi node xuong phia la chung nao no con lon hon dua con nho nhat. */
    private void siftDown(int index) {
        Chunk chunk = items[index];
        int priority = priorities[index];
        int half = size / 2;

        // Chi so >= size/2 la node la, khong con con nao de so sanh nua.
        while (index < half) {
            int child = index * 2 + 1;
            int right = child + 1;
            if (right < size && priorities[right] < priorities[child]) {
                child = right;
            }
            if (priorities[child] >= priority) {
                break;
            }
            items[index] = items[child];
            priorities[index] = priorities[child];
            index = child;
        }

        items[index] = chunk;
        priorities[index] = priority;
    }

    private void grow() {
        int capacity = items.length << 1;

        Chunk[] grownItems = new Chunk[capacity];
        int[] grownPriorities = new int[capacity];
        for (int i = 0; i < size; i++) {
            grownItems[i] = items[i];
            grownPriorities[i] = priorities[i];
        }

        items = grownItems;
        priorities = grownPriorities;
    }
}
