# Shaders — sương mù, ánh sáng ngày đêm, gió lay cỏ

**Files:** `assets/data/shaders/shader.vs`, `assets/data/shaders/shader.fs`

Chạy trên GPU cho **mỗi đỉnh** và **mỗi điểm ảnh**. Đây là nơi các con số mà [FaceLighting](FaceLighting.md) đóng gói được giải nén và sử dụng.

---

## 1. Vertex shader — giải nén ánh sáng ngày/đêm

```glsl
float skyShare = fract(a_color.a * 2.0);
blocklight = (a_color.rgb + baselight) * mix(1.0, u_daylight, skyShare) - baselight;
```

với `baselight = −0.25` (chính là `−SHADER_BIAS`).

### Công thức

Đặt `c = a_color.rgb`, `s = skyShare`, `d = u_daylight`, `b = 0.25`:

```
  blocklight = (c − b) · lerp(1, d, s) + b
             = (c − b) · (1 − s + s·d) + b
```

### Vì sao phải trừ `b` rồi cộng lại?

> Comment trong shader: *"Phải bỏ phần bù 0.25 ra TRƯỚC khi nhân rồi cộng lại, nếu không trời tối sẽ ra số âm."*

`FaceLighting` đã cộng `+0.25` vào màu đỉnh để tránh dải màu răng cưa ở vùng tối. Nếu nhân thẳng:

```
  c · d  =  (thật + 0.25) · d       ✗ SAI — phần bù cũng bị nhân
```

Trừ ra trước ⇒ **chỉ phần màu thật** bị `daylight` tác động, phần bù giữ nguyên.

### Bảng minh hoạ

Màu thật `0.6`, tức `c = 0.85`. Ban đêm `d = 0.2`:

| `skyShare` | Kết quả | Ý nghĩa |
|---|---|---|
| **1.0** (ngoài trời) | `0.6 × 0.2 + 0.25 = 0.37` | Tối hẳn theo trời |
| **0.0** (dưới đuốc) | `0.6 × 1.0 + 0.25 = 0.85` | **Không đổi** — đuốc vẫn sáng |
| 0.5 (nửa nọ nửa kia) | `0.6 × 0.6 + 0.25 = 0.61` | Tối một nửa |

### `fract(a_color.a * 2.0)` — giải nén

`FaceLighting.packSkyShare` đóng gói:

```
  alpha = (windy ? 0 : 0.5) + share × 0.5
```

Giải nén:

```
  alpha × 2 = (windy ? 0 : 1) + share
  fract(...) = share            (vì share ∈ [0, 1))
```

`fract` lấy phần lẻ ⇒ bỏ đi bit "windy" ở phần nguyên, chỉ còn `share`. Một phép GPU duy nhất.

**Kết quả then chốt:** đổi ngày ↔ đêm chỉ cần cập nhật **một uniform** `u_daylight`, không phải dựng lại mesh của hàng trăm chunk. Xem [FaceLighting §4](FaceLighting.md).

---

## 2. Gió lay cỏ

```glsl
if (v_texCoords0.x == 0.375 && v_texCoords0.y == 0.0 && a_color.a < 0.5) {
    dx = 0.1  * sin( mod(a_position.x, 3.5) + u_time );
    dz = 0.25 * cos( mod(a_position.x, 3.5) + u_time );
}
```

### Điều kiện

| Kiểm tra | Ý nghĩa |
|---|---|
| `v_texCoords0.x == 0.375 && y == 0.0` | Đúng góc **trên** của texture cỏ trong atlas |
| `a_color.a < 0.5` | Bit "windy" — xem [FaceLighting §5](FaceLighting.md) |

Chỉ **đỉnh trên** của tấm cỏ lắc; đỉnh dưới đứng yên (gốc cỏ cắm xuống đất) ⇒ cỏ **uốn cong** thay vì trượt ngang.

### Công thức dao động

```
  θ(x, t) = mod(x, 3.5) + t

  Δx = 0.10 · sin θ
  Δz = 0.25 · cos θ
```

Đây là phương trình **ellipse tham số**:

```
   ⎛ Δx ⎞²   ⎛ Δz  ⎞²
   ⎜────⎟ + ⎜─────⎟  = sin²θ + cos²θ = 1
   ⎝0.10⎠   ⎝0.25 ⎠
```

⇒ đầu ngọn cỏ vẽ một **hình elip** bán trục `0.10 × 0.25` khối — chuyển động xoay tròn nhẹ, tự nhiên hơn dao động qua lại một chiều.

### `mod(a_position.x, 3.5)` — lệch pha theo vị trí

Không có nó, **toàn bộ cỏ trên thế giới lắc đồng loạt** như duyệt binh. `mod(x, 3.5)` cho mỗi cột `x` một **pha ban đầu khác nhau**, lặp lại sau 3.5 khối.

