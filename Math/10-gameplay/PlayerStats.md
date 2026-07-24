# PlayerStats — máu, đói, hơi thở, kinh nghiệm

**File:** `core/src/com/voxel/game/play/PlayerStats.java`

Toàn bộ phần "sinh tồn" của game, gói trong các công thức tích luỹ theo thời gian.

---

## 1. Hằng số gốc (theo Minecraft)

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `MAX_HEALTH` | 20 | = 10 trái tim, mỗi trái 2 máu |
| `MAX_FOOD` | 20 | = 10 cái đùi gà |
| `MAX_AIR` | 15 s | = 10 bong bóng |
| `SAFE_FALL` | 3 khối | Rơi dưới mức này không đau |

---

## 2. Hệ thống đói — đồng hồ "exhaustion"

```java
private static final float EXHAUSTION_WALKING = 0.05f;
private static final float EXHAUSTION_IDLE    = 0.005f;
private static final float EXHAUSTION_LIMIT   = 1f;

private void updateFood(float delta, boolean moving) {
    exhaustion += delta * (moving ? EXHAUSTION_WALKING : EXHAUSTION_IDLE);
    while (exhaustion >= EXHAUSTION_LIMIT) {
        exhaustion -= EXHAUSTION_LIMIT;
        food = Math.max(0, food - 1);
    }
    ...
}
```

### Công thức

```
  E(t + Δt) = E(t) + Δt · r        r = 0.05 (đi) hoặc 0.005 (đứng)

  Khi E ≥ 1:  E ← E − 1,  food ← food − 1
```

Đây là **bộ tích luỹ với ngưỡng** (accumulator with threshold) — mẫu chuẩn để biến sự kiện liên tục thành sự kiện rời rạc.

### Thời gian tụt một nấc đói

```
        1
  T =  ───
        r
```

| Trạng thái | `r` | Thời gian/nấc | Thời gian cạn 20 nấc |
|---|---|---|---|
| Đi lại | 0.05 | **20 s** | **400 s** ≈ 6.7 phút |
| Đứng yên | 0.005 | **200 s** | 4 000 s ≈ 66.7 phút |

Đi lại đói nhanh **gấp 10 lần** đứng yên.

### Vì sao tính theo GIÂY chứ không theo khung hình?

> Comment: *"Tính theo GIÂY chứ không theo khung hình, nếu không máy chạy 144 hình/giây sẽ làm người chơi đói nhanh gấp đôi máy 60."*

Nếu viết `exhaustion += 0.05f` (mỗi khung hình):

| FPS | Tốc độ đói |
|---|---|
| 30 | `0.05 × 30 = 1.5`/s |
| 60 | `3.0`/s |
| 144 | `7.2`/s ← **nhanh gấp 4.8 lần** |

Nhân `delta` làm tốc độ **độc lập với FPS** — nguyên tắc cơ bản của mọi logic game theo thời gian. Cùng lý do với `fixedTimeStep` của [PhysicsWorld](../08-physics/PhysicsWorld.md).

### Vì sao dùng `while` chứ không `if`?

```java
while (exhaustion >= EXHAUSTION_LIMIT) { ... }
```

Nếu một khung hình bị treo lâu (`delta = 25` s), `exhaustion` tăng `1.25` ⇒ chỉ cần trừ 1 nấc. Nhưng `delta = 100` s ⇒ tăng `5.0` ⇒ phải trừ **5 nấc**.

`while` xử lý đúng mọi `delta`; `if` sẽ **mất** phần dư và người chơi được lợi khi máy giật.

### Chết đói

```java
if (food > 0) { starveTimer = 0f; return; }
starveTimer += delta;
if (starveTimer >= STARVE_INTERVAL) {     // 4 s
    starveTimer = 0f;
    damage(1);
}
```

Hết đói ⇒ **1 máu mỗi 4 giây** ⇒ chết sau `20 × 4 = 80` giây.

---

## 3. Hơi thở & chết đuối

