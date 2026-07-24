# CheeseCaveCarver — hang phô mai

**File:** `core/src/com/voxel/game/terrain/carve/CheeseCaveCarver.java`

Loại hang đơn giản nhất: lấy thẳng nhiễu 3D làm ngưỡng, cho ra các **bọng rỗng tròn** rải rác như lỗ trong miếng phô mai.

---

## 1. Công thức lõi

```java
if (noise.cave(worldX, y, worldZ) > fadeThreshold)
    scope.clear(localX, y, localZ);
```

```
  Void = { p ∈ R³ : cave(p) > 0.58 }
```

Tập này có **số chiều 3** (là một khối, không phải mặt hay đường) ⇒ các bọng rỗng **tròn trịa, có thể tích**, khác hẳn hang mì Ý 1 chiều.

### Tỉ lệ thể tích bị khoét

Simplex 3D một octave có phân phối gần chuẩn với `σ ≈ 0.35`:

```
  z = 0.58 / 0.35 ≈ 1.66
  P(cave > 0.58) = 1 − Φ(1.66) ≈ 0.048
```

⇒ khoảng **4.8 % thể tích** lòng đất bị khoét thành bọng — vừa đủ để có hang mà không làm rỗng cả thế giới.

---

## 2. Hình dạng bọng — nhiễu bất đẳng hướng

```java
public double cave(int x, int y, int z) {
    return caveField.noise(x * 0.023, y * 0.040, z * 0.023);
}
```

| Trục | Tần số | Bước sóng `λ = 1/f` |
|---|---|---|
| X, Z | 0.023 | ~43 khối |
| Y | 0.040 | ~25 khối |

Tỉ lệ `λ_xz : λ_y = 43 : 25 ≈ 1.7 : 1`

⇒ Bọng rỗng có dạng **ellipsoid dẹt** (oblate): rộng ~15–20 khối theo phương ngang, cao ~8–12 khối. Hang dẹt trông tự nhiên và người chơi đi lại được, khác với hang hình cầu hoàn hảo trông giả tạo.

---

## 3. Fade-out gần trần — chống "tổ ong"

### Vấn đề

Bọng rỗng nằm rải rác **khắp nơi**. Nếu để chúng chạm mặt đất, bề mặt thế giới sẽ **rỗ như tổ ong** — cực xấu.

### Hai lớp bảo vệ

**(a) Trần cứng**

```java
int maxY = Math.min(worldHeight - 2, noise.seaLevel() + CEILING_BELOW_SEA);   // S − 2
```

Hang phô mai **chỉ tồn tại dưới `S − 2`**. Chú ý `CEILING_BELOW_SEA = −2` (âm) — hằng số được đặt tên theo nghĩa "dưới mực biển".

**(b) `SurfaceGuard.sealed()`**

```java
CarveScope scope = new CarveScope(writer, blocks, SurfaceGuard.sealed(), ...);
```

Khác mọi carver còn lại (dùng `withEntrances`), hang phô mai dùng bản **luôn giữ mái dày 4 khối**, không bao giờ được trổ lên mặt đất.

**(c) Fade-out mềm**

```java
double fadeThreshold = THRESHOLD;
int distToTop = maxY - y;
if (distToTop < FADE_BAND) {
    fadeThreshold += (1.0 - THRESHOLD) * (1.0 - (double) distToTop / FADE_BAND);
}
```

### Công thức

Đặt `d = maxY − y` (khoảng cách tới trần), `FADE_BAND = 6`:

```
              ⎧ 0.58                              nếu d ≥ 6
  T(d)   =    ⎨
              ⎩ 0.58 + 0.42·(1 − d/6)             nếu d < 6
```

Đây là **nội suy tuyến tính** từ `THRESHOLD = 0.58` lên `1.0`:

| `d` | `T(d)` | `P(cave > T)` | Ghi chú |
|---|---|---|---|
| ≥ 6 | 0.580 | 4.8 % | Bình thường |
| 5 | 0.650 | 3.1 % | |
| 4 | 0.720 | 2.0 % | |
| 3 | 0.790 | 1.2 % | |
| 2 | 0.860 | 0.7 % | |
| 1 | 0.930 | 0.4 % | |
| 0 | **1.000** | **≈ 0 %** | Tắt hẳn |

Vì Simplex 3D chuẩn hoá không bao giờ vượt 1.0, tại `d = 0` ngưỡng bằng đúng 1.0 ⇒ **không khoét gì cả**. Hang **nhỏ dần rồi tắt mượt** thay vì bị cắt phẳng lì ở đúng độ cao `maxY` — nếu cắt cứng, sẽ thấy một mặt phẳng trần nhân tạo trải khắp thế giới.

---

## 4. Vòng lặp & độ phức tạp

```java
for (localX = 0..15)
  for (localZ = 0..15)
    for (y = 6..maxY)
        1 lần lấy nhiễu 3D + so sánh
```

```
  O(size² · (maxY − MIN_Y))
```

Với `size = 16`, `S = 48`, `maxY = 46`:

```
  16 × 16 × (46 − 6) = 10 240 lần lấy nhiễu / chunk
```

Rẻ hơn spaghetti (chỉ 1 trường nhiễu thay vì 2), và cũng **hoàn toàn stateless** — không cần quét chunk lân cận.

---

## 5. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `THRESHOLD` | 0.58 | Ngưỡng khoét (≈ 4.8 % thể tích) |
| `MIN_Y` | 6 | Đáy |
| `CEILING_BELOW_SEA` | −2 | Trần = `S − 2` |
| `FADE_BAND` | 6 | Dải fade-out (khối) |
| tần số | (0.023, 0.040, 0.023) | Bất đẳng hướng 1 : 1.7 : 1 |

---

## 6. Vai trò trong hệ sinh thái hang

| Carver | Số chiều | Vai trò |
|---|---|---|
| **Cheese** | 3 | **Phòng lớn** — chỗ rộng để dừng chân |
| Spaghetti | 1 | **Hành lang** — nối các phòng |
| Perlin worm | 1 (ống dày) | **Đường hầm chính** |
| Ravine | 2 (tấm dọc) | **Khe nứt** hiếm gặp |

Chạy tuần tự (`CarverPipeline`), kết quả là **hợp** của 4 tập ⇒ một mạng hang phong phú.

---

## 7. Chủ đề DSA / Toán thể hiện

- **Phân ngưỡng trường vô hướng** (thresholding) — kỹ thuật cơ bản của isosurface extraction.
- **Nội suy tuyến tính** cho fade-out.
- **Phân phối chuẩn & z-score** để ước lượng tỉ lệ thể tích.
- **Bất đẳng hướng** trong lấy mẫu nhiễu.

---

## 8. Liên kết

- Luật mái hang: [SurfaceGuard.md](SurfaceGuard.md)
- Anh em: [SpaghettiCaveCarver.md](SpaghettiCaveCarver.md), [PerlinWormCarver.md](PerlinWormCarver.md), [RavineCarver.md](RavineCarver.md)
