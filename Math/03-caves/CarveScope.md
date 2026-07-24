# CarveScope — vùng làm việc & thuật toán khoét

**File:** `core/src/com/voxel/game/terrain/carve/CarveScope.java`

Lớp này giữ **toàn bộ phần hình học** của việc khoét hang: kiểm tra va chạm, voxel hoá ellipsoid, và luật an toàn (không thủng đáy hồ, không mỏng mái hang).

---

## 1. `touches` — loại bỏ sớm bằng AABB

```java
public boolean touches(double worldX, double worldZ, double radius) {
    return worldX + radius >= originX
        && worldX - radius <  originX + size
        && worldZ + radius >= originZ
        && worldZ - radius <  originZ + size;
}
```

### Công thức

Chunk là hình chữ nhật `[oₓ, oₓ+s) × [o_z, o_z+s)`. Hình tròn bán kính `r` tâm `(x, z)` **có thể** chạm nếu hình vuông bao của nó chồng lên chunk:

```
  x + r ≥ oₓ   ∧   x − r < oₓ + s
  z + r ≥ o_z  ∧   z − r < o_z + s
```

Đây là kiểm tra **AABB vs AABB** (hộp bao của hình tròn), `O(1)` với 4 phép so sánh.

**Lưu ý:** đây là kiểm tra *bảo thủ* — có thể trả `true` khi hình tròn thật sự không chạm (trường hợp góc chéo). Nhưng vì nó chỉ dùng để **loại bỏ sớm** (early rejection), false-positive chỉ tốn thêm một vòng lặp rỗng, còn false-negative sẽ mất khối — nên bảo thủ là đúng.

**Hiệu quả:** một đường hầm dài 125 bước trải qua ~11 chunk. Trong vùng quét 169 chunk, chỉ ~1/169 số bước thực sự chạm chunk hiện tại ⇒ `touches` cắt bỏ **>99 %** công việc.

---

## 2. `clearEllipsoid` — voxel hoá khối ellipsoid

```java
public void clearEllipsoid(double worldX, double worldY, double worldZ,
                           double radiusX, double radiusY, double radiusZ) {
    int minX = max(originX, floor(worldX - radiusX));
    int maxX = min(originX + size - 1, ceil(worldX + radiusX));
    ...
    for (int x = minX; x <= maxX; x++) {
        double nx = (x + 0.5 - worldX) / radiusX;
        for (int z = minZ; z <= maxZ; z++) {
            double nz = (z + 0.5 - worldZ) / radiusZ;
            if (nx*nx + nz*nz > 1.0) continue;           // ← cắt tỉa 2D
            for (int y = minY; y <= maxY; y++) {
                double ny = (y + 0.5 - worldY) / radiusY;
                if (nx*nx + ny*ny + nz*nz > 1.0) continue;
                clear(x - originX, y, z - originZ);
            }
        }
    }
}
```

### 2.1 Phương trình ellipsoid chuẩn hoá

```
        ⎛ x − cₓ ⎞²   ⎛ y − c_y ⎞²   ⎛ z − c_z ⎞²
  F  =  ⎜ ────── ⎟ + ⎜ ─────── ⎟ + ⎜ ─────── ⎟   ≤ 1
        ⎝   rₓ   ⎠   ⎝   r_y   ⎠   ⎝   r_z   ⎠
```

Bằng cách chia mỗi trục cho bán trục tương ứng (`nx, ny, nz`), bài toán "điểm có nằm trong ellipsoid không" quy về "điểm có nằm trong **hình cầu đơn vị** không" — chỉ cần so `nx² + ny² + nz² ≤ 1`.

### 2.2 Offset `+ 0.5` — tâm khối

```java
double nx = (x + 0.5 - worldX) / radiusX;
```

Toạ độ nguyên `x` chỉ **góc** của khối; tâm khối là `x + 0.5`. Đo khoảng cách từ **tâm khối** đến tâm ellipsoid cho kết quả voxel hoá đối xứng, không lệch nửa khối về một phía.

### 2.3 Hộp bao — giới hạn vòng lặp

```
  minX = max(oₓ,        ⌊cₓ − rₓ⌋)
  maxX = min(oₓ+s−1,    ⌈cₓ + rₓ⌉)
  minY = max(1,         ⌊c_y − r_y⌋)      ← không đụng bedrock y=0
  maxY = min(H−1,       ⌈c_y + r_y⌉)
```

`max`/`min` **kẹp vào biên chunk** — đây chính là cơ chế "chỉ khoét trong phạm vi chunk hiện tại", cho phép hai chunk cạnh nhau khoét cùng một con giun mà không đâm nhau.

### 2.4 Cắt tỉa 2D trước vòng lặp Y

```java
if (nx*nx + nz*nz > 1.0) continue;      // bỏ qua CẢ CỘT y
```

Đây là tối ưu quan trọng. Nếu `(x, z)` đã nằm ngoài hình tròn chiếu xuống mặt phẳng XZ thì **không có** `y` nào thoả mãn:

```
  nx² + nz² > 1  ⟹  nx² + ny² + nz² > 1  ∀ ny
```

(vì `ny² ≥ 0`).

**Hiệu quả:** hộp bao có `(2rₓ)(2r_z)` cột; hình tròn chỉ chiếm `πrₓr_z`. Tỉ lệ:

```
  π rₓ r_z / (4 rₓ r_z) = π/4 ≈ 78.5 %
```

⇒ loại bỏ được **21.5 %** số cột ngay lập tức, mỗi cột tiết kiệm `2r_y` phép tính.

