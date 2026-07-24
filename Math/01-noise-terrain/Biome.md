# Biome (lớp trừu tượng)

**File:** `core/src/com/voxel/game/terrain/biome/Biome.java`
**Design pattern:** **Template Method** + **Strategy**

---

## 1. Khung chung

```java
public abstract class Biome {
    public abstract double surfaceHeight(TerrainNoise noise, int x, int z);  // BẮT BUỘC
    public Block topBlock(int y, int seaLevel)    { return blocks.grass; }   // tuỳ chọn
    public Block fillerBlock(int y, int seaLevel) { return blocks.dirt;  }   // tuỳ chọn
    public final void decorate(DecorationContext ctx) { ... }                // KHÓA
}
```

- `surfaceHeight` — **Strategy**: mỗi biome là một công thức toán khác nhau, `BiomeSource.blendedHeight` gọi đa hình.
- `decorate` — **Template Method** đã `final`: khung chạy cố định, lớp con chỉ nạp danh sách `Decorator`.

## 2. Chuỗi trách nhiệm khi trang trí

```java
public final void decorate(DecorationContext context) {
    for (Decorator decorator : decorators) {
        if (decorator.decorate(context)) {
            return;               // ai nhận việc trước thì dừng cả chuỗi
        }
    }
}
```

Đây là **Chain of Responsibility**. Hệ quả toán học: nếu decorator thứ `k` có xác suất đặt vật thể là `p_k`, thì xác suất **thực tế** decorator thứ `n` được chạy là

```
                n−1
               ┌───┐
  P(chạy n) =  │   │ (1 − p_k)
               k=0
```

và xác suất nó đặt được vật thể là

```
                       n−1
                      ┌───┐
  P(đặt n) = p_n  ·   │   │ (1 − p_k)
                      k=0
```

**Ví dụ ForestBiome** — thứ tự `[oak 0.045, birch 0.018, flower 0.02, tuft 0.55]`:

| Decorator | `p` khai báo | Xác suất thực tế |
|---|---|---|
| Sồi | 0.045 | 0.045 |
| Bạch dương | 0.018 | `0.018 × 0.955` = 0.0172 |
| Hoa | 0.020 | `0.020 × 0.955 × 0.982` = 0.0188 |
| Cỏ | 0.550 | `0.550 × 0.955 × 0.982 × 0.980` = 0.505 |

⇒ Thứ tự khai báo là **thứ tự ưu tiên**: cây được ưu tiên hơn cỏ, nên gốc cây không bao giờ bị cỏ chiếm chỗ. Xác suất khai báo luôn **hơi cao hơn** xác suất thực tế.

## 3. Liên kết

- Bảng so sánh 12 công thức độ cao: [Biome-heights.md](Biome-heights.md)
- Người gọi: [BiomeSource.md](BiomeSource.md)
- Trang trí: [ScatterDecorator.md](../04-decor/ScatterDecorator.md)
