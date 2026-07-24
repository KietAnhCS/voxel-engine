# RainRenderer — vệt mưa

**File:** `core/src/com/voxel/engine/render/RainRenderer.java`

420 vệt mưa rơi quanh người chơi **vĩnh viễn** mà không cấp phát một hạt nào.

---

## 1. Ý tưởng: hạt mưa không có trạng thái

Cách thông thường: mỗi hạt mưa là một object có `position`, `velocity`, cập nhật mỗi khung hình, tạo mới khi hạt cũ chạm đất.

Cách ở đây: **vị trí là hàm thuần tuý của chỉ số và thời gian**.

```java
float u = hash(i * 2);
float v = hash(i * 2 + 1);
float distance = (float) Math.sqrt(u) * RADIUS;
double angle = v * Math.PI * 2.0;
float x = camera.position.x + (float) Math.cos(angle) * distance;
float z = camera.position.z + (float) Math.sin(angle) * distance;

float cycle = ((elapsed * FALL_SPEED + hash(i + 7331) * HEIGHT) % HEIGHT + HEIGHT) % HEIGHT;
float top = camera.position.y + HEIGHT * 0.5f - cycle;
```

```
  P_i(t) = f( hash(i), t )
```

Không mảng hạt, không cấp phát, không cập nhật trạng thái. Chỉ 420 lần tính lại mỗi khung hình.

---

## 2. Phân bố đều trên hình tròn — vì sao có `√u`

```java
float distance = (float) Math.sqrt(u) * RADIUS;
double angle = v * Math.PI * 2.0;
```

### Bài toán

Muốn rải điểm **đều theo diện tích** trên hình tròn bán kính `R`. Cách sai:

```java
r = u * R;          // ✗ SAI — điểm dồn về tâm
```

### Vì sao sai?

Diện tích hình vành khăn từ `r` đến `r + dr` là `2πr·dr` — **tỉ lệ với `r`**. Vành ngoài rộng hơn vành trong nên phải chứa nhiều điểm hơn.

### Chứng minh công thức đúng

Hàm phân phối tích luỹ (CDF) của bán kính khi rải đều theo diện tích:

```
         diện tích trong bán kính r      πr²      r²
  F(r) = ────────────────────────────  = ───── = ───
              tổng diện tích              πR²     R²
```

Dùng **phương pháp biến đổi nghịch đảo** (inverse transform sampling): nếu `u ~ Uniform(0,1)` thì `r = F⁻¹(u)` có đúng phân phối mong muốn.

```
        r²
  u = ─────   ⟹   r = R·√u        ✔
        R²
```

### Kiểm chứng bằng số

Với `R = 14`, chia hình tròn thành 2 vùng có diện tích bằng nhau (`r < 9.9` và `r > 9.9`):

| Công thức | `u < 0.5` cho `r` | Số điểm vùng trong |
|---|---|---|
| `r = uR` | `r < 7` | ~50 % số điểm trong 25 % diện tích ✗ |
| `r = R√u` | `r < 9.9` | **50 % số điểm trong 50 % diện tích** ✔ |

Đây là kỹ thuật chuẩn khi lấy mẫu trên đĩa, cầu, hoặc bất kỳ miền nào có mật độ không đều.

`angle = 2πv` thì đơn giản — góc phân bố đều tự nhiên.

---

## 3. Rơi tuần hoàn — modulo kép

```java
float cycle = ((elapsed * FALL_SPEED + hash(i + 7331) * HEIGHT) % HEIGHT + HEIGHT) % HEIGHT;
float top = camera.position.y + HEIGHT * 0.5f - cycle;
float bottom = top - STREAK_LENGTH;
```

### Công thức

```
  c_i(t) = ( (v·t + φ_i) mod H + H ) mod H
  y_top  = y_cam + H/2 − c_i(t)
```

với `v = 21` khối/s, `H = 20`, `φ_i = hash(i+7331)·H` là **pha ban đầu** của vệt `i`.

### Modulo kép — chuẩn hoá dương

Cùng mẫu với [`DayNightCycle.setTime`](DayNightCycle.md): `%` trong Java trả số âm cho toán hạng âm. `((a % H) + H) % H` đảm bảo kết quả `∈ [0, H)`.

Ở đây `elapsed ≥ 0` nên về lý thuyết không cần, nhưng viết phòng thủ để an toàn nếu ai đó truyền `elapsed` âm.

### Chu kỳ

```
  T = H / v = 20 / 21 ≈ 0.952 giây
```

Mỗi vệt mưa đi hết hình trụ trong ~0.95 giây rồi **quấn về đỉnh**. Vì mỗi vệt có pha `φ_i` ngẫu nhiên riêng, tại mọi thời điểm chúng phân bố đều theo chiều cao ⇒ mắt không nhận ra tính tuần hoàn.

### Hình trụ bám camera

