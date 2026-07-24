# ChunkStorage — đóng gói chỉ số & bộ đệm ánh sáng

**File:** `core/src/com/voxel/engine/world/ChunkStorage.java`

---

## 1. Làm phẳng mảng 3D thành 1D

```java
private final int sizeShift;              // log₂(chunkSize) = 4
private final int areaShift;              // sizeShift << 1  = 8

public int index(int x, int y, int z) {
    return (y << areaShift) | (x << sizeShift) | z;
}
```

### Công thức chuẩn

```
  index = y · size² + x · size + z
```

Vì `size` **luôn là luỹ thừa của 2** (16), phép nhân trở thành **dịch bit** và phép cộng trở thành **OR**:

```
  y · 256 = y << 8
  x ·  16 = x << 4
  index = (y << 8) | (x << 4) | z
```

### Bố trí bit (size = 16, height = 128)

```
  bit:  14 13 12 11 10 9 8 │ 7 6 5 4 │ 3 2 1 0
       ┌───────────────────┼─────────┼─────────┐
       │        y          │    x    │    z    │
       │      0..127       │  0..15  │  0..15  │
       └───────────────────┴─────────┴─────────┘
```

**Vì sao dùng `|` mà không phải `+`?** Ba trường bit **không chồng lấn** (`z < 16` chỉ chiếm bit 0–3, `x << 4` chỉ chiếm bit 4–7) nên `|` và `+` cho cùng kết quả. `|` không có phép nhớ (carry) nên CPU thực thi nhanh hơn một chút và biểu đạt đúng ý đồ "ghép trường bit".

### Giải nén ngược (dùng trong `LightEngine`)

```java
int z =  index & mask;                    // mask = size − 1 = 15 = 0b1111
int x = (index >> sizeShift) & mask;
int y =  index >> areaShift;
```

`x & 15` tương đương `x % 16` nhưng **không có phép chia** — và đúng cả với số âm (`%` trong Java trả về số âm cho toán hạng âm).

**Độ phức tạp:** `O(1)` — 3 phép bit, không vòng lặp.

### Vì sao không dùng `byte[][][]`?

| | `byte[16][16][128]` | `byte[32768]` |
|---|---|---|
| Object trong heap | `1 + 16 + 256 = 273` | **1** |
| Truy cập | 3 lần dereference | **1 lần** |
| Cục bộ cache | 256 mảng con rải rác | **liền kề** |
| Bộ nhớ phụ | 273 × 16 B header ≈ 4.4 KB | 16 B |

---

## 2. Thứ tự chiều — vì sao `y` ở ngoài cùng?

```
  index = y·256 + x·16 + z
```

Chiều `z` biến thiên nhanh nhất ⇒ duyệt `for(y) for(x) for(z)` truy cập **tuần tự trong RAM** ⇒ tối ưu cache CPU (prefetcher đoán đúng).

Chiều `y` chậm nhất ⇒ một **lát ngang** (`y` cố định) nằm liền một khối 256 byte — thuận cho việc quét theo tầng.

Ngược lại, `columnIndex(x, z) = (x << 4) | z` cho phép truy cập mảng `skyFloor` 2D chỉ với 2 phép bit.

---

## 3. Bộ nhớ mỗi chunk

```java
blocks   = new byte[size * size * height];    // 16 × 16 × 128 = 32 768 B = 32 KB
light    = new byte[size * size * height];    // 32 KB
skyFloor = new short[size * size];            // 256 × 2 = 512 B
```

**Tổng ≈ 64.5 KB/chunk.** Với bán kính tải 12 chunk (`25 × 25 = 625` chunk): **~40 MB**.

Nếu dùng `int` thay `byte` cho block ID: 256 KB/chunk ⇒ 160 MB. Việc giới hạn 256 loại khối (1 byte) là quyết định thiết kế quan trọng.

---

## 4. `skyFloor` — chỉ số chiều cao

```java
private final short[] skyFloor;               // độ cao khối đặc cao nhất + 1
Arrays.fill(skyFloor, (short) height);         // khởi tạo: cột rỗng hoàn toàn
```

Định nghĩa: `skyFloor(x,z)` = `y` nhỏ nhất sao cho mọi ô từ `y` trở lên đều **hở trời**.

Đây là **chỉ mục tăng tốc** (acceleration structure) cho `LightEngine`: thay vì quét cả cột để biết chỗ nào nhận ánh sáng trời, chỉ cần đọc 1 giá trị `O(1)`.

Dùng `short` (2 byte) vì `height ≤ 32 767`.

### 4.1 `rebuildSkyFloor` — quét toàn bộ

```java
for (x, z) {
    int floor = 0;
    for (int y = height - 1; y >= 0; y--)
        if (blocks[index(x, y, z)] != 0) { floor = y + 1; break; }
    skyFloor[columnIndex(x, z)] = (short) floor;
}
```

Quét **từ trên xuống**, dừng ở khối đặc đầu tiên.

**Độ phức tạp:** `O(size² · height)` = 32 768. Chỉ gọi **một lần** sau khi sinh chunk.

### 4.2 `updateSkyFloor` — cập nhật tăng dần

