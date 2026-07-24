# MergeSort — sắp xếp trộn lai insertion sort

**File:** `core/src/com/voxel/engine/util/MergeSort.java`

Sắp xếp **hai mảng song song**: `keys` (dữ liệu) và `priorities` (khoá so sánh). Phần tử thứ `i` của hai mảng luôn đi cùng nhau.

---

## 1. Thuật toán lai (hybrid)

```java
private void mergeSort(long[] keys, int[] priorities, int low, int high) {
    if (high - low + 1 <= INSERTION_CUTOFF) {          // 12
        insertionSort(keys, priorities, low, high);
        return;
    }
    int middle = low + (high - low) / 2;
    mergeSort(keys, priorities, low, middle);
    mergeSort(keys, priorities, middle + 1, high);

    if (priorities[middle] <= priorities[middle + 1]) return;   // ← tối ưu
    merge(keys, priorities, low, middle, high);
}
```

Đây là cách cài đặt **thực tế của hầu hết thư viện chuẩn** (Java `Arrays.sort`, C++ `std::stable_sort`): merge sort đảm bảo `O(n log n)` ở mọi trường hợp, insertion sort nhanh ở đoạn ngắn.

---

## 2. Chia để trị (divide & conquer)

```
  T(n) = 2·T(n/2) + O(n)
```

Giải bằng **định lý thợ (Master Theorem)** với `a = 2, b = 2, f(n) = n`:

```
  log_b a = log₂ 2 = 1,   f(n) = Θ(n^1)   →  Trường hợp 2
  ⇒ T(n) = Θ(n log n)
```

Trực quan: cây đệ quy có `log₂ n` tầng, mỗi tầng làm tổng cộng `O(n)` việc trộn.

```
  n                        ─┐
  n/2   n/2                 │
  n/4  n/4  n/4  n/4        ├─ log₂n tầng, mỗi tầng O(n)
  …                         │
  1 1 1 1 1 1 1 1 …        ─┘
```

---

## 3. Vì sao cắt sang insertion sort ở `n ≤ 12`?

| | Merge sort | Insertion sort |
|---|---|---|
| Độ phức tạp | `O(n log n)` | `O(n²)` |
| Hằng số ẩn | **lớn** — gọi đệ quy, cấp phát khung ngăn xếp, sao chép sang mảng phụ | **rất nhỏ** — 2 vòng lặp lồng |
| Truy cập bộ nhớ | 2 mảng (gốc + scratch) | **tại chỗ**, liền kề |

Với `n` nhỏ, `c₂·n²` **nhỏ hơn** `c₁·n log n` vì `c₂ ≪ c₁`. Điểm hoà vốn thực nghiệm rơi vào khoảng 7–16 — giá trị 12 nằm giữa.

Ngoài ra, cắt ở 12 giảm **số lần gọi đệ quy** từ `2n − 1` xuống còn `2n/12 − 1` ≈ **giảm 12 lần**.

### `insertionSort`

```java
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
```

Cùng kỹ thuật "hole" như [ChunkHeap.siftUp](ChunkHeap.md): giữ phần tử trong biến tạm, **dịch các phần tử lớn hơn sang phải**, đặt một lần cuối. Không dùng `swap`.

`priorities[j] > priority` (dấu `>` chứ không phải `>=`) ⇒ dừng ngay khi gặp phần tử **bằng** ⇒ **giữ tính ổn định**.

---

## 4. Tối ưu "đã sắp sẵn"

```java
if (priorities[middle] <= priorities[middle + 1]) return;
```

Nếu **phần tử lớn nhất của nửa trái ≤ phần tử nhỏ nhất của nửa phải**, thì hai nửa ghép lại đã đúng thứ tự ⇒ **bỏ qua hoàn toàn** bước `merge`.

### Hiệu quả

| Đầu vào | Số lần merge |
|---|---|
| Đã sắp xếp hoàn toàn | **0** — độ phức tạp tụt xuống `O(n)` |
| Gần như đã sắp | rất ít |
| Ngẫu nhiên | gần như đủ `n log n` |

Trong game, danh sách chunk **hiếm khi xáo trộn hoàn toàn** giữa hai khung hình — người chơi chỉ di chuyển một chút nên thứ tự ưu tiên gần như giữ nguyên. Tối ưu này ăn điểm rất lớn ở đây.

### `middle = low + (high − low) / 2`

Không viết `(low + high) / 2` để tránh **tràn số nguyên** khi `low + high > 2³¹ − 1`. Đây là lỗi nổi tiếng từng tồn tại 9 năm trong `java.util.Arrays.binarySearch` của JDK (Joshua Bloch công bố 2006).

---

## 5. `merge` — trộn hai đoạn đã sắp

