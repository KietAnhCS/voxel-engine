# DecorationContext — nội suy song tuyến tính & trường mật độ

**File:** `core/src/com/voxel/game/terrain/decor/DecorationContext.java`

Môi trường làm việc của mọi `Decorator`. Phần toán quan trọng nhất nằm ở hàm `patch()` — **nội suy song tuyến tính (bilinear interpolation)** trên lưới băm.

---

## 1. Ba nguồn ngẫu nhiên

| Hàm | Kiểu | Dùng cho |
|---|---|---|
| `random(salt)` | `float ∈ [0,1)` | Quyết định có đặt hay không |
| `randomInt(salt, min, max)` | `int ∈ [min, max]` | Chiều cao cây, bán kính đá |
| `patch(salt, cellSize)` | `float ∈ [0,1)` **mượt theo không gian** | Trường mật độ để cỏ mọc thành vạt |

Hai hàm đầu ủy quyền thẳng cho [Deterministic](../01-noise-terrain/Deterministic.md). Hàm thứ ba mới là phần đáng nói.

---

## 2. Vấn đề: cỏ rắc đều như muối

Nếu dùng `random(salt) < 0.62` cho cỏ, mỗi ô độc lập tung xúc xắc ⇒ cỏ phủ **đều tăm tắp 62 %** khắp bản đồ. Nhìn như ai đó rắc muối — không có vạt cỏ dày, không có khoảng đất trống.

Thiên nhiên thì **có tương quan không gian**: chỗ này dày, chỗ kia trơ.

---

## 3. Giải pháp: nội suy song tuyến tính trên lưới băm

```java
public float patch(int salt, int cellSize) {
    int cellX = Math.floorDiv(worldX, cellSize);
    int cellZ = Math.floorDiv(worldZ, cellSize);
    float fx = smooth((worldX - cellX * cellSize) / (float) cellSize);
    float fz = smooth((worldZ - cellZ * cellSize) / (float) cellSize);

    float c00 = Deterministic.unit(seed, cellX,     cellZ,     salt);
    float c10 = Deterministic.unit(seed, cellX + 1, cellZ,     salt);
    float c01 = Deterministic.unit(seed, cellX,     cellZ + 1, salt);
    float c11 = Deterministic.unit(seed, cellX + 1, cellZ + 1, salt);

    float top    = c00 + (c10 - c00) * fx;
    float bottom = c01 + (c11 - c01) * fx;
    return top + (bottom - top) * fz;
}
```

### 3.1 Chia lưới

```
  cellX = ⌊ worldX / cellSize ⌋          (floorDiv — đúng cả với toạ độ âm)
  fx    = (worldX − cellX · cellSize) / cellSize  ∈ [0, 1)
```

`fx, fz` là **toạ độ tương đối trong ô** (0 = mép trái, 1 = mép phải).

### 3.2 Băm 4 góc ô

```
       c01 ──────────── c11
        │                │
        │      • (fx,fz) │
        │                │
       c00 ──────────── c10
```

Mỗi góc lấy một giá trị ngẫu nhiên `∈ [0,1)` từ hàm băm. **Hai ô kề nhau dùng chung 2 góc** ⇒ trường kết quả **liên tục qua biên ô**.

### 3.3 Nội suy song tuyến tính

Hai lần lerp theo X, rồi một lần theo Z:

```
  top    = lerp(c00, c10, fx) = c00 + (c10 − c00)·fx
  bottom = lerp(c01, c11, fx) = c01 + (c11 − c01)·fx
  kết quả = lerp(top, bottom, fz)
```

Khai triển đầy đủ:

```
  P(fx, fz) = c00(1−fx)(1−fz) + c10·fx(1−fz) + c01(1−fx)fz + c11·fx·fz
```

Đây là dạng chuẩn của **bilinear interpolation** — tổng có trọng số 4 góc, trọng số bằng diện tích hình chữ nhật đối diện. Tổng trọng số luôn bằng 1:

```
  (1−fx)(1−fz) + fx(1−fz) + (1−fx)fz + fx·fz = 1   ✔
```

⇒ kết quả luôn nằm trong `[min(c), max(c)] ⊂ [0, 1)`.

### 3.4 `smooth()` — làm tròn góc

```java
private static float smooth(float t) {
    return t * t * (3f - 2f * t);
}
```

