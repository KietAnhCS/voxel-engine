# TreeShapes — hình học cây cối & đá tảng

**Files:**
- `core/src/com/voxel/game/terrain/decor/TreeDecorator.java`
- `core/src/com/voxel/game/terrain/decor/BoulderDecorator.java`
- `core/src/com/voxel/game/terrain/decor/{OakShape, BirchShape, PineShape, CactusShape}.java`

---

## 1. TreeDecorator — cổng vào

```java
public boolean decorate(DecorationContext context) {
    if (context.random(salt) >= chance) return false;              // xác suất
    int groundY = context.surfaceY();
    if (groundY + 12 >= context.worldHeight()) return false;       // đủ chỗ cao
    shape.build(context, groundY);
    return true;
}
```

Kiểm tra `groundY + 12 < worldHeight` — **12 là chiều cao tối đa** của cây cao nhất (thông: thân 11 + tán). Chặn trước để `build()` không phải lo ghi tràn trần thế giới.

Khác `ScatterDecorator`, ở đây **không có** điều biến `density` — cây rải đều theo xác suất thuần. Cây quá thưa (`p ≤ 0.045`) nên gom vạt sẽ tạo ra rừng dày đặc xen sa mạc trống, không tự nhiên.

**Strategy:** `TreeShape` là interface, `shape.build(...)` gọi đa hình ⇒ thêm loài cây mới không phải sửa `TreeDecorator`.

---

## 2. OakShape — tán "kẹo mút" 4 tầng

```java
int trunkHeight = context.randomInt(11, 4, 6);        // thân 4..6
context.place(0, groundY, 0, dirt);                    // gốc luôn là đất
for (int step = 1; step <= trunkHeight; step++)
    context.place(0, groundY + step, 0, wood);

int crown = groundY + trunkHeight;
for (int dy = -2; dy <= 1; dy++) {
    int radius = dy <= -1 ? 2 : 1;
    ...
}
```

### Cấu trúc tán

```
  dy = +1 :  radius 1  →  3×3   (góc LUÔN cắt)   ⇒ 5 khối
  dy =  0 :  radius 1  →  3×3   (góc cắt 50 %)   ⇒ 5–9 khối
  dy = −1 :  radius 2  →  5×5   (góc cắt 50 %)   ⇒ 21–25 khối
  dy = −2 :  radius 2  →  5×5   (góc cắt 50 %)   ⇒ 21–25 khối
```

Hai tầng dưới rộng 5×5 **ôm lấy đỉnh thân**, hai tầng trên 3×3 ⇒ dáng "kẹo mút" kinh điển của Minecraft.

Tổng lá: **52–64 khối**.

### Cắt góc ngẫu nhiên — `isTrimmedCorner`

```java
private boolean isTrimmedCorner(DecorationContext context, int dx, int dz, int radius, int dy) {
    if (Math.abs(dx) != radius || Math.abs(dz) != radius) return false;   // không phải góc
    return dy == 1 || context.random(20 + dy * 7 + dx * 3 + dz) < 0.5f;
}
```

**Điều kiện là góc:** `|dx| == radius ∧ |dz| == radius` — đúng 4 ô góc của hình vuông.

**Luật cắt:**
- Tầng trên cùng (`dy == 1`): **luôn** cắt ⇒ đỉnh tán bo tròn.
- Các tầng khác: cắt với xác suất **50 %**.

### Salt động — mẹo hay

```java
salt = 20 + dy*7 + dx*3 + dz
```

Mỗi ô góc của mỗi tầng có một `salt` **khác nhau** ⇒ 4 góc × 3 tầng = 12 quyết định độc lập. Kết quả: mỗi cây có `2¹² = 4096` biến thể tán ⇒ **không cây nào giống cây nào**, thay vì "bốn cái hộp giống hệt".

Các hệ số `7, 3, 1` là số nguyên tố nhỏ khác nhau, đảm bảo `(dy, dx, dz)` khác nhau cho `salt` khác nhau trong phạm vi `dy ∈ [−2,1], dx, dz ∈ [−2,2]`.

> Comment trong code ghi rõ phiên bản trước dùng **đệ quy** để mọc cành như sồi khổng lồ — nhưng sồi Minecraft thường **không có cành**, và tán mỏng khiến nhìn xuyên qua giữa cây. Phiên bản này đơn giản hơn mà đúng hơn.

---

## 3. BirchShape — bạch dương cao gầy

```java
int trunkHeight = context.randomInt(31, 6, 9);        // thân 6..9 (cao hơn sồi)
```

Cấu trúc tán **giống hệt sồi** (4 tầng, radius `2,2,1,1`) nhưng:

