# AStarPathFinder — tìm đường A* trên lưới khối

**File:** `core/src/com/voxel/game/mob/AStarPathFinder.java`

Thuật toán tìm đường kinh điển, cài đặt trên đồ thị **ẩn** (implicit graph) — không lưu đồ thị, sinh đỉnh kề khi cần.

---

## 1. Mô hình đồ thị

| Khái niệm đồ thị | Trong game |
|---|---|
| **Đỉnh** | Một ô đất có thể đứng được `(x, y, z)` |
| **Cạnh** | Bước sang ô kề theo 4 hướng ngang |
| **Trọng số cạnh** | `1 + \|Δy\|` — leo dốc tốn hơn |
| **Đỉnh xuất phát** | Vị trí quái vật |
| **Đỉnh đích** | Vị trí người chơi |

Đồ thị là **ẩn**: không có `adjacencyList`. Hàm `stepY` sinh ra hàng xóm hợp lệ khi được hỏi. Với thế giới vô hạn, đây là cách duy nhất.

---

## 2. Công thức A*

```
  f(n) = g(n) + h(n)
```

| Ký hiệu | Ý nghĩa |
|---|---|
| `g(n)` | Chi phí **thực** từ điểm xuất phát tới `n` (đã biết chắc) |
| `h(n)` | **Ước lượng** chi phí từ `n` tới đích |
| `f(n)` | Ước lượng tổng chi phí của đường đi **qua** `n` |

Hàng đợi ưu tiên luôn mở rộng đỉnh có `f` nhỏ nhất.

### So sánh với các thuật toán họ hàng

| Thuật toán | Công thức | Đặc điểm |
|---|---|---|
| **BFS** | `f = g` (mọi cạnh = 1) | Tối ưu, mở rộng đều mọi hướng |
| **Dijkstra** | `f = g` | Tối ưu, chậm — không biết đích ở đâu |
| **Greedy best-first** | `f = h` | Nhanh, **không tối ưu** |
| **A\*** | `f = g + h` | **Tối ưu** (nếu `h` admissible) **và** nhanh |

A* là sự kết hợp: `g` giữ tính tối ưu của Dijkstra, `h` cho tốc độ của greedy.

---

## 3. Heuristic Manhattan

```java
private static int heuristic(int x, int y, int z, int gx, int gy, int gz) {
    return Math.abs(x - gx) + Math.abs(y - gy) + Math.abs(z - gz);
}
```

```
  h(n) = |x − gx| + |y − gy| + |z − gz|
```

### Tính admissible (không bao giờ ước lượng quá)

**Định lý:** nếu `h(n) ≤ h*(n)` với mọi `n` (`h*` là chi phí thật tới đích), thì A* trả về **đường đi ngắn nhất**.

**Chứng minh cho trường hợp này:** mỗi bước chỉ đổi được **một** trong `x, z` một đơn vị, và `y` tối đa một đơn vị. Chi phí bước là `1 + |Δy|`. Do đó để đi từ `n` tới đích cần ít nhất:

```
  |Δx| + |Δz|   bước ngang, mỗi bước tốn ≥ 1
  |Δy|          đơn vị chi phí leo dốc
```

```
  h*(n) ≥ |Δx| + |Δz| + |Δy| = h(n)     ✔ admissible
```

### Tính nhất quán (consistent / monotone)

Mạnh hơn admissible: `h(n) ≤ c(n, n') + h(n')` với mọi cạnh `n → n'`.

Với mỗi bước, `h` giảm tối đa `1 + |Δy|` (đúng bằng chi phí cạnh) ⇒ **consistent** ✔

Hệ quả: khi một đỉnh được lấy ra khỏi `open`, `g(n)` của nó **đã là tối ưu** ⇒ không cần mở lại đỉnh đã đóng. Đây là lý do đoạn code sau là đúng:

```java
if (closed.contains(nk)) continue;
```

### Vì sao không dùng khoảng cách Euclid?

```
  h_euclid = √(Δx² + Δy² + Δz²)
```

Vẫn admissible (vì `‖·‖₂ ≤ ‖·‖₁`) nhưng **ước lượng thấp hơn** ⇒ A* phải mở rộng nhiều đỉnh hơn. Vì quái vật **không đi chéo** (chỉ 4 hướng), Manhattan là heuristic **chặt nhất** có thể ⇒ nhanh nhất.

