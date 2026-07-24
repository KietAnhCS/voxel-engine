# FluidSimulator — nước chảy kiểu Minecraft

**File:** `core/src/com/voxel/engine/world/FluidSimulator.java`

Toàn bộ hành vi của nước nằm trong **một công thức duy nhất**.

---

## 1. Mô hình mức nước

Mỗi ô nước có **mức (level)** từ 1 đến 8:

| Mức | Ý nghĩa |
|---|---|
| 8 (`MAX_FLUID_LEVEL`) | **Nguồn** — ô đầy, không bao giờ cạn |
| 7 (`FALLING_LEVEL`) | Nước đổ từ trên xuống |
| 1–6 | Nước chảy, càng nhỏ càng nông |
| 0 | Trở lại thành **không khí** |

---

## 2. Công thức lõi

```java
private int inflow(int x, int y, int z) {
    int best = 0, sources = 0;
    for (Direction side : SIDES) {                       // 4 hướng NGANG
        int nx = x + side.dx(), nz = z + side.dz();
        int level = world.blockAt(nx, y, nz).fluidLevel();
        if (level == Block.MAX_FLUID_LEVEL) sources++;
        if (level - 1 > best && !spillsDown(nx, y, nz)) best = level - 1;
    }
    if (sources >= 2)                              return Block.MAX_FLUID_LEVEL;   // (1)
    if (world.blockAt(x, y + 1, z).isLiquid())     return FALLING_LEVEL;           // (2)
    return best;                                                                    // (3)
}
```

### Công thức toán học

```
              ⎧ 8                            nếu số ô nguồn kề ≥ 2
  L'(p)  =    ⎨ 7                            nếu ô ngay TRÊN là nước
              ⎩ max( L(q) − 1 , 0 )          ngược lại
                q ∈ 4 hướng ngang, q không tự chảy xuống
```

### Vì sao một công thức lo được cả hai chiều?

**Khi đặt nguồn (nước lan ra):**

```
  8 → 7 → 6 → 5 → 4 → 3 → 2 → 1 → 0
```

Mỗi ô lấy `max(hàng xóm) − 1` ⇒ nước lan **đúng 7 ô** rồi tắt. Bán kính lan tự động bị chặn.

**Khi phá nguồn (nước rút):**

Một ô chỉ **giữ được mức `M`** khi còn hàng xóm mức `M + 1`. Mất nguồn ⇒ ô kề nguồn tụt xuống, kéo theo ô kế tiếp tụt... Mỗi vòng, **mức lớn nhất còn lại trong vùng giảm ít nhất 1** ⇒ sau ≤ 8 vòng nước rút sạch.

> Không cần luật riêng cho việc rút nước — đây là điểm đẹp nhất của thiết kế này.

### Chứng minh hội tụ

Đặt `M(t) = max{ L(p, t) : p không phải nguồn }`. Nếu không còn nguồn nào kề:

```
  M(t+1) ≤ M(t) − 1
```

Vì `M(0) ≤ 8`, sau tối đa 8 vòng `M = 0` ⇒ hệ **dừng**. Không có dao động vô hạn.

---

## 3. Ba luật đặc biệt

### 3.1 Hai nguồn kề nhau → thành nguồn mới

```java
if (sources >= 2) return Block.MAX_FLUID_LEVEL;
```

> Comment: *"Nhờ luật này, một rãnh đào ra biển sẽ đầy lên thành nước biển thật thay vì một dòng chảy nông, và nước trong biển không bao giờ 'cạn đi'."*

**Vì sao cần?** Không có luật này, mặt biển phẳng sẽ dần bị "ăn mòn": ô rìa biển thấy hàng xóm mức 8 ⇒ nhận mức 7 ⇒ ô kế tiếp nhận 6… và biển tự rút cạn. Luật "2 nguồn = nguồn" giữ khối nước lớn **ổn định vĩnh viễn**.

### 3.2 Nước đổ từ trên → mức 7

```java
if (world.blockAt(x, y + 1, z).isLiquid()) return FALLING_LEVEL;
```

Nước rơi xuống gần như đầy ô (7) — nhưng **không phải nguồn** (8), nên vẫn cạn được khi ngắt dòng phía trên.

### 3.3 `spillsDown` — nước rơi thẳng, không loe ngang

