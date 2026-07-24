# StructureCarver — kim tự tháp & tháp canh

**Files:**
- `core/src/com/voxel/game/terrain/structure/StructureCarver.java`
- `core/src/com/voxel/game/terrain/structure/StructureWriter.java`

Công trình lớn trải nhiều chunk. Chạy **cuối cùng** trong `CarverPipeline` để nhà cửa đứng trên địa hình đã khoét hang xong.

---

## 1. Phân ô (cell partitioning) — cách trải công trình đều mà vẫn ngẫu nhiên

### Vấn đề

Nếu mỗi chunk tự tung xúc xắc `P = 0.005` để sinh kim tự tháp, kết quả là **phân phối Poisson**: có chỗ 3 cái chồng lên nhau, có chỗ trống 5 000 khối. Xấu.

### Giải pháp: chia lưới ô

```java
private static final int CELL_CHUNKS = 12;      // 1 ô = 12 × 12 chunk = 192 × 192 khối
int cellX = Math.floorDiv(chunkX, CELL_CHUNKS);
int cellZ = Math.floorDiv(chunkZ, CELL_CHUNKS);
```

Mỗi ô **192 × 192 khối** chứa **tối đa 1** công trình.

```
  P(ô có công trình) = SPAWN_CHANCE = 0.55

  Mật độ = 0.55 / 192²  ≈ 1.49 × 10⁻⁵ công trình / khối²
         ≈ 1 công trình mỗi 67 000 khối²
         ≈ khoảng cách trung bình ~260 khối
```

Đây là **stratified sampling** (lấy mẫu phân tầng): đảm bảo phân bố đều hơn Poisson thuần, không bao giờ có 2 công trình sát nhau.

### `Math.floorDiv` chứ không phải `/`

```java
Math.floorDiv(−1, 12) = −1        ✔ đúng ô
        (−1) / 12     =  0        ✗ sai! trùng ô với chunk 0..11
```

Phép `/` của Java làm tròn **về 0**, khiến hai ô `[−12, −1]` và `[0, 11]` bị gộp làm một ⇒ nửa thế giới phía âm bị lệch. `floorDiv` làm tròn **xuống** nên đúng ở cả hai phía.

### Lề an toàn

```java
int originX = cellX * cellBlocks + CELL_MARGIN + random.nextInt(cellBlocks - CELL_MARGIN * 2);
```

```
  vị trí ∈ [cellStart + 28,  cellStart + 192 − 28)
         = [cellStart + 28,  cellStart + 164)
```

`CELL_MARGIN = 28` giữ tâm công trình cách biên ô ≥ 28 khối. Vì kim tự tháp rộng nhất `half = 15` ⇒ bán kính 15 + 2 = 17 < 28, công trình **không bao giờ tràn sang ô kế bên** ⇒ hai công trình của hai ô cạnh nhau không chồng lấn.

---

## 2. Tái tạo tất định — quét 3×3 ô

```java
for (int dx = -1; dx <= 1; dx++)
    for (int dz = -1; dz <= 1; dz++)
        buildCell(out, cellX + dx, cellZ + dz, chunkSize);
```

Cùng kỹ thuật **replay** như [PerlinWormCarver](PerlinWormCarver.md): mỗi chunk dựng lại toàn bộ công trình của 9 ô lân cận, nhưng `StructureWriter.place()` **âm thầm bỏ qua** mọi khối rơi ngoài chunk hiện tại.

```java
void place(int worldX, int y, int worldZ, Block block) {
    int localX = worldX - writer.originX();
    int localZ = worldZ - writer.originZ();
    if (localX < 0 || localX >= chunkSize || localZ < 0 || localZ >= chunkSize
            || y < 1 || y >= worldHeight) {
        return;                       // ← ngoài chunk: bỏ qua
    }
    writer.set(localX, y, localZ, block);
}
```

Nhờ vậy công trình được **mô tả bằng toạ độ thế giới** (code dễ đọc: `out.place(cx + dx, ...)`) mà vẫn ghép liền qua biên chunk.

**Vì sao chỉ 3×3 ô?** Công trình luôn cách biên ô ≥ 28 khối và bán kính ≤ 17 ⇒ chỉ ô hiện tại và 8 ô kề mới có thể chạm chunk này.

### Độ cao nền cũng phải tất định

```java
ColumnSample sample = new ColumnSample();
shaper.sample(originX, originZ, sample);
int ground = (int) Math.floor(sample.surfaceHeight());
```

Gọi thẳng **công thức địa hình** thay vì đọc khối từ chunk. Lý do: chunk chứa tâm công trình có thể **chưa được sinh**. Công thức thì luôn tính được, và mọi chunk đều ra cùng một số ⇒ công trình không bị lệch tầng giữa các chunk.

### Lọc vị trí

```java
if (ground <= shaper.seaLevel() + 1) return;    // không xây dưới biển/lòng sông
String biome = shaper.biomes().pick(originX, originZ).name();
if ("desert".equals(biome)) buildPyramid(...);
else                        buildWatchtower(...);
```

