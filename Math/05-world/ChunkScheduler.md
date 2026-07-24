# ChunkScheduler — ba cấu trúc dữ liệu, ba nhiệm vụ

**File:** `core/src/com/voxel/engine/world/ChunkScheduler.java`

Quyết định chunk nào được dựng mesh trước.

---

## 1. Ba cấu trúc

```java
private final Deque<Chunk> urgent     = new ArrayDeque<Chunk>();   // NGĂN XẾP
private final ChunkHeap    background = new ChunkHeap(256);        // HÀNG ĐỢI ƯU TIÊN
private final Set<Chunk>   queued     = new HashSet<Chunk>();      // TẬP HỢP BĂM
```

| Cấu trúc | Vai trò | Nguyên tắc | Vì sao chọn |
|---|---|---|---|
| **Ngăn xếp** `urgent` | Việc gấp — người chơi vừa đặt/phá khối | **LIFO** (vào sau ra trước) | Thao tác **vừa làm** mới là thứ người chơi đang nhìn ⇒ phải thấy ngay |
| **Min-heap** `background` | Việc nền — chunk mới sinh | **Nhỏ nhất ra trước** | Chunk gần người chơi nhất ra trước, **bất kể vào hàng lúc nào** |
| **Hash set** `queued` | Kiểm tra trùng | Tra cứu `O(1)` | Không có nó, mỗi lần `submit` phải quét cả hàng ⇒ `O(n)` |

---

## 2. Vì sao ngăn xếp cho việc gấp, không phải hàng đợi?

Giả sử người chơi phá nhanh 5 khối `A → B → C → D → E`:

| | Thứ tự xử lý | Trải nghiệm |
|---|---|---|
| **Hàng đợi (FIFO)** | A, B, C, D, E | Khối vừa phá (E) hiện **cuối cùng** — cảm giác trễ |
| **Ngăn xếp (LIFO)** | **E**, D, C, B, A | Khối vừa phá hiện **ngay** — cảm giác tức thì |

Người chơi đang nhìn vào E. Các chunk A–D vẫn được xử lý, chỉ là muộn hơn vài mili-giây — mắt không nhận ra.

`ArrayDeque` được chọn thay `Stack` (lớp cũ, `synchronized` thừa) và `LinkedList` (cấp phát node cho mỗi phần tử).

---

## 3. Vì sao min-heap cho việc nền?

Chunk vào hàng theo thứ tự sinh ngẫu nhiên, nhưng phải ra theo **khoảng cách tới người chơi**.

```java
public synchronized void submit(Chunk chunk, boolean isUrgent, int priority) {
    if (!queued.add(chunk)) return;                 // đã có rồi → bỏ qua
    if (isUrgent) urgent.push(chunk);
    else          background.push(chunk, priority);
}
```

`priority` = khoảng cách (bình phương) tới người chơi. Heap luôn trả chunk nhỏ nhất ⇒ thế giới hiện ra **từ gần tới xa** — người chơi thấy mặt đất dưới chân trước, chân trời sau.

Chi tiết heap: [ChunkHeap.md](../06-datastructures/ChunkHeap.md).

---

## 4. Vì sao cần hash set?

```java
if (!queued.add(chunk)) return;
```

`Set.add` trả `false` nếu phần tử đã có ⇒ **một dòng làm hai việc**: kiểm tra trùng và ghi nhận.

### Nếu không có `queued`

```java
// ✗ Phải quét cả ngăn xếp lẫn heap
if (urgent.contains(chunk) || background.contains(chunk)) return;   // O(n)
```

`submit` được gọi mỗi lần một khối thay đổi — có thể hàng nghìn lần/giây khi nước chảy. `O(n)` với `n` = vài trăm chunk ⇒ hàng trăm nghìn phép so sánh/giây, lãng phí.

Với `HashSet`: **`O(1)`**.

### Hệ quả: chunk không bao giờ vào hàng hai lần

Ngăn công việc bị lặp — dựng mesh một chunk tốn hàng chục nghìn phép, làm 2 lần là phí trắng.

---

## 5. `poll` — việc gấp luôn thắng

```java
public synchronized Chunk poll() {
    Chunk chunk = urgent.isEmpty() ? background.pop() : urgent.pop();
    if (chunk != null) queued.remove(chunk);
    return chunk;
}
```

