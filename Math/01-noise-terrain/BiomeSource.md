# BiomeSource

**File:** `core/src/com/voxel/game/terrain/biome/BiomeSource.java`
**Vai trò:** Trả lời hai câu hỏi cho mọi toạ độ `(x, z)`: **biome nào?** và **cao bao nhiêu?** — kèm khoét sông hồ.

---

## 1. Chọn biome — cây quyết định trên 4 tham số khí hậu

Bốn tham số độc lập lấy từ [TerrainNoise](../01-noise-terrain/TerrainNoise.md):

```
C = continent(x,z)     T = temperature(x,z)
E = erosion(x,z)       H = humidity(x,z)
```

### Cây quyết định (thứ tự kiểm tra rất quan trọng)

```
C < −0.32 ──────────────────────────────────► OCEAN
C < −0.24 ──────────────────────────────────► BEACH
    │
    ├─ E < −0.42 ─┬─ T < −0.25 ─────────────► SNOWY_PEAKS
    │             └─ ngược lại ─────────────► MOUNTAINS
    ├─ E < −0.12 ───────────────────────────► HILLS
    ├─ E >  0.50 ─┬─ H > 0.30 ──────────────► SWAMP
    │             └─ ngược lại ─────────────► VALLEY
    ├─ T < −0.35 ───────────────────────────► SNOWY_PLAINS
    ├─ T >  0.22  ∧  H < 0.02 ──────────────► DESERT
    ├─ T >  0.12  ∧  H < 0.15 ──────────────► SAVANNA
    ├─ H >  0.15 ───────────────────────────► FOREST
    └─ mặc định ────────────────────────────► PLAINS
```

**Độ phức tạp:** `O(1)` — tối đa 10 phép so sánh, nhưng phải gọi 4 hàm nhiễu (mỗi hàm 2–4 octave).

### Vì sao ngưỡng lại là những con số đó?

`fBm` với `lacunarity = 2, gain = 0.5` cho phân phối gần **Gauss** tập trung quanh 0, độ lệch chuẩn khoảng `σ ≈ 0.25`. Vậy:

| Ngưỡng | ≈ z-score | Tỉ lệ diện tích thế giới |
|---|---|---|
| `C < −0.32` | −1.28σ | ~10 % → đại dương |
| `C < −0.24` | −0.96σ | ~7 % → bãi biển |
| `E < −0.42` | −1.68σ | ~5 % → núi cao |
| `T > 0.22 ∧ H < 0.02` | — | ~8 % → sa mạc |

Comment trong code ghi rõ: ngưỡng sa mạc từng đặt `> 0.35` khiến sa mạc "bé teo lạc giữa đồng bằng"; hạ xuống `0.22` mới đủ rộng liền mạch. Đây là **hiệu ứng đuôi phân phối** — càng đẩy ngưỡng ra xa trung bình, diện tích thoả mãn co lại theo hàm mũ.

---

## 2. `blendedHeight` — làm mượt biên giới biome

### Vấn đề

Nếu mỗi điểm chỉ dùng công thức của biome tại chính nó, tại biên núi ↔ đồng bằng độ cao nhảy đột ngột từ `sea+60` xuống `sea+8` ⇒ **vách đứng** xấu xí.

### Giải pháp: trung bình có trọng số 9 điểm

```java
for (dx = −8; dx <= 8; dx += 8)
    for (dz = −8; dz <= 8; dz += 8) {
        w = (dx == 0 && dz == 0) ? 3.0 : 1.0;
        total  += pick(x+dx, z+dz).surfaceHeight(noise, x, z) * w;
        weight += w;
    }
return total / weight;
```

### Công thức

```
             1      ⌈  ___                                     ⌉
  h(x,z) = ──────   │  ╲    w(dx,dz) · h_biome(x+dx, z+dz)(x,z) │
            Σw      ⌊  ‾‾‾                                     ⌋
                     (dx,dz) ∈ {−8,0,8}²
```

Ma trận trọng số (kernel 3×3):

```
      dz=−8  dz=0  dz=+8
dx=−8   1     1      1
dx= 0   1   ⟦3⟧     1        Σw = 8·1 + 3 = 11
dx=+8   1     1      1
```

### Điểm tinh tế nhất của hàm này

```java
pick(x + dx, z + dz).surfaceHeight(noise, x, z)
                     ↑                    ↑
              biome LÂN CẬN        nhưng đánh giá TẠI ĐIỂM GỐC
```

Ta lấy **công thức độ cao của biome hàng xóm** rồi tính nó **tại toạ độ đang xét**, chứ không lấy độ cao của điểm hàng xóm. Nhờ vậy:

- Kết quả vẫn là hàm liên tục của `(x, z)` — không bị "trung bình hoá" mất chi tiết.
- Khi đi từ đồng bằng vào núi, trọng số dịch dần từ 9 phiếu đồng bằng → 8/1 → 6/3 → … → 9 phiếu núi ⇒ **sườn dốc liên tục**.

**Vùng chuyển tiếp** rộng `2 × BLEND_STEP = 16` khối.

