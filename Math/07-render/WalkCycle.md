# WalkCycle — nhịp đi bộ theo quãng đường

**File:** `core/src/com/voxel/engine/render/WalkCycle.java`

Cả ba loại nhân vật (người chơi, quái vật, người chơi khác) dùng chung lớp này ⇒ cử động y hệt nhau.

---

## 1. Nguyên tắc cốt lõi: pha theo QUÃNG ĐƯỜNG, không theo thời gian

```java
phase += distance * STRIDE;         // STRIDE = 2
```

```
  φ(s) = φ₀ + 2·s          (s = quãng đường ngang đã đi, khối)
```

### Vì sao không dùng thời gian?

| | `phase += delta × k` (thời gian) | `phase += distance × k` (quãng đường) |
|---|---|---|
| Đi nhanh | vung **cùng tốc độ** ✗ | vung **nhanh hơn** ✔ |
| Đi chậm | vung cùng tốc độ ✗ | vung chậm hơn ✔ |
| Đứng yên | **vẫn vung** ✗ | **đứng im** ✔ |
| Trượt băng | chân vung khi không di chuyển ✗ | chân đứng yên ✔ |

Đây chính là cách Minecraft làm, và là lý do nhân vật không bị hiện tượng "chân trượt" (foot sliding).

### `STRIDE = 2` — bước chân dài bao nhiêu?

Một chu kỳ đầy đủ của `sin` là `2π` radian. Chân trái đi từ trước ra sau rồi về chỗ cũ:

```
              2π       2π
  λ_chu kỳ = ────  =  ──── ≈ 3.14 khối
             STRIDE     2
```

Một **bước chân** (nửa chu kỳ) ≈ **1.57 khối**. Comment ghi "~1.6 khối" — khớp.

---

## 2. Biên độ — bộ lọc thông thấp theo tốc độ

```java
private static final float FULL_SPEED = 4.3f;
private static final float BLEND = 8f;

if (delta > 0f) {
    float speed = distance / delta;
    float target = Math.min(1f, speed / FULL_SPEED);
    amount += (target - amount) * Math.min(1f, delta * BLEND);
}
```

### Công thức

```
  v = s / Δt                            (tốc độ tức thời)
  A* = min(1, v / 4.3)                  (biên độ mục tiêu)
  A ← A + (A* − A) · min(1, 8Δt)        (lerp hàm mũ)
```

### Bộ lọc thông thấp bậc 1

Đây là **exponential moving average** — cùng dạng với `climb` của [PerlinWormCarver](../03-caves/PerlinWormCarver.md).

Hằng số thời gian:

```
        1        1
  τ =  ────  =  ───  = 0.125 giây
       BLEND     8
```

⇒ Biên độ đạt ~63 % giá trị mục tiêu sau 0.125 s, ~95 % sau 0.375 s.

> Comment: *"Biên độ dâu lên / tắt đi trong khoảng 1/8 giây cho mượt."*

### Vì sao cần làm mượt?

Không có nó, khi người chơi thả phím thì `distance = 0` ⇒ `A` nhảy về 0 **tức thì** ⇒ tay chân **giật cứng** về vị trí thẳng. Với bộ lọc, chúng thu về từ từ trong ~1/8 giây.

### `min(1, delta × BLEND)` — chặn khi khung hình giật

Nếu `Δt > 0.125` s (dưới 8 FPS), `8Δt > 1` ⇒ hệ số lerp vượt 1 ⇒ **vọt lố** (overshoot) và dao động. Kẹp tại 1 khiến trường hợp xấu nhất là gán thẳng `A = A*`, vẫn ổn định.

### `FULL_SPEED = 4.3`

Tốc độ chạy của người chơi. Đi bộ chậm hơn ⇒ `A < 1` ⇒ biên độ vung nhỏ hơn. Đây là **tỉ lệ tuyến tính** giữa tốc độ và biên độ, đúng trực giác vật lý.

---

## 3. Góc vung chân

```java
private static final float SWING_ANGLE = 60f;

public float legAngle() {
    return (float) Math.sin(phase) * SWING_ANGLE * amount;
}
```

```
  θ_chân trái(s) = sin(2s) · 60° · A
```

**Chân phải và hai tay lấy ngược dấu** ⇒ lệch pha `180°`:

```
  θ_chân phải = −θ_chân trái
```

Khi chân trái đưa ra trước (`+60°`), chân phải đưa ra sau (`−60°`) ⇒ **hai chân xoạc nhau 120°** lúc chạy hết biên độ. Comment xác nhận con số này.

Tay chéo với chân (tay trái theo chân phải) — dáng đi tự nhiên của người.

### Vì sao hàm sin?

- **Tuần hoàn** — chu kỳ khép kín, không có điểm gãy.
- **Trơn (C∞)** — vận tốc góc `θ' = 2A·60·cos(2s)` cũng liên tục ⇒ không giật.
- **Vận tốc bằng 0 ở hai đầu** — chân dừng lại tự nhiên ở điểm cực trước/cực sau, đúng như chuyển động con lắc.

Đây thực chất là mô hình **con lắc đơn** biên độ nhỏ: `θ(t) = θ₀·sin(ωt)`.

---

## 4. Quơ tay khi đánh

```java
private static final float PUNCH_ANGLE = 70f;
private static final float ARM_DECAY = 4.5f;

public void swingArm() { armSwing = 1f; }

// trong update():
armSwing = Math.max(0f, armSwing - delta * ARM_DECAY);

public float punchAngle() {
    return (float) Math.sin(armSwing * Math.PI) * PUNCH_ANGLE;
}
```

### Suy giảm tuyến tính

```
  a(t) = max(0, 1 − 4.5·t)
```

Thời gian tắt hẳn:

