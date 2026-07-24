# SpaghettiCaveCarver — hang mì Ý (Minecraft 1.18+)

**File:** `core/src/com/voxel/game/terrain/carve/SpaghettiCaveCarver.java`

Loại hang **đẹp nhất về mặt toán học** trong project: không mô phỏng gì cả, chỉ dùng thuần tính chất hình học của tập nghiệm.

---

## 1. Nguyên lý: giao của hai mặt zero

### Định lý nền tảng

Cho `f : R³ → R` là hàm trơn với gradient khác 0. Khi đó tập mức

```
  S = { p ∈ R³ : f(p) = 0 }
```

là một **mặt cong 2 chiều** (đa tạp 2 chiều nhúng trong `R³`).

Với hai hàm độc lập `A, B`, giao của hai mặt là:

```
  C = { p : A(p) = 0 } ∩ { p : B(p) = 0 }
```

**Số chiều:** mỗi phương trình ràng buộc trừ đi 1 bậc tự do:

```
  dim C = 3 − 1 − 1 = 1
```

⇒ `C` là một **đường cong 1 chiều** uốn lượn trong không gian 3D.

### Áp dụng

```java
if (|spaghettiA(x,y,z)| < w  &&  |spaghettiB(x,y,z)| < w)
    scope.clear(...);
```

Khoét mọi khối nằm trong "ống" bán kính `w` quanh đường cong `C` ⇒ **đường hầm dài vô tận, mảnh, xoắn tự nhiên**, tự động nối các hang lớn thành một mạng lưới thám hiểm được.

### So sánh trực quan

| Điều kiện | Số chiều tập nghiệm | Hình dạng |
|---|---|---|
| `A > 0.58` | 3 | **Khối** rỗng tròn → hang phô mai |
| `\|A\| < w` | 2 | **Tấm** cong → vách nứt phẳng (không dùng) |
| `\|A\| < w ∧ \|B\| < w` | 1 | **Sợi** → hang mì Ý |

---

## 2. Độ dày ống — hàm của độ sâu

```java
double width = THICKNESS + Math.max(0, noise.seaLevel() - y) * WIDEN_PER_DEPTH;
```

```
  w(y) = 0.055 + max(0, S − y) · 0.0007
```

| Độ cao `y` | Độ sâu `S − y` | `w` | Bề rộng hầm ≈ |
|---|---|---|---|
| `S + 8` = 56 | 0 | 0.0550 | ~2.2 khối |
| `S` = 48 | 0 | 0.0550 | ~2.2 khối |
| `S − 20` = 28 | 20 | 0.0690 | ~2.8 khối |
| `S − 42` = 6 | 42 | 0.0844 | ~3.4 khối |

**Tăng ~53 %** từ mặt đến đáy: hầm sâu rộng rãi đi lại thoải mái, hầm gần mặt tự bóp nhỏ rồi tắt.

### Từ đơn vị nhiễu sang khối

Gần điểm zero, nhiễu xấp xỉ tuyến tính:

```
  A(p) ≈ ∇A · (p − p₀)
```

Với Simplex 3D tần số `f = 0.014` và biên độ ~1, độ lớn gradient ước lượng:

```
  |∇A| ≈ 2π · f · A_max ≈ 2π × 0.014 × 1 ≈ 0.088  (đơn vị nhiễu / khối)
```

Vậy điều kiện `|A| < w` tương ứng khoảng cách:

```
        w         0.055
  d ≈ ─────  ≈  ───────  ≈ 0.62 khối   (nửa bề rộng)
       |∇A|      0.088
```

⇒ đường hầm rộng khoảng `2 × 0.62 × 2 ≈ 2.5` khối (nhân 2 vì giao hai điều kiện tạo tiết diện gần vuông). Khớp với comment *"roughly a 2-3 block wide corridor"*.

---

## 3. Bất đẳng hướng — hầm dẹt

```java
spaghettiA(x,y,z) = noiseA(x·0.014, y·0.020, z·0.014)
```

`f_y = 0.020` > `f_x = f_z = 0.014` (tỉ lệ 1.43).

Gradient theo `y` lớn hơn ⇒ ống bị **nén theo chiều dọc**:

```
  d_y / d_x = f_x / f_y = 0.014 / 0.020 = 0.7
```

Hầm rộng ngang, thấp trần — dễ đi và trông giống hành lang tự nhiên hơn ống tròn.

---

## 4. Phạm vi độ cao

```java
int maxY = Math.min(worldHeight - 2, noise.seaLevel() + CEILING_ABOVE_SEA);   // S + 8
MIN_Y = 6;
```

```
  y ∈ [6, min(worldHeight − 2, S + 8)]
```

Trần đặt **cao hơn mực nước biển 8 khối** — cố ý cho hầm bò lên sườn đồi để tạo cửa hang tự nhiên. Việc có thủng ra hay không do [SurfaceGuard](SurfaceGuard.md) quyết định.

Đáy `y = 6` để không đụng bedrock (`y = 0`).

---

## 5. Độ phức tạp

```java
for (localX = 0..15)
  for (localZ = 0..15)
    for (y = 6..maxY)
        2 lần lấy nhiễu 3D + so sánh
```

```
  O(size² · (maxY − MIN_Y) · 2)
```

Cụ thể với `size = 16`, `S = 48`, `maxY = 56`:

```
  16 × 16 × (56 − 6) × 2 = 25 600 lần lấy nhiễu 3D / chunk
```

Đây là carver **tốn nhất theo số lần gọi nhiễu**, nhưng bù lại:
- Không đệ quy, không cần quét chunk lân cận (mỗi ô tự quyết định).
- Hoàn toàn **stateless** ⇒ song song hoá dễ dàng.
- Không có vấn đề nối liền qua biên chunk: hai chunk cạnh nhau đọc cùng trường nhiễu nên hầm tự khớp.

**So với PerlinWorm:** worm phải quét 169 chunk lân cận; spaghetti chỉ cần chunk hiện tại. Đổi lại worm cho hầm to, spaghetti cho hầm nhỏ chằng chịt — hai loại bù trừ nhau.

---

## 6. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `THICKNESS` | 0.055 | Nửa bề rộng cơ sở (đơn vị nhiễu) |
| `WIDEN_PER_DEPTH` | 0.0007 | Nở thêm mỗi khối độ sâu |
| `MIN_Y` | 6 | Đáy |
| `CEILING_ABOVE_SEA` | 8 | Trần = `S + 8` |
| tần số | (0.014, 0.020, 0.014) | Bất đẳng hướng 1 : 1.43 : 1 |

---

## 7. Chủ đề DSA / Toán thể hiện

- **Hình học vi phân cơ bản**: số chiều của tập mức, giao của hai đa tạp.
- **Xấp xỉ tuyến tính** (khai triển Taylor bậc 1) để đổi đơn vị nhiễu ↔ khối.
- **Trường vô hướng 3D** & bất đẳng hướng.
- **Thuật toán stateless, song song hoá được** — không phụ thuộc thứ tự.

---

## 8. Liên kết

- Nguồn nhiễu: [TerrainNoise.md](../01-noise-terrain/TerrainNoise.md) §3.3
- Luật mái hang: [SurfaceGuard.md](SurfaceGuard.md)
- Anh em: [CheeseCaveCarver.md](CheeseCaveCarver.md), [PerlinWormCarver.md](PerlinWormCarver.md)