### Độ phức tạp

`O(9)` = `O(1)` theo lý thuyết, **nhưng** mỗi `pick()` gọi 4 hàm nhiễu ⇒ 36 lần lấy nhiễu cho một cột. Nhân với `16 × 16 = 256` cột mỗi chunk:

```
256 × 36 ≈ 9 216 lần gọi nhiễu / chunk
```

Comment trong code nói thẳng: đây là **hàm nặng nhất của toàn bộ quá trình sinh địa hình**.

---

## 3. `carveWater` — khoét sông và hồ

Chạy **sau** khi đã làm mượt, hạ độ cao xuống dưới mực nước biển để `OceanStage` đổ nước vào.

### 3.1 Sông

```java
if (height < sea + 15.0) {                 // RIVER_MAX_RISE
    r = |river(x, z)|;
    if (r < 0.045) {                       // RIVER_WIDTH
        t = 1 − r / 0.045;                 // 0 ở bờ → 1 giữa dòng
        t = t·t·(3 − 2t);                  // smoothstep
        bed = sea − 4.0;                   // RIVER_BED
        if (height > bed) height −= t·(height − bed);
    }
}
```

**Công thức:**

```
        |river(p)|
  u = 1 ────────── ,  u ∈ [0,1]
          0.045

  t = smoothstep(u) = u²(3 − 2u)

  h' = h − t · (h − bed)  =  (1 − t)·h + t·bed        ← nội suy tuyến tính!
```

Tức là `h' = lerp(h, bed, t)`: ở bờ (`t = 0`) giữ nguyên độ cao, ở giữa dòng (`t = 1`) hạ hẳn xuống đáy sông.

### 3.2 Hàm smoothstep

```
S(t) = 3t² − 2t³ = t²(3 − 2t)
```

| Tính chất | Giá trị |
|---|---|
| `S(0)` | 0 |
| `S(1)` | 1 |
| `S′(t) = 6t(1−t)` | `S′(0) = S′(1) = 0` |

Đạo hàm triệt tiêu ở hai đầu ⇒ nối vào địa hình xung quanh **trơn tru, không gãy góc**. Nếu dùng `t` tuyến tính, bờ sông sẽ có nếp gấp nhìn thấy rõ.

### 3.3 Hồ

```java
if (height < sea + 10.0) {                 // LAKE_MAX_RISE
    l = lake(x, z);
    if (l > 0.55) {                        // LAKE_LEVEL
        t = (l − 0.55) / (1 − 0.55);       // chuẩn hoá về [0,1]
        t = t·t·(3 − 2t);
        bed = sea − 5.0;                   // LAKE_BED
        if (height > bed) height −= t·(height − bed);
    }
}
```

```
      lake(p) − 0.55
  u = ──────────────  ,  h' = lerp(h, sea − 5, smoothstep(u))
        1 − 0.55
```

Khác sông ở chỗ dùng **đỉnh** của nhiễu (`l > 0.55`) chứ không dùng đường zero ⇒ hình dạng là những **mảng tròn rời rạc** (lòng chảo) thay vì dải dài.

### 3.4 Vì sao có `MAX_RISE`?

Chỉ khoét nước ở chỗ địa hình đã thấp (`h < sea + 15` cho sông, `< sea + 10` cho hồ). Không có điều kiện này, đường zero của `river` cắt ngang núi cao 60 khối sẽ tạo ra **hẻm nước dựng đứng lơ lửng** — phi vật lý.

---

## 4. Bảng hằng số

| Hằng | Giá trị | Đơn vị | Ý nghĩa |
|---|---|---|---|
| `BLEND_STEP` | 8 | khối | Khoảng cách lấy mẫu blend |
| `RIVER_WIDTH` | 0.045 | đơn vị nhiễu | Bán rộng lòng sông |
| `RIVER_BED` | 4.0 | khối | Đáy sông dưới mực biển |
| `RIVER_MAX_RISE` | 15.0 | khối | Trần độ cao còn khoét sông |
| `LAKE_LEVEL` | 0.55 | đơn vị nhiễu | Ngưỡng thành hồ |
| `LAKE_BED` | 5.0 | khối | Đáy hồ dưới mực biển |
| `LAKE_MAX_RISE` | 10.0 | khối | Trần độ cao còn khoét hồ |

---

## 5. Chủ đề DSA thể hiện

- **Cây quyết định** phân loại đa tham số.
- **Nội suy tuyến tính** (`lerp`) và **smoothstep** (đa thức Hermite bậc 3).
- **Bộ lọc trung bình có trọng số** (weighted average kernel) — chính là *convolution* rời rạc 3×3.
- **Strategy pattern**: mỗi `Biome` là một chiến lược tính độ cao khác nhau.

---

## 6. Liên kết

- Công thức độ cao từng biome: [Biome-heights.md](Biome-heights.md)
- Nguồn nhiễu: [TerrainNoise.md](TerrainNoise.md)
- Người gọi: [TerrainShaper.md](TerrainShaper.md)