---

## 4. Ba cấu trúc dữ liệu

```java
PriorityQueue<Node> open = new PriorityQueue<Node>(Comparator.comparingInt(n -> n.f));
HashMap<Long, Integer> bestG = new HashMap<Long, Integer>();
HashSet<Long> closed = new HashSet<Long>();
```

| Cấu trúc | Vai trò | Chi phí |
|---|---|---|
| `PriorityQueue` (binary heap) | Lấy đỉnh `f` nhỏ nhất | `poll` `O(log n)`, `add` `O(log n)` |
| `HashMap<Long, Integer> bestG` | Chi phí `g` tốt nhất đã biết của mỗi ô | `O(1)` |
| `HashSet<Long> closed` | Ô đã xử lý xong | `O(1)` |

Đây chính xác là bộ ba chuẩn của A*. Xem [ChunkHeap](../06-datastructures/ChunkHeap.md) để hiểu binary heap hoạt động thế nào (`PriorityQueue` của Java dùng cùng thuật toán).

### Khoá 64-bit

```java
private static long key(int x, int y, int z) {
    return ((long) (x + 0x80000) << 40) | ((long) (z + 0x80000) << 20) | (long) (y & 0xFFFFF);
}
```

**Bố trí bit:**

```
  bit: 63 ─── 40 │ 39 ─── 20 │ 19 ─── 0
      ┌──────────┼───────────┼──────────┐
      │ x (20 b) │  z (20 b) │ y (20 b) │
      └──────────┴───────────┴──────────┘
```

`+ 0x80000` (= `2¹⁹` = 524 288) là **độ lệch (bias)** biến số âm thành số dương:

```
  x ∈ [−524 288, +524 287]  →  x + 0x80000 ∈ [0, 1 048 575]
```

Vừa khít 20 bit. Kỹ thuật này đơn giản hơn mở rộng dấu (so với [FluidSimulator.pack](../05-world/FluidSimulator.md)) vì ở đây không cần giải mã ngược.

`y & 0xFFFFF` chỉ mask vì `y ∈ [0, 128)` luôn dương.

Xem thêm [ChunkKey](../06-datastructures/ChunkKey.md) về lợi ích của khoá nguyên thuỷ.

---

## 5. Vòng lặp chính

```java
while (!open.isEmpty() && expansions < MAX_EXPANSIONS) {
    Node cur = open.poll();
    long ck = key(cur.x, cur.y, cur.z);
    if (!closed.add(ck)) continue;              // (1) bản sao cũ trong hàng đợi
    expansions++;

    if (cur.x == gx && cur.z == gz) return reconstruct(cur);   // (2) tới đích

    int h = heuristic(...);
    if (h < closestH) { closestH = h; closest = cur; }          // (3) ghi nhớ ô gần nhất

    for (int[] d : DIRS) {
        int nx = cur.x + d[0], nz = cur.z + d[1];
        int ny = stepY(cur.x, cur.y, cur.z, nx, nz);
        if (ny < 0) continue;
        long nk = key(nx, ny, nz);
        if (closed.contains(nk)) continue;
        int tentativeG = cur.g + 1 + Math.abs(ny - cur.y);      // (4) chi phí cạnh
        Integer prev = bestG.get(nk);
        if (prev == null || tentativeG < prev) {                 // (5) chỉ khi TỐT HƠN
            bestG.put(nk, tentativeG);
            open.add(new Node(nx, ny, nz, tentativeG, tentativeG + heuristic(...), cur));
        }
    }
}
```

### (1) Xử lý "bản sao cũ" — lazy deletion

`PriorityQueue` của Java **không có** thao tác `decreaseKey`. Khi tìm được đường tốt hơn tới ô `n`, ta **thêm một Node mới** thay vì sửa Node cũ ⇒ hàng đợi có nhiều bản sao của cùng một ô.

```java
if (!closed.add(ck)) continue;
```

`HashSet.add` trả `false` nếu đã tồn tại ⇒ **một dòng làm hai việc**: kiểm tra và đánh dấu. Bản sao có `f` lớn hơn sẽ bị bỏ qua khi tới lượt.

Kỹ thuật này gọi là **lazy deletion**. Đánh đổi: hàng đợi phình to hơn (tốn bộ nhớ) nhưng tránh được `decreaseKey` `O(n)` trên binary heap.

