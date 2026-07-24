# 12 công thức độ cao của các Biome

**Files:** `core/src/com/voxel/game/terrain/biome/*Biome.java`

Mọi biome đều là hàm `h(x, z)` trả về độ cao mặt đất, xây từ ba khối lego:

```
S = seaLevel              (hằng số, mực nước biển)
N = hills(x, z)   ∈ [−1, 1]     nhiễu fBm 4 octave, λ ≈ 111 khối
R = ridge(x, z)   ∈ [ 0, 1]     nhiễu sống núi = 1 − |fBm|
C = continent(x,z)∈ [−1, 1]     nhiễu lục địa,  λ ≈ 770 khối
```

---

## 1. Bảng tổng hợp

| Biome | Công thức | Biên độ dao động | Khoảng độ cao lý thuyết |
|---|---|---|---|
| **Ocean** | `S − 6 + min(0, C+0.30)·40 + 3N` | 3 | `S−37 … S−3.8` |
| **Beach** | `S + 1 + 1.5N` | 1.5 | `S−0.5 … S+2.5` |
| **Swamp** | `S + 1 + 2N` | 2 | `S−1 … S+3` |
| **Valley** | `S + 2 + 2.5N` | 2.5 | `S−0.5 … S+4.5` |
| **Plains** | `S + 5 + 3.5N` | 3.5 | `S+1.5 … S+8.5` |
| **Savanna** | `S + 6 + 4N` | 4 | `S+2 … S+10` |
| **SnowyPlains** | `S + 6 + 4N` | 4 | `S+2 … S+10` |
| **Desert** | `S + 6 + 5.5N` | 5.5 | `S+0.5 … S+11.5` |
| **Forest** | `S + 7 + 5N` | 5 | `S+2 … S+12` |
| **Hills** | `S + 13 + 9N + 10R²` | 19 | `S+4 … S+32` |
| **Mountains** | `S + 20 + 40R³ + 6N` | 46 | `S+14 … S+66` |
| **SnowyPeaks** | `S + 26 + 42R³ + 5N` | 47 | `S+21 … S+73` |

> Khoảng lý thuyết dùng `N, R` chạm biên `±1`. Thực tế fBm hiếm khi vượt `|N| > 0.6`, nên độ cao thường gặp hẹp hơn khoảng 40 %.

---

## 2. Phân tích từng nhóm

### 2.1 Nhóm phẳng — dạng `S + b + a·N`

`Beach, Swamp, Valley, Plains, Savanna, SnowyPlains, Desert, Forest`

```
h(x,z) = S + b + a · N(x,z)
```

- `b` = **độ cao nền** (offset) — quyết định biome nằm cao hay thấp so với biển.
- `a` = **biên độ** — quyết định gợn sóng mạnh hay nhẹ.
- Vì `N` là hàm liên tục, `h` cũng liên tục; **độ dốc tối đa** tỉ lệ với `a`:

```
  |∇h| = a · |∇N| ≈ a · 2π·f·A_fBm
```

Với `f = 0.009`, biên độ hiệu dụng `A ≈ 0.5`:
`|∇h| ≈ a × 0.028` khối/khối. Desert (`a = 5.5`) dốc ~0.15 → khoảng 8°, đủ để có đụn cát mà vẫn đi bộ được.

**Sắp xếp theo độ gồ ghề:** `Beach (1.5) < Swamp (2) < Valley (2.5) < Plains (3.5) < Savanna = SnowyPlains (4) < Forest (5) < Desert (5.5)`

Chú ý Desert có biên độ lớn nhất trong nhóm phẳng — mô phỏng **đụn cát**.

### 2.2 Ocean — độ sâu tỉ lệ với lục địa

```java
double depth = Math.min(0.0, noise.continent(x, z) + 0.30) * 40.0;
return noise.seaLevel() - 6.0 + depth + noise.hills(x, z) * 3.0;
```

```
  d(C) = min(0, C + 0.30) · 40
  h    = S − 6 + d(C) + 3N
```

**Hàm `min(0, ·)` là một bộ chỉnh lưu (ReLU âm):**

| `C` | `C + 0.30` | `d` | Ý nghĩa |
|---|---|---|---|
| `≥ −0.30` | `≥ 0` | **0** | Vùng ven — đáy phẳng ở `S−6` |
| `−0.50` | `−0.20` | `−8` | Thềm lục địa |
| `−1.00` | `−0.70` | `−28` | Vực sâu |

Điểm gãy tại `C = −0.30` nằm **rất gần** ngưỡng chọn ocean (`C < −0.32`), nên ngay sát bờ biển đáy chỉ sâu `S−6`, càng ra khơi càng sâu tuyến tính. Đây là mô hình **thềm lục địa → sườn dốc** đơn giản mà hiệu quả.

### 2.3 Hills — luỹ thừa bậc 2 của ridge

```java
double ridge = noise.ridge(x, z);
return S + 13.0 + N * 9.0 + ridge * ridge * 10.0;
```

```
h = S + 13 + 9N + 10R²
```

Vì `R ∈ [0, 1]`, phép bình phương **kéo giá trị nhỏ về gần 0** nhưng giữ nguyên giá trị gần 1:

| `R` | `R²` | Đóng góp |
|---|---|---|
| 0.2 | 0.04 | 0.4 khối |
| 0.5 | 0.25 | 2.5 khối |
| 0.8 | 0.64 | 6.4 khối |
| 1.0 | 1.00 | 10 khối |

