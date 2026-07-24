# RemotePlayer — nội suy vị trí qua mạng

**File:** `core/src/com/voxel/game/net/RemotePlayer.java`

Server chỉ gửi vị trí **~15 lần/giây**, nhưng game vẽ **60 khung hình/giây**. Vẽ thẳng theo dữ liệu nhận được ⇒ nhân vật **giật cục**. Lớp này làm mượt.

---

## 1. Bài toán tần số

```
  Tần số mạng:   ~15 Hz   (66.7 ms giữa hai gói tin)
  Tần số vẽ:      60 Hz   (16.7 ms mỗi khung hình)

  ⟹ Cứ 4 khung hình mới có 1 gói tin mới
```

Không nội suy: nhân vật **đứng yên 3 khung rồi nhảy vọt 1 khung** — hiệu ứng "teleport" khó chịu.

---

## 2. Nội suy hàm mũ

```java
private static final float FOLLOW = 10f;

float t = Math.min(1f, delta * FOLLOW);
feet.x += (targetX - feet.x) * t;
feet.y += (targetY - feet.y) * t;
feet.z += (targetZ - feet.z) * t;
```

### Công thức

```
  p(t + Δt) = p(t) + (target − p(t)) · min(1, k·Δt)
```

Đây là **lerp hàm mũ** (exponential smoothing) — cùng dạng với `amount` của [WalkCycle](../07-render/WalkCycle.md) và `climb` của [PerlinWormCarver](../03-caves/PerlinWormCarver.md).

### Hằng số thời gian

```
        1        1
  τ =  ───  =  ────  = 0.1 giây
        k       10
```

| Thời gian | Đã đi được bao nhiêu quãng đường |
|---|---|
| `τ` = 0.1 s | 63.2 % |
| `2τ` = 0.2 s | 86.5 % |
| `3τ` = 0.3 s | 95.0 % |
| `5τ` = 0.5 s | 99.3 % |

Nghiệm giải tích của phương trình vi phân `dp/dt = k(target − p)`:

```
  p(t) = target − (target − p₀)·e^(−kt)
```

### Chọn `FOLLOW = 10`

**Đánh đổi:**

| `FOLLOW` | Độ trễ | Độ mượt |
|---|---|---|
| 3 | ~330 ms — trễ rõ rệt | rất mượt |
| **10** | **~100 ms** | **mượt** |
| 30 | ~33 ms | gần như giật theo gói tin |
| ∞ | 0 | giật hoàn toàn |

`τ = 0.1` s **xấp xỉ bằng** khoảng cách giữa hai gói tin (`1/15 = 0.067` s) ⇒ vị trí kịp bám sát trước khi gói tiếp theo tới, mà vẫn mượt.

> Comment: *"càng lớn càng bám sát, càng nhỏ càng mượt"*

### `Math.min(1f, delta * FOLLOW)` — chặn ổn định

Nếu `Δt > 1/k = 0.1` s (dưới 10 FPS), `k·Δt > 1` ⇒ hệ số lerp vượt 1 ⇒ **vọt lố** (overshoot) và dao động phân kỳ.

Kẹp tại 1 khiến trường hợp xấu nhất là **nhảy thẳng tới đích** — vẫn ổn định.

Đây là điều kiện ổn định của phương pháp **Euler tiến** cho phương trình vi phân bậc 1.

---

## 3. Nội suy góc — đường ngắn nhất

```java
yaw += shortestAngle(yaw, targetYaw) * t;

private static float shortestAngle(float from, float to) {
    float diff = (to - from) % 360f;
    if (diff > 180f)       diff -= 360f;
    else if (diff < -180f) diff += 360f;
    return diff;
}
```

### Bài toán

Góc là đại lượng **tuần hoàn** (`0° ≡ 360°`). Lerp thẳng cho kết quả sai:

```
  from = 350°, to = 10°
  lerp thẳng: 350 + (10 − 350)·t = 350 − 340t
```

⇒ Quay **340° ngược chiều** thay vì **20° thuận chiều**. Nhân vật xoay tít một vòng.

### Giải pháp

```
  d = (to − from) mod 360
  nếu d > 180  :  d ← d − 360
  nếu d < −180 :  d ← d + 360
```

Đưa hiệu về khoảng `(−180°, 180°]` — **cung ngắn nhất** trên đường tròn.

### Kiểm chứng

| `from` | `to` | `(to−from) % 360` | Sau chỉnh | Ý nghĩa |
|---|---|---|---|---|
| 350 | 10 | −340 | **+20** | Quay phải 20° ✔ |
| 10 | 350 | 340 | **−20** | Quay trái 20° ✔ |
| 0 | 180 | 180 | 180 | Nửa vòng (mơ hồ, chọn dương) |
| 90 | 270 | 180 | 180 | Nửa vòng |

### Vì sao phải xử lý cả hai nhánh?

`%` trong Java giữ **dấu của số bị chia**:

```
  (10 − 350) % 360 = −340 % 360 = −340    (không phải 20)
  (350 − 10) % 360 =  340 % 360 =  340
```

⇒ kết quả nằm trong `(−360, 360)`, cần hai phép chỉnh để đưa về `(−180, 180]`.

Đây là bài toán **khoảng cách trên đường tròn** (circular distance) — xuất hiện ở mọi nơi có góc, phương vị, hoặc thời gian tuần hoàn.

---

## 4. Suy ra nhịp đi bộ từ chuyển động

```java
float fromX = feet.x, fromZ = feet.z;
...
float dx = feet.x - fromX;
float dz = feet.z - fromZ;
walk.update(delta, (float) Math.sqrt(dx * dx + dz * dz));
```

