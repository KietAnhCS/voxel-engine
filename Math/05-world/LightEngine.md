# LightEngine — lan truyền ánh sáng bằng BFS

**File:** `core/src/com/voxel/engine/world/LightEngine.java`

Bài toán đồ thị kinh điển nhất trong project: coi mỗi ô voxel là **đỉnh**, hai ô kề nhau theo 6 hướng là **cạnh**, rồi lan ánh sáng từ nguồn ra bằng **duyệt theo chiều rộng (BFS)**.

---

## 1. Mô hình đồ thị

```
  V = size² × height = 16 × 16 × 128 = 32 768 đỉnh
  E = 6V / 2 = 98 304 cạnh (mỗi ô có 6 hàng xóm, mỗi cạnh đếm 2 lần)
```

Đồ thị là **lưới 3D đều** (3D grid graph) — không cần lưu danh sách kề, hàng xóm tính bằng số học chỉ số.

### Vì sao BFS chứ không phải Dijkstra?

Ánh sáng giảm theo **độ suy hao** `attenuation` mỗi bước. Nếu mọi khối trong suốt đều có `attenuation = 1` thì mọi cạnh **cùng trọng số** ⇒ BFS chính là Dijkstra tối ưu, `O(V + E)` thay vì `O(E log V)`.

Khi có khối suy hao khác 1 (ví dụ nước), thuật toán ở đây là **BFS có kiểm tra cải thiện** (relaxation) — về bản chất là **SPFA / Bellman-Ford dạng hàng đợi**, vẫn hội tụ vì mức sáng bị chặn trên bởi 15.

---

## 2. Đóng gói 2 kênh ánh sáng vào 1 byte

```java
private int skyLight(int index)   { return (light[index] >> 4) & 0x0F; }
private int blockLight(int index) { return  light[index]       & 0x0F; }

private void setSkyLight(int index, int value) {
    light[index] = (byte) ((light[index] & 0x0F) | (value << 4));
}
private void setBlockLight(int index, int value) {
    light[index] = (byte) ((light[index] & 0xF0) | value);
}
```

### Bố trí bit

```
   bit:  7  6  5  4 │ 3  2  1  0
        ┌───────────┼───────────┐
        │  skyLight │ blockLight│
        └───────────┴───────────┘
           0..15        0..15
```

| Thao tác | Phép bit | Giải thích |
|---|---|---|
| Đọc sky | `(b >> 4) & 0x0F` | dịch phải 4, lấy 4 bit thấp |
| Đọc block | `b & 0x0F` | mặt nạ 4 bit thấp |
| Ghi sky | `(b & 0x0F) \| (v << 4)` | xoá nibble cao, chèn `v` |
| Ghi block | `(b & 0xF0) \| v` | xoá nibble thấp, chèn `v` |

**Tiết kiệm bộ nhớ:** 1 byte thay vì 2 ⇒ `32 768` byte = **32 KB/chunk** thay vì 64 KB. Với 400 chunk đang tải: tiết kiệm **12.8 MB**.

`& 0x0F` sau `>> 4` là bắt buộc vì `byte` trong Java **có dấu**: `(byte)0xF0 >> 4 = −1` (mở rộng dấu), phải mask mới ra `15`.

---

## 3. Hai kênh ánh sáng

| | `skyLight` | `blockLight` |
|---|---|---|
| Nguồn | Bầu trời (cột hở lên trời) | Đuốc, đèn (`luminance > 0`) |
| Giá trị nguồn | 15 (`MAX_LIGHT`) | `block.luminance()` |
| Đặc biệt | Đi **thẳng xuống** không suy hao | Suy hao mọi hướng |
| Dùng khi | Nhân với `daylight` của chu kỳ ngày đêm | Luôn sáng, kể cả ban đêm |

`ChunkStorage.combinedLight` lấy **max** của hai kênh:

```java
return sky > block ? sky : block;
```

---

## 4. Gieo mầm ánh sáng trời (`seedSkyLight`)

```java
for (x, z) {
    int floor = storage.skyFloor(x, z);           // khối đặc cao nhất + 1
    for (int y = floor; y < height; y++)
        setSkyLight(storage.index(x, y, z), 15);  // toàn cột trên đó = 15

    if (floor < height) queue.enqueue(storage.index(x, floor, z));

    for (int y = floor + 1; y < height; y++)
        if (hasShadowedNeighbour(storage, x, y, z, size))
            queue.enqueue(storage.index(x, y, z));
}
```

### Tối ưu then chốt: chỉ đưa vào hàng đợi ô "có việc để làm"

Nếu đưa **toàn bộ** cột trời vào hàng đợi, số lần enqueue = số ô trống ≈ 20 000/chunk — lãng phí vì phần lớn ô trời chỉ lan sang ô trời khác cũng đã bằng 15.

