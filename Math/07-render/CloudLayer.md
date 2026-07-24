# CloudLayer — mây khối 3D

**File:** `core/src/com/voxel/engine/render/CloudLayer.java`

Mây là **hộp 3D thật** (12 × 4 × 12 khối), bay quanh và nhìn từ dưới lên hay trên xuống đều được — không phải tấm ảnh phẳng.

---

## 1. Chia lưới ô mây

```java
private static final float CELL = 12f;
private static final int RADIUS_CELLS = 13;
private static final float CLOUD_Y = 132f;
```

Bầu trời chia thành lưới ô `12 × 12` khối. Mỗi ô hoặc có mây (hộp đặc) hoặc trống.

```
  Vùng vẽ = (2×13 + 1)² = 27² = 729 ô = 324 × 324 khối
```

`CLOUD_Y = 132` — cao hơn đỉnh núi cao nhất (`worldHeight = 128`) để mây không cắm vào núi.

---

## 2. `cloudAt` — hai octave băm

```java
private static boolean cloudAt(int cellX, int cellZ, float coverage) {
    float coarse = hash(cellX >> 1, cellZ >> 1);      // ô 2×2 dùng chung giá trị
    float fine   = hash(cellX, cellZ);
    return coarse * 0.65f + fine * 0.35f < coverage;
}
```

### Công thức

```
  V(c) = 0.65 · H(⌊cx/2⌋, ⌊cz/2⌋) + 0.35 · H(cx, cz)
  có mây  ⟺  V(c) < coverage
```

Đây là **fBm rời rạc 2 octave** làm bằng tay:

| Octave | Trọng số | Bước sóng | Vai trò |
|---|---|---|---|
| Thô (`>> 1`) | 0.65 | 24 khối | Gom ô thành **cụm mây lớn** |
| Mịn | 0.35 | 12 khối | **Gặm rìa** cụm cho lởm chởm |

`cellX >> 1` là chia 2 làm tròn xuống ⇒ 4 ô kề nhau (2×2) chia sẻ **cùng** giá trị thô. Nếu chỉ có octave mịn, mây sẽ rải rác từng ô đơn lẻ như hạt tiêu; nếu chỉ có thô, mây sẽ là các khối vuông 24×24 đều tăm tắp.

Trọng số `0.65 : 0.35` — cùng tinh thần `gain = 0.5` của [SimplexNoise.fractal2d](../01-noise-terrain/SimplexNoise.md), nhưng thiên về octave thô hơn để cụm mây rõ ràng.

> Comment: *"pure hashing, no noise library"* — rẻ hơn Simplex nhiều lần, đủ dùng cho mây khối vuông.

### Hàm băm

```java
private static float hash(int x, int z) {
    long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
    h ^= h >>> 29;
    h *= 0xBF58476D1CE4E5B9L;
    h ^= h >>> 32;
    return (h & 0xFFFFFF) / (float) 0x1000000;
}
```

Cùng họ với [Deterministic](../01-noise-terrain/Deterministic.md): hằng số vàng + xor-shift-multiply. Chỉ **2 vòng trộn** (thay vì 3) vì mây không cần chất lượng cao như địa hình.

### Độ phủ

```java
private static final float COVERAGE = 0.38f;
private static final float RAIN_EXTRA_COVERAGE = 0.22f;

float coverage = COVERAGE + RAIN_EXTRA_COVERAGE * rain;
```

```
  coverage(rain) = 0.38 + 0.22 · rain      ∈ [0.38, 0.60]
```

Vì `V` phân bố xấp xỉ đều trên `[0, 1)` (trung bình có trọng số của hai biến đều):

| Trời | `coverage` | Tỉ lệ ô có mây |
|---|---|---|
| Quang | 0.38 | **~38 %** |
| Mưa | 0.60 | **~60 %** |

Trời mưa mây **kín hơn 58 %** — bầu trời khép lại.

---

## 3. Trôi về hướng đông

```java
private static final float DRIFT_SPEED = 0.8f;

float drift = elapsed * DRIFT_SPEED;
int cellX = (int) Math.floor((camera.position.x - drift) / CELL);
int cellZ = (int) Math.floor(camera.position.z / CELL);
...
shader.setUniformf("u_offset", cellX * CELL + drift, CLOUD_Y, cellZ * CELL);
```

### Cơ chế

Mây trôi bằng cách **dịch hệ toạ độ**, không phải dịch từng đỉnh:

```
  cellX = ⌊ (x_cam − drift) / 12 ⌋          ← chỉ số ô trong "không gian mây"
  offset = cellX · 12 + drift                ← đưa về không gian thế giới
```

