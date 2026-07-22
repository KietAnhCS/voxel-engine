package com.voxel.engine.util;

/**
 * Sap xep hai mang song song: {@code keys} la du lieu can sap, {@code priorities}
 * la khoa so sanh. Phan tu thu i cua hai mang luon di cung nhau.
 *
 * Dung MERGE SORT (chia de tri) va chuyen sang INSERTION SORT khi doan da du ngan.
 * Do la cach cai dat thuc te cua hau het thu vien: merge sort cho dam bao O(n log n)
 * o moi truong hop, insertion sort cho nhanh o doan ngan vi khong phai goi de quy
 * va truy cap bo nho lien tuc.
 *
 * Merge sort la thuat toan ON DINH (stable): hai phan tu cung do uu tien giu nguyen
 * thu tu ban dau. Nho vay khi nhieu chunk cach nguoi choi bang nhau, chung van duoc
 * sinh theo thu tu quet co dinh -> the gioi sinh ra giong het nhau moi lan chay.
 *
 * Do phuc tap:
 *   - Thoi gian : O(n log n) o ca truong hop tot, trung binh va xau nhat
 *   - Bo nho    : O(n) cho mang phu (khong cap phat lai o moi lan goi)
 */
public final class MergeSort {

    /** Doan ngan hon nguong nay thi insertion sort nhanh hon merge sort. */
    private static final int INSERTION_CUTOFF = 12;

    private long[] keyScratch = new long[0];
    private int[] priorityScratch = new int[0];

    /**
     * Sap xep {@code count} phan tu dau tien theo do uu tien tang dan.
     *
     * @param keys       du lieu di kem (bi hoan vi theo priorities)
     * @param priorities khoa so sanh, nho hon = xep truoc
     * @param count      so phan tu thuc su dang dung trong hai mang
     */
    public void sort(long[] keys, int[] priorities, int count) {
        if (count < 2) {
            return;
        }
        if (keyScratch.length < count) {
            keyScratch = new long[count];
            priorityScratch = new int[count];
        }
        mergeSort(keys, priorities, 0, count - 1);
    }

    /** Chia doi doan [low..high], sap xep tung nua roi tron lai. */
    private void mergeSort(long[] keys, int[] priorities, int low, int high) {
        if (high - low + 1 <= INSERTION_CUTOFF) {
            insertionSort(keys, priorities, low, high);
            return;
        }

        int middle = low + (high - low) / 2;
        mergeSort(keys, priorities, low, middle);
        mergeSort(keys, priorities, middle + 1, high);

        // Neu nua trai da nho hon toan bo nua phai thi doan nay von da sap xep roi.
        if (priorities[middle] <= priorities[middle + 1]) {
            return;
        }
        merge(keys, priorities, low, middle, high);
    }

    /** Tron hai doan da sap xep [low..middle] va [middle+1..high] thanh mot. */
    private void merge(long[] keys, int[] priorities, int low, int middle, int high) {
        for (int i = low; i <= high; i++) {
            keyScratch[i] = keys[i];
            priorityScratch[i] = priorities[i];
        }

        int left = low;
        int right = middle + 1;

        for (int slot = low; slot <= high; slot++) {
            if (left > middle) {
                keys[slot] = keyScratch[right];
                priorities[slot] = priorityScratch[right];
                right++;
            } else if (right > high) {
                keys[slot] = keyScratch[left];
                priorities[slot] = priorityScratch[left];
                left++;
            } else if (priorityScratch[right] < priorityScratch[left]) {
                keys[slot] = keyScratch[right];
                priorities[slot] = priorityScratch[right];
                right++;
            } else {
                // Dau "<" o tren (khong phai "<=") chinh la cho giu tinh on dinh.
                keys[slot] = keyScratch[left];
                priorities[slot] = priorityScratch[left];
                left++;
            }
        }
    }

    /**
     * Insertion sort: lay tung phan tu roi day nguoc ve dung cho cua no.
     * O(n^2) o truong hop xau nhung rat nhanh voi n nho hoac mang gan nhu da sap xep.
     */
    private static void insertionSort(long[] keys, int[] priorities, int low, int high) {
        for (int i = low + 1; i <= high; i++) {
            long key = keys[i];
            int priority = priorities[i];

            int j = i - 1;
            while (j >= low && priorities[j] > priority) {
                keys[j + 1] = keys[j];
                priorities[j + 1] = priorities[j];
                j--;
            }

            keys[j + 1] = key;
            priorities[j + 1] = priority;
        }
    }
}
