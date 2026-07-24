# Geometries — hình dạng khối (Cube, Cross, Liquid)

**Files:** `core/src/com/voxel/engine/block/geometry/{CubeGeometry, CrossGeometry, LiquidGeometry}.java`

**Design pattern:** **Strategy** + **Flyweight** (mỗi hình dạng là một singleton dùng chung cho mọi khối).

---

## 1. CubeGeometry — khối lập phương

```java
public static final CubeGeometry OPAQUE      = new CubeGeometry(true);
public static final CubeGeometry TRANSLUCENT = new CubeGeometry(false);

public void emit(BlockView view, int x, int y, int z, Block block, QuadEmitter emitter) {
    for (Direction face : Direction.ALL) {
        if (view.occludes(x + face.dx(), y + face.dy(), z + face.dz(), block)) continue;
        shader.shade(view, x, y, z, block, face, corners);
        emitter.quad(x, y, z, face, 1f, 1f, block.textureFor(face), corners);
    }
}
```

Duyệt 6 hướng, bỏ qua mặt bị che. Số mặt sinh ra: `0` (khối chôn trong lòng đất) đến `6` (khối lơ lửng).

### Hai thể hiện Flyweight

| | `OPAQUE` | `TRANSLUCENT` |
|---|---|---|
| `occludesNeighbours()` | `true` | `false` |
| Ví dụ | đá, đất, gỗ | kính, băng |

Chỉ **2 object** cho hàng trăm loại khối và hàng triệu ô. Trạng thái riêng của từng ô (`x, y, z, block`) được truyền qua **tham số**, không lưu trong object ⇒ đúng định nghĩa Flyweight.

### `ThreadLocal<FaceLighting>`

```java
private final ThreadLocal<FaceLighting> lighting = new ThreadLocal<FaceLighting>() {
    protected FaceLighting initialValue() { return new FaceLighting(); }
};
```

`FaceLighting` có một `Color tintBuffer` tái dùng ⇒ **không an toàn đa luồng**. Nhưng geometry là singleton dùng chung bởi nhiều luồng mesh.

`ThreadLocal` cho **mỗi luồng một thể hiện riêng** — vẫn tránh cấp phát trong vòng nóng mà không cần khoá. Số object = số luồng mesh (2–8), không phải số lần gọi.

---

## 2. CrossGeometry — cỏ, hoa hình chữ X

```java
private static final float SPREAD = 0.15f;

public void emit(BlockView view, int x, int y, int z, Block block, QuadEmitter emitter) {
    lighting.get().flat(view, x, y, z, block, 0.94f, color);     // KHÔNG dùng AO

    float jitterX = hash(x, z, 17) * SPREAD;
    float jitterZ = hash(x, z, 31) * SPREAD;

    emitter.billboard(x, y, z, jitterX, jitterZ, false, region, color);
    emitter.billboard(x, y, z, jitterX, jitterZ, true,  region, color);
}
```

Hai tấm chéo nhau tạo hình chữ X — xem [ChunkMesher §6](ChunkMesher.md).

### `flat` thay vì `shade`

Cỏ là tấm phẳng mỏng, không có góc lõm ⇒ ambient occlusion vô nghĩa và sẽ làm nó tối vô cớ. `flat` chỉ đọc **1 ô** thay vì 4 góc × 4 ô. Hệ số `0.94` là `shade` cố định (không phụ thuộc hướng vì tấm nghiêng 45°).

### Hàm băm dịch chuyển ngẫu nhiên

```java
private static float hash(int x, int z, int salt) {
    int h = x * 73856093 ^ z * 19349663 ^ salt * 83492791;
    h = (h ^ (h >>> 13)) * 1274126177;
    return ((h >>> 16) & 0xFF) / 255f - 0.5f;
}
```

**Ba hằng số nguyên tố lớn** `73856093, 19349663, 83492791` — đây là bộ hằng số kinh điển của **spatial hashing** (Teschner et al. 2003), được chọn để `x·p₁ ⊕ z·p₂` phân tán đều trong không gian 2D.