```
        1
  T =  ───  ≈ 0.222 giây
       4.5
```

Comment: *"Cứ quơ tay tắt dần trong ~0.22 giây (1 / 4.5)."* ✔

### Đường cong cú đánh

```
  θ_đấm = sin(π·a) · 70°
```

| `a` | thời gian | `sin(πa)` | Góc |
|---|---|---|---|
| 1.0 | 0.000 s | 0.00 | **0°** ← bắt đầu ở vị trí nghỉ |
| 0.75 | 0.056 s | 0.71 | 49.5° |
| **0.5** | **0.111 s** | **1.00** | **70°** ← đỉnh cú đánh |
| 0.25 | 0.167 s | 0.71 | 49.5° |
| 0.0 | 0.222 s | 0.00 | **0°** ← về chỗ |

### Vì sao `sin(π·a)` chứ không phải `a` trực tiếp?

Dùng `a` trực tiếp: tay **nhảy ngay ra 70°** rồi thu về từ từ ⇒ giật cục.

Dùng `sin(π·a)`: tay **vươn ra rồi thu về** thành một cung trọn vẹn. `sin(πa)` triệt tiêu ở cả hai đầu (`a = 0` và `a = 1`) ⇒ chuyển động khép kín, mượt hai đầu.

Đây là **hàm bao hình sin** (sine envelope) — cùng kỹ thuật với `taper` của [PerlinWormCarver](../03-caves/PerlinWormCarver.md).

`ARM_DECAY = 4.5` chọn để cú đánh kéo dài 0.22 s — khớp với nhịp tấn công của game.

---

## 5. `idleSway` — cử động "thở"

```java
public float idleSway() {
    return (float) (Math.sin(time * 1.13) * 2.2 + Math.sin(time * 1.91) * 0.8);
}
```

### Công thức

```
  sway(t) = 2.2·sin(1.13·t) + 0.8·sin(1.91·t)        (độ)
```

Biên độ tối đa `2.2 + 0.8 = 3.0°` — rất nhẹ, chỉ đủ để nhân vật đứng yên vẫn "sống".

### Vì sao hai tần số?

```
  f₁ = 1.13 rad/s  →  T₁ = 2π/1.13 ≈ 5.56 s
  f₂ = 1.91 rad/s  →  T₂ = 2π/1.91 ≈ 3.29 s
```

**Tỉ số tần số:**

```
  f₂ / f₁ = 1.91 / 1.13 = 1.6903...
```

Đây là số **vô tỉ** (không phải phân số đơn giản) ⇒ tổng hai sóng là hàm **tựa tuần hoàn** (quasi-periodic), **không bao giờ lặp lại chính xác**.

> Comment: *"Hai tần số lệch nhau chút xíu nên cử động không bao giờ lặp lại y hệt (học Minecraft)."*

Nếu tỉ số là số hữu tỉ đơn giản, ví dụ `f₂ = 2f₁`, chu kỳ chung sẽ là `T₁ = 5.56` s và mắt sẽ nhận ra mẫu lặp.

**So sánh:** đây là phiên bản đơn giản của **tổng hợp cộng** (additive synthesis) trong âm thanh — chồng nhiều sóng sin với tần số không cộng hưởng để tạo âm thanh/chuyển động tự nhiên.

Biên độ `2.2 : 0.8` (tỉ lệ ~2.75:1) khiến sóng chậm chiếm ưu thế, sóng nhanh chỉ thêm chút biến động.

---

## 6. Bảng hằng số

| Hằng | Giá trị | Đơn vị | Ý nghĩa |
|---|---|---|---|
| `SWING_ANGLE` | 60 | độ | Biên độ vung chân tối đa |
| `PUNCH_ANGLE` | 70 | độ | Biên độ quơ tay khi đánh |
| `STRIDE` | 2 | rad/khối | Bước chân ≈ 1.57 khối |
| `FULL_SPEED` | 4.3 | khối/s | Tốc độ đạt biên độ tối đa |
| `BLEND` | 8 | 1/s | `τ = 0.125` s |
| `ARM_DECAY` | 4.5 | 1/s | Cú đánh dài 0.222 s |
| `idleSway` tần số | 1.13, 1.91 | rad/s | Tỉ số vô tỉ ⇒ không lặp |
| `idleSway` biên độ | 2.2, 0.8 | độ | Tổng 3° |

---

## 7. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `update` | `O(1)` — 1 chia, 2 lerp |
| `legAngle` | `O(1)` — 1 `sin` |
| `punchAngle` | `O(1)` — 1 `sin` |
| `idleSway` | `O(1)` — 2 `sin` |
| Bộ nhớ | `O(1)` — 4 trường `float` = 16 byte |

Toàn bộ hệ thống hoạt hình của một nhân vật gói trong **16 byte trạng thái**.

---

## 8. Chủ đề Toán thể hiện

- **Tham số hoá theo quãng đường** thay vì thời gian — chống trượt chân.
- **Bộ lọc thông thấp bậc 1** (EMA) & hằng số thời gian `τ = 1/k`.
- **Chuyển động điều hoà đơn** (mô hình con lắc) cho vung chân.
- **Hàm bao hình sin** `sin(πa)` cho chuyển động khép kín.
- **Suy giảm tuyến tính** với chặn dưới.
- **Tổng hợp cộng & tỉ số tần số vô tỉ** ⇒ chuyển động tựa tuần hoàn không lặp.
- **Kẹp hệ số lerp** để ổn định khi khung hình giật.

---

## 9. Liên kết

- Dựng khung xương: [HumanoidFigure.md](HumanoidFigure.md)
- Người dùng: `PlayerModel`, `MonsterRenderer`, `RemotePlayerRenderer`
- Tốc độ di chuyển: [PlayerBody.md](../08-physics/PlayerBody.md)
