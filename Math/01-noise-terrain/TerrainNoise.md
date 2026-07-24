# TerrainNoise

**File:** `core/src/com/voxel/game/terrain/TerrainNoise.java`
**Vai trò:** "Bảng pha màu" của thế giới — gom 14 trường nhiễu độc lập, mỗi trường phụ trách một khía cạnh địa hình. Toàn bộ code sinh thế giới chỉ đọc từ đây nên biên giới giữa các biome luôn liền mạch.

---

## 1. Sinh 14 seed độc lập từ 1 seed gốc

```java
continentField  = new SimplexNoise(seed);
erosionField    = new SimplexNoise(seed * 31L  + 17L);
hillField       = new SimplexNoise(seed ^ 0x5DEECE66DL);
ridgeField      = new SimplexNoise(seed * 131L + 7919L);
...
```

### Công thức

```
seed_k = seed · p_k + q_k
```

với `p_k` là **số nguyên tố** (31, 131, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47) và `q_k` cũng là số nguyên tố lớn (17, 7919, 104729, 15485863, …).

**Vì sao dùng số nguyên tố?**
Phép nhân với số nguyên tố trong vành `Z/2⁶⁴` bảo toàn tính "tán" — hai seed gốc gần nhau (`s` và `s+1`) cho ra 14 bộ seed dẫn xuất khác nhau hoàn toàn, không có trường nào bị **tương quan** (correlated) với trường khác. Nếu dùng `seed + 1, seed + 2, …` thì các bảng hoán vị sinh ra sẽ giống nhau đáng kể → núi và sông sẽ trùng hình.

`hillField` dùng `seed ^ 0x5DEECE66D` — chính là hằng nhân của LCG trong `java.util.Random`, một hằng số đã được chứng minh phân tán bit tốt.

---

## 2. Bảng đầy đủ 14 trường nhiễu

| Hàm | Loại | Octaves | Frequency | Bước sóng ≈ `1/f` | Miền giá trị | Dùng để |
|---|---|---|---|---|---|---|
| `continent(x,z)` | fBm 2D | 3 | 0.0013 | ~770 khối | `[−1, 1]` | Âm = biển, dương = đất liền |
| `erosion(x,z)` | fBm 2D | 2 | 0.0021 | ~476 khối | `[−1, 1]` | Cao = đất bằng, thấp = núi |
| `hills(x,z)` | fBm 2D | 4 | 0.0090 | ~111 khối | `[−1, 1]` | Gợn đồi nhỏ, mọi biome đều dùng |
| `ridge(x,z)` | fBm 2D + gấp | 4 | 0.0042 | ~238 khối | `[0, 1]` | Sống núi sắc |
| `temperature(x,z)` | fBm 2D | 2 | 0.0008 | ~1250 khối | `[−1, 1]` | Âm = lạnh, dương = nóng |
| `humidity(x,z)` | fBm 2D | 2 | 0.0010 | ~1000 khối | `[−1, 1]` | Âm = khô, dương = ẩm |
| `river(x,z)` | fBm 2D | 2 | 0.0016 | ~625 khối | `[−1, 1]` | Đường **zero** = lòng sông |
| `lake(x,z)` | fBm 2D | 2 | 0.0032 | ~312 khối | `[−1, 1]` | Đỉnh cao = lòng chảo hồ |
| `entrance(x,z)` | fBm 2D | 2 | 0.012 | ~83 khối | `[−1, 1]` | Vùng cho phép trổ cửa hang |
| `strata(x,y,z)` | Simplex 3D | 1 | (0.021, 0.115, 0.019) | — | `[−1, 1]` | Vỉa đá ngầm |
| `cave(x,y,z)` | Simplex 3D | 1 | (0.023, 0.040, 0.023) | ~43 khối | `[−1, 1]` | Hang "phô mai" |
| `spaghettiA(x,y,z)` | Simplex 3D | 1 | (0.014, 0.020, 0.014) | ~71 khối | `[−1, 1]` | Mặt zero thứ nhất |
| `spaghettiB(x,y,z)` | Simplex 3D | 1 | (0.014, 0.020, 0.014) | ~71 khối | `[−1, 1]` | Mặt zero thứ hai |
| `worm(t, offset)` | Simplex 2D | 1 | 1.0 (đã scale ngoài) | — | `[−1, 1]` | Hướng bò của perlin worm |

