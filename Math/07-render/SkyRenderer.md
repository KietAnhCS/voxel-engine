# SkyRenderer — billboard mặt trời & mặt trăng

**File:** `core/src/com/voxel/engine/render/SkyRenderer.java`

---

## 1. Trạng thái OpenGL — bầu trời là phông nền vô cực

```java
public void render(PerspectiveCamera camera, DayNightCycle cycle, float elapsed, float rainAmount) {
    Gdx.gl.glEnable(GL20.GL_BLEND);
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    Gdx.gl.glDisable(GL20.GL_CULL_FACE);
    Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);      // ← không kiểm tra độ sâu
    Gdx.gl.glDepthMask(false);                  // ← không GHI độ sâu
    ...
}
```

> Comment: *"Cả lớp này tắt kiểm tra độ sâu và không ghi độ sâu: nó chỉ là tấm phông nền ở vô cùng xa. Nhờ vậy mọi khối đất đá vẽ sau đều che lên trên — mặt trời luôn nằm SAU núi chứ không bao giờ đè lên khối hay lơ lửng trước mặt người chơi."*

**Thứ tự vẽ trong `VoxelEngine.drawWorld`:**

```
  1. glClear (màu nền = màu trời)
  2. sky.render()          ← không ghi depth buffer
  3. thế giới khối (solid) ← ghi depth, đè lên bầu trời
  4. thế giới trong suốt
  5. sky.renderRain()      ← có depth TEST, không ghi
```

Vì bước 2 không ghi depth, bộ đệm độ sâu vẫn "rỗng" khi vẽ thế giới ⇒ mọi khối đều vượt qua depth test và đè lên mặt trời.

**Công thức blending:**

```
  C_kết quả = C_nguồn · α + C_đích · (1 − α)
```

Chuẩn alpha blending.

### Trạng thái sau khi vẽ xong

```java
Gdx.gl.glDepthMask(true);
Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
Gdx.gl.glEnable(GL20.GL_CULL_FACE);
Gdx.gl.glDisable(GL20.GL_BLEND);
```

Khôi phục đúng trạng thái mặc định — quan trọng vì phần vẽ thế giới sau đó giả định trạng thái này.

---

## 2. Billboard — tấm vuông luôn quay mặt về người chơi

```java
private void drawBillboard(PerspectiveCamera camera, Texture texture, Vector3 direction,
                           float distance, float size, float alpha, float r, float g, float b) {
    if (alpha <= 0.01f) return;

    centre.set(direction).scl(distance).add(camera.position);
    right.set(direction).crs(Vector3.Y).nor().scl(size);
    up.set(right).crs(direction).nor().scl(size);

    putCorner(0, -1f, -1f, 0f, 1f, ...);
    putCorner(1,  1f, -1f, 1f, 1f, ...);
    putCorner(2,  1f,  1f, 1f, 0f, ...);
    putCorner(3, -1f,  1f, 0f, 0f, ...);
}
```

### 2.1 Tâm tấm

```
  C = P_camera + d · direction
```

Đặt tấm cách camera một khoảng `d` theo hướng đã cho. Vì `C` **bám theo camera**, mặt trời không bao giờ tới gần được — đúng cảm giác "ở vô cùng xa".

### 2.2 Dựng hệ trục địa phương bằng tích có hướng

```
  r̂ = normalize( d̂ × ŷ ) · s          (ŷ = (0,1,0))
  û = normalize( r̂ × d̂ ) · s
```

**Bước 1:** `d̂ × ŷ` cho vector **vuông góc với cả hướng nhìn lẫn trục dọc** ⇒ đó là hướng "sang phải" của tấm.

**Bước 2:** `r̂ × d̂` cho vector vuông góc với cả hai ⇒ hướng "lên" của tấm.

Kết quả: `{r̂, û, d̂}` là một **cơ sở trực chuẩn** (orthonormal basis) — ba vector đôi một vuông góc, độ dài 1.

Đây là thuật toán **Gram–Schmidt rút gọn** cho trường hợp 3D: từ một vector cho trước, dựng hai vector còn lại bằng hai phép tích có hướng.

### 2.3 Bốn góc

```
  P(i, j) = C + i·r̂ + j·û ,     (i, j) ∈ {−1, +1}²
```

| `corner` | `(i, j)` | UV |
|---|---|---|
| 0 | `(−1, −1)` | `(0, 1)` |
| 1 | `(+1, −1)` | `(1, 1)` |
| 2 | `(+1, +1)` | `(1, 0)` |
| 3 | `(−1, +1)` | `(0, 0)` |

Vì `r̂` và `û` được dựng lại **mỗi khung hình** từ hướng camera, tấm **luôn quay mặt về người chơi** — đó chính là kỹ thuật billboard.

### 2.4 Suy biến ở thiên đỉnh