```java
if (Math.abs(dx) == radius && Math.abs(dz) == radius) continue;   // LUÔN cắt góc
```

Cắt **tất cả** góc ở **mọi** tầng, không ngẫu nhiên ⇒ tán gọn gàng, đối xứng hoàn hảo.

### So sánh sồi vs bạch dương

| | Oak | Birch |
|---|---|---|
| Thân | 4–6 | **6–9** (cao hơn) |
| Gỗ | `wood` | `birchWood` |
| Cắt góc | ngẫu nhiên 50 % | **luôn cắt** |
| Số khối lá | 52–64 | **52** (cố định) |
| Dáng | xù xì, mỗi cây một khác | thanh mảnh, đều đặn |

Đúng đặc trưng thực vật: bạch dương thân trắng cao thẳng, tán nhỏ gọn.

---

## 4. PineShape — thuật toán "váy tầng" (thú vị nhất)

```java
int trunkHeight = context.randomInt(41, 7, 11);
int maxRadius   = context.randomInt(42, 2, 3);
int skirtDepth  = trunkHeight - context.randomInt(43, 2, 3);

int radius = 0;
int layerLimit = 1;
int radiusAfterReset = 0;

for (int step = 0; step <= skirtDepth; step++) {
    ring(context, crown - step, radius, needles);

    if (radius < layerLimit) { radius++; continue; }
    radius = radiusAfterReset;
    radiusAfterReset = 1;
    if (++layerLimit > maxRadius) layerLimit = maxRadius;
}
```

### Máy trạng thái

Đi **từ đỉnh xuống**, bán kính tăng dần; nhưng mỗi lần chạm `layerLimit` thì **tụt về `radiusAfterReset`** và nâng `layerLimit` lên 1. Chỗ tụt chính là **ranh giới giữa hai tầng váy** — cây thắt lại ở đó.

### Bảng mô phỏng (`maxRadius = 3`)

| `step` | `radius` vẽ | `layerLimit` | Hành động sau khi vẽ |
|---|---|---|---|
| 0 | **0** | 1 | `0 < 1` → `radius = 1` |
| 1 | **1** | 1 | `1 ≥ 1` → reset `radius = 0`, `after = 1`, `limit = 2` |
| 2 | **0** | 2 | `0 < 2` → `radius = 1` |
| 3 | **1** | 2 | `1 < 2` → `radius = 2` |
| 4 | **2** | 2 | `2 ≥ 2` → reset `radius = 1`, `limit = 3` |
| 5 | **1** | 3 | `1 < 3` → `radius = 2` |
| 6 | **2** | 3 | `2 < 3` → `radius = 3` |
| 7 | **3** | 3 | `3 ≥ 3` → reset `radius = 1`, `limit = 3` (kẹp) |
| 8 | **1** | 3 | → 2 |
| 9 | **2** | 3 | → 3 |
| 10 | **3** | 3 | → reset 1 |

### Hình dáng thu được

```
  step 0    ·           r=0
  step 1   ···          r=1
  step 2    ·           r=0   ← THẮT (ranh giới tầng)
  step 3   ···          r=1
  step 4  ·····         r=2
  step 5   ···          r=1   ← THẮT
  step 6  ·····         r=2
  step 7 ·······        r=3
  step 8   ···          r=1   ← THẮT
  step 9  ·····         r=2
  step 10·······        r=3
```

⇒ Cây thông có **3–4 tầng váy xoè**, mỗi tầng rộng dần xuống dưới, giữa các tầng thắt lại.

> Comment: phiên bản trước dùng công thức bán kính **giảm đều** nên ra hình nón nhẵn — không giống thông.

### `radiusAfterReset` — vì sao đổi từ 0 sang 1?

Lần reset **đầu tiên** tụt về `0` (đỉnh nhọn). Từ lần thứ hai trở đi tụt về `1` — nếu vẫn tụt về 0, thân cây sẽ lộ ra giữa hai tầng váy.

### Thân dừng sớm 3 khối

```java
for (int step = 1; step <= trunkHeight - 3; step++)
    context.place(0, groundY + step, 0, wood);
```

> Comment: nếu thân mọc lên tận đỉnh, các vòng `radius = 0` gần chóp sẽ đáp xuống đúng thân, để lại một **khúc gỗ trơ ngay dưới ngọn**. Dừng sớm 3 khối làm 3 hàng trên cùng toàn lá.

### `ring` — vẽ một tầng lá

