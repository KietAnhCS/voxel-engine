# DayNightCycle — chu kỳ ngày đêm

**File:** `core/src/com/voxel/engine/render/DayNightCycle.java`

Mọi thứ (màu trời, độ sáng, hướng mặt trời, quái vật spawn) đều suy ra từ **một biến duy nhất**: `time ∈ [0, 1)`.

---

## 1. Đồng hồ

```java
private static final float DAY_LENGTH = 600f;      // 10 phút thực
private static final float START_TIME = 0.06f;

public void advance(float delta) {
    time = (time + delta / DAY_LENGTH) % 1f;
    recompute();
}
```

```
  t(τ) = (t₀ + τ / 600) mod 1
```

Phép `% 1f` khiến `time` **quấn vòng** — đồng hồ chạy vô tận mà không tràn số.

| `time` | Thời điểm |
|---|---|
| 0.00 | Bình minh |
| 0.25 | Giữa trưa |
| 0.50 | Hoàng hôn |
| 0.75 | Nửa đêm |

`START_TIME = 0.06` — bắt đầu ván chơi vào sáng sớm (khoảng 7:26) cho dễ nhìn.

### `setTime` — xử lý số âm

```java
public void setTime(float value) {
    time = ((value % 1f) + 1f) % 1f;
    recompute();
}
```

`%` trong Java trả về **số âm** cho toán hạng âm: `−0.3 % 1 = −0.3`. Công thức `((v % 1) + 1) % 1` đưa mọi giá trị về `[0, 1)`:

```
  v = −0.3  →  (−0.3 + 1) % 1 = 0.7   ✔
  v =  2.3  →  ( 0.3 + 1) % 1 = 0.3   ✔
```

Đây là **chuẩn hoá modulo dương** — mẫu chuẩn cho mọi phép quấn vòng.

---

## 2. Vị trí mặt trời — chuyển động tròn

```java
private void recompute() {
    double angle = time * Math.PI * 2.0;
    sunDirection.set((float) Math.cos(angle), (float) Math.sin(angle), 0f);
    mixSky(skyColor, sunDirection.y);
}
```

### Công thức

```
  θ = 2π · t
  ŝ = (cos θ, sin θ, 0)
```

Vector đơn vị quay trong **mặt phẳng XY** (trục Z = 0). Chuẩn hoá sẵn vì `cos²θ + sin²θ = 1`.

### Vị trí theo thời gian

| `t` | `θ` | `ŝ = (x, y)` | Ý nghĩa |
|---|---|---|---|
| 0.00 | 0 | `(1, 0)` | Mọc ở **Đông** (+X), sát chân trời |
| 0.25 | π/2 | `(0, 1)` | **Đỉnh đầu** — giữa trưa |
| 0.50 | π | `(−1, 0)` | Lặn ở **Tây** (−X) |
| 0.75 | 3π/2 | `(0, −1)` | **Dưới chân thế giới** — nửa đêm |

**Mặt trăng** đi hướng ngược lại: `m̂ = −ŝ` (xem [SkyRenderer](SkyRenderer.md)) ⇒ mặt trời lặn thì mặt trăng mọc, luôn đối đỉnh.

### Đại lượng then chốt: `sunDirection.y`

```
  h = sin(2πt)  ∈ [−1, 1]
```

**Độ cao mặt trời** — mọi thứ khác đều là hàm của `h`:

| `h` | Trạng thái |
|---|---|
| `> 0.35` | Giữa ban ngày |
| `≈ 0` | Bình minh / hoàng hôn |
| `< −0.2` | Ban đêm |

---

## 3. Độ sáng — hàm dốc bị kẹp

```java
private static final float FULL_DAY   = 1f;
private static final float FULL_NIGHT = 0.16f;
private static final float DAWN_BAND  = 0.35f;
private static final float DUSK_BAND  = 0.2f;

private static float brightnessAt(float sunHeight) {
    if (sunHeight >= DAWN_BAND)   return FULL_DAY;
    if (sunHeight <= -DUSK_BAND)  return FULL_NIGHT;
    float t = (sunHeight + DUSK_BAND) / (DAWN_BAND + DUSK_BAND);
    return FULL_NIGHT + (FULL_DAY - FULL_NIGHT) * t;
}
```

### Công thức

```
            ⎧ 1.00                            nếu h ≥ 0.35
            ⎪
  D(h) =    ⎨ 0.16 + 0.84 · (h + 0.2)/0.55    nếu −0.2 < h < 0.35
            ⎪
            ⎩ 0.16                            nếu h ≤ −0.2
```

