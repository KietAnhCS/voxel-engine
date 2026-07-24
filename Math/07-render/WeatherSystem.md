# WeatherSystem — một số điều khiển cả bầu trời

**File:** `core/src/com/voxel/engine/render/WeatherSystem.java`

Thời tiết chỉ có hai trạng thái (CLEAR / RAIN), nhưng chuyển đổi **không bao giờ tức thì**: một biến `rain ∈ [0, 1]` trượt dần và **mọi hiệu ứng hình ảnh đều đọc từ nó**.

---

## 1. Chuyển tiếp tuyến tính

```java
private static final float TRANSITION_TIME = 4f;

public void update(float delta) {
    float target = raining ? 1f : 0f;
    float step = delta / TRANSITION_TIME;
    if      (rain < target) rain = Math.min(target, rain + step);
    else if (rain > target) rain = Math.max(target, rain - step);
}
```

### Công thức

```
  rain(t + Δt) = clamp( rain(t) ± Δt/4 ,  hướng về target )
```

Đây là **nội suy với tốc độ không đổi** (constant-rate lerp), khác với lerp hàm mũ (`x += (target − x) × k`):

| | Tốc độ không đổi | Lerp hàm mũ |
|---|---|---|
| Thời gian tới đích | **Chính xác 4 giây** | Tiệm cận, không bao giờ tới |
| Tốc độ đầu | như cuối | nhanh rồi chậm dần |
| Cần `clamp`? | có (`min`/`max`) | không |

Chọn tốc độ không đổi để **thời gian chuyển tiếp xác định** — 4 giây là đủ để người chơi cảm nhận trời đang đổi mà không phải chờ lâu.

`Math.min`/`Math.max` chặn vượt quá đích khi `Δt` lớn (khung hình giật).

---

## 2. Ba hàm điều biến

### 2.1 Màu trời

```java
private static final Color RAIN_SKY = new Color(0.72f, 0.75f, 0.79f, 1f);

public Color blendSky(Color daySky) {
    return blendedSky.set(daySky).lerp(RAIN_SKY, rain * 0.75f);
}
```

```
  C = lerp( C_ngày , (0.72, 0.75, 0.79) , 0.75·rain )
```

**Hệ số `0.75` (không phải 1.0):** ngay cả khi mưa to nhất, màu trời vẫn giữ **25 %** màu gốc của [DayNightCycle](DayNightCycle.md) ⇒ mưa ban đêm vẫn tối, mưa lúc hoàng hôn vẫn ám cam. Nếu dùng 1.0, mọi lúc mưa đều xám như nhau — mất hết chu kỳ ngày đêm.

> Comment: *"một màu xám-TRẮNG SÁNG, để lớp sương mù mưa dày hơn trông như màn sương trắng cuộn tới chứ không phải thế giới tối sầm lại."*

Màu `(0.72, 0.75, 0.79)` sáng hơn xám trung tính (0.5) — sương mưa đục chứ không tối.

### 2.2 Độ sáng

```java
public float daylightFactor() {
    return 1f - 0.18f * rain;
}
```

```
  D_factor(rain) = 1 − 0.18·rain    ∈ [0.82, 1.00]
```

Mưa chỉ làm tối **18 %** — vừa đủ cảm nhận "trời u ám" mà không khiến người chơi không nhìn thấy gì. Nhân vào `u_daylight`:

```java
shaderProgram.setUniformf("u_daylight", dayCycle.daylight() * weather.daylightFactor());
```

Giữa trưa mưa: `1.00 × 0.82 = 0.82`. Nửa đêm mưa: `0.16 × 0.82 = 0.131`.

### 2.3 Sương mù — hệ số lớn nhất

```java
public float fogFactor() {
    return 1f + 2.6f * rain;
}
```

```
  F_factor(rain) = 1 + 2.6·rain    ∈ [1.0, 3.6]
```

Áp trong `VoxelEngine.drawWorld`:

```java
float fog = submerged ? 0.11f : 0.028f * weather.fogFactor();
```

| Trạng thái | `u_fogstr` |
|---|---|
| Trời quang | 0.028 |
| Mưa to | **0.1008** |
| Dưới nước | 0.11 |

### Hệ quả về tầm nhìn

Sương mù theo mô hình `F(d) = e^(−k²d²/9)` (xem [Shaders §3](Shaders.md)). Định nghĩa "tầm nhìn" là khoảng cách mà `F = 0.5`:

```
              3·√(ln 2)
  d_½  =  ─────────────
                 k
```