**Bước trộn:** `(h ⊕ (h >>> 13)) × 1274126177` — một vòng xor-shift-multiply, đủ tốt cho mục đích thẩm mỹ (không cần chất lượng như `fmix64` của [Deterministic](../01-noise-terrain/Deterministic.md)).

**Chuẩn hoá:**

```
        (h >>> 16) & 0xFF          byte cao thứ 3
  r  =  ─────────────────  − 0.5  ∈ [−0.5, +0.5)
              255
```

Nhân `SPREAD = 0.15` ⇒ dịch chuyển `∈ [−0.075, +0.075]` khối.

**Vì sao cần?** Không có jitter, mọi bụi cỏ nằm chính giữa ô ⇒ nhìn từ xa thấy rõ **lưới vuông đều tăm tắp**. Xê dịch nhẹ phá vỡ tính đều đặn, thảm cỏ trông tự nhiên.

Hai salt khác nhau (17, 31) cho `x` và `z` dịch chuyển độc lập.

---

## 3. LiquidGeometry — mặt nước theo mức

Phức tạp nhất trong ba loại, vì mặt nước **không nằm sát trần ô**.

### 3.1 Độ cao mặt nước

```java
private static final float FULL_SURFACE = 0.875f;      // 7/8

private float topOf(BlockView view, int x, int y, int z, Block block) {
    if (view.blockAt(x, y + 1, z).isLiquid()) return 1f;       // có nước trên đầu → đầy ô
    return FULL_SURFACE * block.fluidLevel() / Block.MAX_FLUID_LEVEL;
}
```

```
             ⎧ 1.0                          nếu ô trên là nước
  top(p) =   ⎨
             ⎩ 0.875 × level / 8            ngược lại
```

| `level` | `top` | Ý nghĩa |
|---|---|---|
| 8 (nguồn) | **0.875** | Mặt nước thấp hơn trần 1/8 khối |
| 7 (rơi) | 0.766 | |
| 4 | 0.438 | |
| 1 | 0.109 | Rất nông |

**Vì sao `0.875` chứ không phải `1.0` cho nguồn?** Đứng trên mặt hồ, đầu người chơi nhô lên khỏi nước một chút — đúng như Minecraft. Cũng tạo khe hở để nhìn thấy mặt nước từ bên cạnh.

**Vì sao ô có nước trên đầu thì `top = 1`?**

> Comment: *"để trong lòng hồ không bị kẻ sọc"*

Nếu mọi ô đều `0.875`, một cột nước sâu 5 ô sẽ có **5 khe hở ngang** giữa các tầng ⇒ nhìn dưới nước thấy sọc ngang. Ô bị phủ thì lấp đầy hoàn toàn.

### 3.2 Hai mặt trên/dưới cho lớp màng

```java
if (!covered) {
    shader.shade(view, x, y, z, block, Direction.UP, corners);
    emitter.quad(x, y - (1f - top), z, Direction.UP, 1f, 1f, region, corners);

    shader.shade(view, x, y, z, block, Direction.DOWN, corners);
    emitter.quad(x, y + top - SKIN, z, Direction.DOWN, 1f, 1f, region, corners);
}
```

**Hạ mặt trên xuống:** `Direction.UP` có `originY = 1`, nên `quad` sẽ đặt mặt ở `y + 1`. Truyền `y − (1 − top)` ⇒ vị trí thật là:

```
  y − (1 − top) + 1 = y + top     ✔ đúng độ cao mặt nước
```

**Mặt dưới nhìn từ trên:** `Direction.DOWN` có `originY = 0`, truyền `y + top − SKIN` ⇒ vị trí `y + top − 0.002`.

### `SKIN = 0.002` — chống z-fighting

Hai mặt (UP và DOWN) nằm **cùng một độ cao** sẽ khiến GPU không biết mặt nào ở trước — kết quả là các mảng nhấp nháy loang lổ khi camera di chuyển (**z-fighting**).

Lùi mặt dưới xuống `0.002` khối (2 mm quy đổi) — đủ để bộ đệm độ sâu 24-bit phân biệt, quá nhỏ để mắt thấy.

**Vì sao cần cả hai mặt?** Người chơi lặn xuống nước phải nhìn thấy mặt nước **từ bên dưới**. Một mặt phẳng chỉ hiển thị từ một phía (back-face culling).

