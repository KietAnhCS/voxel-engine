# MovementStates — State pattern cho di chuyển

**Files:** `core/src/com/voxel/engine/physics/state/{GroundState, SwimmingState, FlightState}.java`

**Design pattern:** **State** — mỗi trạng thái là một lớp, `update()` trả về trạng thái kế tiếp.

---

## 1. Máy trạng thái

```java
public interface MovementState {
    void enter(PlayerBody body);
    MovementState update(PlayerBody body, MovementInput input, float delta);
    String label();
}
```

```
                 nhấn đúp SPACE
        ┌──────────────────────────────┐
        │                              ▼
   ┌─────────┐   ngập nước      ┌──────────────┐
   │ GROUND  │◄────────────────►│  SWIMMING    │
   └─────────┘   nhô lên        └──────────────┘
        ▲                              │
        │ nhấn đúp SPACE               │ nhấn đúp SPACE
        │        ┌──────────────┐      │
        └────────│   FLIGHT     │◄─────┘
                 └──────────────┘
```

| Chuyển | Điều kiện |
|---|---|
| GROUND → SWIMMING | `body.isSubmerged()` |
| SWIMMING → GROUND | `!body.isSubmerged()` |
| bất kỳ → FLIGHT | `input.consumeFlightToggle()` |
| FLIGHT → GROUND | `input.consumeFlightToggle()` |

`update()` trả `this` khi không đổi trạng thái ⇒ nơi gọi chỉ cần:

```java
MovementState next = state.update(body, input, delta);
if (next != state) { state = next; state.enter(body); }
```

---

## 2. Bảng tham số ba trạng thái

| | GROUND | SWIMMING | FLIGHT |
|---|---|---|---|
| Trọng lực | **−26** | **0** | **0** |
| Tốc độ ngang | 0.115 | 0.045 | 0.240 |
| Điều khiển dọc | nhảy (xung lực) | trực tiếp | trực tiếp |
| Lên | `jump(8.6)` | `+0.032` | `+0.20` |
| Xuống | — | `−0.030` | `−0.20` |
| Thả phím | rơi tự do | `−0.008` (chìm) | **đứng yên** |
| `clearVelocity` khi vào | không | **có** | **có** |

### Đổi sang khối/giây

Bộ vật lý chạy **120 bước/giây** (`fixedTimeStep = 1/120`), `walkDirection` là **dịch chuyển mỗi bước**:

```
  v (khối/s) = walkDirection × 120
```

| Trạng thái | Giá trị | Tốc độ thực |
|---|---|---|
| Đi bộ trên đất | 0.115 | **13.8 khối/s** |
| Bơi ngang | 0.045 | **5.4 khối/s** |
| Bay ngang | 0.240 | **28.8 khối/s** |
| Bơi lên | 0.032 | **3.84 khối/s** |
| Bơi xuống | 0.030 | 3.6 khối/s |
| Chìm tự nhiên | 0.008 | **0.96 khối/s** |
| Bay lên/xuống | 0.200 | 24 khối/s |

Comment trong `SwimmingState` xác nhận: *"0.032 cho ra khoảng 3.8 khối/giây — đúng nhịp bơi của Minecraft"* ✔

**Tỉ lệ:** bơi bằng **39 %** tốc độ đi bộ, bay nhanh **gấp 2.1 lần** đi bộ.

> Lưu ý: `btKinematicCharacterController` áp thêm nội bộ một hệ số theo bước thời gian, nên con số "13.8 khối/s" là tốc độ danh nghĩa của `walkDirection`, không phải tốc độ quan sát được cuối cùng. Điều quan trọng là **tỉ lệ giữa ba trạng thái**.

---

## 3. GroundState — trọng lực & nhảy

```java
private static final float GRAVITY = -26f;
private static final float JUMP_IMPULSE = 8.6f;
private static final float WATER_HOP_IMPULSE = 5.4f;

body.setWalkDirection(input.axisX() * SPEED, 0f, input.axisZ() * SPEED);

if (input.rise()) {
    if (body.onGround())            body.jump(JUMP_IMPULSE);
    else if (body.isFeetInWater())  body.jump(WATER_HOP_IMPULSE);
}
```