Chọn 3.5 (số lẻ thập phân) để chu kỳ không trùng với lưới khối nguyên ⇒ mẫu lặp khó nhận ra.

**Hạn chế đã biết:** chỉ dùng `x`, không dùng `z` ⇒ cỏ trên cùng một hàng `x` lắc đồng pha. Nhìn kỹ theo trục Z sẽ thấy sóng đồng bộ.

---

## 3. Sương mù — mô hình exponential squared

### Vertex shader — tính khoảng cách

```glsl
vec3 flen = u_cameraPosition.xyz - pos.xyz;
float fog = dot(flen, flen) * u_cameraPosition.w;
v_fog = min(fog, 1.0);
```

`dot(flen, flen)` = `|flen|²` — **bình phương khoảng cách**, tránh phép `sqrt` đắt đỏ.

`u_cameraPosition.w` mang `1/farPlane²` (libGDX nhét sẵn) ⇒ `fog` là khoảng cách chuẩn hoá bình phương.

### Fragment shader — công thức sương mù

```glsl
const float LOG2 = 1.442695;
float z = (gl_FragCoord.z / gl_FragCoord.w) / 3.0;
float fogFactor = exp2( -v_fogstr * v_fogstr * z * z * LOG2 );
fogFactor = clamp(fogFactor, 0.0, 1.0);

gl_FragColor = mix(v_fogColor, finalColor, fogFactor);
```

### Công thức

```
  F(z) = 2^( −k² · z² · log₂e )  =  e^( −k² · z² )
```

vì `2^(x · log₂e) = e^x`.

Đây là mô hình **fog exponential squared** (`GL_EXP2` của OpenGL cố định cũ):

```
  F = e^(−(k·z)²)
```

| `F` | Ý nghĩa |
|---|---|
| 1 | Không sương — thấy màu thật |
| 0 | Sương đặc — chỉ thấy màu nền |

`mix(fogColor, color, F)` = `lerp` giữa màu sương và màu vật thể.

### Vì sao `exp2` chứ không `exp`?

GPU cài đặt `exp2` như một lệnh phần cứng; `exp(x)` thường được biên dịch thành `exp2(x · log₂e)` — làm sẵn phép nhân đó tiết kiệm một lệnh.

### `gl_FragCoord.z / gl_FragCoord.w` — khoảng cách thật

`gl_FragCoord.w` là `1/w_clip`. Phép chia này khôi phục **độ sâu tuyến tính trong không gian mắt** (eye-space depth) từ toạ độ đã chia phối cảnh. Chia thêm `/3.0` là hệ số tỉ lệ để `u_fogstr` nằm trong khoảng số dễ chỉnh.

### Bảng giá trị thực tế

`u_fogstr` được đặt trong `VoxelEngine.drawWorld`:

```java
float fog = submerged ? 0.11f : 0.028f * weather.fogFactor();
```

Với `k = 0.028` (trời quang), `z = d/3`:

```
  F(d) = e^(−0.028² · d²/9) = e^(−8.71×10⁻⁵ · d²)
```

| `d` (khối) | `F` | Độ mờ |
|---|---|---|
| 32 | 0.914 | 9 % |
| 64 | 0.699 | 30 % |
| 96 | 0.449 | 55 % |
| 128 | 0.244 | 76 % |

Với `k = 0.1008` (trời mưa, `fogFactor = 3.6`):

```
  F(d) = e^(−1.129×10⁻³ · d²)
```

| `d` | `F` | Độ mờ |
|---|---|---|
| 16 | 0.746 | 25 % |
| 32 | 0.316 | **68 %** |
| 48 | 0.074 | 93 % |
| 64 | 0.010 | **99 %** |

⇒ Khi mưa, tầm nhìn thực tế tụt từ ~128 khối xuống **~40 khối**. Đây là con số đáng chú ý: sương mù mưa gần bằng sương mù dưới nước (`k = 0.11`).

---

## 4. Vibrance — tăng độ rực

```glsl
float luma = dot(finalColor.rgb, vec3(0.299, 0.587, 0.114));
finalColor.rgb = clamp(mix(vec3(luma), finalColor.rgb, 1.18), 0.0, 1.0);
```

### Công thức

**Bước 1 — độ chói (luminance)** theo chuẩn ITU-R BT.601:

```
  Y = 0.299·R + 0.587·G + 0.114·B
```

Ba hệ số phản ánh độ nhạy của mắt người: **xanh lá sáng nhất** (58.7 %), đỏ trung bình (29.9 %), **xanh dương tối nhất** (11.4 %). Tổng = 1.0 ⇒ ảnh xám giữ nguyên độ sáng.