---

## 3. Kim tự tháp — hình học bậc thang

```java
int half = 12 + random.nextInt(4);              // 12..15
int baseY = ground - 2;

for (int layer = 0; layer <= half; layer++) {
    int reach = half - layer;
    for (int dx = -reach; dx <= reach; dx++)
        for (int dz = -reach; dz <= reach; dz++)
            out.place(cx + dx, baseY + layer, cz + dz, sandstone);
}
```

### Công thức

```
  reach(L) = half − L,        L = 0, 1, …, half
  cạnh tầng L = 2·reach + 1 = 2(half − L) + 1
```

| Tầng `L` | `reach` | Cạnh | Số khối |
|---|---|---|---|
| 0 (đáy) | 15 | 31 | 961 |
| 5 | 10 | 21 | 441 |
| 10 | 5 | 11 | 121 |
| 15 (đỉnh) | 0 | 1 | 1 |

### Tổng số khối

```
        half
        ___
        ╲                    ²
  V  =  ╱   (2(half − L) + 1)
        ‾‾‾
        L=0

        half
        ___
     =  ╲   (2k + 1)²          (đổi biến k = half − L)
        ‾‾‾
        k=0
```

Áp dụng công thức tổng bình phương số lẻ:

```
   n
   ___
   ╲            ²      (n+1)(2n+1)(2n+3)
   ╱   (2k + 1)    =  ───────────────────
   ‾‾‾                        3
   k=0
```

Với `half = 15` (`n = 15`):

```
  V = (16)(31)(33) / 3 = 16 368 khối
```

Với `half = 12`: `V = (13)(25)(27)/3 = 2925... ` → thực tế `= 13·25·27/3 = 2 925`. Chênh lệch lớn giữa 12 và 15 vì `V ∼ (2·half)³/6` — **bậc 3**.

Kiểm tra xấp xỉ: kim tự tháp đặc có `V ≈ (1/3)·đáy·cao = (1/3)(31²)(16) ≈ 5 125`... khác `16 368`? Vì đây là **kim tự tháp bậc thang đặc** — mỗi tầng là một khối hộp đầy, không phải hình chóp nhẵn. Công thức tổng bình phương ở trên là chính xác.

### Phòng mộ & lối vào

```java
int roomY = baseY + 1;
for (dx = −3..3) for (dz = −3..3) for (dy = 0..3)
    out.place(cx+dx, roomY+dy, cz+dz, air);       // hốc 7 × 7 × 4 = 196 khối
out.place(cx, roomY, cz, lamp);                    // đèn ở tâm
// 4 khối brick ở 4 góc phòng
```

Lối vào — hầm ngang từ mặt Nam:

```java
for (int dz = 4; dz <= half + 2; dz++)
    for (int dy = 1; dy <= 2; dy++)
        out.place(cx, roomY + dy - 1, cz + dz, air);
```

Hầm cao 2 khối, chạy từ `dz = 4` (mép phòng) ra tới `dz = half + 2` (ngoài mặt dốc) ⇒ **luôn xuyên thủng** vỏ kim tự tháp bất kể `half` bằng bao nhiêu.

**Thứ tự ghi quan trọng:** thân đặc → khoét phòng → khoét hầm. Vì `place` ghi đè, các bước sau đục vào bước trước.

---

## 4. Tháp canh — hàm `ring` và vòng tròn voxel

```java
private static int ring(int dx, int dz, int radius) {
    int d2 = dx * dx + dz * dz;
    if (d2 > radius * radius + radius) return -1;                      // ngoài
    return d2 >= (radius - 1) * (radius - 1) + radius ? 1 : 0;         // 1 = tường, 0 = trong
}
```

### Công thức

Đặt `d² = dx² + dz²`, `r = radius = 5`:

```
  d² > r² + r  = 30           →  −1  (ngoài tháp)
  d² ≥ (r−1)² + r = 21        →   1  (trên vòng tường)
  ngược lại                    →   0  (lòng tháp rỗng)
```

| Vùng | Điều kiện | Khoảng cách `d` |
|---|---|---|
| Ngoài | `d² > 30` | `d > 5.48` |
| Tường | `21 ≤ d² ≤ 30` | `4.58 ≤ d ≤ 5.48` |
| Trong | `d² < 21` | `d < 4.58` |

⇒ **Bề dày tường ≈ 0.9 khối** (thực tế 1 lớp voxel).

### Vì sao `+ radius` mà không phải `+ 0.5`?

Đây là kỹ thuật voxel hoá vòng tròn kiểu Minecraft. So sánh `d² ≤ r² + r` tương đương:

```
  d ≤ √(r² + r) ≈ r + 0.5      (khai triển Taylor: √(r²+r) ≈ r√(1+1/r) ≈ r + 0.5)
```

