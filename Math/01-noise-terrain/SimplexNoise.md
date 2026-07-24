# SimplexNoise

**File:** `core/src/com/voxel/engine/generation/SimplexNoise.java`
**Vai trò:** Nguồn ngẫu nhiên *có cấu trúc* duy nhất của cả thế giới. Mọi ngọn núi, con sông, hang động, quặng đá đều bắt nguồn từ lớp này.

---

## 1. Bài toán

Cần một hàm `noise(x, y)` sao cho:

1. **Tất định** — cùng seed + cùng toạ độ ⇒ luôn cùng giá trị (chunk sinh lại vẫn khớp).
2. **Liên tục & mượt** — hai điểm gần nhau cho giá trị gần nhau (địa hình không bị răng cưa).
3. **Vô hạn** — không cần lưu trữ, tính tại chỗ với `O(1)` bộ nhớ.
4. **Không có hướng ưu tiên** — không lộ lưới vuông như Perlin cổ điển.

Simplex noise (Ken Perlin, 2001) giải quyết cả bốn.

---

## 2. Ý tưởng cốt lõi: lưới tam giác thay vì lưới vuông

| | Perlin cổ điển | Simplex |
|---|---|---|
| Ô cơ sở trong 2D | hình vuông, **4** đỉnh | tam giác đều, **3** đỉnh |
| Ô cơ sở trong 3D | hộp, **8** đỉnh | tứ diện, **4** đỉnh |
| Số phép nội suy | `2^n` | `n+1` |
| Độ phức tạp | `O(2^n)` | `O(n²)` |

Trong không gian `n` chiều, **simplex** là đa diện đơn giản nhất có thể lấp đầy không gian:
`n = 2` → tam giác, `n = 3` → tứ diện. Ít đỉnh hơn ⇒ ít phép tính hơn và không bị "vệt lưới".

---

## 3. Hằng số biến đổi toạ độ

```java
F2 = 0.5 * (√3 − 1)        ≈ 0.3660254
G2 = (3 − √3) / 6          ≈ 0.2113249
F3 = 1/3
G3 = 1/6
```

### Công thức tổng quát

Với không gian `n` chiều:

```
F_n = (√(n+1) − 1) / n
G_n = (1 − 1/√(n+1)) / n     (thoả G_n = F_n / (1 + n·F_n))
```

Kiểm chứng `n = 2`:
`F₂ = (√3 − 1)/2 = 0.5(√3 − 1)` ✔
`G₂ = (1 − 1/√3)/2 = (3 − √3)/6` ✔

Kiểm chứng `n = 3`:
`F₃ = (√4 − 1)/3 = 1/3` ✔  `G₃ = (1 − 1/2)/3 = 1/6` ✔

**Ý nghĩa hình học:** `F` là ma trận *skew* (nghiêng) biến lưới tam giác đều thành lưới vuông đơn vị để dễ tìm ô chứa điểm; `G` là *unskew*, phép biến đổi ngược.

---

## 4. Thuật toán `noise(x, y)` — 2D

### Bước 1 — Skew về lưới vuông

```java
s  = (x + y) * F2
i  = floor(x + s)
j  = floor(y + s)
```

`(i, j)` là **góc thứ nhất** của tam giác chứa điểm.

### Bước 2 — Unskew ngược về không gian thật

```java
t  = (i + j) * G2
x0 = x − (i − t)
y0 = y − (j − t)
```

`(x0, y0)` = vector từ góc 0 đến điểm đang xét.

### Bước 3 — Xác định tam giác trên hay dưới

Ô vuông bị đường chéo cắt thành 2 tam giác:

```java
if (x0 > y0) { i1 = 1; j1 = 0; }   // tam giác dưới, đi phải rồi lên
else         { i1 = 0; j1 = 1; }   // tam giác trên, đi lên rồi phải
```

Vector tới 2 góc còn lại:

```
(x1, y1) = (x0 − i1 + G2,   y0 − j1 + G2)
(x2, y2) = (x0 − 1 + 2·G2,  y0 − 1 + 2·G2)
```

### Bước 4 — Gradient ngẫu nhiên tại mỗi góc

```java
gi = permMod12[ii + perm[jj]]      // ii = i & 255, jj = j & 255
```

`perm[]` là bảng hoán vị 0..255 xáo bằng **Fisher–Yates** với `Random(seed)`, nhân đôi lên 512 phần tử để `ii + perm[jj]` (tối đa 255 + 255 = 510) không tràn — mẹo bỏ phép `%` trong vòng lặp nóng.

`GRAD3` gồm 12 vector là **trung điểm 12 cạnh của khối lập phương**:

```
(±1,±1, 0)   (±1, 0,±1)   ( 0,±1,±1)
```

Chọn 12 hướng phân bố đều để nhiễu không lệch về trục nào. Ở 2D chỉ dùng 2 thành phần đầu.

### Bước 5 — Hàm suy giảm (radial attenuation)

Với mỗi góc `k`:

```
t_k = 0.5 − (x_k² + y_k²)
n_k = 0   nếu t_k < 0
n_k = t_k⁴ · (g_k · v_k)   ngược lại
```

Trong đó `g_k · v_k` là **tích vô hướng** giữa gradient và vector khoảng cách.

Bán kính ảnh hưởng `r² = 0.5` — vừa đủ để mỗi điểm chỉ chịu ảnh hưởng của 3 góc tam giác chứa nó, ngoài ra tắt hẳn ⇒ nhiễu **liên tục C²** (đạo hàm bậc 2 liên tục), không giật.