```
  y_top ∈ [ y_cam − H/2 , y_cam + H/2 ]
```

Hình trụ **luôn tâm ở camera**, cao 20 khối, bán kính 14 khối. Người chơi đi đâu mưa theo đó — mưa chỉ cần tồn tại ở nơi nhìn thấy được.

---

## 4. Billboard quanh trục dọc

```java
float rightX = -camera.direction.z;
float rightZ =  camera.direction.x;
float norm = (float) Math.sqrt(rightX * rightX + rightZ * rightZ);
if (norm < 1e-4f) { rightX = 1f; rightZ = 0f; }
else              { rightX /= norm; rightZ /= norm; }
rightX *= STREAK_WIDTH;
rightZ *= STREAK_WIDTH;
```

### Công thức

```
  r = normalize( (−d_z, 0, d_x) ) × w
```

Đây là phép **quay 90° quanh trục Y** của vector hướng nhìn, chiếu xuống mặt phẳng ngang:

```
  R_y(90°) : (x, z) ↦ (−z, x)
```

Kết quả `r` vuông góc với hướng nhìn **trong mặt phẳng ngang** ⇒ bề rộng vệt mưa luôn quay mặt về người chơi.

### Vì sao chỉ quay quanh trục Y?

Mưa rơi **thẳng đứng**. Nếu billboard tự do (như mặt trời), vệt mưa sẽ nghiêng theo camera khi ngước lên — sai vật lý. Khoá trục dọc giữ vệt luôn thẳng đứng, chỉ xoay bề mặt.

Đây gọi là **cylindrical billboard** (billboard trụ), khác **spherical billboard** của [SkyRenderer](SkyRenderer.md).

### Trường hợp suy biến

```java
if (norm < 1e-4f) { rightX = 1f; rightZ = 0f; }
```

Khi camera nhìn **thẳng lên hoặc thẳng xuống**, `d_x = d_z = 0` ⇒ chia cho 0. Dùng hướng mặc định `(1, 0)`. Lúc đó người chơi nhìn dọc theo vệt mưa nên hướng nào cũng như nhau.

---

## 5. Bốn đỉnh & độ trong suốt giảm dần

```java
float alpha = 0.32f * strength;
at = vertex(at, x - rightX, bottom, z - rightZ, alpha);
at = vertex(at, x + rightX, bottom, z + rightZ, alpha);
at = vertex(at, x + rightX, top,    z + rightZ, alpha * 0.4f);
at = vertex(at, x - rightX, top,    z - rightZ, alpha * 0.4f);
```

| Vị trí | Alpha |
|---|---|
| **Đáy vệt** | `0.32 × strength` |
| **Đỉnh vệt** | `0.128 × strength` (40 %) |

**Gradient dọc:** đầu vệt mờ, đuôi đậm ⇒ trông như **vệt chuyển động** (motion blur) của giọt nước rơi nhanh, thay vì một que thẳng cứng.

GPU nội suy tuyến tính alpha giữa 4 đỉnh ⇒ chuyển mượt.

`alpha` gốc chỉ `0.32` — mưa rất trong. 420 vệt chồng lên nhau mới tạo cảm giác màn mưa dày.

### Màu vệt

```java
vertices[at + 5] = 0.62f;   // R
vertices[at + 6] = 0.68f;   // G
vertices[at + 7] = 0.80f;   // B
```

Xám ám xanh lam — màu nước phản chiếu bầu trời.

---

## 6. Trạng thái OpenGL

```java
Gdx.gl.glEnable(GL20.GL_BLEND);
Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
Gdx.gl.glDepthMask(false);          // ← không GHI độ sâu
Gdx.gl.glDisable(GL20.GL_CULL_FACE);
mesh.render(shader, GL20.GL_TRIANGLES);
Gdx.gl.glEnable(GL20.GL_CULL_FACE);
Gdx.gl.glDepthMask(true);
Gdx.gl.glDisable(GL20.GL_BLEND);
```

> Comment: *"Vẽ SAU thế giới với depth TEST bật nhưng depth WRITE tắt: giọt mưa biến mất sau đồi, nhưng không bao giờ đục lỗ vào bất cứ thứ gì vẽ sau."*

| Trạng thái | Lý do |
|---|---|
| Depth **test** BẬT (kế thừa) | Mưa bị núi che ⇒ không thấy mưa xuyên qua địa hình |
| Depth **write** TẮT | Mưa không ghi vào depth buffer ⇒ không chặn thứ vẽ sau nó |
| Cull face TẮT | Vệt mưa là tấm mỏng, phải thấy từ cả hai phía |
| Blend BẬT | Mưa trong suốt |

Khác [SkyRenderer](SkyRenderer.md) ở chỗ mưa **có** depth test (vì nó ở trong thế giới, không phải phông nền).

---

## 7. Thoát sớm

```java
public void render(ShaderProgram shader, PerspectiveCamera camera, float elapsed, float strength) {
    if (strength <= 0.01f) return;
    ...
}
```