Trừ `drift` khi tính ô, cộng lại khi vẽ ⇒ **mẫu mây cố định trong không gian mây**, nhưng cả lớp trượt về +X với tốc độ `0.8` khối/giây.

```
  Một ô mây đi hết chiều rộng của mình sau 12 / 0.8 = 15 giây
```

**Ưu điểm:** mesh giữ nguyên, chỉ đổi một uniform ⇒ trôi mây gần như miễn phí.

### Vì sao mây "đứng yên" khi người chơi đi?

Chỉ số ô tính từ **toạ độ thế giới** của camera. Người chơi đi 12 khối ⇒ `cellX` tăng 1 ⇒ mesh dựng lại quanh ô mới, nhưng **mẫu mây trong thế giới không đổi**. Người chơi thấy mây đứng yên tại chỗ, đúng như thực tế.

---

## 4. Dựng lại có điều kiện

```java
if (cellX != builtCellX || cellZ != builtCellZ || Math.abs(coverage - builtCoverage) > 0.05f) {
    rebuild(cellX, cellZ, coverage);
}
```

**Ba điều kiện kích hoạt:**
1. Người chơi (hoặc drift) vượt sang ô mới.
2. Độ phủ đổi quá `0.05` (khi bắt đầu/kết thúc mưa).

### Tần suất

Người chơi đứng yên: chỉ `drift` làm `cellX` đổi ⇒ **mỗi 15 giây một lần**.
Người chơi chạy `4.3` khối/s: `12 / 4.3 ≈ 2.8` giây một lần.

> Comment: *"about once every 20 seconds, costing well under a millisecond"*

Ngưỡng `0.05` cho `coverage` tạo **vùng chết (dead zone)** — trong 4 giây chuyển tiếp mưa (`TRANSITION_TIME`), độ phủ đi từ 0.38 → 0.60, vượt ngưỡng khoảng `0.22/0.05 ≈ 4` lần ⇒ chỉ dựng lại 4 lần thay vì mỗi khung hình (240 lần).

---

## 5. Làm mờ rìa — tránh cạnh vuông

```java
float distance = (float) Math.sqrt((double) dx * dx + dz * dz) / RADIUS_CELLS;
float alpha = 0.82f * Math.max(0f, 1f - distance * distance);
if (alpha < 0.03f) continue;
```

### Công thức

```
       √(dx² + dz²)
  r = ──────────────  ,   α = 0.82 · max(0, 1 − r²)
           13
```

| `r` | `α` | Ghi chú |
|---|---|---|
| 0 (ngay trên đầu) | **0.82** | Đục nhất |
| 0.5 | 0.615 | |
| 0.7 | 0.418 | |
| 0.9 | 0.156 | |
| ≥ 1.0 | **0** | Bị loại |

`1 − r²` là **parabol** — mờ chậm ở gần, nhanh ở xa. Nhờ vậy lớp mây tan dần vào chân trời thay vì kết thúc bằng **một cạnh vuông sắc lẹm** ở biên `27 × 27` ô.

Vì `r` dùng khoảng cách **Euclid** (không phải Chebyshev), vùng mây thực tế là **hình tròn** nội tiếp hình vuông ⇒ nhìn từ mọi hướng đều giống nhau.

Ngưỡng `α < 0.03` loại luôn các ô góc quá mờ ⇒ tiết kiệm khoảng `1 − π/4 ≈ 21 %` số hộp.

---

## 6. Gộp mặt giữa các ô mây kề nhau

```java
boolean west  = cloudAt(cellX + dx - 1, cellZ + dz, coverage);
boolean east  = cloudAt(cellX + dx + 1, cellZ + dz, coverage);
boolean north = cloudAt(cellX + dx, cellZ + dz - 1, coverage);
boolean south = cloudAt(cellX + dx, cellZ + dz + 1, coverage);

if (!west)  { ...vẽ mặt tây... }
if (!east)  { ...vẽ mặt đông... }
if (!north) { ...vẽ mặt bắc... }
if (!south) { ...vẽ mặt nam... }
```

Đây chính là **face culling** — cùng nguyên lý với [ChunkMesher](ChunkMesher.md), áp dụng cho lưới mây.

Hai ô mây kề nhau **không vẽ vách ngăn giữa chúng** ⇒ chúng hợp thành một khối mây lớn liền lạc, không thấy đường kẻ ô.

### Tiết kiệm

Với `coverage = 0.38`, xác suất một hàng xóm cũng có mây ≈ 0.38 (xấp xỉ, thực tế cao hơn vì octave thô làm chúng tụ cụm):