Đây là **hàm dốc bị kẹp** (clamped ramp) — cùng dạng với `density` của [ScatterDecorator](../04-decor/ScatterDecorator.md).

```
     D(h)
   1.00 ┤          ┌─────────
        │         ╱
        │        ╱
   0.16 ┼───────┘
        └───┬────┬─────┬──── h
          −0.2   0    0.35
```

### Vì sao dải bình minh (0.35) rộng hơn dải hoàng hôn (0.2)?

**Bất đối xứng có chủ ý.** Vùng chuyển tiếp trải từ `h = −0.2` tới `h = 0.35`, tức lệch về phía **trên** đường chân trời.

Hệ quả: trời **sáng nhanh** sau khi mặt trời mọc (chỉ cần lên tới `h = 0.35`) nhưng **tối chậm** sau khi lặn (phải xuống tới `h = −0.2`) — mô phỏng **hoàng hôn kéo dài** (twilight) của khí quyển thật.

### Chuyển đổi sang thời gian thực

`h = sin(2πt)`, dải chuyển tiếp `h ∈ (−0.2, 0.35)`:

```
  h = 0.35  →  t = arcsin(0.35)/(2π) ≈ 0.0568
  h = −0.2  →  t ≈ 0.5321  (sau hoàng hôn)
```

Với `DAY_LENGTH = 600` s:

```
  Bình minh:  0.0568 × 600 ≈ 34 giây
  Hoàng hôn:  (0.5321 − 0.5) × 600 ≈ 19 giây
```

### `FULL_NIGHT = 0.16`

Ban đêm không đen kịt — giữ 16 % để nhìn thấy đường đi dưới ánh trăng. Cùng triết lý với sàn `0.055` của [LIGHT_CURVE](FaceLighting.md).

**Lưu ý quan trọng:** `daylight()` chỉ nhân vào **phần ánh sáng trời** (`skyShare`), không chạm tới quầng đuốc — xem [Shaders §1](Shaders.md).

---

## 4. Màu trời — nội suy 3 màu

```java
private static final Color DAY_SKY    = new Color(0.44f, 0.66f, 0.94f, 1f);   // xanh
private static final Color SUNSET_SKY = new Color(0.95f, 0.48f, 0.26f, 1f);   // đỏ cam
private static final Color NIGHT_SKY  = new Color(0.03f, 0.04f, 0.10f, 1f);   // xanh thẫm

private static void mixSky(Color out, float sunHeight) {
    if (sunHeight >= DAWN_BAND)       out.set(DAY_SKY);
    else if (sunHeight >= 0f)         out.set(SUNSET_SKY).lerp(DAY_SKY, sunHeight / DAWN_BAND);
    else if (sunHeight >= -DUSK_BAND) out.set(NIGHT_SKY).lerp(SUNSET_SKY, 1f + sunHeight / DUSK_BAND);
    else                              out.set(NIGHT_SKY);
}
```

### Bốn khoảng

```
            ⎧ DAY                                          h ≥ 0.35
            ⎪ lerp(SUNSET, DAY,   h / 0.35)                0 ≤ h < 0.35
  C(h) =    ⎨
            ⎪ lerp(NIGHT,  SUNSET, 1 + h / 0.2)           −0.2 ≤ h < 0
            ⎩ NIGHT                                        h < −0.2
```

### Kiểm tra tính liên tục

| Điểm nối | Từ trái | Từ phải | Khớp? |
|---|---|---|---|
| `h = 0.35` | `lerp(SUNSET, DAY, 1)` = DAY | `DAY` | ✔ |
| `h = 0` | `lerp(NIGHT, SUNSET, 1)` = SUNSET | `lerp(SUNSET, DAY, 0)` = SUNSET | ✔ |
| `h = −0.2` | `NIGHT` | `lerp(NIGHT, SUNSET, 0)` = NIGHT | ✔ |

Hàm **liên tục** tại mọi điểm nối ⇒ màu trời chuyển mượt, không nhảy màu đột ngột.

### Bảng màu theo thời gian

| `t` | Giờ | `h` | Màu trời |
|---|---|---|---|
| 0.00 | 06:00 | 0.00 | **Đỏ cam** thuần |
| 0.03 | 06:43 | 0.19 | Cam pha xanh 54 % |
| 0.06 | 07:26 | 0.36 | **Xanh** hoàn toàn |
| 0.25 | 12:00 | 1.00 | Xanh |
| 0.50 | 18:00 | 0.00 | **Đỏ cam** thuần |
| 0.53 | 18:43 | −0.19 | Cam pha thẫm 95 % |
| 0.75 | 00:00 | −1.00 | **Xanh thẫm** |