```java
public void updateSkyFloor(int x, int y, int z, boolean placed) {
    int column = columnIndex(x, z);
    if (placed) {
        if (y + 1 > skyFloor[column]) skyFloor[column] = (short) (y + 1);
        return;                                                    // O(1)
    }
    if (y + 1 == skyFloor[column]) {                                // phá khối TRÊN CÙNG
        int floor = 0;
        for (int scan = y; scan >= 0; scan--)
            if (blocks[index(x, scan, z)] != 0) { floor = scan + 1; break; }
        skyFloor[column] = (short) floor;
    }
    // phá khối dưới mặt đất → skyFloor không đổi → O(1)
}
```

### Phân tích ba trường hợp

| Hành động | Chi phí | Vì sao |
|---|---|---|
| **Đặt** khối | **`O(1)`** | Chỉ cần so sánh và có thể nâng `skyFloor` |
| **Phá** khối dưới mặt đất | **`O(1)`** | `y + 1 ≠ skyFloor` ⇒ không đổi gì |
| **Phá** khối trên cùng | `O(y)` | Phải quét xuống tìm khối đặc kế tiếp |

So với gọi `rebuildSkyFloor()` (32 768 phép) mỗi lần người chơi đặt/phá một khối, đây là **tăng tốc hàng nghìn lần** cho trường hợp phổ biến nhất.

Đây là ví dụ điển hình của **cập nhật tăng dần (incremental update)** thay cho tính lại toàn bộ.

---

## 5. `nonAirCount` — đếm tăng dần

```java
public void setBlockId(int x, int y, int z, byte id) {
    int i = index(x, y, z);
    byte previous = blocks[i];
    if (previous == id) return;                 // không đổi gì
    if (previous == 0)  nonAirCount++;          // air → đặc
    else if (id == 0)   nonAirCount--;          // đặc → air
    blocks[i] = id;
}
```

Bảng chuyển trạng thái:

| `previous` | `id` | `nonAirCount` |
|---|---|---|
| air (0) | đặc | **+1** |
| đặc | air (0) | **−1** |
| đặc A | đặc B | không đổi |
| bằng nhau | — | thoát sớm |

`nonAirCount == 0` ⇒ chunk **hoàn toàn rỗng** ⇒ bỏ qua bước dựng mesh và vẽ. Kiểm tra `O(1)` thay vì quét 32 768 ô.

---

## 6. Bộ đệm ánh sáng `volatile` — copy-on-write

```java
private volatile byte[] light;

public byte rawLight(int index)         { return light[index]; }
public void commitLight(byte[] rebuilt) { this.light = rebuilt; }
```

> Comment trong code: *"Luồng mesh đọc mảng này trong khi luồng ánh sáng có thể đang tính lại chunk. Đó là lý do `LightEngine` không bao giờ ghi thẳng vào nó: nó tính một bản sao hoàn chỉnh mới rồi gọi `commitLight` để chuyển sang — một phép gán duy nhất. Người đọc luôn thấy một bản hoàn chỉnh, không bao giờ thấy mảng bị xoá dở (chính là thứ làm chunk nhấp nháy đen)."*

### Vì sao `volatile` là đủ?

1. **Gán tham chiếu là nguyên tử** trong JVM (đảm bảo bởi JLS §17.7 cho mọi kiểu trừ `long`/`double` không volatile).
2. `volatile` tạo quan hệ **happens-before**: mọi thao tác ghi vào mảng mới **trước** `commitLight` sẽ hiển thị cho luồng đọc **sau** khi nó đọc tham chiếu mới.
3. Không cần khoá ⇒ không có tranh chấp, không có deadlock.

Chi tiết BFS: xem [LightEngine.md](LightEngine.md) §7.

### Giải nén ánh sáng

```java
public int skyLight(int index)   { return (light[index] >> 4) & 0x0F; }
public int blockLight(int index) { return  light[index]       & 0x0F; }
public int combinedLight(int index) {
    byte packed = light[index];
    int sky   = (packed >> 4) & 0x0F;
    int block =  packed       & 0x0F;
    return sky > block ? sky : block;             // MAX của hai kênh
}
```

`combinedLight` đọc mảng **một lần** rồi tách hai nibble — tránh 2 lần truy cập bộ nhớ.

---

## 7. Tổng hợp độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `index`, `columnIndex` | `O(1)` — 3 phép bit |
| `blockId`, `setBlockId` | `O(1)` |
| `skyLight`, `blockLight`, `combinedLight` | `O(1)` |
| `commitLight` | `O(1)` — một phép gán |
| `rebuildSkyFloor` | `O(size² · height)` = 32 768 |
| `updateSkyFloor` | `O(1)` thường, `O(y)` khi phá khối trên cùng |
| Bộ nhớ | `O(size² · height)` ≈ 64.5 KB |

---

## 8. Chủ đề DSA thể hiện

- **Làm phẳng mảng đa chiều** bằng dịch bit (chỉ khi kích thước là luỹ thừa 2).
- **Đóng gói trường bit** — 2 giá trị 4-bit trong 1 byte.
- **Chỉ mục tăng tốc** (`skyFloor`) & **cập nhật tăng dần**.
- **Bộ đếm tăng dần** (`nonAirCount`) thay cho quét lại.
- **Copy-on-write + `volatile`** — đồng bộ không khoá.
- **Cục bộ bộ nhớ** — thứ tự chiều tối ưu cho cache.

---

## 9. Liên kết

- BFS ánh sáng: [LightEngine.md](LightEngine.md)
- Người dùng chỉ số: [ChunkMesher.md](../07-render/ChunkMesher.md)
- Khoá chunk 64-bit: [ChunkKey.md](../06-datastructures/ChunkKey.md)
