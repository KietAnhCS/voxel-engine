# ChunkHeap — hàng đợi ưu tiên (binary min-heap)

**File:** `core/src/com/voxel/engine/world/ChunkHeap.java`

Dùng để luôn sinh/nạp **chunk gần người chơi nhất trước**.

---

## 1. Heap là gì

**Cây nhị phân hoàn chỉnh** (complete binary tree) được **trải phẳng ra mảng** — không có node, không có con trỏ. Quan hệ cha–con chỉ là phép tính chỉ số:

```
  cha(i)      = (i − 1) / 2
  con trái(i) = 2i + 1
  con phải(i) = 2i + 2
```

```
            0                     mảng: [0][1][2][3][4][5][6]
          /   \
         1     2
        / \   / \
       3   4 5   6
```

**Tính chất MIN-HEAP:** mọi node có độ ưu tiên **≤** cả hai con của nó.

⇒ Gốc (chỉ số 0) **luôn là phần tử nhỏ nhất** — lấy ra trong `O(1)`.

### Vì sao trải phẳng ra mảng?

| | Cây có con trỏ | Mảng |
|---|---|---|
| Bộ nhớ mỗi node | dữ liệu + 2–3 tham chiếu (16–24 B) | **chỉ dữ liệu** |
| Cục bộ cache | node rải rác khắp heap | **liền kề trong RAM** |
| Tìm cha/con | dereference | **1 phép dịch bit** |

Cây nhị phân **hoàn chỉnh** (mọi tầng đầy, tầng cuối dồn trái) là điều kiện để ánh xạ mảng không có lỗ hổng.

---

## 2. Mảng song song

```java
private Chunk[] items;
private int[] priorities;
```

Phần tử `i` của hai mảng luôn đi cùng nhau. So với việc tạo class `Entry { Chunk c; int p; }`:

- Không cấp phát object bao bọc ⇒ không rác GC.
- `priorities` là mảng `int` **liền kề** ⇒ các phép so sánh trong `siftDown` đọc rất nhanh từ cache.

---

## 3. `push` — siftUp

```java
public void push(Chunk chunk, int priority) {
    if (size == items.length) grow();
    items[size] = chunk;
    priorities[size] = priority;
    siftUp(size);
    size++;
}

private void siftUp(int index) {
    Chunk chunk = items[index];
    int priority = priorities[index];

    while (index > 0) {
        int parent = (index - 1) / 2;
        if (priorities[parent] <= priority) break;      // đã đúng chỗ
        items[index] = items[parent];                    // kéo cha xuống
        priorities[index] = priorities[parent];
        index = parent;
    }
    items[index] = chunk;                                // đặt một lần duy nhất
    priorities[index] = priority;
}
```

### Thuật toán

1. Đặt phần tử mới vào **cuối mảng** (lá phải nhất) — cây vẫn hoàn chỉnh.
2. So với cha; nếu nhỏ hơn thì đi lên.
3. Lặp tới khi gặp cha nhỏ hơn hoặc chạm gốc.

### Tối ưu "hole"

Không dùng `swap` 3 phép gán mỗi bước, mà **giữ giá trị cần chèn trong biến tạm**, chỉ **kéo cha xuống** rồi đặt một lần cuối cùng.

```
  swap:  3 phép gán × log n bước = 3·log n
  hole:  1 phép gán × log n bước + 1 = log n + 1
```

Tiết kiệm ~2/3 số phép gán. Cùng kỹ thuật với insertion sort.

**Độ phức tạp:** chiều cao cây = `⌊log₂ n⌋` ⇒ **`O(log n)`**.

---

## 4. `pop` — siftDown

```java
public Chunk pop() {
    if (size == 0) return null;
    Chunk top = items[0];
    size--;
    if (size > 0) {
        items[0] = items[size];              // node CUỐI nhảy lên gốc
        priorities[0] = priorities[size];
        siftDown(0);                          // rồi cho nó CHÌM xuống
    }
    items[size] = null;                       // ← quan trọng: tránh rò rỉ bộ nhớ
    return top;
}

private void siftDown(int index) {
    Chunk chunk = items[index];
    int priority = priorities[index];
    int half = size / 2;

    while (index < half) {                    // ← chỉ số ≥ size/2 là LÁ
        int child = index * 2 + 1;
        int right = child + 1;
        if (right < size && priorities[right] < priorities[child]) child = right;   // chọn con NHỎ hơn
        if (priorities[child] >= priority) break;
        items[index] = items[child];
        priorities[index] = priorities[child];
        index = child;
    }
    items[index] = chunk;
    priorities[index] = priority;
}
```

### Vì sao lấy node cuối lên gốc?

Xoá gốc để lại "lỗ". Muốn cây vẫn **hoàn chỉnh**, phải bỏ đi đúng node cuối cùng. Đưa nó lên gốc rồi cho chìm xuống là cách duy nhất giữ cả hai tính chất (hoàn chỉnh + heap).