```java
private void updateAir(float delta, boolean headUnderwater) {
    if (headUnderwater) {
        air = Math.max(0f, air - delta);
        if (air > 0f) { drownTimer = 0f; return; }
        drownTimer += delta;
        while (drownTimer >= DROWN_DAMAGE_INTERVAL) {      // 1 s
            drownTimer -= DROWN_DAMAGE_INTERVAL;
            damage(DROWN_DAMAGE);                            // 2 máu
        }
        return;
    }
    air = Math.min(MAX_AIR, air + delta * AIR_REFILL_SPEED); // ×5
    drownTimer = 0f;
}
```

### Công thức bất đối xứng

```
              ⎧ air − Δt          nếu đầu ngập nước   (tốc độ 1×)
  air'   =    ⎨
              ⎩ air + 5Δt         nếu trên mặt nước   (tốc độ 5×)
```

| Hành động | Thời gian |
|---|---|
| Cạn hết hơi | **15 s** |
| Hồi đầy hơi | `15/5 = ` **3 s** |

Hồi nhanh **gấp 5 lần** — người chơi ngoi lên thở một hơi ngắn là đủ lặn tiếp. Nếu đối xứng, mỗi lần lặn phải chờ 15 giây trên mặt nước — chán.

### Chết đuối

```
  2 máu / giây  ⟹  chết sau 20/2 = 10 giây kể từ khi hết hơi
  Tổng thời gian sống dưới nước = 15 + 10 = 25 giây
```

`while` (không phải `if`) cho `drownTimer` — cùng lý do với `exhaustion`.

---

## 4. Sát thương rơi

```java
private void updateFall(boolean onGround, float feetY, boolean bodyInWater) {
    if (bodyInWater) { airborne = false; return; }            // (1) nước đỡ
    if (!onGround) {
        if (!airborne) { airborne = true; fallPeakY = feetY; } // (2) bắt đầu rơi
        fallPeakY = Math.max(fallPeakY, feetY);                // (3) theo dõi đỉnh
        return;
    }
    if (airborne) {                                            // (4) chạm đất
        airborne = false;
        int fallDamage = (int) Math.floor(fallPeakY - feetY - SAFE_FALL);
        if (fallDamage > 0) damage(fallDamage);
    }
}
```

### Công thức

```
  damage = ⌊ y_đỉnh − y_chạm đất − 3 ⌋      (chỉ khi > 0)
```

### Bảng sát thương

| Độ cao rơi | Sát thương | Trái tim mất |
|---|---|---|
| ≤ 3 | **0** | 0 |
| 4 | 1 | 0.5 |
| 5 | 2 | 1 |
| 10 | 7 | 3.5 |
| 20 | 17 | 8.5 |
| **23** | **20** | **CHẾT** |

Ngưỡng chết đúng `SAFE_FALL + MAX_HEALTH = 3 + 20 = 23` khối.

### Vì sao theo dõi `fallPeakY` chứ không dùng vận tốc?

Người chơi có thể rơi xuống, **chạm mép gờ**, bật lên, rồi rơi tiếp. Nếu chỉ đo vận tốc lúc chạm đất, có thể bỏ sót. `fallPeakY` ghi **điểm cao nhất trong suốt lần trên không** ⇒ chính xác.

`Math.max` cập nhật mỗi khung hình xử lý cả trường hợp người chơi **nhảy lên trong lúc đang rơi** (không thể trong game này, nhưng an toàn).

### Máy trạng thái nhị phân `airborne`

```
  onGround  ──►  !onGround   :  airborne = true,  fallPeakY = feetY
  !onGround ──►  onGround    :  airborne = false, tính sát thương
```

Cờ `airborne` là **phát hiện cạnh** (edge detection): sát thương chỉ tính **một lần** ở khoảnh khắc tiếp đất, không phải mỗi khung hình đứng trên đất.

### Nước đỡ hoàn toàn

```java
if (bodyInWater) { airborne = false; return; }
```

Rơi xuống nước ⇒ reset `airborne` ⇒ không có sát thương. Khớp với `SwimmingState.enter()` gọi `clearVelocity()` — xem [MovementStates](../08-physics/MovementStates.md).