### Vì sao bình minh và hoàng hôn cùng màu?

> Comment: *"bình minh và hoàng hôn dùng chung một dải màu vì mặt trời ở cùng độ cao"*

Hàm chỉ nhận `h = sin(2πt)`. Vì `sin(2π·0.03) = sin(2π·0.47)`, hai thời điểm khác nhau cho cùng độ cao ⇒ cùng màu. Đây là hệ quả của **tính đối xứng của hàm sin** quanh `t = 0.25` và `t = 0.75` — và đúng về mặt vật lý: màu trời phụ thuộc quãng đường ánh sáng đi qua khí quyển, tức phụ thuộc góc mặt trời.

Cùng đúng cho `daylight()`: `D(h)` là hàm chỉ của `h`.

---

## 5. `clockLabel` — đồng hồ hiển thị

```java
public String clockLabel() {
    float hours = (time * 24f + 6f) % 24f;
    return String.format("%02d:%02d", (int) hours, (int) ((hours - (int) hours) * 60f));
}
```

```
  H = (24t + 6) mod 24
  giờ  = ⌊H⌋
  phút = ⌊(H − ⌊H⌋) × 60⌋
```

`+6` vì quy ước `time = 0` là bình minh = **6 giờ sáng**.

| `time` | `H` | Hiển thị |
|---|---|---|
| 0.00 | 6.0 | `06:00` |
| 0.25 | 12.0 | `12:00` |
| 0.50 | 18.0 | `18:00` |
| 0.75 | 0.0 | `00:00` |

Phần phân số nhân 60 để đổi ra phút — cùng nguyên tắc với việc đổi độ thập phân sang độ-phút-giây.

---

## 6. `isNight` — điều kiện quái vật

```java
public boolean isNight() {
    return sunDirection.y < -0.05f;
}
```

```
  đêm  ⟺  sin(2πt) < −0.05
       ⟺  t ∈ (0.508, 0.992)
```

Kéo dài `(0.992 − 0.508) × 600 ≈ 290` giây ≈ **4.8 phút** thực, tức **48.4 %** thời gian.

Ngưỡng `−0.05` (chứ không phải 0) cho người chơi một khoảng ân hạn ngắn sau hoàng hôn trước khi quái vật xuất hiện. Xem [MonsterManager](../09-ai/MonsterManager.md).

---

## 7. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `DAY_LENGTH` | 600 s | Một ngày đêm = 10 phút (Minecraft: 20 phút) |
| `START_TIME` | 0.06 | Bắt đầu lúc ~07:26 |
| `FULL_DAY` | 1.00 | Độ sáng giữa trưa |
| `FULL_NIGHT` | 0.16 | Độ sáng nửa đêm |
| `DAWN_BAND` | 0.35 | Trên mức này là ban ngày hẳn |
| `DUSK_BAND` | 0.20 | Dưới `−0.2` là đêm hẳn |
| Ngưỡng `isNight` | −0.05 | Quái vật bắt đầu spawn |

---

## 8. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `advance` | `O(1)` — 1 `sin` + 1 `cos` + trộn màu, **mỗi khung hình một lần** |
| `daylight`, `skyColor`, `isNight` | `O(1)` — đọc trường đã tính sẵn |
| Bộ nhớ | `O(1)` — 1 `Vector3` + 1 `Color` |

Tất cả tính **một lần** trong `recompute()`, các getter chỉ đọc — tránh gọi `sin`/`cos` nhiều lần mỗi khung hình.

---

## 9. Chủ đề Toán thể hiện

- **Chuyển động tròn đều** & toạ độ cực → Descartes.
- **Chuẩn hoá modulo dương** `((v % 1) + 1) % 1`.
- **Hàm dốc bị kẹp** cho độ sáng.
- **Nội suy tuyến tính nhiều đoạn** (piecewise lerp) & kiểm tra liên tục.
- **Bất đối xứng có chủ ý** để mô phỏng hoàng hôn kéo dài.
- **Tính chẵn/đối xứng của hàm sin** ⇒ bình minh = hoàng hôn.
- **Tính sẵn (precompute)** thay vì tính lại trong getter.

---

## 10. Liên kết

- Vẽ mặt trời/trăng: [SkyRenderer.md](SkyRenderer.md)
- Áp `daylight` vào thế giới: [Shaders.md](Shaders.md), [FaceLighting.md](FaceLighting.md)
- Pha màu mưa: [WeatherSystem.md](WeatherSystem.md)
- Quái vật spawn ban đêm: [MonsterManager.md](../09-ai/MonsterManager.md)