```java
private boolean hasShadowedNeighbour(ChunkStorage storage, int x, int y, int z, int size) {
    if (x == 0 || z == 0 || x == size - 1 || z == size - 1) return true;   // ô biên
    return storage.skyFloor(x - 1, z) > y
        || storage.skyFloor(x + 1, z) > y
        || storage.skyFloor(x, z - 1) > y
        || storage.skyFloor(x, z + 1) > y;
}
```

Chỉ enqueue ô trời **có hàng xóm bị che** (`skyFloor` hàng xóm cao hơn `y`) — đó là nơi ánh sáng thật sự cần tràn ngang vào bóng râm. Cộng thêm ô đáy cột (`y = floor`) để sáng lan xuống hang.

Ô biên chunk luôn `true` vì không biết `skyFloor` của chunk bên cạnh.

---

## 5. Vòng lặp BFS (`propagate`)

```java
while (!queue.isEmpty()) {
    int index = queue.dequeue();
    int level = sky ? skyLight(index) : blockLight(index);
    if (level <= 1) continue;                       // ← điều kiện dừng

    int z =  index & mask;
    int x = (index >> shift) & mask;
    int y =  index >> (shift << 1);

    for (Direction direction : Direction.ALL) {
        int nx = x + direction.dx(), ny = y + direction.dy(), nz = z + direction.dz();
        if (ny < 0 || ny >= height || nx < 0 || nz < 0 || nx >= size || nz >= size) continue;

        Block neighbour = registry.byId(storage.blockId(nx, ny, nz));
        if (neighbour.isOpaque()) continue;

        int cost = neighbour.attenuation();
        if (sky && direction == Direction.DOWN && level == 15 && cost == 1) cost = 0;   // (*)

        int target = level - cost;
        if (target <= 0) continue;

        int neighbourIndex = storage.index(nx, ny, nz);
        int current = sky ? skyLight(neighbourIndex) : blockLight(neighbourIndex);
        if (target > current) {                     // ← chỉ enqueue khi CẢI THIỆN
            setLight(neighbourIndex, target);
            queue.enqueue(neighbourIndex);
        }
    }
}
```

### 5.1 Giải nén chỉ số ngược

```java
int z =  index & mask;                 // mask = size − 1 = 15 = 0b1111
int x = (index >> shift) & mask;       // shift = log₂(size) = 4
int y =  index >> (shift << 1);        // shift·2 = 8
```

Đảo ngược phép đóng gói `index = (y << 8) | (x << 4) | z` của [ChunkStorage](ChunkStorage.md). Toàn phép bit, không có chia hay modulo.

### 5.2 Công thức lan truyền

```
  L(hàng xóm) = max( L(hàng xóm),  L(hiện tại) − attenuation(hàng xóm) )
```

Đây chính là phép **relaxation** của bài toán đường đi ngắn nhất, nhưng đảo dấu (tìm **max** thay vì min).

### 5.3 Vì sao `if (target > current)` là mấu chốt?

Ô chỉ được đẩy vào hàng đợi khi ánh sáng của nó **thực sự tăng**. Vì:

```
  0 ≤ L(v) ≤ 15
```

mỗi đỉnh chỉ có thể tăng tối đa **15 lần** ⇒ tổng số lần enqueue ≤ `15V`, hữu hạn và tuyến tính theo `V`.

Không có điều kiện này, BFS sẽ lặp vô hạn (ô A đẩy sang B, B đẩy lại A…).

### 5.4 Cắt tỉa `level <= 1`

Nếu `level = 1` thì `target = 1 − cost ≤ 0` với mọi `cost ≥ 1` ⇒ không lan được nữa. Bỏ qua sớm tiết kiệm 6 lần kiểm tra hàng xóm.

### 5.5 Ánh sáng trời đi thẳng xuống không suy hao (*)

```java
if (sky && direction == Direction.DOWN && level == Block.MAX_LIGHT && cost == 1) cost = 0;
```

Ánh sáng mặt trời chiếu **thẳng đứng** nên xuống dưới không mất gì — giữ nguyên 15 cho tới khi chạm khối đặc. Đây là quy tắc của Minecraft: đứng dưới đáy giếng sâu 30 khối vẫn sáng như trên mặt đất.

Điều kiện `level == 15` giới hạn ưu đãi này cho **ánh sáng trực tiếp**; ánh sáng đã bị giảm (14, 13…) là ánh sáng tán xạ, xuống dưới vẫn phải suy hao bình thường.

---

## 6. Ánh sáng qua biên chunk

### Gieo từ hàng xóm

```java
private void trySeed(..., int localX, int y, int localZ, int worldX, int worldY, int worldZ) {
    Block own = registry.byId(storage.blockId(localX, y, localZ));
    if (own.isOpaque()) return;
    int outside = sky ? world.skyLightAt(worldX, worldY, worldZ)
                      : world.blockLightAt(worldX, worldY, worldZ);
    int target = outside - own.attenuation();
    if (target <= 0) return;
    ...
}
```