Với hình cầu: tỉ lệ ô thực sự bị khoét trên hộp bao là `(4/3)πr³ / 8r³ = π/6 ≈ 52.4 %`.

---

## 3. `clear` — bốn luật an toàn

```java
public void clear(int localX, int y, int localZ) {
    if (y < 1 || y >= worldHeight) return;                          // (1)
    Block current = writer.get(localX, y, localZ);
    if (current.isAir() || current.isLiquid()) return;              // (2)
    if (writer.get(localX, y + 1, localZ).isLiquid()) return;       // (3)
    int roof = guard.requiredRoof(originX + localX, originZ + localZ);
    if (y + roof > surfaceHeight(localX, localZ)) return;           // (4)
    writer.set(localX, y, localZ, blocks.air);
}
```

| # | Luật | Lý do |
|---|---|---|
| 1 | `1 ≤ y < H` | Giữ lớp bedrock `y = 0`, không tràn mảng |
| 2 | Chỉ khoét khối **đặc** | Khoét không khí là vô nghĩa; khoét nước làm mất nước |
| 3 | Ngay trên đầu **không phải nước** | Nếu không, khoét sẽ **thủng đáy hồ/biển** — nước chảy hết xuống hang |
| 4 | `y + roof ≤ surfaceHeight` | Giữ mái hang đủ dày, xem [SurfaceGuard](SurfaceGuard.md) |

### Bất đẳng thức mái hang

```
  y + roof(x, z)  ≤  surfaceHeight(x, z)
```

Diễn giải: nếu khoét ô ở độ cao `y`, thì từ `y` lên tới mặt đất phải còn **ít nhất `roof` khối**. Với `roof = 4` và mặt đất ở 60, ô cao nhất được khoét là `y = 56`.

---

## 4. Memoization — `surfaceHeight` có nhớ

```java
private final int[] surface;                    // size × size
Arrays.fill(surface, UNKNOWN);                  // UNKNOWN = Integer.MIN_VALUE

private int surfaceHeight(int localX, int localZ) {
    int index = localX * size + localZ;
    if (surface[index] != UNKNOWN) return surface[index];   // cache hit
    int top = 0;
    for (int y = worldHeight - 1; y > 0; y--) {
        Block block = writer.get(localX, y, localZ);
        if (!block.isAir() && !block.isLiquid()) { top = y; break; }
    }
    surface[index] = top;                                    // ghi cache
    return top;
}
```

### Phân tích

- **Không cache:** mỗi lời gọi `clear()` quét `O(H)` = 128 bước. Một chunk có thể gọi `clear()` hàng chục nghìn lần ⇒ `10⁴ × 128 = 1.28 × 10⁶` phép.
- **Có cache:** mỗi cột quét đúng **1 lần**. Tổng: `size² × H = 16 × 16 × 128 = 32 768` phép.

```
  Tăng tốc ≈ 39 lần
```

- **Bộ nhớ:** `int[16 × 16]` = 256 × 4 B = **1 KB** mỗi carver.
- Cache là **lazy** (tính khi cần) — cột không bị khoét thì không tốn gì.

`UNKNOWN = Integer.MIN_VALUE` làm sentinel vì `0` là giá trị hợp lệ (cột toàn không khí).

### Chỉ số 1D từ 2D

```java
int index = localX * size + localZ;
```

Phép **làm phẳng mảng 2D thành 1D** kiểu row-major: `idx = row × width + col`. Nhanh hơn `int[][]` trong Java vì tránh một lần dereference con trỏ và giữ dữ liệu liền kề trong cache CPU.

### Định nghĩa "mặt đất"

Quét từ **trên xuống**, lấy khối đầu tiên **không phải air và không phải chất lỏng**.

> Comment trong code: đo bằng **mặt đất của cột** chứ không phải "có không khí ngay trên đầu không" — nhờ vậy **hai đường hầm cắt nhau vẫn thông nhau bình thường**, chỉ riêng chỗ sắp thủng lên trời mới bị chặn.

Nếu dùng cách kia, khi hầm A đã khoét qua thì hầm B đi tới sẽ tưởng "trên đầu là không khí = đã lộ thiên" và từ chối khoét ⇒ mạng hang bị đứt đoạn.

Nước không tính là mặt đất ⇒ hang không đâm lên đáy hồ.

---

## 5. Độ phức tạp tổng hợp

| Thao tác | Chi phí |
|---|---|
| `touches` | `O(1)` — 4 so sánh |
| `clearEllipsoid` | `O(rₓ · r_y · r_z)`, thực tế `≈ π/6 · 8rₓr_yr_z` |
| `clear` | `O(1)` sau khi cache nóng |
| `surfaceHeight` lần đầu | `O(H)` |
| `surfaceHeight` các lần sau | `O(1)` |
| Bộ nhớ | `O(size²)` = 1 KB |

---

## 6. Chủ đề DSA thể hiện

- **Memoization / caching** với sentinel — tăng tốc 39 lần.
- **Làm phẳng mảng 2D → 1D** (row-major indexing).
- **Voxel hoá khối cong** (ellipsoid rasterization).
- **Cắt tỉa vòng lặp** bằng chiếu 2D.
- **AABB collision test**.
- **Lazy evaluation**.

---

## 7. Liên kết

- Luật mái hang: [SurfaceGuard.md](SurfaceGuard.md)
- Người dùng: [PerlinWormCarver.md](PerlinWormCarver.md), [RavineCarver.md](RavineCarver.md), [CheeseCaveCarver.md](CheeseCaveCarver.md), [SpaghettiCaveCarver.md](SpaghettiCaveCarver.md)