Khi `d̂ ≈ ŷ` (mặt trời đúng đỉnh đầu, giữa trưa), `d̂ × ŷ ≈ 0` ⇒ `nor()` chia cho ~0 ⇒ hướng tấm không xác định.

Thực tế không xảy ra vì `sunDirection = (cos θ, sin θ, 0)` luôn có thành phần X hoặc Y khác 0, và tại `θ = π/2` chính xác thì `d̂ = (0,1,0)`... **đây là trường hợp biên có thật** ở đúng `time = 0.25`. Vì `time` là số thực chạy liên tục, xác suất rơi đúng điểm đó gần như bằng 0, và nếu có thì chỉ nhấp nháy 1 khung hình.

---

## 3. Kích thước & khoảng cách

```java
private static final float SKY_DISTANCE_RATIO = 0.8f;
private static final float SUN_SIZE_RATIO     = 0.075f;
private static final float MOON_SIZE_RATIO    = 0.05f;

float distance = camera.far * SKY_DISTANCE_RATIO;
drawBillboard(camera, sunTexture, sun, distance, distance * SUN_SIZE_RATIO, ...);
```

```
  d = far × 0.8
  s_mặt trời = d × 0.075
  s_mặt trăng = d × 0.05
```

### Vì sao kích thước tỉ lệ với khoảng cách?

Góc nhìn (angular size) của vật thể:

```
  α = 2 · arctan(s / d)
```

Nếu `s = k · d` thì `α = 2·arctan(k)` — **hằng số**, không phụ thuộc `d`.

```
  Mặt trời: α = 2·arctan(0.075) ≈ 8.6°
  Mặt trăng: α = 2·arctan(0.05) ≈ 5.7°
```

⇒ Đổi `camera.far` (tầm nhìn xa) không làm mặt trời to nhỏ theo. Rất tiện khi cho người chơi chỉnh render distance.

> So sánh: mặt trời thật có góc nhìn ~0.53°. Game phóng đại ~16 lần cho dễ thấy — Minecraft cũng làm vậy.

### `0.8 × far` — vừa đủ xa

Đặt ở `0.8` chứ không `1.0` để tấm **nằm trong tầm nhìn** (far plane), không bị mặt phẳng cắt xén. Đủ xa để trông như ở vô cực.

---

## 4. Mờ dần ở chân trời

```java
private static float horizonFade(float height) {
    return Math.max(0f, Math.min(1f, (height + 0.08f) * 8f));
}
```

### Công thức

```
  f(h) = clamp( 8·(h + 0.08), 0, 1 )
```

| `h` (độ cao) | `f` | Trạng thái |
|---|---|---|
| ≤ −0.08 | **0** | Khuất hẳn dưới chân trời |
| −0.04 | 0.32 | Đang mờ |
| 0.00 | 0.64 | Ngay chân trời |
| ≥ 0.045 | **1** | Hiện đầy đủ |

Dải chuyển tiếp rất hẹp: `h ∈ [−0.08, 0.045]`, rộng `0.125`. Với `h = sin(2πt)`:

```
  Δt ≈ 0.125 / (2π) ≈ 0.02  →  0.02 × 600 = 12 giây
```

Mặt trời mờ dần rồi tắt trong **12 giây** khi lặn.

**Vì sao lệch `+0.08`?** Điểm tắt hoàn toàn ở `h = −0.08`, tức **dưới** đường chân trời một chút — mặt trời còn ló nửa vành khi `h = 0`, tắt hẳn sau khi đã chìm.

### Nhân với `clearSky`

```java
float clearSky = 1f - rainAmount;
drawBillboard(camera, sunTexture, sun, distance, distance * SUN_SIZE_RATIO,
              horizonFade(sun.y) * clearSky, 1f, 1f, 1f);
```

```
  α_cuối = horizonFade(h) × (1 − rain)
```

Trời mưa `rain = 1` ⇒ `α = 0` ⇒ mặt trời/trăng **khuất hẳn sau mây**. Xem [WeatherSystem](WeatherSystem.md).

---

## 5. Màu sắc

```java
drawBillboard(..., sunTexture,  ..., 1f,    1f,    1f);      // trắng ngà
drawBillboard(..., moonTexture, ..., 0.9f,  0.93f, 1f);      // hơi xanh lạnh
```

Mặt trăng nhân `(0.9, 0.93, 1.0)` — **ám xanh lam nhẹ**, mô phỏng ánh trăng lạnh. Chênh lệch nhỏ (10 % ở kênh đỏ) nhưng đủ để mắt phân biệt.

---

## 6. Texture sinh bằng code

```java
private static Texture square(int size, Color inner, Color outer) {
    Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
    float centre = (size - 1) * 0.5f;
    for (int y = 0; y < size; y++)
        for (int x = 0; x < size; x++) {
            float distance = Math.max(Math.abs(x - centre), Math.abs(y - centre)) / centre;
            float mix = Math.min(1f, distance * distance);
            pixmap.setColor( lerp(inner, outer, mix) );
            pixmap.drawPixel(x, y);
        }
    return toTexture(pixmap, ClampToEdge, Nearest);
}
```