Duyệt **4 mặt bên** của chunk, đọc mức sáng của ô ngay bên kia biên từ `World`, rồi gieo vào hàng đợi. Nhờ vậy ánh sáng chảy liên tục qua ranh giới chunk.

Chi phí: `O(4 · size · height)` = `4 × 16 × 128 = 8 192` ô biên.

### `borderDiffers` — chống dây chuyền tính lại

```java
private boolean borderDiffers(ChunkStorage storage) {
    for (int y = 0; y < storage.height(); y++)
        for (int edge = 0; edge < size; edge++)
            if (differsAt(storage, 0, y, edge) || differsAt(storage, last, y, edge)
             || differsAt(storage, edge, y, 0) || differsAt(storage, edge, y, last))
                return true;
    return false;
}
```

Chunk bên cạnh chỉ cần tính lại **nếu viền của chunk này thay đổi**.

> Comment trong code: trước đây hàm này trả `true` miễn là ánh sáng *có thể* lan ra khỏi chunk — mà chunk nào cũng có cột trời sáng chạm mép, nên nó **luôn đúng**, và mỗi lần sửa một khối kéo theo `1 + 4 + 16 = 21` lần quét lại. So sánh thật thì hầu hết các lần sửa ở giữa chunk đều không động đến viền, **dây chuyền đó tắt hẳn**.

**Chi phí:** `O(size × height)` = `16 × 128 = 2 048` phép so sánh — rẻ hơn **một lần quét chunk** (32 768 ô) tới 16 lần, mà tiết kiệm được tới 20 lần quét.

---

## 7. Copy-on-write — an toàn đa luồng không cần khoá

```java
// Mảng MỚI, không dùng lại mảng cũ: mảng cũ còn đang được luồng mesh đọc để vẽ.
light = new byte[storage.lightBufferSize()];
... tính toán đầy đủ ...
storage.commitLight(light);      // một phép gán duy nhất
light = null;
```

Trong `ChunkStorage`:

```java
private volatile byte[] light;

public void commitLight(byte[] rebuilt) { this.light = rebuilt; }
```

### Vì sao?

Luồng mesh **đọc** `light[]` để tô màu đỉnh trong khi luồng ánh sáng **ghi**. Nếu ghi trực tiếp, mesh sẽ thấy mảng đang bị xoá dở ⇒ **chunk nhấp nháy đen** (comment trong code nói đúng hiện tượng này).

**Copy-on-write:**
1. Tính toàn bộ vào mảng mới.
2. Gán tham chiếu — thao tác **nguyên tử** trong JVM.
3. `volatile` đảm bảo mọi luồng thấy tham chiếu mới ngay (happens-before).

Người đọc luôn thấy **một bản hoàn chỉnh**, cũ hoặc mới, không bao giờ thấy bản dở dang. Không cần `synchronized`, không có tranh chấp khoá.

Đánh đổi: cấp phát 32 KB mỗi lần tính lại. Chấp nhận được vì tính lại không xảy ra mỗi khung hình.

---

## 8. Độ phức tạp

| Giai đoạn | Chi phí |
|---|---|
| `seedSkyLight` | `O(size² · height)` = 32 768 |
| `seedNeighbour*` | `O(4 · size · height)` = 8 192 × 2 |
| `seedBlockLight` | `O(size² · height)` = 32 768 |
| `propagate` | `O(V + E)` = `O(7V)` ≈ 229 000 (chặn trên: `15V` lần enqueue) |
| `borderDiffers` | `O(size · height)` = 2 048 |
| **Tổng** | **`O(size² · height)`** — tuyến tính theo số ô |
| Bộ nhớ | `O(size² · height)` = 32 KB (bản copy) + hàng đợi |

---

## 9. Chủ đề DSA thể hiện

- **BFS trên đồ thị lưới 3D** — `O(V + E)`.
- **Relaxation / SPFA** khi cạnh có trọng số khác nhau.
- **Chặn trên số lần enqueue** (`≤ 15V`) để chứng minh dừng.
- **Đóng gói bit** — 2 giá trị 4-bit trong 1 byte.
- **Hàng đợi mảng vòng nguyên thuỷ** — xem [IntQueue.md](../06-datastructures/IntQueue.md).
- **Copy-on-write + volatile** — đồng bộ không khoá.
- **Cắt tỉa thông minh** (`hasShadowedNeighbour`, `borderDiffers`).

---

## 10. Liên kết

- Hàng đợi: [IntQueue.md](../06-datastructures/IntQueue.md)
- Lưu trữ & đóng gói chỉ số: [ChunkStorage.md](ChunkStorage.md)
- 6 hướng: [Direction.md](../06-datastructures/Direction.md)
- Người dùng ánh sáng: [FaceLighting.md](../07-render/FaceLighting.md)
