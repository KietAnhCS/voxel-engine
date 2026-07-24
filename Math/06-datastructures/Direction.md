# Direction — hệ trục 6 mặt & bảng tra hình học

**File:** `core/src/com/voxel/engine/util/Direction.java`

Một `enum` mang **13 con số** cho mỗi trong 6 mặt của khối lập phương. Đây là **bảng tra cứu (lookup table)** — thay toàn bộ logic điều kiện của bước dựng mesh bằng truy cập mảng `O(1)`.

---

## 1. Bảng đầy đủ

```java
EAST ( 1, 0, 0,  1,0,1,   0,0,-1,  0,1,0,  0.72f),
WEST (-1, 0, 0,  0,0,0,   0,0, 1,  0,1,0,  0.72f),
UP   ( 0, 1, 0,  1,1,0,  -1,0, 0,  0,0,1,  1.00f),
DOWN ( 0,-1, 0,  0,0,0,   1,0, 0,  0,0,1,  0.48f),
SOUTH( 0, 0, 1,  0,0,1,   1,0, 0,  0,1,0,  0.86f),
NORTH( 0, 0,-1,  1,0,0,  -1,0, 0,  0,1,0,  0.86f);
```

| Hướng | Pháp tuyến `(dx,dy,dz)` | Gốc mặt `(ox,oy,oz)` | Tiếp tuyến `u` | Tiếp tuyến `v` | `shade` |
|---|---|---|---|---|---|
| EAST (+X) | `( 1, 0, 0)` | `(1,0,1)` | `( 0,0,−1)` | `(0,1,0)` | 0.72 |
| WEST (−X) | `(−1, 0, 0)` | `(0,0,0)` | `( 0,0, 1)` | `(0,1,0)` | 0.72 |
| UP (+Y) | `( 0, 1, 0)` | `(1,1,0)` | `(−1,0, 0)` | `(0,0,1)` | **1.00** |
| DOWN (−Y) | `( 0,−1, 0)` | `(0,0,0)` | `( 1,0, 0)` | `(0,0,1)` | **0.48** |
| SOUTH (+Z) | `( 0, 0, 1)` | `(0,0,1)` | `( 1,0, 0)` | `(0,1,0)` | 0.86 |
| NORTH (−Z) | `( 0, 0,−1)` | `(1,0,0)` | `(−1,0, 0)` | `(0,1,0)` | 0.86 |

---

## 2. Ba vai trò của các con số

### 2.1 `(dx, dy, dz)` — vector pháp tuyến / bước hàng xóm

```java
int nx = x + direction.dx();
int ny = y + direction.dy();
int nz = z + direction.dz();
```

Sáu vector đơn vị `±e₁, ±e₂, ±e₃` — cơ sở chính tắc của `R³` và các vector đối. Dùng để:

- BFS ánh sáng duyệt hàng xóm ([LightEngine](../05-world/LightEngine.md))
- Nước lan sang ô kề ([FluidSimulator](../05-world/FluidSimulator.md))
- Kiểm tra mặt nào bị che khi dựng mesh

Duyệt `Direction.ALL` thay cho 6 khối `if` lặp lại ⇒ code ngắn, không sót hướng nào.

### 2.2 `(ox, oy, oz)` + `u` + `v` — dựng 4 đỉnh của mặt

Mỗi mặt hình vuông được sinh bằng công thức:

```
  P(i, j) = O + i·u + j·v ,      i, j ∈ {0, 1}
```

Bốn đỉnh:

```
  P(0,0) = O
  P(1,0) = O + u
  P(1,1) = O + u + v
  P(0,1) = O + v
```

**Ví dụ mặt UP** (`O = (1,1,0)`, `u = (−1,0,0)`, `v = (0,0,1)`):

```
  P(0,0) = (1, 1, 0)
  P(1,0) = (0, 1, 0)
  P(1,1) = (0, 1, 1)
  P(0,1) = (1, 1, 1)
```

Bốn góc của mặt trên khối đơn vị ✔

### 2.3 Vì sao mỗi hướng có gốc `O` khác nhau?

Để đảm bảo **thứ tự đỉnh ngược chiều kim đồng hồ khi nhìn từ ngoài vào** (counter-clockwise winding). GPU dùng thứ tự này để phân biệt mặt trước/mặt sau (**back-face culling**) — nếu sai, mặt sẽ tàng hình.

Kiểm chứng bằng tích có hướng:

```
  u × v phải cùng chiều với pháp tuyến (dx, dy, dz)
```

**Mặt UP:** `u × v = (−1,0,0) × (0,0,1)`

```
        │ i   j   k │
  u×v = │−1   0   0 │ = i(0·1 − 0·0) − j(−1·1 − 0·0) + k(0)
        │ 0   0   1 │
      = (0, 1, 0)  ✔ = pháp tuyến UP
```

**Mặt EAST:** `u × v = (0,0,−1) × (0,1,0)`