```
  S(t) = 3t² − 2t³
  S(0) = 0,  S(1) = 1
  S′(t) = 6t(1 − t)  ⇒  S′(0) = S′(1) = 0
```

**Vì sao cần?** Nội suy tuyến tính thuần cho trường liên tục **C⁰** nhưng đạo hàm **gián đoạn** tại biên ô ⇒ nhìn thấy rõ các đường kẻ ô vuông trong thảm cỏ.

Áp `smooth` lên `fx, fz` trước khi nội suy khiến đạo hàm triệt tiêu ở biên ⇒ trường trở thành **C¹**, biên ô biến mất hoàn toàn.

Đây chính là hàm ease của **Perlin noise cổ điển** (1985). (Perlin sau này đổi sang `6t⁵ − 15t⁴ + 10t³` để có C², nhưng ở đây C¹ là quá đủ.)

---

## 4. Vì sao không dùng luôn SimplexNoise?

| | `patch()` | `SimplexNoise` |
|---|---|---|
| Chi phí | 4 lần băm + 6 phép nhân | ~30 phép toán |
| Chất lượng | Có nhẹ hướng ưu tiên theo lưới | Không hướng ưu tiên |
| Bộ nhớ | 0 | 2 KB / thể hiện |
| Tuỳ chỉnh cỡ vạt | tham số `cellSize` trực tiếp | phải đổi `frequency` |
| Cần seed riêng | không — dùng `salt` | phải tạo object mới |

Với mục đích "cỏ mọc thành vạt", chất lượng của bilinear là **thừa đủ** và rẻ hơn nhiều. Mỗi decorator chỉ cần đổi `salt` thay vì cấp phát một `SimplexNoise` mới.

---

## 5. Bảng cỡ vạt thực tế

| Biome | `inPatches(n)` | Cỡ vạt |
|---|---|---|
| Mountains | 18 | 18 khối |
| Swamp | 20 | 20 khối |
| Forest | 22 | 22 khối |
| Valley | 24 | 24 khối |
| Hills | 26 | 26 khối |
| Plains | 30 | 30 khối |
| Savanna | 34 | 34 khối |

Savanna có vạt lớn nhất (34) — thảo nguyên là những mảng cỏ rộng; núi nhỏ nhất (18) — cỏ mọc lốm đốm giữa đá.

---

## 6. Các hàm ghi khối

```java
public void place(int dx, int y, int dz, Block block)          // ghi đè
public void placeIfEmpty(int dx, int y, int dz, Block block)   // chỉ ghi nếu trống
public Block blockAt(int dx, int y, int dz)                    // đọc, ngoài biên → air
```

Toạ độ **tương đối** với cột hiện tại (`localX + dx`). Tất cả đều kẹp `1 ≤ y < worldHeight`.

`placeIfEmpty` quan trọng cho lá cây: hai cây mọc gần nhau, tán lá của cây sau **không được xoá thân cây trước**.

---

## 7. Tái sử dụng object

```java
public void moveTo(int localX, int localZ, int worldX, int worldZ, int surfaceY) { ... }
```

Một `DecorationContext` duy nhất cho cả chunk, `moveTo` chỉ ghi đè 5 trường `int`. Thay vì 256 object ⇒ **không sinh rác GC** trong vòng lặp nóng. Cùng kỹ thuật với `ColumnSample`.

---

## 8. Độ phức tạp

| Hàm | Chi phí |
|---|---|
| `random`, `randomInt` | `O(1)` — ~12 lệnh |
| `patch` | `O(1)` — 4 lần băm (~48 lệnh) + ~10 phép nhân |
| `place`, `blockAt` | `O(1)` |
| Bộ nhớ | `O(1)` — một object cho cả chunk |

---

## 9. Chủ đề DSA / Toán thể hiện

- **Nội suy song tuyến tính** (bilinear interpolation) — tổng có trọng số 4 góc.
- **Hàm ease Hermite** `3t² − 2t³` và tính liên tục C¹.
- **Value noise trên lưới băm** — thay thế rẻ cho gradient noise.
- **`Math.floorDiv`** cho toạ độ âm.
- **Object pooling** (`moveTo`).

---

## 10. Liên kết

- Hàm băm nền: [Deterministic.md](../01-noise-terrain/Deterministic.md)
- Người dùng: [ScatterDecorator.md](ScatterDecorator.md), [TreeShapes.md](TreeShapes.md)