---

## 5. Hồi máu

```java
private void updateRegen(float delta) {
    if (health >= MAX_HEALTH || air < MAX_AIR || food < FOOD_TO_REGEN) {
        regenTimer = 0f;
        return;
    }
    regenTimer += delta;
    if (regenTimer >= REGEN_INTERVAL) {          // 4 s
        regenTimer = 0f;
        health = Math.min(MAX_HEALTH, health + 1);
    }
}
```

### Ba điều kiện chặn

```
  hồi máu  ⟺  health < 20  ∧  air = 15  ∧  food ≥ 18
```

| Điều kiện | Ý nghĩa |
|---|---|
| `health < MAX_HEALTH` | Chưa đầy máu |
| `air == MAX_AIR` | Không đang ngạt nước |
| `food ≥ 18` | Đủ no (ngưỡng Minecraft) |

`FOOD_TO_REGEN = 18` trên tổng 20 = **90 %** — phải gần no hẳn mới hồi. Đây là cơ chế tạo áp lực tìm thức ăn.

### Tốc độ hồi

```
  1 máu / 4 giây  ⟹  hồi đầy từ 1 máu: 19 × 4 = 76 giây
```

### Reset khi trúng đòn

```java
public void damage(int amount) {
    ...
    regenTimer = 0f;      // ← reset đồng hồ hồi máu
}
```

Trúng đòn liên tục ⇒ `regenTimer` không bao giờ đạt 4 giây ⇒ **không hồi máu trong lúc giao tranh**. Cơ chế đơn giản mà hiệu quả.

---

## 6. Kinh nghiệm & lên cấp

```java
public void addExperience(int amount) {
    progress += amount / (float) experienceToNextLevel();
    while (progress >= 1f) { progress -= 1f; level++; }
}

public int experienceToNextLevel() {
    return 7 + level * 2;
}
```

### Công thức

```
  XP_cần(L) = 7 + 2L
```

**Cấp số cộng** với công sai 2.

### Tổng XP để đạt cấp `n`

```
        n−1                    n−1
        ___                    ___
  S(n) = ╲   (7 + 2L)  =  7n + 2 ╲   L  =  7n + 2 · n(n−1)/2
        ‾‾‾                    ‾‾‾
        L=0                    L=0

       = 7n + n² − n  =  n² + 6n
```

| Cấp | XP cho cấp đó | Tổng XP tích luỹ |
|---|---|---|
| 0 → 1 | 7 | 7 |
| 1 → 2 | 9 | 16 |
| 2 → 3 | 11 | 27 |
| 5 → 6 | 17 | 66 |
| 10 → 11 | 27 | 160 |
| 20 → 21 | 47 | 520 |
| 30 → 31 | 67 | 1 080 |

Kiểm chứng `n = 10`: `10² + 6×10 = 160` ✔

**Tăng trưởng bậc 2** — mỗi cấp khó hơn tuyến tính, tổng công sức tăng theo `O(n²)`. Đường cong quen thuộc của RPG: lên cấp đầu nhanh, về sau chậm dần đều.

### Bug tiềm ẩn khi cộng XP lớn

```java
progress += amount / (float) experienceToNextLevel();
while (progress >= 1f) { progress -= 1f; level++; }
```

`experienceToNextLevel()` được tính **một lần** trước vòng lặp. Nếu `amount` đủ lớn để lên nhiều cấp, các cấp sau vẫn dùng ngưỡng của cấp cũ (rẻ hơn).

Ví dụ ở cấp 0, cộng 30 XP: `progress = 30/7 = 4.29` ⇒ lên **4 cấp**. Đúng ra phải là: `7 + 9 + 11 = 27 ≤ 30 < 40` ⇒ chỉ **3 cấp**.

Trong game này `KILL_EXPERIENCE` nhỏ (vài điểm mỗi con quái) nên `progress` hiếm khi vượt 2.0 và sai lệch không đáng kể. Nếu muốn chính xác tuyệt đối, cần tính lại ngưỡng bên trong vòng lặp.

