# IntQueue — hàng đợi mảng vòng

**File:** `core/src/com/voxel/engine/util/IntQueue.java`

---

## 1. Cấu trúc

```java
private int[] items;
private int head;      // vị trí lấy ra
private int tail;      // vị trí ghi vào
private int size;      // số phần tử đang có
```

**Mảng vòng (circular buffer):** hai chỉ số chạy vòng quanh mảng, chạm cuối thì quay về 0.

```
  items: [ _ , _ , C , D , E , _ , _ , _ ]
                   ↑           ↑
                  head        tail       size = 3
```

Sau vài lần enqueue nữa, `tail` vượt qua cuối mảng và **quấn về đầu**:

```
  items: [ H , I , C , D , E , F , G , _ ]
                   ↑                   ↑
                  head                tail   size = 7
```

---

## 2. Phép quấn vòng

```java
tail = tail + 1 == items.length ? 0 : tail + 1;
head = head + 1 == items.length ? 0 : head + 1;
```

Tương đương `(tail + 1) % items.length` nhưng dùng **so sánh + rẽ nhánh** thay vì phép chia.

**Vì sao nhanh hơn?** Phép `%` với số không phải luỹ thừa 2 là lệnh `idiv` — tốn ~20–40 chu kỳ CPU. So sánh + nhánh dự đoán được (branch predictor đoán đúng gần 100 % vì hiếm khi quấn) chỉ tốn ~1 chu kỳ.

> Nếu `items.length` luôn là luỹ thừa 2, có thể dùng `(tail + 1) & (length − 1)`. Ở đây độ dài ban đầu tuỳ ý (`4096` cho light, `64` mặc định) nên dùng so sánh.

---

## 3. Vì sao `int` chứ không phải `Queue<Integer>`?

```java
// ✗ Cách Java thông thường
Queue<Integer> queue = new ArrayDeque<>();
queue.add(index);          // AUTOBOXING: new Integer(index)
```

BFS ánh sáng đẩy vào hàng đợi **hàng trăm nghìn chỉ số mỗi lần tính** (chặn trên `15V ≈ 490 000`). Với `Integer`:

| | `Queue<Integer>` | `IntQueue` |
|---|---|---|
| Bộ nhớ / phần tử | 16 byte (object header 12 + int 4) + 4–8 byte tham chiếu | **4 byte** |
| Cấp phát | 1 object mỗi lần enqueue* | **0** |
| Truy cập | dereference con trỏ (cache miss) | liền kề trong RAM |
| Áp lực GC | rất cao | **không** |

\* Java cache `Integer` từ −128..127, nhưng chỉ số chunk lên tới 32 767 nên gần như luôn cấp phát mới.

**Ước tính:** 490 000 × 16 byte = **7.8 MB rác** mỗi lần tính sáng một chunk. Nhân với hàng chục chunk mỗi giây ⇒ GC chạy liên tục, game giật.

`IntQueue` với 4096 phần tử = **16 KB**, cấp phát **một lần**, tái dùng mãi mãi.

---

## 4. `grow()` — nhân đôi & "duỗi thẳng"

```java
private void grow() {
    int[] grown = new int[items.length << 1];       // nhân đôi
    for (int i = 0; i < size; i++) {
        grown[i] = items[(head + i) % items.length];
    }
    items = grown;
    head = 0;
    tail = size;
}
```

Sao chép theo **thứ tự logic** (từ `head` đi tới) sang mảng mới bắt đầu từ chỉ số 0 ⇒ mảng vòng được **duỗi thẳng**, `head = 0`, `tail = size`.

Nếu sao chép nguyên xi `System.arraycopy(items, 0, grown, 0, size)` thì vùng dữ liệu bị quấn sẽ sai thứ tự.

### Phân tích khấu hao (amortized analysis)

Một lần `grow()` tốn `O(n)`. Nhưng vì mỗi lần đều **nhân đôi** sức chứa, tổng chi phí của `n` phép `enqueue` từ mảng rỗng:

```
                                                        ___
                                                   n    ╲    1        1
  1 + 2 + 4 + 8 + … + n  =  2n − 1  <  2n     (vì  ─  ·  ╱   ── = ─────── = 2 )
                                                   1    ‾‾‾  2ᵏ    1 − ½
```

```
                 2n
  Chi phí / phép = ── = O(1) khấu hao
                  n
```

Đây là ví dụ chuẩn của **phân tích khấu hao kiểu tổng gộp** (aggregate method).

**Nếu tăng thêm hằng số** (`length + 1`) thay vì nhân đôi:

```
  1 + 2 + 3 + … + n = n(n+1)/2 = O(n²)      ⇒ O(n) mỗi phép — chậm gấp n lần
```

---

## 5. Độ phức tạp

| Thao tác | Thời gian | Ghi chú |
|---|---|---|
| `enqueue` | **`O(1)` khấu hao** | `O(n)` khi phải grow |
| `dequeue` | **`O(1)`** | không dịch mảng |
| `isEmpty` | `O(1)` | |
| `size` | `O(1)` | biến đếm sẵn |
| `clear` | **`O(1)`** | chỉ reset 3 chỉ số |
| Bộ nhớ | `O(capacity)` | 4 byte/phần tử |

### So sánh với các cách khác

| Cấu trúc | `enqueue` | `dequeue` | Rác GC |
|---|---|---|---|
| `ArrayList` + `remove(0)` | `O(1)`* | **`O(n)`** ← dịch cả mảng | có |
| `LinkedList` | `O(1)` | `O(1)` | **rất nhiều** (node + Integer) |
| `ArrayDeque<Integer>` | `O(1)`* | `O(1)` | **có** (autoboxing) |
| **`IntQueue`** | **`O(1)`\*** | **`O(1)`** | **không** |

\* khấu hao

`clear()` là `O(1)` — không cần xoá nội dung vì `int` không giữ tham chiếu nào cho GC. (Với `Object[]` thì phải gán `null` để tránh rò rỉ bộ nhớ — xem [ChunkHeap](ChunkHeap.md) làm đúng điều đó.)

---

## 6. Chủ đề DSA thể hiện

- **Mảng vòng (circular buffer / ring buffer)**.
- **Phân tích khấu hao** — chiến lược nhân đôi.
- **Tránh autoboxing** — kiểu nguyên thuỷ vs kiểu bọc.
- **Cục bộ bộ nhớ (cache locality)** — mảng liền kề vs danh sách liên kết.

---

## 7. Liên kết

- Người dùng chính: [LightEngine.md](../05-world/LightEngine.md)
- Anh em cấu trúc dữ liệu: [ChunkHeap.md](ChunkHeap.md), [MergeSort.md](MergeSort.md)
