# RavineCarver — khe nứt

**File:** `core/src/com/voxel/game/terrain/carve/RavineCarver.java`

Cùng ý tưởng perlin worm nhưng khoét **hình ellipsoid cao** thay vì hình cầu ⇒ một vết nứt dài, hẹp ngang, sâu hun hút. Rất hiếm gặp.

---

## 1. Độ hiếm

```java
Random random = new Random(CaveSeeds.forChunk(seed, cx, cz, 2));
if (random.nextInt(SPAWN_RARITY) != 0) return;      // SPAWN_RARITY = 90
```

```
  P(một chunk sinh khe nứt) = 1/90 ≈ 1.11 %
```

So sánh với perlin worm (`1/5 = 20 %`) — khe nứt **hiếm gấp 18 lần**. Đây là chủ ý thiết kế: khe nứt là điểm nhấn cảnh quan, gặp thường xuyên sẽ mất giá trị.

Vùng quét `CHUNK_RADIUS = 5` ⇒ `11 × 11 = 121` chunk:

```
  E[số khe nứt tái tạo mỗi lần sinh chunk] = 121 / 90 ≈ 1.34
```

`salt = 2` (perlin worm dùng `salt = 1`) đảm bảo hai carver dùng cùng `(seed, cx, cz)` nhưng cho hai chuỗi ngẫu nhiên **độc lập** — xem [CaveSeeds.md](CaveSeeds.md).

---

## 2. Tham số khởi tạo

```java
double x = cx·chunkSize + rand(chunkSize);
double z = cz·chunkSize + rand(chunkSize);
double y = 20 + rand(18);              // [20, 37]
double heading = rand() · 2π;
int steps  = 90 + rand(70);            // [90, 159] bước
double width = 2.0 + rand()·1.6;       // [2.0, 3.6]  bán trục NGANG
double depth = 8.0 + rand()·6.0;       // [8.0, 14.0] bán trục DỌC
```

### Tỉ lệ hình dạng

```
  depth / width ∈ [8/3.6, 14/2.0] = [2.2, 7.0]
```

Trung bình `≈ 11/2.8 ≈ 3.9` — khe nứt **cao gấp ~4 lần bề ngang**. Chiều cao thực tế `2 × 11 = 22` khối, bề ngang `2 × 2.8 = 5.6` khối.

Comment trong code: đặt `y ∈ [20, 37]` cố ý **nông hơn** perlin worm để đỉnh khe có thể chạm lớp đất mặt, và ở nơi `SurfaceGuard` cho phép thì **xé toạc ra tận mặt đất** — tạo cảnh khe nứt khổng lồ nhìn từ trên xuống.

---

## 3. Vòng lặp bò — đơn giản hơn perlin worm

```java
for (int step = 0; step < steps; step++) {
    heading += noise.worm(step * 0.04, wobble) * 0.20;
    x += Math.cos(heading);
    z += Math.sin(heading);
    y += noise.worm(step * 0.03, wobble + 256.0) * 0.5;
    ...
}
```

### Khác biệt so với PerlinWorm

| | PerlinWorm | Ravine |
|---|---|---|
| Bước ngang | `cosθ·cosφ`, `sinθ·cosφ` (toạ độ cầu) | `cosθ`, `sinθ` (**luôn ngang**) |
| Thay đổi độ cao | qua góc `φ` có bộ lọc IIR | `y += worm(·)·0.5` trực tiếp |
| Biên độ lái | 0.35 rad | **0.20 rad** (thẳng hơn) |
| `Δt` nhiễu | 0.06 | **0.04** (chu kỳ ~25 bước, dài hơn) |
| Đệ quy | có, 2 cấp | **không** |

**Vì sao thẳng hơn?** Khe nứt trong tự nhiên hình thành do đứt gãy địa chất — chúng chạy theo đường gần thẳng dài. Biên độ lái nhỏ (0.20 rad) + chu kỳ nhiễu dài (25 bước) cho một đường **thoải, ít ngoằn ngoèo**.

**Độ cao trôi tự do:** `y += worm(0.03k, w+256)·0.5` — độ cao dao động ±0.5 khối mỗi bước theo nhiễu liên tục, tổng dịch chuyển kiểu **random walk tương quan**. Không có kẹp biên nào, nhưng vì bán trục dọc `depth ≈ 11` đã rất lớn nên khe nứt luôn xuyên qua nhiều tầng.