### `half = size / 2` — chặn vòng lặp

Node có chỉ số `i ≥ ⌊n/2⌋` **không có con** (vì `2i + 1 ≥ n`). Kiểm tra `index < half` rẻ hơn kiểm tra `child < size` bên trong.

Chứng minh: `2i + 1 < n  ⟺  i < (n−1)/2`. Với `i ≥ n/2` thì `2i + 1 ≥ n + 1 > n` ✔

### Phải chọn **con nhỏ hơn**

Nếu chìm xuống con lớn hơn, con nhỏ sẽ trở thành cha của nó ⇒ vi phạm min-heap.

**Độ phức tạp:** **`O(log n)`**.

### `items[size] = null` — chống rò rỉ bộ nhớ

Nếu không gán `null`, mảng vẫn giữ tham chiếu tới `Chunk` đã bị pop ⇒ GC không thể thu hồi. Với chunk nặng ~40 KB, quên dòng này là rò rỉ nghiêm trọng.

Đây là lý do `IntQueue.clear()` là `O(1)` (kiểu nguyên thuỷ) còn `ChunkHeap.clear()` phải `O(n)`:

```java
public void clear() {
    for (int i = 0; i < size; i++) items[i] = null;
    size = 0;
}
```

---

## 5. `remove` — xoá phần tử bất kỳ

```java
public boolean remove(Chunk chunk) {
    for (int i = 0; i < size; i++) {
        if (items[i] != chunk) continue;
        size--;
        if (i == size) { items[size] = null; return true; }

        items[i] = items[size];
        priorities[i] = priorities[size];
        items[size] = null;

        siftDown(i);      // ← phải thử CẢ HAI chiều
        siftUp(i);
        return true;
    }
    return false;
}
```

### Vì sao gọi cả `siftDown` lẫn `siftUp`?

Phần tử cuối chuyển tới vị trí `i` có thể **lớn hơn** con (cần chìm) hoặc **nhỏ hơn** cha (cần nổi) — không biết trước.

An toàn khi gọi cả hai: một trong hai sẽ không làm gì (thoát ngay ở lần so sánh đầu). Không thể cả hai cùng di chuyển vì heap chỉ vi phạm ở **một** phía.

**Độ phức tạp:** `O(n)` để **tìm** vị trí + `O(log n)` để sửa = **`O(n)`**.

> Nếu cần `remove` nhanh, phải thêm `HashMap<Chunk, Integer>` để tra vị trí — đánh đổi bộ nhớ lấy tốc độ. Ở đây `remove` hiếm khi được gọi (chỉ khi chunk bị gỡ khỏi thế giới) nên `O(n)` chấp nhận được.

---

## 6. Tổng hợp độ phức tạp

| Thao tác | Thời gian | Ghi chú |
|---|---|---|
| `push` | **`O(log n)`** | + `O(n)` khi grow (khấu hao `O(1)`) |
| `pop` | **`O(log n)`** | |
| `peek` (đọc `items[0]`) | `O(1)` | |
| `remove(chunk)` | `O(n)` | quét tuyến tính |
| `clear` | `O(n)` | phải gán null |
| `size`, `isEmpty` | `O(1)` | |
| Bộ nhớ | `O(n)` | 2 mảng song song |

### So sánh với các cách khác

| Cấu trúc | `push` | `pop-min` | Ghi chú |
|---|---|---|---|
| Mảng chưa sắp | `O(1)` | **`O(n)`** | phải quét tìm min |
| Mảng đã sắp | **`O(n)`** | `O(1)` | phải chèn đúng chỗ |
| Cây tìm kiếm cân bằng | `O(log n)` | `O(log n)` | tốn con trỏ, cache kém |
| **Binary heap** | **`O(log n)`** | **`O(log n)`** | không con trỏ, cache tốt |

---

## 7. Ứng dụng trong game

Độ ưu tiên = **khoảng cách tới người chơi** (bình phương, để tránh căn bậc hai). `pop()` luôn trả chunk gần nhất ⇒ thế giới hiện ra **từ gần tới xa**, người chơi thấy mặt đất dưới chân trước tiên thay vì chờ cả vùng nạp xong.

---

## 8. Chủ đề DSA thể hiện

- **Binary heap** & cây nhị phân hoàn chỉnh trải phẳng.
- **`siftUp` / `siftDown`** với tối ưu "hole".
- **Hàng đợi ưu tiên** — bài toán chọn phần tử nhỏ nhất động.
- **Mảng song song** thay cho object bao bọc.
- **Quản lý tham chiếu để tránh rò rỉ bộ nhớ**.
- **Nhân đôi sức chứa** — khấu hao `O(1)`.

---

## 9. Liên kết

- Anh em: [IntQueue.md](IntQueue.md), [MergeSort.md](MergeSort.md)
- Người dùng: [ChunkScheduler.md](../05-world/ChunkScheduler.md)