```java
private boolean spillsDown(int x, int y, int z) {
    Block below = world.blockAt(x, y - 1, z);
    return below.isAir() || (below.isLiquid() && below.fluidLevel() < Block.MAX_FLUID_LEVEL);
}
```

Trong `inflow`, ô hàng xóm **đang tự chảy xuống** thì **không được** tính vào `best`:

```java
if (level - 1 > best && !spillsDown(nx, y, nz)) best = level - 1;
```

> Comment: *"nước rơi từ trên cao tạo thành một cột thay vì một vũng loe ra giữa không trung, đúng như Minecraft."*

Về mặt vật lý: dòng nước ưu tiên **trọng lực** (xuống) hơn **áp suất ngang**. Điều kiện `below.fluidLevel() < MAX` để nước rơi xuống hồ đầy thì dừng rơi và bắt đầu lan ngang.

---

## 4. Đóng gói toạ độ vào `long`

```java
private static long pack(int x, int y, int z) {
    return ((long) x & 0x3FFFFFFL) << 38
         | ((long) y & 0xFFFL)     << 26
         | ((long) z & 0x3FFFFFFL);
}
```

### Bố trí bit

```
  bit: 63 ────────── 38 │ 37 ── 26 │ 25 ────────── 0
      ┌──────────────────┼──────────┼─────────────────┐
      │      x (26 bit)  │ y (12 b) │   z (26 bit)    │
      └──────────────────┴──────────┴─────────────────┘
```

| Trường | Bit | Miền giá trị |
|---|---|---|
| `x` | 26 | `−33 554 432 … 33 554 431` |
| `y` | 12 | `0 … 4 095` (thừa cho `height = 128`) |
| `z` | 26 | `−33 554 432 … 33 554 431` |

Tổng `26 + 12 + 26 = 64` bit — vừa khít một `long`.

### Giải nén — mẹo mở rộng dấu

```java
private static int unpackX(long cell) { return (int) (cell >> 38); }                 // dịch có dấu
private static int unpackY(long cell) { return (int) (cell >> 26) & 0xFFF; }         // y ≥ 0, mask
private static int unpackZ(long cell) { return (int) (cell << 38 >> 38); }           // ⭐
```

**`cell << 38 >> 38`** là thủ thuật **mở rộng dấu (sign extension)** cho trường 26-bit:

1. `<< 38` đẩy bit 25 (bit dấu của `z`) lên vị trí 63 — bit dấu của `long`.
2. `>> 38` là dịch phải **có dấu** ⇒ nhân bản bit 63 xuống, khôi phục đúng số âm.

Ví dụ `z = −5`: 26 bit thấp là `11...1011`. Nếu chỉ mask `& 0x3FFFFFF` sẽ ra `67 108 859` (số dương sai). Thủ thuật này trả về đúng `−5`.

`x` không cần vì `>> 38` đã là dịch có dấu và `x` chiếm bit cao nhất.

### Vì sao đóng gói?

Thay vì `Set<Point3D>` với object 3 trường (≈32 byte + tham chiếu, phải viết `hashCode`/`equals`), dùng `Long` (16 byte, hash sẵn có, so sánh 1 lệnh CPU). Với hàng nghìn ô chờ mỗi vòng, khác biệt rất lớn.

---

## 5. Kiến trúc chạy — 3 cấu trúc dữ liệu

```java
private final Queue<Long> inbox = new ConcurrentLinkedQueue<Long>();   // liên luồng
private final Set<Long>   pending = new LinkedHashSet<Long>();          // hàng chờ vòng này
private final List<Long>  batch = new ArrayList<Long>();                // ảnh chụp
private final Set<Chunk>  dirty = new LinkedHashSet<Chunk>();           // chunk cần dựng lại
```

| Cấu trúc | Vai trò | Vì sao chọn kiểu đó |
|---|---|---|
| `inbox` | Hộp thư liên luồng | `ConcurrentLinkedQueue` — **lock-free**, luồng sinh chunk gọi `seed()` không chặn luồng game |
| `pending` | Hàng chờ | `LinkedHashSet` — **khử trùng lặp** `O(1)` + **giữ thứ tự chèn** ⇒ tất định |
| `batch` | Ảnh chụp vòng hiện tại | `ArrayList` — duyệt tuần tự nhanh |
| `dirty` | Chunk cần tính lại sáng | `LinkedHashSet` — mỗi chunk **chỉ một lần** |

