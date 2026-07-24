# PlayerController — vector di chuyển & xoay camera

**File:** `core/src/com/voxel/engine/input/PlayerController.java`

---

## 1. Từ phím bấm sang vector di chuyển

```java
float forwardAxis = 0f, strafeAxis = 0f;
if (W) forwardAxis += 1f;
if (S) forwardAxis -= 1f;
if (D) strafeAxis  += 1f;
if (A) strafeAxis  -= 1f;
```

**Cộng dồn thay vì `if/else`:** giữ W và S cùng lúc ⇒ `+1 − 1 = 0` ⇒ đứng yên. Đúng trực giác và không cần xử lý riêng.

---

## 2. Hệ trục địa phương của người chơi

```java
forward.set(camera.direction.x, 0f, camera.direction.z).nor();
right.set(forward).crs(Vector3.Y).nor();
```

### Công thức

```
  f̂ = normalize( (d_x, 0, d_z) )        ← chiếu hướng nhìn xuống MẶT PHẲNG NGANG
  r̂ = normalize( f̂ × ŷ )
```

### Vì sao phải chiếu xuống mặt phẳng ngang?

Nếu dùng thẳng `camera.direction`, khi người chơi **ngước lên trời** rồi bấm W, thành phần `y` khác 0 ⇒ nhân vật **bay lên**. Đặt `y = 0` rồi chuẩn hoá lại giữ chuyển động luôn nằm ngang.

Sau khi đặt `y = 0`, độ dài vector giảm còn `√(d_x² + d_z²) = cos(pitch)` ⇒ **bắt buộc** chuẩn hoá lại, nếu không ngước lên sẽ đi chậm hơn nhìn ngang.

### Tích có hướng cho hướng phải

```
        │ i    j    k  │
  f̂×ŷ = │ f_x  0   f_z │ = i(0·0 − f_z·1) − j(0) + k(f_x·1 − 0)
        │ 0    1    0  │
      = (−f_z, 0, f_x)
```

Đây chính là phép **quay 90° quanh trục Y** — cùng công thức với billboard của [RainRenderer](../07-render/RainRenderer.md).

Vì `f̂` đã chuẩn hoá và vuông góc với `ŷ`, tích có hướng đã có độ dài 1; `.nor()` chỉ để phòng sai số dấu phẩy động.

`{f̂, ŷ, r̂}` tạo thành **cơ sở trực chuẩn** trong đó người chơi di chuyển.

---

## 3. Tổ hợp & chuẩn hoá đường chéo

```java
float moveX = right.x * strafeAxis + forward.x * forwardAxis;
float moveZ = right.z * strafeAxis + forward.z * forwardAxis;

float length = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
if (length > 1f) { moveX /= length; moveZ /= length; }
```

### Công thức

```
  m⃗ = a_strafe · r̂ + a_forward · f̂
  m⃗ ← m⃗ / max(1, ‖m⃗‖)
```

### Bài toán "chạy chéo nhanh hơn"

Giữ **W + D** cùng lúc: `a_forward = 1`, `a_strafe = 1`.

```
  ‖m⃗‖ = √(1² + 1²) = √2 ≈ 1.414
```

⇒ Không chuẩn hoá, người chơi chạy chéo **nhanh hơn 41.4 %**. Đây là lỗi kinh điển trong game FPS ("strafe running" / "bunny hopping" của Quake).

Chuẩn hoá đưa về đúng tốc độ 1.

### Vì sao `if (length > 1f)` chứ không chia luôn?

Khi chỉ bấm một phím, `‖m⃗‖ = 1` ⇒ chia là thừa. Khi **không bấm gì**, `‖m⃗‖ = 0` ⇒ chia cho 0 cho ra `NaN` — làm hỏng toàn bộ vật lý.

Điều kiện `> 1` xử lý cả hai: chỉ can thiệp khi thực sự vượt.

---

## 4. Xoay camera — yaw và pitch