---

## 7. Hiệu ứng nhấp nháy đỏ

```java
damageFlash = Math.max(0f, damageFlash - delta * 2f);
// khi trúng đòn:
damageFlash = 1f;
```

```
  flash(t) = max(0, 1 − 2t)
```

**Suy giảm tuyến tính**, tắt hẳn sau:

```
  T = 1/2 = 0.5 giây
```

Dùng trong [Hud.drawDamageFlash](Hud.md) làm độ mờ của lớp phủ đỏ:

```java
ui.rect(batch, Color.RED, flash * 0.3f, 0f, 0f, width, height);
```

Đỉnh điểm chỉ `0.3` alpha — đỏ nhạt, không che khuất tầm nhìn.

---

## 8. Chế độ sáng tạo — thoát sớm

```java
if (mode.isCreative()) {
    health = MAX_HEALTH;
    air = MAX_AIR;
    food = MAX_FOOD;
    airborne = false;
    return;
}
```

Ghi đè cả ba chỉ số về tối đa **mỗi khung hình** thay vì chỉ bỏ qua cập nhật. Nhờ vậy chuyển từ sinh tồn sang sáng tạo là **hồi phục ngay lập tức**, và chuyển ngược lại bắt đầu từ trạng thái đầy đủ.

`airborne = false` ngăn sát thương rơi tích luỹ trong lúc bay.

---

## 9. Bảng hằng số đầy đủ

| Hằng | Giá trị | Hệ quả |
|---|---|---|
| `MAX_HEALTH` | 20 | 10 trái tim |
| `MAX_FOOD` | 20 | 10 đùi gà |
| `MAX_AIR` | 15 s | 10 bong bóng |
| `EXHAUSTION_WALKING` | 0.05 | 1 nấc đói / 20 s |
| `EXHAUSTION_IDLE` | 0.005 | 1 nấc đói / 200 s |
| `FOOD_TO_REGEN` | 18 | Cần 90 % no mới hồi |
| `STARVE_INTERVAL` | 4 s | 1 máu/4 s khi hết đói |
| `SAFE_FALL` | 3 khối | Chết khi rơi 23 khối |
| `DROWN_DAMAGE` | 2 | Chết sau 10 s ngạt |
| `DROWN_DAMAGE_INTERVAL` | 1 s | |
| `REGEN_INTERVAL` | 4 s | 1 máu/4 s |
| `AIR_REFILL_SPEED` | 5× | Hồi hơi trong 3 s |
| XP mỗi cấp | `7 + 2L` | Tổng `n² + 6n` |
| `damageFlash` decay | 2/s | Tắt sau 0.5 s |

---

## 10. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `update` | `O(1)` — vài phép cộng, so sánh |
| `addExperience` | `O(số cấp lên)` — thường 0 hoặc 1 |
| Bộ nhớ | `O(1)` — 12 trường nguyên thuỷ |

---

## 11. Chủ đề Toán / Thiết kế thể hiện

- **Bộ tích luỹ có ngưỡng** — rời rạc hoá sự kiện liên tục.
- **Độc lập FPS** bằng nhân `delta`.
- **`while` thay `if`** để xử lý `delta` lớn.
- **Bất đối xứng có chủ ý** (hồi hơi nhanh gấp 5 lần).
- **Cấp số cộng & tổng bậc 2** cho đường cong kinh nghiệm.
- **Phát hiện cạnh** (cờ `airborne`) để kích hoạt một lần.
- **Theo dõi cực trị** (`fallPeakY`).
- **Suy giảm tuyến tính** cho hiệu ứng hình ảnh.
- **Reset đồng hồ khi có sự kiện** để chặn hồi máu trong giao tranh.

---

## 12. Liên kết

- Trạng thái di chuyển: [MovementStates.md](../08-physics/MovementStates.md)
- Hiển thị: [Hud.md](Hud.md)
- Kinh nghiệm từ giết quái: [MonsterManager.md](../09-ai/MonsterManager.md)