### (4) Chi phí cạnh

```
  c(n, n') = 1 + |Δy|
```

| Bước | Chi phí |
|---|---|
| Đi ngang cùng độ cao | **1** |
| Bước lên 1 khối | **2** |
| Bước xuống 1 khối | **2** |

Leo dốc tốn gấp đôi ⇒ quái vật **ưu tiên đường bằng**, chỉ leo khi không còn cách khác. Đây là mô hình chi phí đơn giản nhưng cho hành vi rất tự nhiên.

### (5) Điều kiện thư giãn (relaxation)

Chỉ đưa vào hàng đợi khi tìm được đường **rẻ hơn** đường đã biết. Đây là bước cốt lõi của mọi thuật toán đường đi ngắn nhất.

---

## 6. Chặn tài nguyên & đường lui

```java
private static final int MAX_EXPANSIONS = 500;
```

> Comment: *"Số ô được mở rộng bị chặn bởi `MAX_EXPANSIONS` để một lần tìm đường không làm khung hình giật; nếu không tới đích trong giới hạn đó, trả về đường tới ô GẦN đích nhất đã tìm thấy."*

### Đường lui (graceful degradation)

```java
Node closest = start;
int closestH = heuristic(sx, sy, sz, gx, gy, gz);
...
if (h < closestH) { closestH = h; closest = cur; }
...
return reconstruct(closest);       // hết giới hạn hoặc bị tường chắn
```

Ghi nhớ đỉnh có `h` nhỏ nhất từng gặp. Khi hết ngân sách hoặc `open` rỗng (đích bị tường bao kín), trả về đường tới đỉnh đó.

**Hành vi thu được:** quái vật **vẫn tiến về phía người chơi** dù không tới được — bò tới sát bức tường ngăn cách thay vì đứng ngơ ngác. Trải nghiệm tốt hơn nhiều so với "không tìm được đường ⇒ đứng im".

### Chi phí có chặn

```
  ≤ 500 lần mở rộng × 4 hàng xóm × (O(log n) heap + O(1) hash) ≈ 2 000 thao tác
```

Với `n ≤ 2 000` node trong heap: `log₂ 2000 ≈ 11` ⇒ khoảng **22 000 phép** cho lần tìm đường xấu nhất. Dưới 1 ms — không làm giật khung hình.

---

## 7. Đi trên địa hình 3D

### `walkable` — ô đứng được

```java
private boolean walkable(int x, int y, int z) {
    if (y < 1 || y >= worldHeight - 1) return false;
    return world.blockAt(x, y,     z).isAir()          // chỗ đặt chân trống
        && world.blockAt(x, y + 1, z).isAir()          // chỗ cho đầu trống
        && world.blockAt(x, y - 1, z).isCollidable();  // dưới có nền cứng
}
```

Ba điều kiện tương ứng với **hộp va chạm cao 2 khối** của quái vật đứng trên mặt đất.

### `stepY` — quyết định độ cao khi bước sang

```java
private int stepY(int cx, int cy, int cz, int nx, int nz) {
    if (walkable(nx, cy, nz)) return cy;                                          // ưu tiên 1: bằng
    if (walkable(nx, cy + 1, nz) && world.blockAt(cx, cy + 2, cz).isAir())         // ưu tiên 2: lên
        return cy + 1;
    if (walkable(nx, cy - 1, nz)) return cy - 1;                                   // ưu tiên 3: xuống
    return -1;                                                                     // không đi được
}
```

**Thứ tự ưu tiên** phản ánh chi phí: đi bằng (`c = 1`) rẻ hơn leo (`c = 2`).

Điều kiện phụ `world.blockAt(cx, cy + 2, cz).isAir()` — khi **bước lên**, phải có đủ khoảng trống **phía trên chỗ đang đứng** để nhảy. Không có nó, quái vật sẽ "leo" lên trong hầm trần thấp và kẹt đầu.

`return −1` là **giá trị sentinel** báo "không đi được" — đơn giản hơn ném exception hay trả `Integer`.

### `standY` — tìm chỗ đứng gần nhất

```java
private int standY(int x, int y, int z) {
    int hi = Math.min(worldHeight - 2, y + 2);
    int lo = Math.max(1, y - 3);
    for (int yy = hi; yy >= lo; yy--)
        if (walkable(x, yy, z)) return yy;
    return -1;
}
```