```java
float sensitivity = GameSettings.get().mouseSensitivity();
float yaw = -Gdx.input.getDeltaX() * sensitivity;
float pitchDelta = -Gdx.input.getDeltaY() * sensitivity;

camera.direction.rotate(Vector3.Y, yaw);

rotationAxis.set(camera.direction).crs(Vector3.Y).nor();
float currentPitch = (float) Math.toDegrees(Math.asin(camera.direction.y));
float clamped = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, currentPitch + pitchDelta));
camera.direction.rotate(rotationAxis, clamped - currentPitch);

camera.up.set(Vector3.Y);
```

### 4.1 Yaw — quay quanh trục dọc

```
  d̂ ← R_y(−Δx · s) · d̂
```

Trục quay là `ŷ` **toàn cục** (không phải trục địa phương) ⇒ chân trời luôn nằm ngang.

Dấu âm vì kéo chuột sang phải (`Δx > 0`) phải quay camera sang phải, mà quay dương quanh `+Y` là ngược chiều kim đồng hồ nhìn từ trên.

### 4.2 Pitch — trích xuất góc hiện tại

```java
float currentPitch = Math.toDegrees(Math.asin(camera.direction.y));
```

```
  pitch = arcsin(d_y)
```

Vì `d̂` là **vector đơn vị**, thành phần `y` chính là `sin` của góc nâng:

```
  d_y = sin(pitch)   ⟹   pitch = arcsin(d_y)
```

`arcsin` trả giá trị trong `[−90°, +90°]` — chính xác miền của pitch. Không có mơ hồ như `arctan`.

### 4.3 Kẹp góc & xoay bù

```java
float clamped = clamp(currentPitch + pitchDelta, −89°, +89°);
camera.direction.rotate(rotationAxis, clamped - currentPitch);
```

**Không** xoay thẳng `pitchDelta`, mà tính **góc thực sự cần xoay** = `clamped − currentPitch`.

Nhờ đó khi đã ở giới hạn (`currentPitch = 89°`) và người chơi tiếp tục kéo lên:

```
  clamped = min(89, 89 + 5) = 89
  góc xoay = 89 − 89 = 0        ✔ không xoay
```

Nếu xoay thẳng `pitchDelta` rồi mới kẹp, camera sẽ **lật ngược** trước khi bị sửa ⇒ giật hình.

### 4.4 `MAX_PITCH = 89°` — vì sao không 90°?

Tại đúng `90°`, hướng nhìn `d̂ = (0, 1, 0)` ⇒ song song với `ŷ` ⇒

```
  rotationAxis = d̂ × ŷ = 0
```

`.nor()` chia cho 0 ⇒ `NaN` ⇒ camera hỏng vĩnh viễn.

Đây là hiện tượng **khoá gimbal** (gimbal lock) của hệ Euler. Kẹp ở `89°` giữ `rotationAxis` luôn có độ dài `≥ sin(1°) ≈ 0.0175` — đủ để chuẩn hoá an toàn.

### 4.5 Trục xoay pitch

```java
rotationAxis.set(camera.direction).crs(Vector3.Y).nor();
```

```
  â = normalize( d̂ × ŷ )
```

Vector vuông góc với **cả hướng nhìn lẫn trục dọc** ⇒ đúng trục "ngang qua tầm mắt". Đây chính là `r̂` (hướng phải) tính lại.

### 4.6 Khoá `camera.up`

```java
camera.up.set(Vector3.Y);
```

Đặt lại mỗi khung hình để chống **trôi roll** (nghiêng đầu). Sau nhiều phép quay liên tiếp, sai số dấu phẩy động tích luỹ khiến `up` lệch dần khỏi trục dọc ⇒ chân trời nghiêng. Gán cứng loại bỏ hoàn toàn.

---

## 5. Nhấn đúp SPACE để bay

```java
private static final long DOUBLE_TAP_WINDOW_MS = 260L;

if (keycode == Input.Keys.SPACE && !jumpHeld && flightAllowed) {
    jumpHeld = true;
    long now = System.currentTimeMillis();
    if (now - lastJumpTapMillis < DOUBLE_TAP_WINDOW_MS) {
        input.requestFlightToggle();
        lastJumpTapMillis = 0L;
    } else {
        lastJumpTapMillis = now;
    }
}
```