```
      = i(0·0 − (−1)·1) − j(0·0 − (−1)·0) + k(0·1 − 0·0)
      = (1, 0, 0)  ✔ = pháp tuyến EAST
```

Cả 6 hướng đều thoả `u × v = n` — đây chính là ràng buộc đã quyết định các con số trong bảng.

---

## 3. `shade` — chiếu sáng theo hướng mặt

```
  UP     = 1.00      (100 %)  ← nhận trực tiếp ánh sáng trời
  SOUTH  = 0.86      ( 86 %)
  NORTH  = 0.86      ( 86 %)
  EAST   = 0.72      ( 72 %)
  WEST   = 0.72      ( 72 %)
  DOWN   = 0.48      ( 48 %)  ← quay lưng vào đất
```

Đây là **mô hình chiếu sáng giả** (fake directional lighting) của Minecraft: thay vì tính góc giữa pháp tuyến và tia sáng mỗi khung hình, gán sẵn một hệ số cố định cho mỗi hướng.

### Xấp xỉ mô hình Lambert

Chiếu sáng Lambert thật: `I = max(0, n · l)` với `l` là hướng nguồn sáng. Nếu ánh sáng đến từ hướng chếch `l ≈ (0.3, 0.9, 0.3)` chuẩn hoá:

| Mặt | `n · l` | `shade` thực tế |
|---|---|---|
| UP | 0.92 | 1.00 |
| SOUTH/NORTH | ±0.31 | 0.86 |
| EAST/WEST | ±0.31 | 0.72 |
| DOWN | −0.92 | 0.48 |

Giá trị dùng ở đây **nén lại** (không cho về 0) để mặt dưới vẫn nhìn thấy chi tiết. Hai cặp `EAST/WEST` và `NORTH/SOUTH` được tách 0.72 vs 0.86 để **các mặt vuông góc phân biệt được nhau** — nếu bằng nhau, góc khối trông phẳng lì.

### Tính đối xứng đáng chú ý

```
  EAST = WEST     (cặp trục X)
  NORTH = SOUTH   (cặp trục Z)
  UP ≠ DOWN       (trục Y — trọng lực phá đối xứng)
```

Chỉ trục dọc bất đối xứng vì ánh sáng đến từ trên trời.

**Chi phí:** đọc 1 `float` từ enum thay vì tính tích vô hướng + chuẩn hoá mỗi đỉnh. Với hàng triệu đỉnh mỗi khung hình, tiết kiệm rất lớn.

---

## 4. `Direction.ALL` — mảng cache

```java
public static final Direction[] ALL = values();
```

`values()` của Java enum **tạo một mảng mới mỗi lần gọi** (để bảo vệ tính bất biến). Trong vòng lặp BFS gọi hàng triệu lần, đó là hàng triệu mảng rác.

Lưu vào hằng `static final` ⇒ **cấp phát một lần** cho cả vòng đời chương trình.

Vì lý do này, code duyệt bằng chỉ số thay vì for-each:

```java
for (int i = 0; i < Direction.ALL.length; i++) {
    Direction direction = Direction.ALL[i];
    ...
}
```

for-each trên mảng thực ra cũng biên dịch thành vòng lặp chỉ số, nhưng viết tường minh đảm bảo không có iterator nào được tạo.

### `SIDES` — chỉ 4 hướng ngang

```java
private static final Direction[] SIDES = {
        Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
```

`FluidSimulator` chỉ lan ngang (trên/dưới có luật riêng) nên giữ một mảng con 4 phần tử — tiết kiệm 2/6 = 33 % số vòng lặp.

---

## 5. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| Mọi getter | `O(1)` — đọc trường `final` |
| `Direction.ALL` | `O(1)` — mảng tĩnh |
| Bộ nhớ | 6 object enum × ~64 B = **384 B** cho cả chương trình |

---

## 6. Chủ đề DSA / Toán thể hiện

- **Bảng tra cứu (lookup table)** thay cho tính toán lặp lại.
- **Cơ sở chính tắc của `R³`** và 6 vector đơn vị.
- **Tích có hướng** (`u × v = n`) và quy tắc bàn tay phải.
- **Hệ toạ độ địa phương** (origin + 2 tiếp tuyến) để tham số hoá mặt phẳng.
- **Xấp xỉ mô hình Lambert** bằng hằng số.
- **Tránh cấp phát** (`values()` cache).
- **Enum có dữ liệu** — pattern thay cho hằng số rời rạc.

---

## 7. Liên kết

- Dựng mặt: [ChunkMesher.md](../07-render/ChunkMesher.md), [CubeGeometry.md](../07-render/CubeGeometry.md)
- Duyệt hàng xóm: [LightEngine.md](../05-world/LightEngine.md), [FluidSimulator.md](../05-world/FluidSimulator.md)
- Đổ bóng góc: [FaceLighting.md](../07-render/FaceLighting.md)