```
  E[mặt bên bị bỏ] ≈ 4 × 0.38 = 1.52 / hộp
  Tiết kiệm ≈ 1.52 / 6 ≈ 25 % số mặt
```

**Mặt trên và dưới luôn vẽ** — không có ô mây nào nằm trên/dưới ô khác (lớp mây chỉ một tầng).

---

## 7. Bóng nướng sẵn (baked shading)

```java
quad(..., alpha, 1.00f, ... THICKNESS ...);   // mặt TRÊN   — sáng nhất
quad(..., alpha, 0.68f, ... 0f ...);          // mặt DƯỚI   — tối nhất
quad(..., alpha, 0.84f, ... x0/x1 ...);       // Đông/Tây
quad(..., alpha, 0.90f, ... z0/z1 ...);       // Bắc/Nam
```

| Mặt | `shade` |
|---|---|
| Trên | **1.00** |
| Bắc/Nam | 0.90 |
| Đông/Tây | 0.84 |
| Dưới | **0.68** |

Cùng ý tưởng với `Direction.shade()` (xem [Direction](../06-datastructures/Direction.md)) nhưng **nướng thẳng vào màu đỉnh** lúc dựng mesh, không tính lúc vẽ. Dải giá trị hẹp hơn (`0.68–1.00` so với `0.48–1.00`) vì mây trắng cần giữ độ sáng cao.

---

## 8. Cấp phát bộ nhớ một lần

```java
int side = RADIUS_CELLS * 2 + 1;                       // 27
int maxBoxes = side * side;                            // 729
vertices = new float[maxBoxes * 6 * 4 * 9];            // 729 × 216 = 157 464 float
indices  = new short[maxBoxes * 6 * 6];                // 26 244 short
```

**Kích thước tối đa** (mọi ô đều có mây, không mặt nào bị gộp):

```
  vertices: 157 464 × 4 B = 630 KB
  indices :  26 244 × 2 B =  52 KB
```

Cấp phát **một lần** trong constructor, `rebuild()` chỉ ghi đè phần đầu và gọi:

```java
mesh.setVertices(vertices, 0, floatCursor);
mesh.setIndices(indices, 0, indexCursor);
```

Truyền `count` để GPU chỉ nạp phần thực dùng. Không cấp phát trong `rebuild()` ⇒ không rác GC.

**Đỉnh tối đa:** `729 × 6 × 4 = 17 496` — an toàn dưới giới hạn 65 536 của chỉ số `short`.

---

## 9. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `render` (không dựng lại) | `O(1)` — 3 uniform + 1 lệnh vẽ |
| `rebuild` | `O(729 × 5)` lần `cloudAt` ≈ **3 645** lần băm |
| Bộ nhớ | **682 KB** cố định |
| Tần suất dựng lại | ~1 lần / 3–15 giây |

`cloudAt` được gọi 5 lần mỗi ô (1 cho chính nó + 4 hàng xóm). Có thể tối ưu bằng cache một hàng, nhưng vì `rebuild` hiếm nên không cần.

---

## 10. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `CELL` | 12 | Cạnh ô mây (khối) |
| `THICKNESS` | 4 | Chiều cao hộp mây |
| `RADIUS_CELLS` | 13 | Bán kính vùng vẽ (ô) |
| `CLOUD_Y` | 132 | Độ cao lớp mây |
| `DRIFT_SPEED` | 0.8 | Khối/giây về hướng đông |
| `COVERAGE` | 0.38 | Độ phủ trời quang |
| `RAIN_EXTRA_COVERAGE` | 0.22 | Cộng thêm khi mưa |
| `alpha` gốc | 0.82 | Độ đục tối đa |

---

## 11. Chủ đề DSA / Toán thể hiện

- **fBm rời rạc** làm bằng hai octave băm với trọng số.
- **Băm không gian** (spatial hashing) thay thư viện nhiễu.
- **Dịch hệ toạ độ** để tạo chuyển động mà không đụng mesh.
- **Dựng lại có điều kiện** + **vùng chết** để giảm tần suất.
- **Face culling** trên lưới ô.
- **Làm mờ parabol** `1 − r²` để tránh cạnh cứng.
- **Bóng nướng sẵn** (baked lighting).
- **Cấp phát trước theo trường hợp xấu nhất** — không rác GC.

---

## 12. Liên kết

- Vẽ chung shader: [SkyRenderer.md](SkyRenderer.md)
- Độ phủ theo mưa: [WeatherSystem.md](WeatherSystem.md)
- Hàm băm anh em: [Deterministic.md](../01-noise-terrain/Deterministic.md)