### 3.3 Mặt bên — chỉ vẽ khi thấy được

```java
private boolean needsSide(BlockView view, int x, int y, int z, Block block, Direction face, float top) {
    Block neighbour = view.blockAt(x + face.dx(), y, z + face.dz());
    if (neighbour.isLiquid()) {
        return topOf(view, nx, y, nz, neighbour) < top;    // hàng xóm THẤP hơn → lộ bậc nước
    }
    return !view.occludes(nx, y, nz, block);
}
```

Ba trường hợp:

| Hàng xóm | Vẽ mặt bên? | Lý do |
|---|---|---|
| Nước **thấp hơn** | ✔ | Lộ ra một "bậc nước" — thấy rõ hướng chảy |
| Nước **cao bằng/hơn** | ✗ | Mặt nằm trong lòng nước, vẽ chỉ tốn tam giác |
| Khối đặc | ✗ | Bị che |
| Không khí | ✔ | Thành nước lộ thiên |

```java
emitter.quad(x, y, z, face, 1f, top, region, corners);
                              ↑    ↑
                          rộng 1  cao = top
```

Mặt bên **chỉ cao bằng mực nước**, không phải 1 khối đầy.

### 3.4 Đáy dòng nước

```java
if (!view.blockAt(x, y - 1, z).isLiquid() && !view.occludes(x, y - 1, z, block)) {
    shader.shade(view, x, y, z, block, Direction.DOWN, corners);
    emitter.quad(x, y, z, Direction.DOWN, 1f, 1f, region, corners);
}
```

Mặt đáy đầy đủ ở `y` (không phải `y + top`) — cho trường hợp nước lơ lửng (thác đang rơi), nhìn từ dưới lên thấy đáy dòng nước.

### 3.5 `occludesNeighbours() = false`

Nước **không che** hàng xóm ⇒ khối đá dưới đáy hồ vẫn vẽ mặt trên. Kết hợp với luật `block == source` trong [`ChunkMesher.occludes`](ChunkMesher.md), hai ô nước kề nhau vẫn không vẽ mặt chung.

---

## 4. So sánh ba hình dạng

| | `CubeGeometry` | `CrossGeometry` | `LiquidGeometry` |
|---|---|---|---|
| Số quad | 0–6 | **4** (2 billboard × 2 mặt) | 0–7 |
| Chiếu sáng | `shade` (có AO) | `flat` (không AO) | `shade` (có AO) |
| `occludesNeighbours` | tuỳ thể hiện | `false` | `false` |
| Mesh đích | `solid` | `solid` (lượt 2) | **`fluid`** (trong suốt) |
| Đặc biệt | — | jitter theo hash | độ cao theo mức, `SKIN` |

---

## 5. Độ phức tạp

| | Chi phí mỗi khối |
|---|---|
| `CubeGeometry` | `O(6)` lần `occludes` + `O(mặt sinh ra × 44)` cho AO |
| `CrossGeometry` | `O(1)` — 2 lần hash + 1 lần `flat` + 4 quad |
| `LiquidGeometry` | `O(4)` lần `needsSide` + `O(mặt × 44)` |

`CrossGeometry` **rẻ nhất** dù sinh 4 quad cố định — vì bỏ qua toàn bộ AO.

---

## 6. Chủ đề DSA / Pattern thể hiện

- **Strategy** — `BlockGeometry` interface, 3 cài đặt.
- **Flyweight** — singleton dùng chung, trạng thái truyền qua tham số.
- **`ThreadLocal`** — an toàn đa luồng không khoá, vẫn tránh cấp phát.
- **Spatial hashing** (Teschner) cho jitter.
- **Chống z-fighting** bằng offset epsilon.
- **Nội suy tuyến tính** — độ cao mặt nước theo mức.

---

## 7. Liên kết

- Sinh quad: [ChunkMesher.md](ChunkMesher.md)
- Đổ bóng: [FaceLighting.md](FaceLighting.md)
- Mức nước: [FluidSimulator.md](../05-world/FluidSimulator.md)
- Hệ trục mặt: [Direction.md](../06-datastructures/Direction.md)