**Bước 2 — ngoại suy khỏi màu xám:**

```
  C' = lerp(Y, C, 1.18) = Y + 1.18·(C − Y)
```

Hệ số `t = 1.18 > 1` ⇒ đây là **ngoại suy (extrapolation)**, không phải nội suy. Nó **đẩy màu ra xa trục xám** thêm 18 % ⇒ tăng độ bão hoà.

| `t` | Hiệu ứng |
|---|---|
| 0 | Ảnh xám hoàn toàn |
| 1 | Giữ nguyên |
| **1.18** | Rực hơn 18 % |
| 2 | Bão hoà quá mức, màu bệt |

`clamp(0, 1)` chặn tràn ở vùng màu đã gần bão hoà.

> Comment: *"học từ gói shader Complementary để màu sống hơn mà không làm cháy chi tiết. Áp lên màu đã chiếu sáng, không lên sương mù."*

**Thứ tự quan trọng:** áp vibrance **trước** khi trộn sương mù — nếu sau, màu sương cũng bị đẩy bão hoà, chân trời sẽ loè loẹt.

---

## 5. Alpha test — cắt nền trong suốt

```glsl
vec4 texColor = texture2D(u_diffuseTexture, v_texCoords0.xy).rgba;
if (texColor.a < 0.5) discard;
```

`discard` **loại bỏ hẳn** điểm ảnh — không ghi màu, không ghi độ sâu.

**Vì sao không dùng alpha blending?** Cỏ, hoa, lá có phần trong suốt hoàn toàn. Blending đòi hỏi **sắp xếp theo độ sâu từ xa tới gần** — bất khả thi với hàng triệu tấm cỏ. Alpha test không cần sắp xếp, chỉ cần độ trong suốt là nhị phân (có/không) — đúng với texture kiểu Minecraft.

Ngưỡng `0.5` là điểm giữa; texture Minecraft chỉ dùng alpha 0 hoặc 255.

---

## 6. Chiếu sáng cuối cùng

```glsl
vec3 light = blocklight.rgb + baselight;          // baselight = −0.25 → trừ phần bù
vec4 finalColor = vec4(texColor.xyz * light.rgb, texColor.a);
```

Màu texture **nhân** với ánh sáng đỉnh (đã được GPU nội suy tuyến tính giữa 4 góc ⇒ smooth lighting).

Chuỗi đầy đủ từ đầu tới cuối:

```
  1. FaceLighting: brightness = B(L) × AO(occ) × shade(face)
  2. FaceLighting: color = tint × brightness + 0.25,  alpha = pack(skyShare, windy)
  3. GPU: nội suy tuyến tính 4 góc → mỗi điểm ảnh
  4. Vertex: blocklight = (color − 0.25) × mix(1, daylight, skyShare) + 0.25
  5. Fragment: light = blocklight − 0.25
  6. Fragment: finalColor = texture × light
  7. Fragment: vibrance 1.18
  8. Fragment: mix(fogColor, finalColor, e^(−k²z²))
```

---

## 7. Bảng uniform

| Uniform | Nguồn | Ý nghĩa |
|---|---|---|
| `u_daylight` | `dayCycle.daylight() × weather.daylightFactor()` | Độ sáng ban ngày `[0, 1]` |
| `u_fogstr` | `0.028 × weather.fogFactor()` hoặc `0.11` (dưới nước) | Hệ số `k` của sương mù |
| `u_time` | `elapsed` | Thời gian cho gió lay cỏ |
| `u_fogColor` | màu bầu trời đã pha mưa | Màu sương mù |
| `u_cameraPosition` | vị trí camera + `1/far²` ở `w` | Tính khoảng cách |

---

## 8. Chủ đề Toán / Đồ hoạ thể hiện

- **Nội suy & ngoại suy tuyến tính** (`mix` với `t > 1` cho vibrance).
- **Mô hình sương mù mũ bình phương** `e^(−k²z²)`.
- **Đổi cơ số logarit** — `exp2(x·log₂e) = e^x`.
- **Độ chói ITU-R BT.601** — trọng số cảm nhận màu của mắt người.
- **Phương trình elip tham số** cho dao động gió.
- **Lệch pha theo không gian** (`mod(x, 3.5)`) chống đồng bộ.
- **Giải nén trường bit từ float** (`fract(a × 2)`).
- **Alpha test vs alpha blending** — đánh đổi sắp xếp độ sâu.
- **Tránh `sqrt`** bằng cách làm việc với bình phương khoảng cách.

---

## 9. Liên kết

- Nguồn màu đỉnh: [FaceLighting.md](FaceLighting.md)
- Chu kỳ ngày đêm: [DayNightCycle.md](DayNightCycle.md)
- Thời tiết: [WeatherSystem.md](WeatherSystem.md)