| Trạng thái | `k` | `d_½` |
|---|---|---|
| Trời quang | 0.028 | **89 khối** |
| Mưa to | 0.1008 | **25 khối** |
| Dưới nước | 0.11 | 23 khối |

⇒ Mưa to giảm tầm nhìn xuống còn **28 %**, gần bằng dưới nước. Đây là hiệu ứng mạnh nhất của hệ thống thời tiết — và cũng là con số đáng cân nhắc nếu thấy mưa quá dày.

---

## 3. Bảng tổng hợp ảnh hưởng

| Hệ thống | Đọc gì | Trời quang | Mưa to | Thay đổi |
|---|---|---|---|---|
| Màu trời | `blendSky` | màu ngày | pha 75 % xám | — |
| Độ sáng | `daylightFactor` | 1.00 | 0.82 | **−18 %** |
| Sương mù | `fogFactor` | 0.028 | 0.1008 | **+260 %** |
| Tầm nhìn | (hệ quả) | 89 khối | 25 khối | **−72 %** |
| Mặt trời/trăng | `rain` | `α = 1` | `α = 0` | **khuất hẳn** |
| Độ phủ mây | `rain` | 0.38 | 0.60 | **+58 %** |
| Sáng mây | `rain` | ×1.00 | ×0.65 | **−35 %** |
| Vệt mưa | `rain` | không vẽ | 420 vệt | — |

**Tám hệ thống, một biến.** Đây là ví dụ sạch sẽ của việc tách một tham số điều khiển ra khỏi các hiệu ứng.

---

## 4. Lệnh `/weather`

Đăng ký trong `PlaySession`:

```java
console.register("weather", new Command() {
    public String run(String[] args) {
        if (args.length == 0)
            return "Weather: " + (engine.weather().isRaining() ? "rain" : "clear");
        String wanted = args[0].toLowerCase();
        if (wanted.equals("rain"))  { engine.weather().setRaining(true);  return "Rain is coming..."; }
        if (wanted.equals("clear")) { engine.weather().setRaining(false); return "The sky is clearing up"; }
        return "Usage: /weather rain|clear";
    }
});
```

`setRaining` chỉ đổi cờ boolean; `update()` lo phần chuyển tiếp mượt.

**Lưu ý về HUD:** hệ thống thời tiết **không** đọc hay tác động tới bất kỳ thành phần giao diện 2D nào. Nếu HUD khó đọc khi mưa, nguyên nhân là nền trắng của sương mù dày (`fogFactor = 3.6`) làm chữ/biểu tượng sáng màu bị chìm — chỉnh `fogFactor` hoặc thêm nền tối sau HUD sẽ xử lý được.

---

## 5. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `TRANSITION_TIME` | 4 s | Thời gian chuyển clear ↔ rain |
| `RAIN_SKY` | `(0.72, 0.75, 0.79)` | Màu xám sáng của trời mưa |
| Hệ số pha trời | 0.75 | Giữ 25 % màu ngày đêm |
| Hệ số tối | 0.18 | Mưa làm tối 18 % |
| Hệ số sương mù | 2.6 | Sương dày gấp 3.6 lần |

---

## 6. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `update` | `O(1)` — 1 phép chia, 1 so sánh |
| `blendSky` | `O(1)` — 1 lerp 3 kênh, dùng lại `Color` đệm |
| `daylightFactor`, `fogFactor` | `O(1)` |
| Bộ nhớ | `O(1)` — 1 `Color` + 2 trường |

`blendedSky` là trường tái dùng ⇒ `blendSky` được gọi mỗi khung hình mà **không cấp phát**.

---

## 7. Chủ đề Toán / Thiết kế thể hiện

- **Nội suy tốc độ không đổi** vs lerp hàm mũ.
- **Một tham số điều khiển nhiều hệ thống** (parameter-driven design).
- **Nội suy có hệ số < 1** để bảo toàn thông tin gốc.
- **Hàm affine** `a + b·rain` cho các hệ số điều biến.
- **Suy luận ngược từ mô hình sương mù** để tính tầm nhìn `d_½ = 3√(ln2)/k`.

---

## 8. Liên kết

- Màu trời gốc: [DayNightCycle.md](DayNightCycle.md)
- Sương mù: [Shaders.md](Shaders.md)
- Vệt mưa: [RainRenderer.md](RainRenderer.md)
- Mây: [CloudLayer.md](CloudLayer.md)
- Mặt trời/trăng mờ đi: [SkyRenderer.md](SkyRenderer.md)
