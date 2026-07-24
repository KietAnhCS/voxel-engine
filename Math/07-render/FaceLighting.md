# FaceLighting — ambient occlusion & đường cong ánh sáng

**File:** `core/src/com/voxel/engine/block/geometry/FaceLighting.java`

Tính màu cho **4 góc** của mỗi mặt khối. Đây là file có mật độ toán cao nhất trong phần dựng hình.

---

## 1. Đường cong ánh sáng — hàm mũ

```java
private static final float[] LIGHT_CURVE = new float[Block.MAX_LIGHT + 1];

static {
    for (int level = 0; level <= Block.MAX_LIGHT; level++) {
        LIGHT_CURVE[level] = Math.max(0.055f, (float) Math.pow(0.82, Block.MAX_LIGHT - level));
    }
}
```

### Công thức

```
  B(L) = max( 0.055 ,  0.82^(15 − L) ),      L = 0…15
```

### Bảng giá trị

| `L` | `0.82^(15−L)` | `B(L)` |
|---|---|---|
| 15 | 1.000 | **1.000** |
| 14 | 0.820 | 0.820 |
| 13 | 0.672 | 0.672 |
| 12 | 0.551 | 0.551 |
| 10 | 0.371 | 0.371 |
| 8 | 0.249 | 0.249 |
| 6 | 0.168 | 0.168 |
| 4 | 0.113 | 0.113 |
| 2 | 0.076 | 0.076 |
| 1 | 0.062 | 0.062 |
| 0 | 0.051 | **0.055** ← bị kẹp |

### Vì sao hàm mũ chứ không tuyến tính?

**Mắt người cảm nhận độ sáng theo hàm logarit** (định luật Weber–Fechner): để cảm thấy "sáng gấp đôi", cường độ vật lý phải tăng nhiều hơn gấp đôi.

Nếu dùng tuyến tính `B = L/15`:

| `L` | Tuyến tính | Cảm nhận |
|---|---|---|
| 15 → 14 | 1.00 → 0.93 | gần như không đổi |
| 2 → 1 | 0.13 → 0.07 | **tối sầm đột ngột** |

Hàm mũ giữ **tỉ lệ đều nhau** giữa các mức liên tiếp:

```
  B(L) / B(L−1) = 0.82   với mọi L
```

⇒ mỗi bước giảm sáng **cảm nhận như nhau** — không có chỗ nào nhảy vọt.

### Vì sao đúng 0.82?

```
  0.82^15 = 0.0513
```

Từ mức sáng tối đa xuống mức 0, độ sáng giảm còn ~5 % — đủ tối để hang trông đáng sợ nhưng vẫn thấy đường. Nếu chọn 0.75: `0.75^15 = 0.013` (1.3 %) — tối đen như mực.

### Sàn 0.055

```java
Math.max(0.055f, ...)
```

Đảm bảo **không bao giờ đen tuyệt đối**. Màn hình đen hoàn toàn khiến người chơi mất phương hướng; giữ 5.5 % cho phép nhận ra đường viền vật thể.

### Bảng tra thay vì `Math.pow`

`Math.pow` tốn ~50–100 chu kỳ CPU. Tính sẵn 16 giá trị vào mảng `static final` ⇒ mỗi lần dùng chỉ là **một lần đọc mảng**. Với 4 góc × hàng nghìn mặt × hàng trăm chunk, tiết kiệm khổng lồ.

---

## 2. Ambient Occlusion — bóng ở góc

### Nguyên lý

Góc lõm nhận ít ánh sáng phản xạ hơn mặt phẳng thoáng. AO mô phỏng hiệu ứng này bằng cách đếm số khối chắn quanh mỗi **góc** của mặt.

### Ba ô cần kiểm tra

Với mỗi góc, xét 3 ô nằm **trên mặt phẳng ngay trước mặt đang vẽ**:

```java
int nx = x + face.dx();      // ô ngay trước mặt
...
int uSign = (corner == 1 || corner == 2) ? 1 : -1;
int vSign = (corner == 2 || corner == 3) ? 1 : -1;

// A: lệch theo tiếp tuyến u
int ax = nx + face.tangentX() * uSign;   ...
// B: lệch theo tiếp tuyến v
int bx = nx + face.bitangentX() * vSign; ...
// C: lệch theo CẢ HAI (đường chéo)
int cx = ax + face.bitangentX() * vSign; ...

boolean sideA    = view.occludes(ax, ay, az, block);
boolean sideB    = view.occludes(bx, by, bz, block);
boolean diagonal = view.occludes(cx, cy, cz, block);
```