**Hai offset nhiễu khác nhau** (`wobble` cho hướng, `wobble + 256` cho độ cao) đảm bảo hai đại lượng không tương quan — nếu dùng chung, khe nứt sẽ luôn "rẽ trái thì đi lên".

---

## 4. Thu nhỏ hai đầu

```java
double taper = Math.sin(Math.PI * (step + 1.0) / (steps + 1.0));
double radiusX = width * (0.4 + 0.9 * taper);
double radiusY = depth * (0.4 + 0.9 * taper);
```

```
  taper(k) = sin( π(k+1) / (n+1) )  ∈ (0, 1]
  r(k)     = r₀ · (0.4 + 0.9 · taper(k))
```

| Vị trí | Hệ số |
|---|---|
| Hai đầu | 0.40 |
| Giữa | **1.30** |

So với perlin worm (`0.55 + 0.75·taper`), khe nứt **thắt chặt hơn ở hai đầu** (0.40 vs 0.55) — vết nứt nhọn dần rồi khép lại, đúng hình dáng đứt gãy thật.

Cả hai bán trục cùng nhân `taper` ⇒ khe nứt **thu nhỏ đồng dạng**, giữ nguyên tỉ lệ cao/rộng suốt chiều dài.

---

## 5. Khoét ellipsoid

```java
if (scope.touches(x, z, radiusX + 1.0)) {
    scope.clearEllipsoid(x, y, z, radiusX, radiusY, radiusX);
}
```

Phương trình ellipsoid trục chính (xem [CarveScope.md](CarveScope.md)):

```
   ⎛ x − cx ⎞²   ⎛ y − cy ⎞²   ⎛ z − cz ⎞²
   ⎜ ────── ⎟ + ⎜ ────── ⎟ + ⎜ ────── ⎟  ≤ 1
   ⎝   rₓ   ⎠   ⎝   r_y  ⎠   ⎝   r_z  ⎠
```

với `rₓ = r_z = radiusX` (đối xứng ngang) và `r_y = radiusY` (kéo dài dọc).

Chú ý `touches(x, z, radiusX + 1.0)` — cộng thêm 1 khối lề an toàn để không bỏ sót ô ở biên do làm tròn.

---

## 6. Độ phức tạp

Một khe nứt:

```
  O(steps · rₓ² · r_y)
```

Thể tích hộp bao mỗi bước: `(2rₓ)(2r_y)(2r_z) = 8 · 2.8² · 11 ≈ 690` ô.

```
  125 bước × 690 ≈ 86 000 phép kiểm tra / khe nứt
```

Nhưng `touches()` loại sớm gần hết, và xác suất có khe nứt chỉ 1.11 % ⇒ chi phí trung bình mỗi chunk:

```
  121 chunk × (1/90) × 86 000 / 121 ≈ 956 phép kiểm tra thực sự
```

Rẻ hơn nhiều so với spaghetti (25 600 lần lấy nhiễu).

---

## 7. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `CHUNK_RADIUS` | 5 | Vùng quét 11×11 chunk |
| `SPAWN_RARITY` | 90 | 1/90 chunk có khe nứt |
| `salt` | 2 | Tách khỏi perlin worm (salt 1) |
| `y` khởi tạo | [20, 37] | Nông, để có thể xé lên mặt đất |
| `steps` | [90, 159] | Dài hơn perlin worm |
| `width` | [2.0, 3.6] | Bán trục ngang |
| `depth` | [8.0, 14.0] | Bán trục dọc |
| biên độ lái | 0.20 rad | Thẳng hơn perlin worm |
| `Δt` nhiễu | 0.04 | Chu kỳ ~25 bước |
| taper | `0.4 + 0.9·sin` | Thắt chặt hai đầu |

---

## 8. Chủ đề DSA / Toán thể hiện

- **Phương trình ellipsoid** & voxel hoá khối cong.
- **Bước ngẫu nhiên tương quan** trên mặt phẳng ngang.
- **Hàm bao hình sin** (envelope) để thu nhỏ hai đầu.
- **Tách seed bằng salt** — cùng nguồn, hai chuỗi độc lập.
- **Loại bỏ sớm bằng hộp bao**.

---

## 9. Liên kết

- Khoét ellipsoid: [CarveScope.md](CarveScope.md)
- Seed: [CaveSeeds.md](CaveSeeds.md)
- Anh em: [PerlinWormCarver.md](PerlinWormCarver.md)