⇒ Phần lớn diện tích đồi chỉ nhô nhẹ, chỉ **dọc theo sống núi** mới vọt lên. Tạo hình "đồi bát úp có gờ".

### 2.4 Mountains & SnowyPeaks — luỹ thừa bậc 3

```
Mountains  : h = S + 20 + 40R³ + 6N
SnowyPeaks : h = S + 26 + 42R³ + 5N
```

Bậc 3 làm hiệu ứng "chỉ đỉnh mới cao" mạnh hơn nữa:

| `R` | `R²` (Hills) | `R³` (Mountains) | Cao (Mountains) |
|---|---|---|---|
| 0.2 | 0.040 | **0.008** | 0.3 khối |
| 0.5 | 0.250 | **0.125** | 5 khối |
| 0.8 | 0.640 | **0.512** | 20.5 khối |
| 0.95 | 0.903 | **0.857** | 34.3 khối |
| 1.0 | 1.000 | **1.000** | 40 khối |

**So sánh trực quan:**

```
    R²  ────────╱‾‾      cong nhẹ, sườn thoải
    R³  ──────╱│         cong gắt, chân núi phẳng + vách dựng
        0        1
```

⇒ `R³` cho **chân núi rộng thoải, đỉnh nhọn dựng đứng** — đúng dáng núi đá thật. Đây là lý do Mountains trông "ra dáng núi" hơn Hills.

**Vì sao SnowyPeaks cao hơn?** Nền `26 > 20` và hệ số `42 > 40` ⇒ đỉnh tuyết luôn là điểm cao nhất thế giới (`≈ S + 73`), khớp với việc nó chỉ xuất hiện khi `erosion < −0.42 ∧ temperature < −0.25`.

### 2.5 Biên độ `N` giảm dần khi lên cao

| Biome | Hệ số `N` |
|---|---|
| Hills | 9 |
| Mountains | 6 |
| SnowyPeaks | 5 |

Càng lên cao, nhiễu đồi càng nhẹ để `R³` chiếm ưu thế — nếu giữ `N` lớn, đỉnh núi sẽ bị gợn lởm chởm mất dáng.

---

## 3. Khối bề mặt theo độ cao

### MountainBiome — hai đường ranh giới

```java
TREE_LINE = 34;   ROCK_LINE = 26;

topBlock(y):     y > S+34 → cobblestone
                 y > S+26 → stone
                 ngược lại → grass
fillerBlock(y):  y > S+26 → stone, ngược lại → dirt
```

Mô phỏng **ranh giới thực vật** (tree line) trên núi thật: trên một độ cao nhất định đất bị rửa trôi, chỉ còn đá trần.

### OceanBiome — đáy nông vs đáy sâu

```java
topBlock(y): y < S−12 → gravel, ngược lại → sand
```

Ngưỡng `S − 12`: cát ở vùng nông (sóng đánh), sỏi ở vùng sâu.

### DesertBiome

```java
topBlock  → sand
fillerBlock → sandstone     (cát bị nén thành sa thạch)
```

### SnowyPeaks / SnowyPlains

```java
topBlock → cobblestone
```

`SnowyPeaks` **buộc phải** ghi đè `fillerBlock → stone`; nếu không sẽ lấy mặc định `dirt` của lớp cha, cho ra đỉnh núi đá phủ đất — vô lý.

---

## 4. Bảng decorator & mật độ

| Biome | Cây | `p` | Bụi/hoa | `p` |
|---|---|---|---|---|
| Plains | Oak | 0.006 | hoa ×2 (0.015), cỏ 0.62 (vá 30) | |
| Forest | Oak 0.045 + Birch 0.018 | | hoa 0.02, cỏ 0.55 (vá 22) | |
| Savanna | Oak | 0.004 | cỏ 0.70 (vá 34) | |
| Desert | Cactus | 0.008 | deadBush 0.05 (chỉ trên `sand`) | |
| Swamp | Oak | 0.020 | cỏ 0.65 (vá 20) | |
| Valley | Oak | 0.012 | hoa ×2 (0.02), cỏ 0.58 (vá 24) | |
| Hills | Oak 0.020 + Boulder 0.004 | | cỏ 0.50 (vá 26) | |
| Mountains | Pine 0.015 + Boulder 0.006 | | cỏ 0.34 (vá 18) | |
| SnowyPeaks | Pine | 0.004 | — | |
| SnowyPlains | Pine | 0.030 | — | |
| Ocean / Beach | — | — | — | |

**Mật độ cây trung bình** trong một chunk 16×16 = 256 ô:

```
E[số cây] = 256 · p
```

- Forest: `256 × (0.045 + 0.018×0.955) ≈ 15.9` cây/chunk — rừng rậm.
- Savanna: `256 × 0.004 ≈ 1.0` cây/chunk — thưa thớt.
- SnowyPeaks: `256 × 0.004 ≈ 1.0` cây/chunk — gần trơ trọi.

Mỗi decorator mang một **salt** riêng (2, 3, 4, …, 27) đưa vào [Deterministic.unit](Deterministic.md) để hai loại thực vật khác nhau không bao giờ trùng vị trí.

---

## 5. Chủ đề DSA thể hiện

- **Đa hình / Strategy** — 12 lớp con, một lời gọi `surfaceHeight`.
- **Template Method** — khung `decorate()` cố định.
- **Chain of Responsibility** — chuỗi decorator, xác suất tích luỹ.
- **Biến đổi hàm phi tuyến** — `R²`, `R³`, `min(0, ·)` để nắn hình dạng.