Trời quang (`strength = 0`) ⇒ **không tính gì cả**. Vì `WeatherSystem.rain()` chuyển dần trong 4 giây, ngưỡng `0.01` tắt hẳn phần tính toán ngay khi mưa gần dứt.

---

## 8. Hàm băm

```java
private static float hash(int i) {
    long h = i * 0x9E3779B97F4A7C15L;
    h ^= h >>> 31;
    h *= 0xBF58476D1CE4E5B9L;
    h ^= h >>> 30;
    return (h & 0xFFFFFF) / (float) 0x1000000;
}
```

Cùng họ splitmix64 với [Deterministic](../01-noise-terrain/Deterministic.md) và [CloudLayer](CloudLayer.md). Một tham số (chỉ số vệt) thay vì ba.

**Ba giá trị độc lập cho mỗi vệt** lấy từ ba chỉ số khác nhau:

```java
hash(i * 2)      → bán kính
hash(i * 2 + 1)  → góc
hash(i + 7331)   → pha rơi
```

`i*2` và `i*2+1` không bao giờ trùng nhau. `i + 7331` (số nguyên tố) tách khỏi hai cái kia — về lý thuyết có thể trùng với `i*2` khi `i = 7331`, nhưng `i < 420` nên an toàn.

---

## 9. Bộ nhớ cấp phát trước

```java
private static final int STREAKS = 420;
private static final int FLOATS_PER_VERTEX = 9;

private final Mesh mesh;
private final float[] vertices = new float[STREAKS * 4 * FLOATS_PER_VERTEX];   // 15 120 float
```

**Chỉ số dựng một lần trong constructor** (chúng không bao giờ đổi):

```java
short[] indices = new short[STREAKS * 6];       // 2 520 chỉ số
for (int i = 0; i < STREAKS; i++) {
    short base = (short) (i * 4);
    indices[6i..6i+5] = { base, base+1, base+2, base+2, base+3, base };
}
mesh.setIndices(indices);
```

Mỗi khung hình chỉ ghi lại **đỉnh** (`mesh.setVertices`), chỉ số giữ nguyên.

```
  Bộ nhớ = 15 120 × 4 B + 2 520 × 2 B ≈ 65 KB
  Đỉnh   = 420 × 4 = 1 680   (an toàn dưới 65 536)
  Tam giác = 420 × 2 = 840
```

---

## 10. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `STREAKS` | 420 | Số vệt mưa |
| `RADIUS` | 14 | Bán kính hình trụ (khối) |
| `HEIGHT` | 20 | Chiều cao hình trụ |
| `STREAK_LENGTH` | 0.9 | Chiều dài một vệt |
| `STREAK_WIDTH` | 0.03 | Nửa bề rộng vệt |
| `FALL_SPEED` | 21 | Khối/giây |
| Chu kỳ | 0.952 s | `HEIGHT / FALL_SPEED` |
| Alpha đáy | `0.32 × strength` | |
| Alpha đỉnh | `0.128 × strength` | 40 % của đáy |

**Mật độ mưa:** 420 vệt trong hình trụ `π × 14² × 20 ≈ 12 315` khối³ ⇒ 1 vệt mỗi 29 khối³.

---

## 11. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `render` | `O(STREAKS)` = 420 lần lặp, mỗi lần ~3 hàm băm + 1 `sqrt` + 1 `sin` + 1 `cos` |
| Cấp phát mỗi khung hình | **0** |
| Bộ nhớ | 65 KB cố định |
| GPU | 840 tam giác — không đáng kể |

Chi phí chính là `Math.sin`/`Math.cos`/`Math.sqrt` × 420 ≈ **1 260 lời gọi hàm siêu việt** mỗi khung hình. Có thể tối ưu bằng cách tính sẵn `(x, z)` một lần (chúng không đổi theo thời gian, chỉ theo vị trí camera), nhưng ở quy mô này không cần.

---

## 12. Chủ đề Toán thể hiện

- **Lấy mẫu đều trên đĩa** & phương pháp biến đổi nghịch đảo (`r = R√u`).
- **Hệ thống hạt không trạng thái** (stateless particle system).
- **Chuyển động tuần hoàn** bằng modulo + pha ngẫu nhiên.
- **Cylindrical billboard** & phép quay 90° quanh trục Y.
- **Xử lý trường hợp suy biến** (camera nhìn thẳng đứng).
- **Gradient alpha** mô phỏng motion blur.
- **Quản lý depth write vs depth test**.
- **Cấp phát trước & chỉ số bất biến**.

---

## 13. Liên kết

- Cường độ mưa: [WeatherSystem.md](WeatherSystem.md)
- Shader dùng chung: [SkyRenderer.md](SkyRenderer.md)
- Hàm băm anh em: [Deterministic.md](../01-noise-terrain/Deterministic.md)
