# TerrainShaper + pipeline sinh địa hình

**Files:**
- `core/src/com/voxel/game/terrain/TerrainShaper.java`
- `core/src/com/voxel/engine/generation/TerrainPipeline.java`
- `core/src/com/voxel/engine/generation/TerrainStage.java`
- `core/src/com/voxel/engine/generation/CarverPipeline.java`
- `core/src/com/voxel/game/terrain/{BedrockStage, SurfaceStage, StoneStage, OceanStage, SkyStage}.java`

**Design pattern:** **Facade** (TerrainShaper) + **Chain of Responsibility** (TerrainStage) + **Composite** (CarverPipeline)

---

## 1. Facade — 3 việc duy nhất mà Chunk cần biết

```java
void   sample(worldX, worldZ, out)          // cột này cao bao nhiêu, biome nào
Block  resolve(sample, worldX, y, worldZ)   // ở độ cao y là khối gì
Carver carvers()                            // khoét hang sau khi đổ đất
```

Toàn bộ 14 trường nhiễu, 12 biome, 5 stage, 5 carver bị giấu sau ba phương thức này.

---

## 2. Chain of Responsibility — quyết định khối

### Cơ chế

```java
public final Block resolve(S sample, int x, int y, int z) {
    Block resolved = tryResolve(sample, x, y, z);
    if (resolved != null) return resolved;          // tôi trả lời được
    return successor == null ? null                  // hết chuỗi
                             : successor.resolve(sample, x, y, z);  // hỏi người sau
}
```

`null` = **"tôi không biết"**. Chuỗi dừng ở stage đầu tiên trả về khác `null`.

### Thứ tự chuỗi (thứ tự này QUYẾT ĐỊNH kết quả)

```
BedrockStage → SurfaceStage → StoneStage → OceanStage → SkyStage
```

| # | Stage | Điều kiện trả về khối | Khối |
|---|---|---|---|
| 1 | **Bedrock** | `y == 0` | cobblestone |
| 2 | **Surface** | `y ≤ h` và `h − y < 1` | `gravel` nếu `y < S−1`, ngược lại `biome.topBlock` |
| | | `y ≤ h` và `1 ≤ h − y < 4` | `biome.fillerBlock` |
| 3 | **Stone** | `y ≤ h` | theo `strata` (xem dưới) |
| 4 | **Ocean** | `y ≤ S` | water |
| 5 | **Sky** | luôn luôn | air |

### Công thức độ sâu

```
d = h − y            (h = surfaceHeight, số thực; y = số nguyên)

d ∈ [0, 1)  → lớp mặt      (grass / sand / snow / gravel)
d ∈ [1, 4)  → lớp lót      (dirt / sand / sandstone / stone)   SOIL_DEPTH = 4
d ≥ 4       → đá sâu       (chuyển cho StoneStage)
```

**Vì sao `SkyStage` luôn trả về air?** Nó là **terminator** của chuỗi — bảo đảm `resolve` không bao giờ trả `null`, nên nơi gọi không phải kiểm tra null. Kỹ thuật **Null Object pattern**.

### StoneStage — chia vỉa theo nhiễu 3D

```java
double band = noise.strata(x, y, z);       // Simplex 3D, f = (0.021, 0.115, 0.019)
if (band >  0.46) return sandstone;
if (band < −0.42) return cobblestone;
return stone;
```

```
  band > +0.46  →  sa thạch
  band < −0.42  →  đá cuội
  còn lại       →  đá thường
```

Với phân phối gần chuẩn `σ ≈ 0.35` của Simplex 1 octave:

| Vỉa | Ngưỡng | z-score | Tỉ lệ thể tích ≈ |
|---|---|---|---|
| Sa thạch | `> 0.46` | +1.31σ | ~9 % |
| Đá cuội | `< −0.42` | −1.20σ | ~11 % |
| Đá thường | giữa | — | ~80 % |

Tần số `f_y = 0.115` **gấp 5.5 lần** `f_x = 0.021` ⇒ vỉa **dẹt nằm ngang**, dày khoảng `1/(2×0.115) ≈ 4` khối — đúng dáng địa tầng trầm tích thật.

### SurfaceStage — mẹo sỏi dưới nước

```java
if (depth < 1.0) {
    if (y < seaLevel - 1) return blocks.gravel;   // dưới nước thì sỏi
    return biome.topBlock(y, seaLevel);
}
```

Không cần biome riêng cho lòng sông/hồ: hễ mặt đất nằm dưới `S − 1` thì tự động lát sỏi. Kết hợp với `carveWater` của [BiomeSource](BiomeSource.md), lòng sông khoét ra sẽ tự có đáy sỏi.

---

## 3. Composite — chuỗi khoét hang

```java
this.carvers = CarverPipeline.of(
        new PerlinWormCarver(noise, blocks, seed),
        new RavineCarver(noise, blocks, seed),
        new CheeseCaveCarver(noise, blocks),
        new SpaghettiCaveCarver(noise, blocks),
        new StructureCarver(this, blocks, seed));   // LUÔN CUỐI CÙNG
```