### Độ cao nhảy

```
        v₀²      8.6²      73.96
  h =  ─────  = ──────  = ───────  ≈ 1.42 khối
        2g      2 × 26      52
```

⇒ Nhảy lên được khối cao **1 đơn vị** (cần ~1.0–1.2 khối tính cả `stepHeight`), nhưng **không** lên được 2 khối. Đúng quy tắc Minecraft.

### Thời gian nhảy

```
        v₀      8.6
  t↑ = ────  = ──── ≈ 0.331 s        (lên tới đỉnh)
         g      26

  T = 2t↑ ≈ 0.662 s                   (cả cú nhảy)
```

### Cú nhún dưới nước

```java
else if (body.isFeetInWater()) body.jump(WATER_HOP_IMPULSE);
```

> Comment: *"Nhờ nó mà bơi vào bờ rồi giữ phím cách là trèo được lên bờ, đúng như Minecraft — chứ không bị kẹt mãi ở mép nước vì chân không chạm đất nên không nhảy được."*

Tình huống: người chơi bơi tới bờ, **đầu đã nhô lên** khỏi mặt nước ⇒ chuyển sang `GroundState`, nhưng **chân vẫn trong nước** ⇒ `onGround() == false` ⇒ không nhảy được ⇒ **kẹt vĩnh viễn**.

`WATER_HOP_IMPULSE = 5.4` cho phép nhún:

```
        5.4²
  h =  ──────  ≈ 0.56 khối
       2 × 26
```

Đủ để nhô lên nửa khối mỗi lần nhún ⇒ giữ SPACE là trèo dần lên bờ. Nhỏ hơn cú nhảy thường (1.42) nên không thể "nhảy trên mặt nước".

---

## 4. SwimmingState — vì sao tắt trọng lực

```java
public void enter(PlayerBody body) {
    body.setGravity(0f);
    body.clearVelocity();
}
```

> Comment: *"Trong nước KHÔNG dùng trọng lực của bộ vật lý: nếu để trọng lực chạy, vận tốc rơi cứ tích luỹ mãi (dưới nước không bao giờ 'chạm đất' để xoá) — ngâm càng lâu càng chìm nhanh và giữ phím cách cũng không nổi lên được. Thay vào đó vận tốc dọc được đặt TRỰC TIẾP mỗi bước, nên bơi lên / chìm xuống lúc nào cũng đều và đoán trước được."*

### Phân tích toán học

**Với trọng lực bật:** vận tốc dọc tích phân liên tục

```
  v(t) = v₀ − g·t
```

Character controller chỉ **xoá vận tốc khi chạm đất** (`onGround`). Dưới nước sâu, `onGround` luôn `false` ⇒ `v` giảm không giới hạn.

Sau 5 giây ngâm nước: `v = −26 × 5 = −130` khối/s. Lực bơi lên `+3.84` khối/s **không thể nào** thắng nổi.

**Với trọng lực tắt:** vận tốc dọc được **gán** mỗi bước:

```
  v_y = { +0.032   nếu giữ SPACE
        { −0.030   nếu giữ SHIFT
        { −0.008   nếu thả cả hai
```

Không tích phân ⇒ không tích luỹ ⇒ hành vi **đoán trước được** và **đảo chiều tức thì**.

### `clearVelocity()` khi vào nước

Xoá vận tốc rơi mang theo từ trên không. Không có nó, nhảy từ vách đá cao xuống hồ sẽ chìm thẳng xuống đáy với vận tốc `−40` khối/s.

> Comment: *"nước 'đỡ' cú rơi lại ngay khi chạm nước"*

Về mặt vật lý, đây là mô hình **lực cản nhớt vô hạn** — chất lỏng hấp thụ toàn bộ động năng tức thì. Không đúng vật lý thật (thực tế có giai đoạn giảm tốc), nhưng cho cảm giác game tốt hơn.

### Ba mức vận tốc dọc

```
  SWIM_UP    = 0.032    → +3.84 khối/s
  SWIM_DOWN  = 0.030    → −3.60 khối/s
  DRIFT_DOWN = 0.008    → −0.96 khối/s
```