Luỹ thừa 4 (`t*=t` rồi `t0*t0`) chính là `(0.5 − d²)⁴`: một đường cong hình chuông tắt mượt về 0.

### Bước 6 — Chuẩn hoá

```java
return 70.0 * (n0 + n1 + n2);      // 2D → khoảng [−1, 1]
return 32.0 * (n0 + n1 + n2 + n3); // 3D, bán kính 0.6 → khoảng [−1, 1]
```

Hằng 70 và 32 là hệ số tỉ lệ thực nghiệm để biên độ đầu ra lấp đầy `[−1, 1]`.

---

## 5. Thuật toán 3D — khác gì?

- Ô cơ sở là **tứ diện**, 4 góc thay vì 3.
- Bán kính suy giảm đổi `0.5 → 0.6`.
- Bước "chọn tam giác" trở thành **sắp xếp thứ tự 3 thành phần** `x0, y0, z0`:

```java
if (x0 >= y0) {
    if (y0 >= z0)      { i1,j1,k1 = 1,0,0 ;  i2,j2,k2 = 1,1,0 }  // x ≥ y ≥ z
    else if (x0 >= z0) { i1,j1,k1 = 1,0,0 ;  i2,j2,k2 = 1,0,1 }  // x ≥ z > y
    else               { i1,j1,k1 = 0,0,1 ;  i2,j2,k2 = 1,0,1 }  // z > x ≥ y
} else { ... }  // 3 nhánh đối xứng
```

Đây là **sắp xếp giảm dần thủ công 3 phần tử** (unrolled sort): đi theo trục lớn nhất trước, rồi trục lớn nhì. Có `3! = 6` thứ tự ⇒ 6 nhánh, đúng bằng 6 tứ diện chia một khối lập phương.

Chi phí: 2–3 phép so sánh, không nhánh lặp — nhanh hơn gọi hàm sort.

---

## 6. Fractal Brownian Motion (fBm)

```java
public double fractal2d(x, z, octaves, frequency, lacunarity, gain)
```

Cộng nhiều lớp nhiễu chồng lên nhau:

```
                 O−1
                 ___
                 ╲     k              k
       Σ  =      ╱   g  · noise(f · l  · p)
                 ‾‾‾
                 k=0

                 O−1
                 ___
                 ╲     k
       N  =      ╱   g            (chuẩn hoá)
                 ‾‾‾
                 k=0

       fBm(p) = Σ / N
```

| Tham số | Ý nghĩa | Giá trị dùng trong project |
|---|---|---|
| `octaves` (O) | số lớp | 2 – 4 |
| `frequency` (f) | tần số lớp đầu — nghịch đảo là "kích thước đặc trưng" | 0.0008 – 0.012 |
| `lacunarity` (l) | hệ số nhân tần số mỗi lớp | luôn `2.0` |
| `gain` (g) | hệ số nhân biên độ mỗi lớp | luôn `0.5` |

Với `l = 2, g = 0.5` (chuẩn "pink noise"): mỗi lớp chi tiết gấp đôi nhưng chỉ đóng góp nửa biên độ — đúng quy luật tự nhiên của núi non, mây, bờ biển.

**Chuẩn hoá** chia cho `N = 1 + 0.5 + 0.25 + … ` (tổng cấp số nhân) giữ kết quả trong `[−1, 1]` bất kể số octave.

Tổng chuỗi vô hạn: `N∞ = 1/(1 − g) = 2`. Với 4 octave: `N = 1.875`.

---

## 7. Hàm phụ

### `floor(double)` tự viết

```java
int truncated = (int) value;
return value < truncated ? truncated - 1 : truncated;
```

`(int)` trong Java làm tròn **về 0**, nên `−0.3 → 0` (sai, cần `−1`). Đoạn này sửa lại cho số âm. Nhanh hơn `Math.floor()` vì tránh xử lý double đầy đủ.

### Fisher–Yates shuffle

```java
for (int i = 255; i > 0; i--) {
    int j = random.nextInt(i + 1);
    swap(source[i], source[j]);
}
```

Xáo mảng 256 phần tử **đều tuyệt đối** (mỗi hoán vị trong `256!` có xác suất bằng nhau), `O(n)`.

---

## 8. Độ phức tạp

| Hàm | Thời gian | Bộ nhớ |
|---|---|---|
| Khởi tạo | `O(512)` | `O(512)` = 2 KB |
| `noise2D` | `O(1)` — ~3 gradient, ~30 phép toán | `O(1)` |
| `noise3D` | `O(1)` — ~4 gradient, ~50 phép toán | `O(1)` |
| `fractal2d` | `O(octaves)` | `O(1)` |

---

## 9. Ai gọi lớp này

`TerrainNoise` tạo **14 thể hiện** `SimplexNoise` với 14 seed dẫn xuất khác nhau → 14 trường nhiễu độc lập. Xem [TerrainNoise.md](TerrainNoise.md).

---

## 10. Chủ đề DSA thể hiện

- **Hàm băm / bảng hoán vị** (`perm`, `permMod12`) — tra cứu `O(1)`.
- **Fisher–Yates shuffle** — thuật toán xáo trộn tối ưu.
- **Nội suy & hàm suy giảm đa thức** — `(r² − d²)⁴`.
- **Chuỗi cấp số nhân** — chuẩn hoá fBm.
- **Bit masking** (`& 255`) thay `% 256` — tối ưu số học.