Khác với `TerrainStage`, `CarverPipeline` **chạy hết tất cả** — mỗi carver khoét thêm vào kết quả của carver trước:

```java
for (Carver carver : carvers) {
    carver.carve(writer, chunkX, chunkZ, chunkSize, worldHeight);
}
```

**Về mặt tập hợp:** nếu `Aᵢ` là tập ô mà carver `i` khoét, thì khoảng rỗng cuối cùng là **hợp**:

```
  Void = A₁ ∪ A₂ ∪ A₃ ∪ A₄
```

Trong khi `TerrainStage` là phép chọn **loại trừ lẫn nhau** (stage đầu tiên thắng).

**Vì sao `StructureCarver` phải cuối?** Comment trong code: *"Structures build LAST so towers and pyramids stand on carved terrain"* — nếu dựng tháp trước rồi mới khoét hang, hang sẽ đục thủng nền móng tháp.

---

## 4. `columnTop` — giới hạn vòng lặp dọc

```java
public int columnTop(ColumnSample sample) {
    return (int) Math.ceil(Math.max(sample.surfaceHeight(), seaLevel));
}
```

```
  top = ⌈ max(h, S) ⌉
```

Chỉ duyệt `y ∈ [0, top]` thay vì `[0, worldHeight)`. Với `worldHeight = 128`, `S = 62`, `h ≈ 70`:

```
tiết kiệm = (128 − 71) / 128 ≈ 45 % số ô phải xét
```

Dùng `max(h, S)` chứ không chỉ `h` vì cột dưới đáy biển vẫn cần đổ nước lên tới mực biển.

### Trần thế giới

```java
this.ceiling = worldHeight - 2;
double height = Math.min(ceiling, biomes.blendedHeight(worldX, worldZ));
```

Kẹp `h ≤ worldHeight − 2` để đỉnh SnowyPeaks (`S + 73`) không tràn khỏi mảng chunk.

---

## 5. Vòng lặp sinh chunk (OverworldChunk)

```java
for (x = 0..15)
  for (z = 0..15) {
      shaper.sample(worldX, worldZ, sample);        // ~36 lần lấy nhiễu
      top = min(height−1, shaper.columnTop(sample));
      for (y = 0..top)
          block = shaper.resolve(sample, worldX, y, worldZ);
  }
```

### Độ phức tạp

| Giai đoạn | Chi phí |
|---|---|
| `sample` | `256 × 36` ≈ **9 216** lần lấy nhiễu |
| `resolve` | `256 × 71` ≈ **18 176** lần, mỗi lần đi tối đa 5 stage |
| `carveCaves` | xem `03-caves/` |
| `decorate` | `256 × O(height)` để tìm mặt đất |
| **Tổng** | `O(size² · worldHeight)` = `O(16² × 128)` ≈ **32 768** ô/chunk |

`ColumnSample` được **tái sử dụng** (`sample.prepare(...)` ghi đè) — một object cho cả chunk thay vì 256 object ⇒ không sinh rác GC trong vòng lặp nóng.

---

## 6. `groundLevel` — tìm mặt đất sau khi khoét hang

```java
for (int y = height - 2; y > shaper.seaLevel(); y--) {
    Block block = writer.get(x, y, z);
    if (!isPlantable(block)) continue;
    if (writer.get(x, y + 1, z).isAir()) return y;
}
return -1;
```

**Quét từ trên xuống**, tìm khối đặc đầu tiên thoả:
1. thuộc `{grass, dirt, sand, cobblestone}` — trồng được;
2. ngay trên đầu là không khí.

Chạy **sau** `carveCaves` nên miệng hang không bị cây phủ kín.

Độ phức tạp `O(worldHeight)` mỗi cột ⇒ `O(size² · worldHeight)` cả chunk.

**Vì sao dừng ở `y > seaLevel`?** Không trồng cây dưới mực nước biển.

---

## 7. Chủ đề DSA thể hiện

| Pattern / thuật toán | Nơi thể hiện |
|---|---|
| **Facade** | `TerrainShaper` |
| **Chain of Responsibility** | `TerrainStage.resolve` |
| **Composite** | `CarverPipeline` |
| **Null Object** | `SkyStage` luôn trả air |
| **Object pooling** | tái dùng `ColumnSample` |
| **Cắt tỉa vòng lặp** | `columnTop` giảm 45 % công việc |
| **Quét tuyến tính** | `groundLevel` |

---

## 8. Liên kết

- Nguồn nhiễu: [TerrainNoise.md](TerrainNoise.md)
- Chọn biome & làm mượt: [BiomeSource.md](BiomeSource.md)
- Công thức độ cao: [Biome-heights.md](Biome-heights.md)
- Khoét hang: [thư mục 03-caves](../03-caves/)