### Vì sao `LinkedHashSet` chứ không phải `HashSet`?

`HashSet` duyệt theo thứ tự băm — **không xác định** giữa các lần chạy JVM. `LinkedHashSet` giữ thứ tự chèn ⇒ nước lan **giống hệt nhau** mỗi lần chạy, dễ tái hiện lỗi.

---

## 6. Vòng `tick`

```java
public void tick() {
    if (++steps < STEPS_PER_SPREAD) return;     // 15 bước vật lý = 1 vòng nước
    steps = 0;

    for (Long cell = inbox.poll(); cell != null; cell = inbox.poll())
        pending.add(cell);                       // hút hết hộp thư
    if (pending.isEmpty()) return;

    batch.clear();
    batch.addAll(pending);
    pending.clear();                             // ← ô thêm vào lúc đang tính thuộc VÒNG SAU

    int handled = Math.min(batch.size(), MAX_CELLS_PER_SPREAD);
    for (int i = 0; i < handled; i++) update(batch.get(i));
    for (int i = handled; i < batch.size(); i++) pending.add(batch.get(i));   // hoãn phần dư

    for (Chunk chunk : dirty) world.relightAsync(chunk, true);
    dirty.clear();
}
```

### 6.1 Nhịp lan — `STEPS_PER_SPREAD = 15`

Với 60 bước vật lý/giây:

```
  60 / 15 = 4 vòng lan / giây
```

Đúng nhịp Minecraft (5 tick = 4 lần/giây với tickrate 20). Nếu lan mỗi bước vật lý, nước chảy nhanh như tia laser — mất cảm giác chất lỏng.

### 6.2 Copy-then-clear — tách vòng

```java
batch.addAll(pending);
pending.clear();
```

Ô được thêm **trong lúc đang xử lý** rơi vào `pending` (đã rỗng) ⇒ thuộc **vòng sau**. Nhờ vậy nước lan **đúng một ô mỗi vòng** — dòng chảy bò dần ra thay vì xuất hiện tức thì.

Nếu không tách, vòng lặp sẽ ăn luôn các ô mới thêm ⇒ toàn bộ vũng nước hiện ra trong một khung hình.

### 6.3 Chặn trên `MAX_CELLS_PER_SPREAD = 4096`

Ngăn "vụ nổ nước" (người chơi phá đáy hồ lớn) làm treo khung hình. Phần dư được **hoãn sang vòng sau**, không bị mất.

```
  Chi phí mỗi vòng ≤ 4096 × O(6 lần đọc khối) — có chặn cứng
```

### 6.4 Gộp việc dựng lại — điểm quyết định hiệu năng

```java
for (Chunk chunk : dirty) world.relightAsync(chunk, true);
```

> Comment: *"Cả vòng tính lại ánh sáng và dựng lại hình học MỘT LẦN mỗi chunk, thay vì một lần mỗi ô thay đổi. Đây chính là thứ quyết định giữa mượt và giật."*

**So sánh:** 4096 ô thay đổi trong 1 chunk
- Không gộp: 4096 × (BFS ánh sáng 32 768 ô + dựng mesh) ≈ **1.3 × 10⁸** phép
- Có gộp: 1 × (BFS + mesh) ≈ **3.3 × 10⁴** phép

⇒ **Nhanh hơn ~4000 lần.**

### `markDirty` — đánh dấu 3×3 chunk

```java
private void markDirty(int x, int z) {
    for (int dx = -1; dx <= 1; dx++)
        for (int dz = -1; dz <= 1; dz++) {
            Chunk chunk = world.chunkContaining(x + dx, z + dz);
            if (chunk != null) dirty.add(chunk);
        }
}
```

Ô nằm sát biên chunk ⇒ hình học và ánh sáng của chunk bên cạnh cũng phải dựng lại. Dùng `Set` nên chunk trùng chỉ được thêm một lần.

---

## 7. `seed` — đánh thức nước sau khi sinh chunk

```java
public void seed(Chunk chunk) {
    for (int y = 1; y < storage.height(); y++)
        for (int x = 0; x < size; x++)
            for (int z = 0; z < size; z++) {
                if (!registry.byId(storage.blockId(x, y, z)).isLiquid()) continue;
                if (hasOutlet(storage, registry, x, y, z))
                    schedule(chunk.originX() + x, y, chunk.originZ() + z);
            }
}
```