**Bước sóng đặc trưng** `λ ≈ 1/f`: `f = 0.0013` ⇒ một "lục địa" rộng khoảng 770 khối. `f = 0.0008` ⇒ một đới khí hậu rộng ~1250 khối, đủ lớn để sa mạc không bị bé tí lọt thỏm giữa đồng bằng.

---

## 3. Các công thức đáng chú ý

### 3.1 Ridge noise — biến nhiễu mượt thành sống núi sắc

```java
public double ridge(int x, int z) {
    return 1.0 - Math.abs(ridgeField.fractal2d(x, z, 4, 0.0042, 2.0, 0.5));
}
```

```
ridge(p) = 1 − |fBm(p)|
```

**Phân tích:**

| `fBm` | `|fBm|` | `ridge` |
|---|---|---|
| −1 | 1 | 0 (thung lũng) |
| 0 | 0 | **1 (đỉnh nhọn)** |
| +1 | 1 | 0 (thung lũng) |

Hàm `|·|` tạo một **điểm gãy** (không khả vi) tại `fBm = 0`. Vì tập `{fBm = 0}` là một đường cong dài uốn lượn trong mặt phẳng, kết quả là một **dãy sống núi** liên tục có đỉnh sắc — chính xác hình dạng núi thật, khác hẳn nhiễu tròn trịa gốc.

Đây là kỹ thuật kinh điển gọi là **ridged multifractal**.

### 3.2 Nhiễu 3D bất đẳng hướng (anisotropic)

```java
public double cave(int x, int y, int z) {
    return caveField.noise(x * 0.023, y * 0.040, z * 0.023);
}
```

Chú ý `f_y = 0.040` gần **gấp đôi** `f_x = f_z = 0.023`.

```
λ_x = λ_z ≈ 43 khối,   λ_y ≈ 25 khối
```

⇒ Bong bóng hang bị **nén theo chiều dọc**: rộng và dẹt thay vì hình cầu. Hang dẹt trông tự nhiên hơn và người chơi đi lại được.

`strata` còn cực đoan hơn: `f_y = 0.115` so với `f_x ≈ 0.02` → tỉ lệ ~6:1, tạo ra các **vỉa nằm ngang** mỏng đúng như địa tầng thật.

### 3.3 Spaghetti caves — giao của hai mặt zero

```java
spaghettiA(x,y,z) = noiseA(0.014x, 0.020y, 0.014z)
spaghettiB(x,y,z) = noiseB(0.014x, 0.020y, 0.014z)
```

**Lý thuyết:** Trong `R³`, tập `{f = 0}` của một hàm trơn là một **mặt cong 2 chiều**. Giao của hai mặt cong độc lập là một **đường cong 1 chiều**:

```
dim = 3 − 1 − 1 = 1
```

Vậy khoét đá ở nơi `|A| < ε` **và** `|B| < ε` cho ra những **đường hầm dài, mảnh, xoắn tự nhiên trong không gian 3D** — đúng cách Minecraft 1.18+ làm. Xem [SpaghettiCaveCarver.md](../03-caves/SpaghettiCaveCarver.md).

### 3.4 River — đường zero làm lòng sông

Cùng nguyên lý ở 2D: tập `{river = 0}` trong mặt phẳng là một **đường cong dài uốn lượn**. Lấy `|river| < RIVER_WIDTH` được một dải hẹp bám theo đường đó ⇒ dòng sông ngoằn ngoèo. Chỉ dùng 2 octave để bờ sông lượn nhẹ chứ không lởm chởm.

---

## 4. An toàn đa luồng

Mọi phương thức **chỉ đọc**: `SimplexNoise` sau khi dựng xong thì `perm[]` bất biến, `noise()` không có trạng thái. Nhờ vậy nhiều luồng sinh chunk gọi song song mà không cần khoá — quan trọng vì `ChunkScheduler` chạy đa luồng.

---

## 5. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| Khởi tạo `TerrainNoise` | `O(14 × 512)` ≈ 7 000 phép — một lần duy nhất |
| Bộ nhớ | 14 × 2 KB ≈ **28 KB** |
| Một lần gọi `fractal2d` 4 octave | ~4 × 30 = 120 phép toán |
| Sinh 1 chunk (16×16 cột) | 256 cột × 9 điểm blend × 4 trường ≈ **9 216** lần gọi nhiễu |

---

## 6. Liên kết

- Nguồn: [SimplexNoise.md](SimplexNoise.md)
- Người dùng chính: [BiomeSource.md](BiomeSource.md), [TerrainShaper.md](TerrainShaper.md), các carver trong `03-caves/`
