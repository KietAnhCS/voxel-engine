# Deterministic — hàm băm không gian

**File:** `core/src/com/voxel/game/terrain/Deterministic.java`
**Vai trò:** Sinh số "ngẫu nhiên" từ `(seed, x, z, salt)` mà **không cần đối tượng `Random`**, không cần trạng thái, không cần khoá đa luồng.

---

## 1. Vấn đề

Trang trí thế giới cần ngẫu nhiên: "ô này có mọc cây không?". Nhưng:

- Dùng `new Random()` → mỗi lần vào game cây mọc chỗ khác.
- Dùng `Random(seed)` chung → thứ tự gọi quyết định kết quả, mà chunk sinh **song song trên nhiều luồng** ⇒ thứ tự không xác định.
- Dùng `new Random(hash)` mỗi ô → cấp phát 256 object/chunk, rác GC.

**Giải pháp:** hàm băm thuần tuý `f(seed, x, z, salt) → [0, 1)`, gọi bao nhiêu lần, ở luồng nào, thứ tự nào cũng cho cùng kết quả.

---

## 2. Mã nguồn

```java
public static float unit(long seed, int x, int z, int salt) {
    long hash = seed;
    hash ^= x    * 0x9E3779B97F4A7C15L;
    hash ^= z    * 0xC2B2AE3D27D4EB4FL;
    hash ^= salt * 0x165667B19E3779F9L;
    hash ^= hash >>> 33;
    hash *= 0xFF51AFD7ED558CCDL;
    hash ^= hash >>> 33;
    hash *= 0xC4CEB9FE1A85EC53L;
    hash ^= hash >>> 33;
    return (hash >>> 40) / (float) (1 << 24);
}
```

---

## 3. Phân tích từng bước

### 3.1 Trộn đầu vào bằng hằng số vàng

```
hash = seed ⊕ (x · K₁) ⊕ (z · K₂) ⊕ (salt · K₃)
```

| Hằng | Giá trị | Nguồn gốc |
|---|---|---|
| `K₁` | `0x9E3779B97F4A7C15` | `⌊2⁶⁴ / φ⌋` với `φ = (1+√5)/2` — **tỉ lệ vàng 64-bit** |
| `K₂` | `0xC2B2AE3D27D4EB4F` | hằng trộn của xxHash64 |
| `K₃` | `0x165667B19E3779F9` | hằng trộn của xxHash64 (biến thể) |

**Vì sao là tỉ lệ vàng?**
`φ` là số vô tỉ "khó xấp xỉ bằng phân số nhất" (định lý Hurwitz). Nhân với `⌊2⁶⁴/φ⌋` trong số học modulo `2⁶⁴` khiến các bội số `x·K₁` phân bố **đều nhất có thể** trên toàn dải 64 bit — không có chu kỳ ngắn nào lộ ra. Đây là kỹ thuật *Fibonacci hashing* (Knuth, TAOCP vol. 3).

Ba hằng khác nhau cho `x`, `z`, `salt` đảm bảo hoán vị đầu vào cho kết quả khác nhau: `f(1, 2, 3) ≠ f(3, 2, 1)`.

### 3.2 Bộ trộn cuối — `fmix64` của MurmurHash3

```
h ⊕= h >>> 33
h ×= 0xFF51AFD7ED558CCD
h ⊕= h >>> 33
h ×= 0xC4CEB9FE1A85EC53
h ⊕= h >>> 33
```

Đây là **finalizer chuẩn của MurmurHash3** (Austin Appleby), được tìm ra bằng tìm kiếm tự động để tối ưu **hiệu ứng tuyết lở** (avalanche effect):

> Đổi **1 bit** ở đầu vào ⇒ mỗi bit đầu ra đổi với xác suất ≈ **0.5**.

Cơ chế:
- `h ⊕= h >>> 33` — **khuếch tán**: đẩy thông tin từ bit cao xuống bit thấp.
- `h ×= K` — **trộn**: phép nhân lan toả bit thấp lên bit cao (carry propagation).
- Xen kẽ hai phép này 2–3 lần thì mọi bit ảnh hưởng lên mọi bit.