### Tối ưu `hasOutlet` — chỉ đánh thức nước CÓ LỐI THOÁT

```java
private boolean hasOutlet(...) {
    if (isOpen(storage, registry, x, y - 1, z)) return true;      // dưới trống
    for (Direction side : SIDES)
        if (isOpen(storage, registry, x + side.dx(), y, z + side.dz())) return true;
    return false;
}
```

> Comment: *"Trên mặt biển phẳng không ô nào thoả điều kiện này, nên cả một đại dương không bao giờ bị đánh thức vô ích."*

**Phân tích:** một chunk giữa đại dương có ~20 000 ô nước. Không có `hasOutlet`, cả 20 000 ô vào hàng đợi ⇒ 5 vòng lan liên tục cho **kết quả không đổi**. Với `hasOutlet`, số ô được đánh thức là **0**.

Chỉ nước ở **thành hang bị khoét vào biển/hồ** mới có lối thoát ⇒ biển tự chảy vào hang mà không cần người chơi đào.

`isOpen` kiểm tra `storage.contains(...)` ⇒ **bỏ qua hàng xóm ngoài chunk** — chunk kế tiếp sẽ tự quét phần của nó. Nhờ vậy `seed` chỉ đọc mảng nội bộ, chạy an toàn trên luồng sinh chunk.

**Độ phức tạp:** `O(size² · height)` = 32 768, **một lần** mỗi chunk.

---

## 8. `update` — hai điều kiện thoát sớm

```java
Block current = world.blockAt(x, y, z);
if (current.fluidLevel() == Block.MAX_FLUID_LEVEL) return;   // nguồn không bao giờ đổi
if (!current.isAir() && !current.isLiquid())        return;  // đất đá: nước không vào được
```

Ô nguồn là **bất động** — nếu tính lại, nó có thể tự tụt mức và cả biển sẽ cạn dần.

---

## 9. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `MAX_FLUID_LEVEL` | 8 | Mức nguồn |
| `FALLING_LEVEL` | 7 | Nước rơi từ trên |
| `STEPS_PER_SPREAD` | 15 | 4 vòng lan/giây |
| `MAX_CELLS_PER_SPREAD` | 4096 | Chặn trên mỗi vòng |
| Bán kính lan | 7 ô | Hệ quả của `8 − 1 × 7 = 1` |

---

## 10. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `schedule` | `O(1)` — 7 lần `add` vào queue |
| `seed` | `O(size² · height)` = 32 768, một lần/chunk |
| `inflow` | `O(1)` — 4 hướng × 2 lần đọc khối |
| `tick` một vòng | `O(min(k, 4096))` với `k` = số ô chờ |
| Dựng lại | `O(số chunk dirty)` thay vì `O(số ô đổi)` |
| Bộ nhớ | `O(k)` — 8 byte/ô chờ |

Tổng thể **`O(k)` mỗi vòng, `k` có chặn trên** — không phụ thuộc kích thước thế giới.

---

## 11. Chủ đề DSA thể hiện

- **Automat tế bào (cellular automaton)** — một luật cục bộ sinh hành vi toàn cục.
- **BFS theo lớp** (level-synchronous) — copy-then-clear tách vòng.
- **Chứng minh hội tụ** bằng hàm thế năng giảm dần `M(t)`.
- **Đóng gói toạ độ vào `long`** + **mở rộng dấu** (`<< 38 >> 38`).
- **Khử trùng lặp bằng `LinkedHashSet`** — `O(1)` + tất định.
- **Hàng đợi lock-free** (`ConcurrentLinkedQueue`) cho giao tiếp liên luồng.
- **Gộp công việc (batching)** — dựng lại 1 lần/chunk thay vì 1 lần/ô.
- **Điều tiết tải (throttling)** — `MAX_CELLS_PER_SPREAD`.
- **Cắt tỉa thông minh** — `hasOutlet` tránh đánh thức cả đại dương.

---

## 12. Liên kết

- Lưu trữ khối: [ChunkStorage.md](ChunkStorage.md)
- Tính lại ánh sáng: [LightEngine.md](LightEngine.md)
- 6 hướng: [Direction.md](../06-datastructures/Direction.md)