Sơ đồ (nhìn thẳng vào mặt):

```
        B ──── C
        │      │
        │  ●   │      ● = góc đang tính
        │      │      N = ô ngay trước mặt (tâm)
        N ──── A
```

### Bảng dấu 4 góc

| `corner` | `uSign` | `vSign` | Vị trí |
|---|---|---|---|
| 0 | −1 | −1 | góc `P(0,0)` |
| 1 | +1 | −1 | góc `P(1,0)` |
| 2 | +1 | +1 | góc `P(1,1)` |
| 3 | −1 | +1 | góc `P(0,1)` |

Khớp đúng thứ tự 4 đỉnh mà [`ChunkMesher.quad`](ChunkMesher.md) sinh ra.

### Công thức AO

```java
int occlusion = sideA && sideB ? 0 : 3 - (count(sideA) + count(sideB) + count(diagonal));
```

```
             ⎧ 0                                nếu A ∧ B
  occ    =   ⎨
             ⎩ 3 − (⟦A⟧ + ⟦B⟧ + ⟦C⟧)           ngược lại
```

### Bảng đầy đủ

| A | B | C | `occ` | Ý nghĩa |
|---|---|---|---|---|
| 0 | 0 | 0 | **3** | Góc hoàn toàn thoáng — sáng nhất |
| 0 | 0 | 1 | 2 | Chỉ đường chéo bị chắn |
| 0 | 1 | 0 | 2 | Một cạnh bị chắn |
| 1 | 0 | 0 | 2 | Một cạnh bị chắn |
| 0 | 1 | 1 | 1 | Cạnh + chéo |
| 1 | 0 | 1 | 1 | Cạnh + chéo |
| 1 | 1 | 0 | **0** ← đặc biệt | |
| 1 | 1 | 1 | **0** | Góc kín hoàn toàn — tối nhất |

### Vì sao `A ∧ B → 0` bất kể `C`?

Nếu **cả hai cạnh** bị chắn, ô đường chéo `C` **bị kẹt trong góc** — dù nó là không khí thì cũng không có ánh sáng nào tới được. Đây là trường hợp đặc biệt kinh điển của thuật toán AO cho voxel (0fps.net, 2013).

Không có luật này, một góc kín có `C` rỗng sẽ nhận `occ = 1` (hơi sáng) trong khi nó phải tối nhất ⇒ hiện tượng "góc bị rò sáng".

### Đường cong AO

```java
private static final float[] OCCLUSION_CURVE = {0.42f, 0.60f, 0.79f, 1.00f};
```

| `occ` | Hệ số | Ghi chú |
|---|---|---|
| 0 | **0.42** | Góc kín — tối 58 % |
| 1 | 0.60 | |
| 2 | 0.79 | |
| 3 | **1.00** | Thoáng — không giảm |

**Bước nhảy:** `0.42 → 0.60 → 0.79 → 1.00`, chênh lệch `0.18, 0.19, 0.21` — gần như tuyến tính, hơi cong lên. Được chỉnh tay để bóng góc rõ mà không quá gắt.

---

## 3. Ánh sáng làm mượt (smooth lighting)

Thay vì lấy mức sáng của một ô, **lấy trung bình các ô quanh góc**:

```java
int skySum = view.skyLightAt(nx, ny, nz);
int torchSum = view.blockLightAt(nx, ny, nz);
int lightCount = 1;

if (!sideA)                        { skySum += ...(a); torchSum += ...(a); lightCount++; }
if (!sideB)                        { skySum += ...(b); torchSum += ...(b); lightCount++; }
if (!diagonal && !(sideA && sideB)){ skySum += ...(c); torchSum += ...(c); lightCount++; }

int skyLevel   = min(15, skySum   / lightCount);
int torchLevel = min(15, torchSum / lightCount);
int level      = max(skyLevel, torchLevel);
```

### Công thức

```
             1     ___
  L̄  =      ───  ·  ╲   L(q)
            |Q|     ╱
                    ‾‾‾
                   q ∈ Q
```