```java
private void ring(DecorationContext context, int y, int radius, Block needles) {
    for (int dx = -radius; dx <= radius; dx++)
        for (int dz = -radius; dz <= radius; dz++) {
            if (radius > 0 && Math.abs(dx) == radius && Math.abs(dz) == radius) continue;
            context.placeIfEmpty(dx, y, dz, needles);
        }
}
```

Vẽ **hình vuông đặc** `(2r+1)²` đã cắt 4 góc ⇒ hình **thoi vuông vắt góc**, số khối:

```
  N(r) = (2r + 1)² − 4    (với r > 0)
  N(0) = 1,  N(1) = 5,  N(2) = 21,  N(3) = 45
```

`placeIfEmpty` để tầng dưới không xoá lá tầng trên đã đặt.

---

## 5. CactusShape — đơn giản nhất

```java
int height = context.randomInt(51, 2, 4);
for (int step = 1; step <= height; step++)
    context.placeIfEmpty(0, groundY + step, 0, cactus);
```

Một cột 2–4 khối. `placeIfEmpty` tránh đè lên vật thể khác.

---

## 6. BoulderDecorator — nửa quả cầu voxel

```java
int radius = context.randomInt(salt + 1, 1, 2);
int baseY  = context.surfaceY() + 1;

for (int dx = -radius; dx <= radius; dx++)
    for (int dy = 0; dy <= radius; dy++)              // ← chỉ NỬA TRÊN
        for (int dz = -radius; dz <= radius; dz++) {
            if (dx*dx + dy*dy + dz*dz > radius*radius + 1) continue;
            context.placeIfEmpty(dx, baseY + dy, dz, stone);
        }
```

### Phương trình

```
  dx² + dy² + dz² ≤ r² + 1,     dy ≥ 0
```

**`+1` làm gì?** Cùng ý tưởng với `ring()` của [StructureCarver](../03-caves/StructureCarver.md): nới bán kính hiệu dụng lên `√(r²+1)` để hình cầu voxel **tròn đầy đặn** thay vì bị khuyết ở các trục chéo.

| `r` | `r² + 1` | Số khối (nửa trên) |
|---|---|---|
| 1 | 2 | 6 |
| 2 | 5 | 23 |

**`dy` bắt đầu từ 0** (không phải `−r`) ⇒ chỉ nửa trên quả cầu — tảng đá **nằm trên** mặt đất, không chôn nửa dưới xuống đất.

### `salt + 1` — mẹo tách nguồn ngẫu nhiên

```java
if (context.random(salt) >= chance) return false;        // salt   : có đá không
int radius = context.randomInt(salt + 1, 1, 2);          // salt+1 : đá to hay nhỏ
```

Nếu dùng chung `salt`, "có đá" và "kích thước" sẽ **tương quan hoàn toàn**: mọi tảng đá được sinh ra đều có cùng kích thước (vì đều thoả `u < chance`, tức `u` rất nhỏ, kéo theo `radius` luôn = 1). Đổi salt phá bỏ tương quan đó.

---

## 7. Bảng tổng hợp

| Hình dạng | Salt | Thân | Tán | Tổng khối |
|---|---|---|---|---|
| **Oak** | 11 | 4–6 | 4 tầng `2,2,1,1`, góc cắt ngẫu nhiên | 57–71 |
| **Birch** | 31 | 6–9 | 4 tầng `2,2,1,1`, luôn cắt góc | 59–62 |
| **Pine** | 41,42,43 | 7–11 (dừng sớm 3) | váy tầng, `maxRadius` 2–3 | ~100–180 |
| **Cactus** | 51 | 2–4 | không | 2–4 |
| **Boulder** | salt, salt+1 | — | nửa cầu `r` 1–2 | 6–23 |

---

## 8. Chủ đề DSA / Toán thể hiện

- **Strategy pattern** — `TreeShape` interface, 4 cài đặt.
- **Máy trạng thái hữu hạn** — vòng lặp váy tầng của thông.
- **Voxel hoá hình cầu** với nới bán kính (`≤ r² + 1`).
- **Salt động** (`20 + dy·7 + dx·3 + dz`) để sinh biến thể.
- **Tách nguồn ngẫu nhiên** bằng `salt + 1` chống tương quan.
- **Thứ tự ghi & `placeIfEmpty`** để các vật thể không xoá nhau.

---

## 9. Liên kết

- Nguồn ngẫu nhiên: [DecorationContext.md](DecorationContext.md), [Deterministic.md](../01-noise-terrain/Deterministic.md)
- Mật độ theo biome: [Biome-heights.md](../01-noise-terrain/Biome-heights.md) §4
- Anh em: [ScatterDecorator.md](ScatterDecorator.md)