Nếu bỏ bước này, `x` và `x+1` sẽ cho hai giá trị gần nhau → cây mọc thành hàng thẳng lối.

### 3.3 Đưa về `[0, 1)`

```java
return (hash >>> 40) / (float) (1 << 24);
```

```
       h >>> 40        h_top24
  u = ─────────  =  ───────────  ∈ [0, 1)
        2²⁴           16 777 216
```

- `>>> 40` (dịch **không dấu**) lấy **24 bit cao nhất** — bit cao có chất lượng ngẫu nhiên tốt nhất sau phép nhân.
- Chia `2²⁴ = 16 777 216` đúng bằng số giá trị phân biệt.
- **24 bit** là chính xác số bit định trị (mantissa) của `float` IEEE-754 ⇒ mọi giá trị biểu diễn được chính xác, không mất mát làm tròn.
- Dịch không dấu `>>>` (không phải `>>`) đảm bảo kết quả không âm.

---

## 4. `range` — số nguyên trong khoảng

```java
public static int range(long seed, int x, int z, int salt, int min, int max) {
    float value = unit(seed, x, z, salt);
    return min + (int) (value * (max - min + 1));
}
```

```
  r = min + ⌊ u · (max − min + 1) ⌋ ,   u ∈ [0, 1)
```

Vì `u < 1` nghiêm ngặt, `⌊u·n⌋ ≤ n−1` ⇒ `r ≤ max`. Bao gồm cả hai đầu mút (`[min, max]`).

**Sai lệch (bias):** phép chia modulo kiểu này về lý thuyết lệch cỡ `n / 2²⁴`. Với `n ≤ 10` (chiều cao cây), sai lệch `< 10⁻⁶` — hoàn toàn bỏ qua được.

---

## 5. Tính chất & độ phức tạp

| Tính chất | Đánh giá |
|---|---|
| Thời gian | `O(1)` — 3 nhân + 3 xor + 2 nhân + 3 shift ≈ **12 lệnh CPU** |
| Bộ nhớ | `O(1)` — không cấp phát, không có trường tĩnh |
| Thread-safe | ✔ Hàm thuần tuý (pure function), không trạng thái |
| Tất định | ✔ Cùng đầu vào ⇒ cùng đầu ra, mọi lúc mọi máy |
| Chu kỳ | Không có khái niệm chu kỳ (không phải PRNG tuần tự) |

So với `new Random(seed).nextFloat()`: nhanh hơn ~5 lần và không cấp phát object.

---

## 6. Vai trò của `salt`

Mỗi decorator giữ một `salt` riêng (2, 3, 4, …, 27 — xem [Biome-heights.md](Biome-heights.md)):

```java
new TreeDecorator(0.045f, 4, new OakShape())      // salt = 4
new ScatterDecorator(0.02f, 6, s -> s.flower)     // salt = 6
```

Nhờ vậy tại cùng ô `(x, z)`, hàm băm cho hai giá trị **độc lập thống kê** — cây và hoa không bao giờ "đồng bộ" mọc chung một chỗ hay né nhau một cách máy móc.

---

## 7. Chủ đề DSA thể hiện

- **Hàm băm** — thiết kế, hiệu ứng tuyết lở, chất lượng bit.
- **Fibonacci hashing** & tỉ lệ vàng.
- **Số học modulo `2⁶⁴`** và lan truyền nhớ.
- **Pure function** & bất biến ⇒ an toàn đa luồng không cần khoá.
- **Biểu diễn dấu phẩy động IEEE-754** (24-bit mantissa).

---

## 8. Liên kết

- Người dùng: [ScatterDecorator.md](../04-decor/ScatterDecorator.md), [TreeDecorator.md](../04-decor/TreeDecorator.md), [StructureCarver.md](../03-caves/StructureCarver.md)
- Nguồn ngẫu nhiên "có cấu trúc" bổ trợ: [SimplexNoise.md](SimplexNoise.md)