### Khoảng cách Chebyshev

```
  d(x, y) = max( |x − c| , |y − c| ) / c
```

Đây là **chuẩn vô cùng** (`L∞`), khác chuẩn Euclid (`L²`):

| Chuẩn | Công thức | Hình dạng mức |
|---|---|---|
| `L¹` (Manhattan) | `\|dx\| + \|dy\|` | Hình thoi |
| `L²` (Euclid) | `√(dx² + dy²)` | **Hình tròn** |
| `L∞` (Chebyshev) | `max(\|dx\|, \|dy\|)` | **Hình vuông** |

> Comment: *"Mặt trời và mặt trăng là ô VUÔNG đặc như Minecraft, không phải đĩa tròn."*

Dùng `L∞` cho các đường đồng mức là **hình vuông đồng tâm** ⇒ mặt trời vuông vắn.

### Trộn màu bậc 2

```
  mix = min(1, d²)
  C = lerp(inner, outer, mix)
```

Bình phương làm vùng giữa **rộng và đều màu**, chỉ rìa mới chuyển sang màu ngoài ⇒ ô vuông có tâm sáng, viền đậm hơn một chút cho có chiều sâu.

### Bộ lọc `Nearest` + `ClampToEdge`

- `Nearest` — không làm mượt điểm ảnh ⇒ **cạnh vuông sắc nét**, đúng phong cách pixel.
- `ClampToEdge` — không lặp texture ở biên, tránh viền lạ.

Texture chỉ `16 × 16` = 256 điểm ảnh × 4 byte = **1 KB** mỗi ảnh.

---

## 7. Chia sẻ shader

`SkyRenderer` sở hữu một shader nhỏ tự viết (vị trí + ảnh + màu), dùng chung cho:

| Người dùng | Uniform đặc biệt |
|---|---|
| Billboard mặt trời/trăng | `u_uvFromWorld = 0` (dùng UV đỉnh) |
| [CloudLayer](CloudLayer.md) | `u_offset` = vị trí lớp mây |
| [RainRenderer](RainRenderer.md) | `u_offset = 0`, `u_scroll = 0` |

```glsl
vec2 worldUv = world.xz / u_tileSize + u_scroll;
v_uv = mix(a_texCoord0, worldUv, u_uvFromWorld);
```

`u_uvFromWorld` là **công tắc nội suy**: `0` = dùng UV của đỉnh, `1` = tính UV từ toạ độ thế giới. Một shader phục vụ hai chế độ lấy toạ độ texture khác nhau mà không cần rẽ nhánh `if` (GPU không thích rẽ nhánh).

---

## 8. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `drawBillboard` | `O(1)` — 2 tích có hướng, 2 chuẩn hoá, 4 đỉnh |
| `render` toàn bộ | `O(1)` + chi phí [CloudLayer](CloudLayer.md) |
| Khởi tạo texture | `O(16²)` × 2 — một lần |
| Bộ nhớ | 2 KB texture + 1 mesh 4 đỉnh |

Toàn bộ bầu trời (trừ mây) chỉ tốn **8 tam giác**.

---

## 9. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `SKY_DISTANCE_RATIO` | 0.8 | Khoảng cách = `0.8 × far` |
| `SUN_SIZE_RATIO` | 0.075 | Góc nhìn ≈ 8.6° |
| `MOON_SIZE_RATIO` | 0.05 | Góc nhìn ≈ 5.7° |
| `horizonFade` độ dốc | 8 | Dải mờ rộng 0.125 (~12 giây) |
| `horizonFade` lệch | 0.08 | Tắt hẳn dưới chân trời |
| Kích thước texture | 16×16 | 1 KB mỗi ảnh |

---

## 10. Chủ đề Toán / Đồ hoạ thể hiện

- **Billboard** & dựng cơ sở trực chuẩn bằng tích có hướng.
- **Gram–Schmidt** rút gọn cho 3D.
- **Góc nhìn (angular size)** và vì sao kích thước phải tỉ lệ với khoảng cách.
- **Các chuẩn khoảng cách** `L¹, L², L∞` — chọn `L∞` cho hình vuông.
- **Hàm dốc bị kẹp** cho `horizonFade`.
- **Quản lý trạng thái depth buffer** để xếp lớp đúng.
- **Alpha blending** và thứ tự vẽ.

---

## 11. Liên kết

- Nguồn hướng & màu: [DayNightCycle.md](DayNightCycle.md)
- Mây: [CloudLayer.md](CloudLayer.md)
- Mưa: [RainRenderer.md](RainRenderer.md)
- Thời tiết: [WeatherSystem.md](WeatherSystem.md)