Quét cửa sổ `[y − 3, y + 2]` (6 ô) **từ trên xuống** ⇒ ưu tiên bề mặt cao hơn.

Cần thiết vì toạ độ thực của quái vật/người chơi có thể là `y = 64.7` (đang rơi) hoặc lệch nửa khối. Cửa sổ bất đối xứng (rộng hơn về phía dưới) vì người chơi hay ở trên cao hơn mặt đất (đang nhảy) hơn là chìm dưới đất.

---

## 8. Điều kiện dừng — chỉ khớp cột

```java
if (cur.x == gx && cur.z == gz) return reconstruct(cur);
```

**Không so `y`.** Quái vật chỉ cần tới **cùng cột** với người chơi. Nếu người chơi đứng trên tháp cao 10 khối, quái vật tới chân tháp là "đủ gần" — không cố leo lên vô vọng.

---

## 9. `reconstruct` — lần ngược chuỗi cha

```java
private List<Vector3> reconstruct(Node node) {
    ArrayList<Vector3> path = new ArrayList<Vector3>();
    for (Node n = node; n != null; n = n.parent)
        path.add(new Vector3(n.x + 0.5f, n.y, n.z + 0.5f));
    Collections.reverse(path);
    if (!path.isEmpty()) path.remove(0);
    return path;
}
```

| Bước | Lý do |
|---|---|
| Lần theo `parent` | Xây đường **ngược** từ đích về xuất phát |
| `Collections.reverse` | Đảo lại thành thứ tự đi |
| `+ 0.5f` cho x, z | **Tâm ô** — quái vật đi giữa ô, không cạ tường |
| `path.remove(0)` | Bỏ ô đang đứng — điểm đến đầu tiên phải là ô **kế tiếp** |

Mỗi `Node` giữ tham chiếu `parent` ⇒ cây tìm kiếm được lưu **ngầm** trong chuỗi liên kết, không cần `HashMap<Node, Node> cameFrom` riêng.

**Độ phức tạp:** `O(L)` với `L` = độ dài đường đi (`reverse` cũng `O(L)`).

---

## 10. Độ phức tạp tổng hợp

| Thao tác | Chi phí |
|---|---|
| `open.poll()` | `O(log n)` |
| `open.add()` | `O(log n)` |
| `closed.add/contains` | `O(1)` |
| `bestG.get/put` | `O(1)` |
| Mỗi lần mở rộng | `O(4 · log n)` |
| **Tổng** | **`O(E log V)`** = `O(4 × 500 × log 2000)` ≈ 22 000 |
| Bộ nhớ | `O(V)` = 500 node đóng + ~2 000 node trong heap |

**Có chặn cứng** nhờ `MAX_EXPANSIONS` ⇒ thời gian tìm đường **không phụ thuộc** kích thước thế giới.

---

## 11. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `DIRS` | 4 hướng ngang | Không đi chéo |
| `MAX_EXPANSIONS` | 500 | Chặn thời gian mỗi lần tìm đường |
| Chi phí đi bằng | 1 | |
| Chi phí leo/xuống | 2 | `1 + \|Δy\|` |
| Cửa sổ `standY` | `[y−3, y+2]` | 6 ô |
| Bias khoá | `0x80000` | Toạ độ `±524 288` |

---

## 12. Chủ đề DSA thể hiện

- **A\*** — heuristic admissible & consistent, chứng minh tối ưu.
- **Đồ thị ẩn** (implicit graph) — sinh đỉnh kề khi cần.
- **Hàng đợi ưu tiên** (binary heap).
- **Bảng băm** cho `closed` và `bestG`.
- **Lazy deletion** thay `decreaseKey`.
- **Đóng gói 3 toạ độ vào `long`** với bias.
- **Chặn tài nguyên & degradation duyên dáng** (đường lui).
- **Lần ngược con trỏ cha** để dựng lại đường.
- **Giá trị sentinel** (`−1`) thay exception.

---

## 13. Liên kết

- Binary heap: [ChunkHeap.md](../06-datastructures/ChunkHeap.md)
- Đóng gói khoá: [ChunkKey.md](../06-datastructures/ChunkKey.md)
- Người dùng: [Monster.md](Monster.md), [ChaseState.md](Monster.md)
