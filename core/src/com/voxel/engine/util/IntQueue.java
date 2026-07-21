package com.voxel.engine.util;

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