```java
for (int i = low; i <= high; i++) {           // sao chép ra mảng phụ
    keyScratch[i] = keys[i];
    priorityScratch[i] = priorities[i];
}

int left = low, right = middle + 1;
for (int slot = low; slot <= high; slot++) {
    if (left > middle)                          { lấy từ phải; }
    else if (right > high)                      { lấy từ trái; }
    else if (priorityScratch[right] < priorityScratch[left]) { lấy từ phải; }
    else                                        { lấy từ trái; }   // ← ổn định
}
```

### Bốn nhánh

| Điều kiện | Hành động |
|---|---|
| Nửa trái đã hết | Lấy từ phải |
| Nửa phải đã hết | Lấy từ trái |
| Phải **thực sự nhỏ hơn** trái | Lấy từ phải |
| Ngược lại (kể cả **bằng nhau**) | **Lấy từ trái** |

### Tính ổn định (stability)

> Comment trong code: *"Dấu `<` ở trên (không phải `<=`) chính là chỗ giữ tính ổn định."*

Khi hai phần tử **bằng độ ưu tiên**, luôn lấy phần tử **bên trái** trước ⇒ thứ tự ban đầu được bảo toàn.

### Vì sao game cần ổn định?

> Comment: *"nhờ vậy khi nhiều chunk cách người chơi bằng nhau, chúng vẫn được sinh theo thứ tự quét cố định → thế giới sinh ra giống hệt nhau mỗi lần chạy."*

Rất nhiều chunk có **cùng khoảng cách** tới người chơi (các chunk trên cùng một vòng tròn). Nếu sắp xếp không ổn định, thứ tự sinh của chúng thay đổi giữa các lần chạy ⇒ khó tái hiện lỗi, và thứ tự carve/decorate có thể khác nhau.

---

## 6. Mảng phụ tái sử dụng

```java
private long[] keyScratch = new long[0];
private int[] priorityScratch = new int[0];

public void sort(long[] keys, int[] priorities, int count) {
    if (count < 2) return;
    if (keyScratch.length < count) {                 // chỉ cấp phát khi CẦN LỚN HƠN
        keyScratch = new long[count];
        priorityScratch = new int[count];
    }
    mergeSort(keys, priorities, 0, count - 1);
}
```

`MergeSort` là **object có trạng thái** (không phải static utility) để giữ mảng phụ qua các lần gọi.

- Lần đầu: cấp phát.
- Các lần sau: **tái dùng**, chỉ cấp phát lại nếu cần lớn hơn.
- Mảng chỉ **lớn lên**, không bao giờ co lại ⇒ sau vài khung hình, không còn cấp phát nào.

Đây là điểm khác biệt lớn so với `Arrays.sort()` của Java — hàm đó cấp phát mảng phụ **mỗi lần gọi**, sinh rác GC trong vòng lặp game.

---

## 7. Độ phức tạp

| | Tốt nhất | Trung bình | Xấu nhất |
|---|---|---|---|
| **Thời gian** | `O(n)` * | `O(n log n)` | **`O(n log n)`** |
| **Bộ nhớ phụ** | `O(n)` | `O(n)` | `O(n)` |
| **Ổn định** | ✔ | ✔ | ✔ |

\* nhờ tối ưu "đã sắp sẵn"; không có nó thì tốt nhất cũng là `O(n log n)`.

### So sánh với các thuật toán khác

| Thuật toán | Trung bình | Xấu nhất | Bộ nhớ | Ổn định |
|---|---|---|---|---|
| **Merge sort** | `O(n log n)` | **`O(n log n)`** | `O(n)` | **✔** |
| Quick sort | `O(n log n)` | **`O(n²)`** | `O(log n)` | ✗ |
| Heap sort | `O(n log n)` | `O(n log n)` | `O(1)` | ✗ |
| Insertion sort | `O(n²)` | `O(n²)` | `O(1)` | ✔ |

Chọn merge sort vì cần **ổn định** và cần **đảm bảo `O(n log n)` ở trường hợp xấu nhất** (game không được phép giật đột ngột).

---

## 8. Chủ đề DSA thể hiện

- **Chia để trị** & định lý thợ.
- **Thuật toán lai** (hybrid) — cắt sang insertion sort.
- **Tính ổn định** của sắp xếp và vì sao nó quan trọng.
- **Phòng tràn số nguyên** khi tính trung điểm.
- **Mảng song song**.
- **Tái sử dụng bộ đệm** để tránh rác GC.
- **Tối ưu trường hợp đặc biệt** (đã sắp sẵn).

---

## 9. Liên kết

- Anh em: [ChunkHeap.md](ChunkHeap.md), [IntQueue.md](IntQueue.md)