**Ưu tiên tuyệt đối** (strict priority): chỉ khi ngăn xếp gấp rỗng mới đụng tới hàng nền.

### Có nguy cơ bỏ đói (starvation) không?

Về lý thuyết có: nếu người chơi liên tục phá khối, hàng nền không bao giờ được xử lý.

Thực tế không: người chơi phá tối đa vài khối/giây, mỗi thao tác sinh ≤ 9 chunk gấp, trong khi luồng mesh xử lý hàng trăm chunk/giây ⇒ ngăn xếp gấp luôn cạn rất nhanh.

**Độ phức tạp:** `O(log n)` (do `background.pop()`).

---

## 6. `forget` — gỡ chunk khỏi hàng

```java
public synchronized void forget(Chunk chunk) {
    if (queued.remove(chunk)) {                     // O(1) — kiểm tra trước
        urgent.remove(chunk);                        // O(n)
        background.remove(chunk);                    // O(n)
    }
}
```

Gọi khi chunk bị gỡ khỏi thế giới (người chơi đi xa). Nếu không gỡ, luồng mesh sẽ dựng hình cho một chunk đã chết.

**Tối ưu:** kiểm tra `queued.remove` (`O(1)`) **trước**. Nếu chunk không có trong hàng, thoát ngay mà không cần quét hai cấu trúc `O(n)`.

**Độ phức tạp:** `O(1)` khi chunk không có trong hàng (trường hợp phổ biến), `O(n)` khi có.

---

## 7. Đồng bộ đa luồng

```java
public synchronized void submit(...)
public synchronized Chunk poll()
public synchronized void forget(...)
public synchronized int pending()
public synchronized void clear()
```

**Ba luồng** cùng gọi vào đây:

| Luồng | Gọi gì |
|---|---|
| Sinh chunk (worker pool) | `submit` khi chunk sinh xong |
| Dựng mesh (worker) | `poll` để lấy việc |
| Vẽ hình (main/render) | `submit` khi người chơi sửa khối, `forget` khi gỡ chunk |

`synchronized` trên **cùng một object** (`this`) tạo một khoá duy nhất bảo vệ cả ba cấu trúc — quan trọng vì chúng phải **nhất quán với nhau**: một chunk có trong `queued` thì phải có trong `urgent` hoặc `background`.

### Vì sao không dùng `ConcurrentLinkedQueue` như `FluidSimulator`?

`FluidSimulator.inbox` chỉ có **một** cấu trúc, thao tác đơn lẻ là nguyên tử. Ở đây cần **thao tác nguyên tử trên nhiều cấu trúc cùng lúc** (`queued.add` + `heap.push` phải cùng thành công) ⇒ bắt buộc dùng khoá.

Vùng găng (critical section) rất ngắn (`O(log n)` phép) nên tranh chấp khoá không đáng kể.

---

## 8. Tổng hợp độ phức tạp

| Thao tác | Chi phí | Ghi chú |
|---|---|---|
| `submit` gấp | **`O(1)`** | `HashSet.add` + `Deque.push` |
| `submit` nền | **`O(log n)`** | `HashSet.add` + `Heap.push` |
| `poll` | **`O(log n)`** | `Heap.pop` |
| `forget` (không có trong hàng) | **`O(1)`** | thoát sớm |
| `forget` (có trong hàng) | `O(n)` | quét ngăn xếp + heap |
| `pending` | `O(1)` | `Set.size()` |
| `clear` | `O(n)` | phải gán null trong heap |

---

## 9. Chủ đề DSA thể hiện

- **Ngăn xếp (LIFO)** vs **hàng đợi (FIFO)** — chọn đúng ngữ nghĩa cho đúng bài toán.
- **Hàng đợi ưu tiên** (binary heap).
- **Tập hợp băm** làm chỉ mục phụ để tránh quét tuyến tính.
- **Nhiều cấu trúc bổ trợ nhau** — mỗi cái một điểm mạnh.
- **Lập lịch ưu tiên tuyệt đối** & phân tích bỏ đói.
- **Đồng bộ bằng khoá** khi cần nguyên tử trên nhiều cấu trúc.

---

## 10. Liên kết

- Heap: [ChunkHeap.md](../06-datastructures/ChunkHeap.md)
- Sắp xếp chunk theo khoảng cách: [MergeSort.md](../06-datastructures/MergeSort.md)
- Người dùng: [WorldRenderer.md](../07-render/WorldRenderer.md)