Nghĩa là "bán kính hiệu dụng `r + 0.5`" — bao đủ các khối mà tâm nằm trong đường tròn bán kính `r`, cho vòng tròn voxel **tròn đều, không có góc nhọn**. Dùng số nguyên nên **không có phép chia hay căn**, chạy rất nhanh.

### Cấu trúc tháp

```java
int height = 20 + random.nextInt(8);            // 20..27
int radius = 5;
int baseY  = ground + 1;
```

**(a) Móng** — đổ thẳng xuống 6 khối để tháp bám vào sườn dốc:

```java
for (y = max(1, ground − 6); y <= ground; y++)
    if (ring(dx, dz, radius) >= 0) place(..., cobblestone);
```

Chú ý `>= 0` — đổ **cả tường lẫn lòng** thành khối đặc (móng đặc).

**(b) Thân + sàn gỗ**

```java
if (where > 0)       place(cobblestone);                       // tường
else if (where == 0) place(dy % 6 == 0 ? planks : air);        // sàn mỗi 6 khối
```

```
  Sàn tại dy ∈ {0, 6, 12, 18, 24}  →  ⌊height/6⌋ + 1 ≈ 4–5 tầng
```

**(c) Đuốc** — cùng chu kỳ 6:

```java
for (int dy = 1; dy < height; dy += 6) place(cx, baseY + dy, cz, torch);
```

Đặt tại `dy = 1, 7, 13, 19, 25` — tức **ngay trên mỗi sàn** (`sàn ở 0, 6, 12…`). Lệch 1 để đuốc đứng trên sàn chứ không nằm trong sàn.

**(d) Lỗ châu mai (battlements)** — xen kẽ theo bàn cờ:

```java
if (where > 0 && (dx + dz & 1) == 0)
    out.place(cx + dx, topY, cz + dz, cobblestone);
```

`(dx + dz) & 1 == 0` ⇔ `dx + dz` **chẵn** — đây là **tô màu bàn cờ** (checkerboard parity). Trên vòng tường, các ô chẵn/lẻ xen kẽ ⇒ răng cưa đều đặn như lâu đài thật.

`&` (bitwise AND) nhanh hơn `% 2`, và đúng cả với số âm — `−3 % 2 = −1 ≠ 0` nhưng `−3 & 1 = 1`, vẫn phân loại đúng chẵn/lẻ.

**(e) Cửa** — mặt Nam, cao 2 khối, có lanh tô gỗ:

```java
place(cx, baseY,     cz + radius, air);
place(cx, baseY + 1, cz + radius, air);
place(cx, baseY + 2, cz + radius, planks);
```

---

## 5. Độ phức tạp

| Công trình | Số khối ghi |
|---|---|
| Kim tự tháp `half = 15` | 16 368 (thân) + 196 (phòng) + ~34 (hầm) ≈ **16 600** |
| Tháp canh `h = 27` | móng `(11² × 7 ≈ 847`) + thân `(11² × 27 = 3 267`) + mái ≈ **4 100** |

Mỗi chunk quét 9 ô, mỗi ô có `P = 0.55` ⇒ trung bình `9 × 0.55 ≈ 5` công trình được **replay**:

```
  ≈ 5 × 16 600 ≈ 83 000 lần gọi place()
```

Nhưng ~99 % bị `StructureWriter.place()` loại ngay bằng 6 phép so sánh ⇒ chi phí thực tế nhỏ. Đây là đánh đổi **CPU lấy tính không cần đồng bộ hoá**.

---

## 6. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `CELL_CHUNKS` | 12 | Ô = 12×12 chunk = 192×192 khối |
| `SPAWN_CHANCE` | 0.55 | Xác suất một ô có công trình |
| `CELL_MARGIN` | 28 | Lề tối thiểu tới biên ô |
| `SALT` | 77 | Tách khỏi carver khác |
| Kim tự tháp `half` | 12–15 | Nửa cạnh đáy |
| Tháp `height` | 20–27 | Chiều cao |
| Tháp `radius` | 5 | Bán kính |
| Chu kỳ sàn / đuốc | 6 | khối |

---

## 7. Chủ đề DSA / Toán thể hiện

- **Stratified sampling** (phân ô) thay cho Poisson thuần.
- **`Math.floorDiv`** — chia lấy nguyên đúng với số âm.
- **Tổng bình phương số lẻ** — đếm khối kim tự tháp.
- **Voxel hoá vòng tròn** bằng số nguyên (`d² ≤ r² + r`).
- **Checkerboard parity** (`(dx+dz) & 1`).
- **Replay tất định** & ghi có lọc phạm vi (`StructureWriter`).
- **Thứ tự ghi đè** để đục lỗ (thân đặc → phòng → hầm).

---

## 8. Liên kết

- Kỹ thuật replay: [PerlinWormCarver.md](PerlinWormCarver.md), [CaveSeeds.md](CaveSeeds.md)
- Nguồn địa hình: [TerrainShaper.md](../01-noise-terrain/TerrainShaper.md)
