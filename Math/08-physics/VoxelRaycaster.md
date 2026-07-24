# VoxelRaycaster — thuật toán DDA (Amanatides & Woo)

**File:** `core/src/com/voxel/engine/physics/VoxelRaycaster.java`

Bắn một tia từ mắt người chơi để tìm khối đang ngắm. Thuật toán duyệt **đúng** các ô mà tia đi qua — không bỏ sót, không xét thừa.

---

## 1. Vì sao không lấy mẫu từng đoạn nhỏ?

Cách ngây thơ:

```java
for (float t = 0; t < 6; t += 0.05f) {
    Vector3 p = origin + direction * t;
    if (solid(floor(p))) return hit;
}
```

| Vấn đề | Hậu quả |
|---|---|
| Bước quá lớn | **Bỏ sót** khối mỏng — tia xuyên qua tường |
| Bước quá nhỏ | **Lãng phí** — cùng một ô bị kiểm tra hàng chục lần |
| Không biết mặt nào bị chạm | Không đặt được khối mới đúng phía |

Với bước `0.05` và tầm `6` khối: **120 lần kiểm tra** cho tối đa ~18 ô thực sự đi qua ⇒ lãng phí 85 %, mà vẫn có thể sót khối.

**DDA** (Digital Differential Analyzer) duyệt **đúng** số ô, mỗi ô đúng một lần, và biết chính xác mặt nào bị đâm vào.

---

## 2. Ba nhóm biến

```java
int x = (int) Math.floor(origin.x);          // ô hiện tại
int stepX = signum(direction.x);              // đi về phía nào: +1, −1, hoặc 0
float deltaX = Math.abs(1f / direction.x);    // đi hết BỀ RỘNG 1 ô tốn bao nhiêu t
float maxX = boundaryDistance(...);           // t để chạm biên ô KẾ TIẾP theo trục X
```

Tia được tham số hoá:

```
  P(t) = origin + t · direction
```

### 2.1 `delta` — chi phí vượt một ô

```
          1
  Δ_X = ──────
        |d_x|
```

**Ý nghĩa:** cần tăng `t` bao nhiêu để toạ độ `x` thay đổi đúng 1 đơn vị.

Kiểm chứng: `x(t) = o_x + t·d_x`. Muốn `Δx = 1` thì `t·d_x = 1` ⇒ `t = 1/d_x`. Lấy trị tuyệt đối vì `t` luôn dương.

**Trường hợp `d_x = 0`** (tia song song với trục X): không bao giờ vượt biên X ⇒ `Δ_X = +∞` (`Float.MAX_VALUE`). Tránh chia cho 0.

### 2.2 `max` — khoảng cách tới biên đầu tiên

```java
private static float boundaryDistance(float origin, int voxel, int step, float direction) {
    if (step == 0) return Float.MAX_VALUE;
    float boundary = step > 0 ? voxel + 1 - origin : origin - voxel;
    return boundary / Math.abs(direction);
}
```

```
            ⎧ (⌊o⌋ + 1 − o) / |d|      nếu đi về phía +
  t_max  =  ⎨
            ⎩ (o − ⌊o⌋)     / |d|      nếu đi về phía −
```

**Ví dụ:** `o_x = 3.7`, `d_x > 0` ⇒ biên tiếp theo ở `x = 4`, cách `4 − 3.7 = 0.3` đơn vị không gian ⇒ `t = 0.3/|d_x|`.

**Ví dụ ngược:** `o_x = 3.7`, `d_x < 0` ⇒ biên tiếp theo ở `x = 3`, cách `3.7 − 3 = 0.7` ⇒ `t = 0.7/|d_x|`.

---

## 3. Vòng lặp — luôn chọn trục chạm biên sớm nhất

```java
while (travelled <= maxDistance) {
    Direction face;
    if (maxX < maxY && maxX < maxZ) {
        x += stepX;  travelled = maxX;  maxX += deltaX;
        face = stepX > 0 ? Direction.WEST : Direction.EAST;
    } else if (maxY < maxZ) {
        y += stepY;  travelled = maxY;  maxY += deltaY;
        face = stepY > 0 ? Direction.DOWN : Direction.UP;
    } else {
        z += stepZ;  travelled = maxZ;  maxZ += deltaZ;
        face = stepZ > 0 ? Direction.NORTH : Direction.SOUTH;
    }
    if (travelled > maxDistance) return false;
    Block block = world.blockAt(x, y, z);
    if (isTarget(block)) { hit.set(x, y, z, face, travelled); return true; }
}
```

### Bất biến của vòng lặp

Ở mỗi thời điểm:

```
  maxX = giá trị t nhỏ nhất mà tia vượt biên X tiếp theo
  maxY = tương tự cho Y
  maxZ = tương tự cho Z
```

**Ô tiếp theo** là ô đạt được khi vượt biên **sớm nhất**:

```
  t_next = min(maxX, maxY, maxZ)
```

Chỉ cần **2 phép so sánh** để tìm min của 3 số.

### Cập nhật tăng dần

```java
maxX += deltaX;
```

Sau khi vượt biên X, biên X **tiếp theo** cách đúng một bề rộng ô ⇒ cộng `Δ_X`. Không cần tính lại từ đầu — đây là ý tưởng cốt lõi của DDA: **mọi thứ đều là phép cộng**, không có phép chia trong vòng lặp.

### Mặt bị đâm vào

| Trục vượt | `step > 0` | `step < 0` |
|---|---|---|
| X | **WEST** (mặt −X) | EAST (mặt +X) |
| Y | **DOWN** (mặt −Y) | UP (mặt +Y) |
| Z | **NORTH** (mặt −Z) | SOUTH (mặt +Z) |