### Máy trạng thái phát hiện nhấn đúp

```
  ┌─────────┐  SPACE (t)   ┌──────────────┐  SPACE (t' − t < 260ms)  ┌────────┐
  │  IDLE   │─────────────►│ CHỜ LẦN 2    │─────────────────────────►│ TOGGLE │
  └─────────┘              │ lastTap = t  │                          └────────┘
       ▲                   └──────┬───────┘
       │                          │ SPACE (t' − t ≥ 260ms)
       └──────────────────────────┘ lastTap = t'
```

### Ba chi tiết

**`!jumpHeld`** — chỉ tính **cạnh xuống** (key-down edge). Không có nó, giữ SPACE sẽ tạo sự kiện lặp liên tục và kích hoạt bay ngay.

**`lastJumpTapMillis = 0L`** sau khi kích hoạt — **tiêu thụ** lần nhấn, tránh lần nhấn thứ 3 lại toggle ngay. Nếu không reset, nhấn ba lần nhanh sẽ bật rồi tắt bay.

**`flightAllowed`** — chế độ sinh tồn chặn hoàn toàn nhánh này.

**260 ms** là ngưỡng kinh nghiệm: đủ dài để tay bình thường nhấn kịp, đủ ngắn để hai cú nhảy liên tiếp có chủ ý không bị hiểu nhầm.

---

## 6. Hàng đợi tương tác an toàn đa luồng

```java
private final Deque<InteractionRequest> requests = new ArrayDeque<InteractionRequest>();

public InteractionRequest pollRequest() {
    synchronized (requests) { return requests.pollFirst(); }
}

// trong touchDown:
synchronized (requests) { requests.addLast(request); }
```

`touchDown` chạy trên **luồng sự kiện đầu vào**, `pollRequest` chạy trên **luồng game**. `synchronized` bảo vệ `ArrayDeque` (vốn không thread-safe).

**FIFO** (`addLast` / `pollFirst`) — thao tác được xử lý đúng thứ tự người chơi thực hiện. Khác [ChunkScheduler](../05-world/ChunkScheduler.md) dùng LIFO cho việc gấp.

Dùng hàng đợi thay vì xử lý ngay để **không bỏ sót** cú click xảy ra giữa hai khung hình.

---

## 7. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `MAX_PITCH` | 89° | Chống khoá gimbal |
| `DOUBLE_TAP_WINDOW_MS` | 260 ms | Cửa sổ nhấn đúp |
| `mouseSensitivity` | từ Settings | độ/pixel |

---

## 8. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `poll` | `O(1)` — 4 kiểm tra phím, 1 tích có hướng, 1 `sqrt` |
| `applyMouseLook` | `O(1)` — 1 `asin`, 2 phép quay, 1 tích có hướng |
| `pollRequest` | `O(1)` |

Cả hai chạy **một lần mỗi khung hình** — chi phí không đáng kể.

---

## 9. Chủ đề Toán thể hiện

- **Phép chiếu vector** xuống mặt phẳng + chuẩn hoá lại.
- **Tích có hướng** để dựng cơ sở trực chuẩn.
- **Chuẩn hoá có điều kiện** — sửa lỗi "chạy chéo nhanh hơn", tránh chia 0.
- **`arcsin` để trích góc** từ vector đơn vị.
- **Kẹp góc bằng hiệu** thay vì kẹp sau khi xoay.
- **Khoá gimbal** & vì sao 89° chứ không 90°.
- **Chống tích luỹ sai số** bằng cách gán cứng `up`.
- **Máy trạng thái phát hiện nhấn đúp**.
- **Phát hiện cạnh** (edge detection) cho phím bấm.

---

## 10. Liên kết

- Áp vector di chuyển: [MovementStates.md](MovementStates.md)
- Hộp va chạm: [PhysicsWorld.md](PhysicsWorld.md)
- Ngắm khối: [VoxelRaycaster.md](VoxelRaycaster.md)
