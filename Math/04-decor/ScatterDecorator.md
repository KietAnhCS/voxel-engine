# ScatterDecorator — rắc cỏ, hoa, bụi theo vạt

**File:** `core/src/com/voxel/game/terrain/decor/ScatterDecorator.java`

---

## 1. Điều kiện đặt khối

```java
public boolean decorate(DecorationContext context) {
    if (context.random(salt) >= chance * density(context)) return false;   // (1)
    int y = context.surfaceY() + 1;
    if (!context.blockAt(0, y, 0).isAir()) return false;                   // (2)
    if (ground != null) {
        Block below = context.blockAt(0, y - 1, 0);
        if (below != ground.select(context.blocks())) return false;        // (3)
    }
    context.place(0, y, 0, selector.select(context.blocks()));
    return true;
}
```

| # | Điều kiện | Ý nghĩa |
|---|---|---|
| 1 | `u < chance × density` | Xác suất, có điều biến theo vạt |
| 2 | Ô đặt phải trống | Không đè lên cây/công trình |
| 3 | Khối nền đúng loại (nếu yêu cầu) | Bụi khô chỉ mọc trên cát |

Trả `true` = "tôi đã nhận việc" ⇒ chuỗi `Decorator` dừng lại (xem [Biome.md](../01-noise-terrain/Biome.md) §2).

---

## 2. Điều biến xác suất — hàm `density`

```java
private float density(DecorationContext context) {
    if (patchSize <= 0) return 1f;                              // rắc đều
    float noise  = context.patch(salt, patchSize);              // ∈ [0, 1)
    float scaled = (noise - PATCH_MIN) / (PATCH_MAX - PATCH_MIN);
    return Math.max(0f, Math.min(1f, scaled));                  // clamp
}
```

### Công thức

Với `PATCH_MIN = 0.45`, `PATCH_MAX = 0.78`:

```
                    ⎛  P(x,z) − 0.45  ⎞
  density(x,z) = clamp⎜ ─────────────── , 0, 1 ⎟
                    ⎝  0.78 − 0.45    ⎠

                    ⎛ P − 0.45 ⎞
               = clamp⎜ ──────── ⎟
                    ⎝   0.33   ⎠
```

trong đó `P = patch(salt, patchSize)` là trường **nội suy song tuyến tính mượt** — xem [DecorationContext.md](DecorationContext.md).

### Bảng giá trị

| `P` | `scaled` | `density` | Cảnh quan |
|---|---|---|---|
| 0.00 – 0.45 | ≤ 0 | **0.00** | Đất trống hoàn toàn |
| 0.50 | 0.152 | 0.15 | Cỏ thưa |
| 0.60 | 0.455 | 0.45 | Cỏ vừa |
| 0.70 | 0.758 | 0.76 | Cỏ dày |
| ≥ 0.78 | ≥ 1 | **1.00** | Giữa vạt, dày nhất |

### Đây là hàm gì?

Một **hàm dốc bị kẹp** (clamped ramp) — hay còn gọi là **hard sigmoid** / hàm kích hoạt tuyến tính bão hoà:

```
       density
         1 ┤        ┌────────────
           │       ╱
       0.5 ┤      ╱
           │     ╱
         0 ┼────┘─────────────────  P
           0   0.45  0.78    1
```

**Tác dụng của việc kẹp dưới:** vì `P` là giá trị nội suy 4 góc ngẫu nhiên đều, phân phối của nó tập trung quanh 0.5 (định lý giới hạn trung tâm nhẹ). Cắt ở 0.45 khiến **khoảng 45 % diện tích** rơi vào `density = 0` — đó chính là **những khoảng trống trần trụi** giữa các vạt cỏ.

> Comment trong code: *"most of the map falls to 0 — those are the bare gaps"*.

**Tác dụng của việc kẹp trên:** ở giữa vạt, `density` bão hoà tại 1.0 ⇒ mật độ đúng bằng `chance` khai báo, không vượt quá.

---

## 3. Xác suất hiệu dụng

```
  P(đặt tại (x,z)) = chance × density(x,z)
```

Với `PlainsBiome`: `new ScatterDecorator(0.62f, 3, s -> s.tuft).inPatches(30)`

| Vùng | `density` | Xác suất thực |
|---|---|---|
| Trống | 0.00 | **0 %** |
| Rìa vạt | 0.30 | 18.6 % |
| Giữa vạt | 1.00 | **62 %** |

Mật độ **trung bình** trên toàn bản đồ thấp hơn 62 % nhiều — ước lượng `E[density] ≈ 0.28` ⇒ độ phủ thực tế `≈ 0.62 × 0.28 ≈ 17 %`. Nhưng phân bố **không đều**: nơi thì dày đặc, nơi thì trống trơn — đúng như cỏ thật.

---

## 4. Ràng buộc khối nền — Strategy qua lambda

```java
new ScatterDecorator(0.05f, 12, source -> source.deadBush, source -> source.sand)
                                 ↑ khối đặt              ↑ khối nền bắt buộc
```

`BlockSelector` là **functional interface**: `Blocks → Block`. Dùng lambda để trì hoãn việc chọn khối tới lúc chạy (lúc khai báo biome, `Blocks` đã có sẵn nhưng cách này giữ decorator độc lập với registry cụ thể).

`ground == null` ⇒ đặt ở đâu cũng được. `ground != null` ⇒ so sánh **tham chiếu** (`!=`) vì `Blocks` giữ các singleton bất biến — nhanh hơn `equals`.

Ứng dụng: bụi khô (`deadBush`) chỉ mọc trên **cát**, không mọc trên sa thạch lộ thiên.

---

## 5. Builder pattern nhẹ

```java
public ScatterDecorator inPatches(int cellSize) {
    this.patchSize = cellSize;
    return this;              // trả về this → nối chuỗi
}
```

```java
new ScatterDecorator(0.62f, 3, source -> source.tuft).inPatches(30)
```

`patchSize` mặc định `0` ⇒ `density()` trả `1f` ⇒ rắc đều. Hoa (`flower`, `flowerYellow`) không gọi `inPatches` — chúng rải đều với `chance` rất thấp (0.015–0.02), đúng kiểu hoa dại mọc lẻ tẻ.

---

## 6. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `PATCH_MIN` | 0.45 | Dưới ngưỡng này = đất trống |
| `PATCH_MAX` | 0.78 | Trên ngưỡng này = giữa vạt |
| Dải hoạt động | 0.33 | `PATCH_MAX − PATCH_MIN` |
| `patchSize` mặc định | 0 | Rắc đều, không vạt |

---

## 7. Độ phức tạp

| | |
|---|---|
| Thời gian | `O(1)` — 1 lần băm + 4 lần băm (nếu có vạt) + 2 lần đọc khối |
| Bộ nhớ | `O(1)` |
| Mỗi chunk | `256 × (số decorator)` lần gọi, tối đa ~4 ⇒ ~1 024 lần |

---

## 8. Chủ đề DSA / Toán thể hiện

- **Điều biến xác suất** bằng trường liên tục (probability modulation).
- **Hàm dốc bị kẹp** (clamped ramp / hard sigmoid).
- **Strategy pattern** qua functional interface + lambda.
- **Builder pattern** (`inPatches` trả `this`).
- **Chain of Responsibility** (trả `true`/`false`).

---

## 9. Liên kết

- Trường mật độ: [DecorationContext.md](DecorationContext.md)
- Chuỗi decorator & xác suất tích luỹ: [Biome.md](../01-noise-terrain/Biome.md)
- Anh em: [TreeShapes.md](TreeShapes.md)