`DRIFT_DOWN` nhỏ hơn `SWIM_DOWN` **3.75 lần** — thả phím thì **chìm từ từ**, mô phỏng độ nổi gần trung tính của cơ thể người. Chủ động lặn xuống nhanh gấp gần 4 lần.

`SWIM_UP` **hơi lớn hơn** `SWIM_DOWN` (0.032 vs 0.030) — bơi lên dễ hơn lặn xuống một chút, giúp người chơi không bị chết đuối.

---

## 5. FlightState — điều khiển tuyệt đối

```java
private static final float SPEED = 0.24f;
private static final float VERTICAL_SPEED = 0.2f;

float vertical = 0f;
if (input.rise()) vertical += VERTICAL_SPEED;
if (input.sink()) vertical -= VERTICAL_SPEED;

body.setWalkDirection(input.axisX() * SPEED, vertical, input.axisZ() * SPEED);
```

**Cộng dồn** thay vì `if/else`: giữ cả SPACE và SHIFT ⇒ `+0.2 − 0.2 = 0` ⇒ **đứng yên tại chỗ**. Cùng kỹ thuật với trục W/S trong [PlayerController](PlayerController.md).

Thả cả hai phím ⇒ `vertical = 0` ⇒ **lơ lửng bất động**. Khác `SwimmingState` (chìm từ từ) vì bay là chế độ sáng tạo, người chơi cần đứng yên để xây.

Tốc độ ngang `0.24` = **2.1 lần** tốc độ đi bộ — di chuyển nhanh khi xây dựng.

---

## 6. `consumeFlightToggle` — cờ dùng một lần

```java
if (input.consumeFlightToggle()) return new FlightState();
```

Tên `consume` cho biết đây là thao tác **đọc-rồi-xoá** (test-and-clear):

```java
public boolean consumeFlightToggle() {
    boolean value = flightToggle;
    flightToggle = false;
    return value;
}
```

Nếu chỉ đọc mà không xoá, cờ vẫn bật ở khung hình sau ⇒ `FlightState.update` lại thấy `true` ⇒ **chuyển ngay về GroundState** ⇒ bay bật/tắt liên tục mỗi khung hình.

Đây là mẫu chuẩn cho **sự kiện một lần** (one-shot event) trong vòng lặp game.

---

## 7. Bảng hằng số đầy đủ

| Hằng | Lớp | Giá trị | Khối/s |
|---|---|---|---|
| `GRAVITY` | Ground | −26 | khối/s² |
| `SPEED` | Ground | 0.115 | 13.8 |
| `JUMP_IMPULSE` | Ground | 8.6 | cao 1.42 khối |
| `WATER_HOP_IMPULSE` | Ground | 5.4 | cao 0.56 khối |
| `SPEED` | Swimming | 0.045 | 5.4 |
| `SWIM_UP` | Swimming | 0.032 | 3.84 |
| `SWIM_DOWN` | Swimming | 0.030 | 3.60 |
| `DRIFT_DOWN` | Swimming | 0.008 | 0.96 |
| `SPEED` | Flight | 0.240 | 28.8 |
| `VERTICAL_SPEED` | Flight | 0.200 | 24.0 |

---

## 8. Chủ đề DSA / Toán thể hiện

- **State pattern** — mỗi trạng thái một lớp, tự quyết định chuyển tiếp.
- **Công thức chuyển động ném** `h = v₀²/2g`, `t = v₀/g`.
- **Tích phân vs gán trực tiếp** — vì sao tắt trọng lực dưới nước.
- **Cờ tiêu thụ một lần** (consume flag) cho sự kiện.
- **Cộng dồn trục** thay `if/else` để tự triệt tiêu.
- **Xử lý trường hợp biên** (kẹt ở mép nước).

---

## 9. Liên kết

- Bộ vật lý: [PhysicsWorld.md](PhysicsWorld.md)
- Nguồn input: [PlayerController.md](PlayerController.md)
- Sát thương rơi: [PlayerStats.md](../10-gameplay/PlayerStats.md)