Đi về phía **đông** thì đâm vào mặt **tây** của khối — luôn là mặt hướng **ngược** với chiều đi.

Nhờ đó `Hit.adjacentX/Y/Z()` cho biết chỗ đặt khối mới:

```java
public int adjacentX() { return x + face.dx(); }
```

Đặt khối ở phía mặt bị ngắm — đúng cách Minecraft hoạt động.

---

## 4. Ví dụ chạy tay

Tia từ `(0.5, 0.5, 0.5)` hướng `(0.8, 0.6, 0)` (đã chuẩn hoá).

```
  stepX = 1,   stepY = 1,   stepZ = 0
  ΔX = 1/0.8 = 1.25,   ΔY = 1/0.6 = 1.667,   ΔZ = ∞
  maxX = (1 − 0.5)/0.8 = 0.625
  maxY = (1 − 0.5)/0.6 = 0.833
  maxZ = ∞
```

| Bước | So sánh | Trục | Ô mới | `travelled` | `max` mới |
|---|---|---|---|---|---|
| 1 | `0.625 < 0.833` | **X** | `(1,0,0)` | 0.625 | `maxX = 1.875` |
| 2 | `0.833 < 1.875` | **Y** | `(1,1,0)` | 0.833 | `maxY = 2.500` |
| 3 | `1.875 < 2.500` | **X** | `(2,1,0)` | 1.875 | `maxX = 3.125` |
| 4 | `2.500 < 3.125` | **Y** | `(2,2,0)` | 2.500 | `maxY = 4.167` |
| 5 | `3.125 < 4.167` | **X** | `(3,2,0)` | 3.125 | `maxX = 4.375` |

Tia đi theo đúng "cầu thang" của các ô nó xuyên qua — không sót, không thừa.

---

## 5. Kiểm tra ô xuất phát

```java
Block start = world.blockAt(x, y, z);
if (isTarget(start)) {
    hit.set(x, y, z, Direction.UP, 0f);
    return true;
}
```

Vòng lặp bắt đầu bằng việc **bước sang ô kế tiếp**, nên ô chứa điểm xuất phát phải kiểm tra riêng. Trường hợp này xảy ra khi đầu người chơi kẹt trong khối.

`Direction.UP` là mặt mặc định (không có mặt nào thực sự bị đâm) — không quan trọng vì trường hợp này hiếm.

---

## 6. `isTarget` — bỏ qua chất lỏng

```java
private static boolean isTarget(Block block) {
    return !block.isAir() && !block.isLiquid();
}
```

Tia **xuyên qua nước** ⇒ người chơi đứng dưới nước vẫn phá được khối đá dưới đáy, không bị "ngắm trúng nước".

---

## 7. Độ phức tạp

### Số ô duyệt

Với tia dài `L` theo hướng `d̂ = (d_x, d_y, d_z)`, số biên vượt qua:

```
  N = L · (|d_x| + |d_y| + |d_z|)
```

Vì `d̂` chuẩn hoá (`|d| = 1`), theo bất đẳng thức giữa chuẩn `L¹` và `L²`:

```
  1 ≤ |d_x| + |d_y| + |d_z| ≤ √3 ≈ 1.732
```

| Hướng tia | `‖d‖₁` | Số ô cho `L = 6` |
|---|---|---|
| Dọc trục (1,0,0) | 1.000 | **6** |
| Chéo mặt (0.707, 0.707, 0) | 1.414 | 8.5 |
| Chéo khối (0.577×3) | **1.732** | **10.4** |

```
  N ∈ [L, L√3]  ⟹  O(L)
```

Với `REACH = 6`: tối đa **11 lần** kiểm tra khối.

So sánh với lấy mẫu bước `0.05`: **120 lần** ⇒ DDA nhanh hơn **~11 lần** và chính xác tuyệt đối.

### Chi phí mỗi bước

| Phép | Số lượng |
|---|---|
| So sánh số thực | 2 |
| Cộng số thực | 2 |
| Cộng số nguyên | 1 |
| Đọc khối | 1 |

**Không có phép chia, không có `sqrt`, không có nhân** trong vòng lặp — mọi phép chia đã làm xong ở phần khởi tạo.

### Bộ nhớ

`O(1)` — 12 biến cục bộ. Object `Hit` được **truyền vào** (`out parameter`) thay vì cấp phát mới ⇒ gọi mỗi khung hình mà không sinh rác.

---

## 8. Chủ đề DSA / Toán thể hiện

- **DDA / Bresenham 3D** — duyệt lưới bằng phép cộng tăng dần.
- **Tham số hoá tia** `P(t) = o + t·d`.
- **Chọn min của 3 số** bằng 2 phép so sánh.
- **Xử lý trường hợp suy biến** (`d = 0` → `+∞`).
- **Bất đẳng thức chuẩn** `‖d‖₂ ≤ ‖d‖₁ ≤ √3·‖d‖₂` để chặn số bước.
- **Out parameter** để tránh cấp phát.
- **Loại bỏ phép chia khỏi vòng lặp nóng**.

---

## 9. Ứng dụng trong game

| Tính năng | Dùng gì |
|---|---|
| Phá khối | `hit.blockX/Y/Z()` |
| Đặt khối | `hit.adjacentX/Y/Z()` |
| Vẽ khung ngắm | `hit.blockX/Y/Z()` + `hit.face()` |
| Kiểm tra tầm với | `hit.distance() ≤ REACH` |

---

## 10. Liên kết

- Hệ trục mặt: [Direction.md](../06-datastructures/Direction.md)
- Vật lý va chạm: [PhysicsWorld.md](PhysicsWorld.md)
- Nhắm đánh quái: [MeleeAim.md](../09-ai/MeleeAim.md)