> Comment: *"Server không gửi 'đang đi hay đứng yên', nên nhịp bước được SUY RA từ quãng đường người đó vừa trượt được: có đi thì tay chân vung, dừng lại thì duỗi thẳng — giống Minecraft."*

**Ưu điểm:** không tốn thêm băng thông. `WalkCycle` vốn đã tham số hoá theo **quãng đường** (xem [WalkCycle §1](../07-render/WalkCycle.md)) nên chỉ cần đưa vào khoảng dịch chuyển thực tế của khung hình đó.

**Hệ quả tự nhiên:** vì vị trí trượt theo hàm mũ, khi người chơi kia dừng lại, quãng đường mỗi khung hình giảm dần về 0 ⇒ tay chân **thu về từ từ** thay vì đứng khựng.

---

## 5. Lần đầu xuất hiện — nhảy thẳng

```java
if (!placed) {
    feet.set(targetX, targetY, targetZ);
    yaw = targetYaw;
    placed = true;
    return;
}
```

> Comment: *"Lần đầu thấy: nhảy thẳng tới nơi, không trượt từ gốc toạ độ."*

Không có cờ `placed`, `feet` khởi tạo tại `(0,0,0)` ⇒ người chơi mới vào sẽ **bay từ gốc toạ độ thế giới** tới vị trí thật, mất vài giây.

Đây là mẫu **khởi tạo lười** (lazy initialization) cho bộ lọc — mọi bộ lọc mượt đều cần xử lý riêng mẫu đầu tiên.

---

## 6. An toàn đa luồng

```java
// Ghi từ luồng WebSocket:
private volatile float targetX, targetY, targetZ, targetYaw;

// Chỉ đọc/ghi trên luồng game:
private final Vector3 feet = new Vector3();
private float yaw;
```

### Phân tách rõ ràng

| Trường | Luồng ghi | Luồng đọc | Cơ chế |
|---|---|---|---|
| `target*` | **WebSocket** | game | `volatile` |
| `feet`, `yaw` | game | game | không cần gì |

`volatile` trên `float` đảm bảo:
1. **Khả kiến** (visibility) — luồng game thấy giá trị mới ngay, không bị cache thanh ghi.
2. **Nguyên tử** — đọc/ghi `float` (32-bit) là nguyên tử trong JVM.

### Vì sao không cần khoá?

Bốn trường `targetX/Y/Z/Yaw` được ghi **riêng lẻ**, nên về lý thuyết luồng game có thể đọc `targetX` của gói mới và `targetZ` của gói cũ ("xé" dữ liệu).

Thực tế **vô hại**: sai lệch tối đa là vị trí của một khung hình, và bộ lọc mượt sẽ hấp thụ ngay trong 0.1 giây. Đánh đổi này tránh được chi phí khoá trong đường dẫn nóng.

> Nếu cần chặt chẽ, có thể gói 4 giá trị vào một object bất biến và dùng một tham chiếu `volatile` — cùng kỹ thuật copy-on-write với [ChunkStorage](../05-world/ChunkStorage.md).

---

## 7. PvP — sát thương ở máy chủ nhà

```java
@Override
public void takeHit(int damage) {
    sender.sendHit(name, damage);
}
```

> Comment: *"Mình đánh trúng họ: máu của họ nằm trên MÁY CỦA HỌ, nên chỉ báo lên server, server chuyển tiếp cho đúng người đó trừ máu."*

`RemotePlayer` **không có trường `health`**. Đây là mô hình **authoritative client** cho máu: mỗi máy tự quản lý máu của mình.

```
  Máy A: MeleeAim ngắm trúng RemotePlayer("B")
     ↓ sendHit("B", 2)
  Server: chuyển tiếp
     ↓
  Máy B: PlayerStats.damage(2)   ← máu thật nằm ở đây
```

**Ưu:** đơn giản, không cần đồng bộ máu.
**Nhược:** client gian lận được (bỏ qua sát thương). Chấp nhận được cho đồ án; game thương mại cần server authoritative.

`RemotePlayer` vẫn cài `Attackable` ⇒ [MeleeAim](../09-ai/MeleeAim.md) xử lý nó **y hệt** quái vật, không cần biết sự khác biệt. Đa hình đúng chỗ.

---

## 8. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `FOLLOW` | 10 | `τ = 0.1` s |
| Tần số mạng | ~15 Hz | 66.7 ms/gói |
| Tần số vẽ | 60 Hz | 16.7 ms/khung |
| Khoảng góc | `(−180°, 180°]` | Cung ngắn nhất |

---

## 9. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `setTarget` | `O(1)` — 4 phép ghi volatile |
| `advance` | `O(1)` — 4 lerp + 1 `sqrt` + `WalkCycle.update` |
| Bộ nhớ | `O(1)` — 1 `Vector3` + 5 `float` + 1 `WalkCycle` |

---

## 10. Chủ đề Toán / Mạng thể hiện

- **Nội suy hàm mũ** (exponential smoothing) & hằng số thời gian `τ = 1/k`.
- **Điều kiện ổn định Euler tiến** — kẹp hệ số lerp.
- **Khoảng cách trên đường tròn** & nội suy góc theo cung ngắn nhất.
- **Suy diễn trạng thái từ dẫn xuất** — nhịp đi từ vận tốc, tiết kiệm băng thông.
- **`volatile` cho giao tiếp liên luồng** đơn giản.
- **Khởi tạo lười** cho bộ lọc.
- **Đa hình** — cùng interface `Attackable` với quái vật.

---

## 11. Liên kết

- Hoạt hình: [WalkCycle.md](../07-render/WalkCycle.md)
- Ngắm đánh: [MeleeAim.md](../09-ai/MeleeAim.md)
- Bộ lọc tương tự: [PerlinWormCarver.md](../03-caves/PerlinWormCarver.md) §4.2