với `Q` = tập các ô **không bị chắn** trong 4 ô `{N, A, B, C}` (`N` luôn thuộc `Q`).

### Vì sao chỉ lấy trung bình ô KHÔNG bị chắn?

Ô bị chắn là khối đặc — mức sáng lưu trong đó **vô nghĩa** (thường bằng 0). Đưa vào trung bình sẽ kéo góc tối đi một cách sai lệch.

Điều kiện `!(sideA && sideB)` lặp lại luật "góc kín" ở trên: nếu hai cạnh chắn, ô chéo không đóng góp ánh sáng.

### Hiệu ứng

Vì mỗi góc lấy trung bình **khác nhau**, 4 góc của một mặt có **4 độ sáng khác nhau**. GPU nội suy tuyến tính giữa chúng khi tô tam giác ⇒ **chuyển sáng mượt** trên bề mặt, thay vì mỗi khối một màu phẳng lì (flat lighting).

Đây chính là điều Minecraft gọi là "Smooth Lighting".

---

## 4. `skyShare` — mẹo để đổi ngày/đêm không cần dựng lại mesh

```java
private static float skyShare(int skyLevel, int torchLevel) {
    float sky = LIGHT_CURVE[skyLevel];
    float torch = LIGHT_CURVE[torchLevel];
    return sky <= torch ? 0f : (sky - torch) / sky;
}
```

### Công thức

```
              ⎧ 0                        nếu B(sky) ≤ B(torch)
  share  =    ⎨
              ⎩ (B(sky) − B(torch)) / B(sky)     ngược lại
```

### Ý nghĩa

Bao nhiêu phần độ sáng tại đây đến **từ bầu trời** (0 = không phụ thuộc trời, 1 = hoàn toàn từ trời).

| Tình huống | `sky` | `torch` | `share` |
|---|---|---|---|
| Ngoài trời | 15 | 0 | **1.00** — trời tối là tối theo |
| Dưới đuốc trong hang | 0 | 14 | **0.00** — trời tối không ảnh hưởng |
| Cửa hang có đuốc | 10 | 12 | 0.00 (đuốc mạnh hơn) |
| Cửa hang đuốc yếu | 12 | 8 | `(0.551−0.249)/0.551` = **0.55** |

### Vì sao quan trọng?

> Comment: *"Nhờ phần lẻ đó mà khi mặt trời lặn, shader chỉ làm tối phần ánh sáng trời và giữ nguyên quầng đuốc/đèn — đúng như Minecraft, không phải băm lại lưới cả thế giới mỗi lần trời tối."*

Không có `share`, mỗi lần `daylight` thay đổi phải **dựng lại mesh của toàn bộ thế giới** (hàng trăm chunk × hàng chục nghìn phép) — vài lần mỗi giây. Với `share`, shader tự tính:

```
  màu cuối = màu đỉnh × (1 − share + share · daylight)
```

Chỉ cần đổi **một uniform** `u_daylight`.

---

## 5. Đóng gói vào kênh alpha

```java
private static float packSkyShare(float share, boolean windy) {
    float clamped = Math.max(0f, Math.min(0.999f, share));
    return (windy ? 0f : 0.5f) + clamped * 0.5f;
}
```

### Sơ đồ

```
  alpha ∈ [0.0, 0.5)  →  khối ĐUA THEO GIÓ (cỏ, hoa),  share = alpha × 2
  alpha ∈ [0.5, 1.0)  →  khối thường,                  share = (alpha − 0.5) × 2
```

**Hai thông tin trong một số thực:**
- **Phần nguyên** (nửa trên/nửa dưới): có lắc theo gió không.
- **Phần lẻ**: tỉ lệ ánh sáng trời.

Shader giải nén:

```glsl
bool windy = a_color.a < 0.5;
float skyShare = fract(a_color.a * 2.0);   // hoặc (a - 0.5) * 2
```

**Vì sao phải nhét vào alpha?** Định dạng đỉnh chỉ có 4 kênh màu (RGBA). RGB đã dùng cho màu thật. Thêm một thuộc tính đỉnh mới sẽ tốn thêm 4 byte/đỉnh × hàng triệu đỉnh. Đóng gói vào alpha là **miễn phí**.

Kẹp `0.999` để `share = 1` không tràn sang khoảng của loại khối khác.

---

## 6. `SHADER_BIAS` — bù trừ cho shader

```java
private static final float SHADER_BIAS = 0.25f;

out[corner].set(
    tintBuffer.r * brightness + SHADER_BIAS,
    tintBuffer.g * brightness + SHADER_BIAS,
    tintBuffer.b * brightness + SHADER_BIAS,
    ...);
```

Cộng hằng `0.25` vào RGB. Shader sẽ trừ lại phần này khi áp `daylight`. Mục đích: giữ giá trị màu đỉnh **không quá gần 0**, nơi độ chính xác của định dạng màu 8-bit (`1/255 ≈ 0.004`) gây ra dải màu răng cưa (banding) ở vùng tối.

---

## 7. Công thức màu cuối cùng

```
  brightness = B(L) × AO(occ) × shade(face)

  color.rgb  = tint.rgb × brightness + 0.25
  color.a    = pack(skyShare, windy)
```

**Ba hệ số nhân độc lập:**

| Hệ số | Nguồn | Miền |
|---|---|---|
| `B(L)` | Mức sáng BFS | `[0.055, 1.00]` |
| `AO(occ)` | Ambient occlusion | `[0.42, 1.00]` |
| `shade(face)` | Hướng mặt | `[0.48, 1.00]` — xem [Direction](../06-datastructures/Direction.md) |

**Tích tối thiểu:** `0.055 × 0.42 × 0.48 = 0.011` — góc kín, mặt dưới, trong hang tối.
**Tích tối đa:** `1.00 × 1.00 × 1.00 = 1.00` — mặt trên, thoáng, giữa trưa.

`tint` là màu riêng của khối (cỏ xanh theo biome, lá cây…), lấy từ `block.tint().apply(x, y, z, ...)`.

---

## 8. `flat` — phiên bản rút gọn

```java
public void flat(BlockView view, int x, int y, int z, Block block, float shade, Color out) {
    int skyLevel   = min(15, view.skyLightAt(x, y, z));
    int torchLevel = min(15, view.blockLightAt(x, y, z));
    int level = max(skyLevel, torchLevel);
    float brightness = LIGHT_CURVE[level] * shade;
    ...
}
```

**Không có AO, không lấy trung bình** — chỉ đọc 1 ô. Dùng cho cỏ/hoa (`CrossGeometry`): chúng là tấm mỏng không có góc lõm, AO vô nghĩa và sẽ làm chúng tối vô cớ.

Chi phí: **1** lần đọc ánh sáng thay vì 4 góc × 4 ô = 16 lần.

---

## 9. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `LIGHT_CURVE` khởi tạo | `O(16)` — một lần cho cả chương trình |
| `shade()` một mặt | 4 góc × (3 `occludes` + ≤4 lần đọc sáng × 2 kênh) = **~44 lần truy cập** |
| `flat()` | **2 lần truy cập** |
| Bộ nhớ | `O(1)` — 2 mảng hằng 16 + 4 phần tử, 1 `Color` tái dùng |

Với ~2 000 mặt/chunk: `2 000 × 44 ≈ 88 000` lần truy cập — đây là **phần nặng nhất của việc dựng mesh**, nhưng đổi lại chất lượng hình ảnh cao hơn hẳn flat lighting.

---

## 10. Chủ đề DSA / Toán thể hiện

- **Bảng tra tính sẵn** thay `Math.pow` — `LIGHT_CURVE`, `OCCLUSION_CURVE`.
- **Hàm mũ & định luật Weber–Fechner** — cảm nhận độ sáng logarit.
- **Ambient occlusion** — thuật toán đếm ô chắn 3 điểm.
- **Trung bình có điều kiện** — smooth lighting.
- **Đóng gói 2 thông tin vào 1 float** (kênh alpha).
- **Tách biến để tránh tính lại** — `skyShare` cho phép đổi ngày đêm bằng 1 uniform.
- **Nội suy tam giác của GPU** — tận dụng phần cứng để làm mượt.

---

## 11. Liên kết

- Nguồn mức sáng: [LightEngine.md](../05-world/LightEngine.md)
- Hệ trục & `shade`: [Direction.md](../06-datastructures/Direction.md)
- Người gọi: [ChunkMesher.md](ChunkMesher.md), [Geometries.md](Geometries.md)
- Shader dùng `skyShare`: [Shaders.md](Shaders.md)
