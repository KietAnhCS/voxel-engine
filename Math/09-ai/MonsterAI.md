# Monster AI — State pattern & hành vi quái vật

**Files:** `core/src/com/voxel/game/mob/{Monster, MonsterContext, IdleState, ChaseState, AttackState, FuseState}.java`

**Design pattern:** **State** — mỗi trạng thái AI là một lớp, `update()` trả về trạng thái kế tiếp.

---

## 1. Sơ đồ máy trạng thái

```
                    d ≤ 16
      ┌──────┐  ─────────────►  ┌───────┐  d ≤ 1.7 (zombie)  ┌────────┐
      │ IDLE │                  │ CHASE │ ─────────────────► │ ATTACK │
      └──────┘  ◄─────────────  └───────┘  ◄───────────────  └────────┘
                    d > 28          │  ▲       d > 2.2
                                    │  │
                       d ≤ 1.7      │  │  d > 4.5
                       (creeper)    ▼  │
                                 ┌───────┐
                                 │ FUSE  │ ── timer ≥ 1.5s ──► 💥 nổ
                                 └───────┘
```

`d` = khoảng cách **ngang** (bỏ qua độ cao).

### Hiện tượng trễ (hysteresis)

| Chuyển | Ngưỡng |
|---|---|
| IDLE → CHASE | `d ≤ 16` (`DETECT_RANGE`) |
| CHASE → IDLE | `d > 28` (`LOSE_RANGE`) |

Hai ngưỡng **khác nhau** tạo vùng đệm `16 < d ≤ 28`. Nếu dùng chung một ngưỡng, người chơi đứng ngay ranh giới sẽ khiến quái **rung lắc giữa hai trạng thái** mỗi khung hình.

Đây là **trễ Schmitt** (Schmitt trigger) — kỹ thuật chuẩn để ổn định máy trạng thái với đầu vào liên tục có nhiễu.

Tương tự: `ATTACK → CHASE` dùng `ATTACK_RANGE + 0.5 = 2.2` chứ không phải `1.7`.

---

## 2. Bảng hằng số hành vi

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `DETECT_RANGE` | 16 khối | Bắt đầu phát hiện |
| `LOSE_RANGE` | 28 khối | Bỏ cuộc |
| `ATTACK_RANGE` | 1.7 khối | Vào tầm đánh |
| `REACH_UP` | 1.2 khối | Với lên được bao cao |
| `REACH_DOWN` | 2.0 khối | Với xuống được bao sâu |
| `CHASE_SPEED` | 3.2 khối/s | Tốc độ đuổi |
| `ATTACK_INTERVAL` | 1.0 s | Nhịp giữa hai cú đánh |
| `ATTACK_DAMAGE` | 2 | = 1 trái tim |
| `REPATH_INTERVAL` | 0.5 s | Tính lại A* |
| `WAYPOINT_REACHED` | 0.35 khối | Coi như tới điểm giữa |
| `FUSE_TIME` | 1.5 s | Creeper đếm ngược |
| `DEFUSE_RANGE` | 4.5 khối | Chạy xa hơn thì creeper hạ hoả |
| `MAX_HEALTH` | 20 | |
| `BURN_DAMAGE` | 4/s | Cháy nắng |

### Cân bằng chiến đấu

```
  DPS của quái = ATTACK_DAMAGE / ATTACK_INTERVAL = 2 máu/giây
  Thời gian giết người chơi = 20 / 2 = 10 giây
```

`CHASE_SPEED = 3.2` so với tốc độ người chơi — **người chơi chạy nhanh hơn**, luôn có thể bỏ chạy. Đây là quyết định cân bằng quan trọng.

---

## 3. `withinReachHeight` — tầm với theo chiều cao

```java
public static boolean withinReachHeight(Vector3 monsterFeet, Vector3 playerFeet) {
    float dy = playerFeet.y - monsterFeet.y;
    return dy <= REACH_UP && dy >= -REACH_DOWN;
}
```

```
  −2.0 ≤ Δy ≤ 1.2
```

**Bất đối xứng có chủ ý:**

| Hướng | Tầm với | Ý nghĩa |
|---|---|---|
| Lên trên | 1.2 khối | Kê 2 khối gạch là thoát |
| Xuống dưới | 2.0 khối | Đứng dưới hố vẫn bị túm chân |

> Comment: *"Kê một cột gạch leo lên 2 ô là thoát — đúng như Minecraft, dùng để đứng yên trên tháp mà vẫn ăn đòn."*

Đây là **cơ chế gameplay được mã hoá thành một bất đẳng thức** — người chơi học được rằng kê 2 khối là an toàn.

### `AttackState` xử lý khi với không tới

```java
if (!MonsterContext.withinReachHeight(monster.position(), target)) {
    cooldown = RETRY_DELAY;         // 0.25 s
    return this;
}
```

Quái **không chuyển trạng thái**, chỉ chờ `0.25` s rồi thử lại.

> Comment: *"Với không tới thì chờ chút nữa thử lại, đừng để đánh miễn phí ngay khi con mồi tụt xuống."*

Không có `RETRY_DELAY`, quái sẽ thử mỗi khung hình và **đánh trúng ngay khoảnh khắc** người chơi tụt xuống — cảm giác bất công. `0.25` s cho người chơi một khe thời gian phản ứng.

---

## 4. ChaseState — dùng A*

```java
repathTimer -= delta;
if (repathTimer <= 0f || index >= path.size()) {
    path.clear();
    path.addAll(ctx.pathFinder().findPath(monster.position(), target));
    index = 0;
    repathTimer = REPATH_INTERVAL;
}

if (index < path.size()) {
    Vector3 waypoint = path.get(index);
    monster.stepToward(ctx.world(), waypoint.x, waypoint.z, CHASE_SPEED, delta);
    if (horizontalDist(monster.position(), waypoint) < WAYPOINT_REACHED) index++;
} else {
    monster.stepToward(ctx.world(), target.x, target.z, CHASE_SPEED, delta);   // đường lui
}
```

### Vì sao tính lại đường mỗi 0.5 giây?

Người chơi luôn di chuyển ⇒ đường tính từ 5 giây trước đã lỗi thời.

**Đánh đổi:**

| `REPATH_INTERVAL` | Bám sát | Chi phí CPU |
|---|---|---|
| mỗi khung hình (1/60 s) | hoàn hảo | `60 × 22 000` = **1.3 M phép/s/con** |
| 0.5 s | rất tốt | `2 × 22 000` = **44 K phép/s/con** |
| 2 s | quái chạy vòng | 11 K phép/s |

`0.5` s giảm chi phí **30 lần** so với mỗi khung hình mà mắt gần như không phân biệt được.

Với `MAX_MONSTERS = 4`: tổng `4 × 44 000 = 176 000` phép/giây — không đáng kể.

### Điều kiện tính lại thứ hai

```java
if (repathTimer <= 0f || index >= path.size())
```

`index >= path.size()` — đã đi hết đường mà chưa tới đích ⇒ tính lại **ngay**, không chờ hết 0.5 s. Xảy ra khi đường quá ngắn (A* bị chặn bởi `MAX_EXPANSIONS`).

### Đường lui khi không có đường

```java
} else {
    monster.stepToward(ctx.world(), target.x, target.z, CHASE_SPEED, delta);
}
```

> Comment: *"Không tìm ra đường (bị kẹt): cứ lao thẳng tới người chơi."*

Kết hợp với đường lui **bên trong** [A*](AStarPathFinder.md) (trả về ô gần đích nhất), quái vật **không bao giờ đứng đơ** — luôn có hành vi hợp lý.

---

## 5. FuseState — creeper

```java
private static final float FUSE_TIME = 1.5f;
private static final float DEFUSE_RANGE = 4.5f;

if (ctx.player().isDead() || distance > DEFUSE_RANGE) {
    monster.setFuse(0f);
    return new ChaseState();          // hạ hoả, đuổi tiếp
}
monster.faceToward(target.x, target.z);
timer += delta;
monster.setFuse(timer / FUSE_TIME);
if (timer >= FUSE_TIME) monster.requestExplosion();
```

### Cửa sổ thoát hiểm

```
  Người chơi phải chạy từ d = 1.7 tới d > 4.5 trong 1.5 giây

  Δd = 4.5 − 1.7 = 2.8 khối
  v_cần = 2.8 / 1.5 ≈ 1.87 khối/giây
```

Tốc độ đi bộ thường đã vượt xa ⇒ **luôn thoát được nếu phản ứng kịp**. Nhưng phải nhận ra tiếng xì ngay — đó là phần kỹ năng.

### `setFuse` chuẩn hoá

```java
public void setFuse(float fuse) { this.fuse = Math.max(0f, Math.min(1f, fuse)); }
```

`fuse = timer / FUSE_TIME ∈ [0, 1]` — tiến độ chuẩn hoá, dùng cho hiệu ứng hình ảnh.

### Nhấp nháy theo tiến độ

```java
public boolean isFlashing() {
    return isHurt() || (fuse > 0f && ((int) (fuse * 8f) & 1) == 1);
}
```

```
  nhấp nháy  ⟺  ⌊8·fuse⌋ lẻ
```

`fuse` đi từ 0 → 1 trong 1.5 s, nhân 8 rồi lấy phần nguyên ⇒ **8 khoảng**, mỗi khoảng `1.5/8 = 0.1875` s. Bit chẵn/lẻ tạo nhấp nháy:

```
  ⌊8f⌋:  0  1  2  3  4  5  6  7
  sáng:  ·  ▓  ·  ▓  ·  ▓  ·  ▓      → 4 lần nháy trong 1.5 s
```

Tần số **2.67 Hz** — đủ nhanh để báo động, đủ chậm để mắt theo kịp.

`& 1` thay `% 2` — nhanh hơn và đúng với mọi giá trị.

### Vì sao trạng thái không tự nổ?

> Comment: *"Trạng thái chỉ đốt ngòi; vụ nổ thật (phá khối, làm đau người chơi, báo cho server) do `MonsterManager` làm trên vòng lặp game, vì một object trạng thái không nên phải giữ tham chiếu tới mạng."*

`requestExplosion()` chỉ bật cờ; `MonsterManager.update` kiểm tra và xử lý. Đây là **tách biệt trách nhiệm** — trạng thái AI thuần logic, không phụ thuộc hạ tầng.

---

## 6. Vụ nổ — voxel hoá hình cầu

```java
private static final float BLAST_RADIUS = 2.8f;
private static final float BLAST_HURT_RANGE = 5f;
private static final int BLAST_MAX_DAMAGE = 24;

int reach = (int) Math.ceil(BLAST_RADIUS);        // 3
float radius2 = BLAST_RADIUS * BLAST_RADIUS;      // 7.84

for (dx, dy, dz ∈ [−3, 3]) {
    if (dx*dx + dy*dy + dz*dz > radius2) continue;
    ...
    if (block.isAir() || block.isLiquid()) continue;    // nước hấp thụ vụ nổ
    world.setBlock(x, y, z, blocks.air);
}
```

### Thể tích hố

```
  V ≈ (4/3)π r³ = (4/3)π × 2.8³ ≈ 92 khối
```

Vòng lặp duyệt hộp bao `7³ = 343` ô, chỉ ~92 ô thoả `d² ≤ 7.84` ⇒ tỉ lệ `π/6 ≈ 52 %` (đúng như [CarveScope](../03-caves/CarveScope.md) §2.4).

### Sát thương giảm tuyến tính

```java
float distance = horizontalDist(at, player.position());
if (distance < BLAST_HURT_RANGE && !player.isDead()) {
    player.hit(Math.round(BLAST_MAX_DAMAGE * (1f - distance / BLAST_HURT_RANGE)));
}
```

```
              ⎛      d   ⎞
  D(d) = 24 · ⎜ 1 − ──── ⎟ ,   d < 5
              ⎝      5   ⎠
```

| `d` (khối) | Sát thương | Trái tim |
|---|---|---|
| 0 | **24** | 12 → **CHẾT** (máu tối đa 20) |
| 1 | 19 | 9.5 |
| 2 | 14 | 7 |
| 3 | 10 | 5 |
| 4 | 5 | 2.5 |
| ≥ 5 | 0 | 0 |

Điểm chết: `D(d) ≥ 20 ⟺ d ≤ 5(1 − 20/24) = 0.83` khối. Đứng sát creeper là **chết chắc**.

> Vật lý thật: năng lượng vụ nổ giảm theo `1/d²`. Ở đây dùng tuyến tính vì dễ hiểu và dễ cân bằng hơn — người chơi ước lượng được "lùi thêm 1 khối bớt 5 sát thương".

### Nước hấp thụ vụ nổ

```java
if (block.isAir() || block.isLiquid()) continue;
```

Đứng dưới nước ⇒ hố không đào được ⇒ mẹo phòng thủ, giống Minecraft.

### `y <= 1` — giữ bedrock

```java
if (y <= 1) continue;
```

Không đục thủng đáy thế giới.

---

## 7. Luật spawn — mô phỏng Minecraft

```java
int torchLight = world.blockLightAt(x, y, z);
if (torchLight > MAX_SPAWN_LIGHT) return;                    // (1) 7

int skyLight = world.skyLightAt(x, y, z);
if (!night && skyLight > MAX_SPAWN_LIGHT) return;            // (2)

int light = Math.max(torchLight, night ? 0 : skyLight);      // (3)
if (random.nextInt(MAX_SPAWN_LIGHT + 1) < light) return;     // (4)
```

### Bốn tầng lọc

| # | Luật | Ý nghĩa |
|---|---|---|
| 1 | `torchLight ≤ 7` **tuyệt đối** | Cắm đuốc quanh nhà là ngủ yên |
| 2 | Ban ngày: `skyLight ≤ 7` | Ngoài trời ban ngày không spawn |
| 3 | Ban đêm: bỏ qua `skyLight` | Đêm thì ngoài trời cũng spawn |
| 4 | Xác suất theo độ sáng | Càng sáng càng khó |

### Xác suất tầng 4

```
                              light        8 − light
  P(spawn | light) = 1 − ───────────  =  ────────────
                              8               8
```

| `light` | `P(spawn)` |
|---|---|
| 0 (tối đen) | **8/8 = 100 %** |
| 2 | 75 % |
| 4 | 50 % |
| 6 | 25 % |
| 7 | 12.5 % |

`random.nextInt(8)` cho giá trị đều `0..7`; điều kiện `< light` đúng với `light` giá trị ⇒ xác suất **thất bại** = `light/8`.

### Vòng tròn spawn

```java
float angle = random.nextFloat() * MathUtils.PI2;
float dist = MIN_SPAWN_DIST + random.nextFloat() * (MAX_SPAWN_DIST - MIN_SPAWN_DIST);
int x = (int) Math.floor(p.x + MathUtils.cos(angle) * dist);
int z = (int) Math.floor(p.z + MathUtils.sin(angle) * dist);
```

```
  θ ~ U(0, 2π)
  r ~ U(12, 22)
```

Spawn trong **vành khăn** bán kính 12–22 khối:
- **≥ 12**: không xuất hiện ngay trước mặt (giật mình vô lý).
- **≤ 22**: đủ gần để người chơi gặp trong tầm nhìn.

> Lưu ý: `r ~ U(12, 22)` cho phân bố **dày hơn ở vòng trong** (vì diện tích vành ngoài lớn hơn). Nếu muốn đều theo diện tích cần `r = √(r₁² + u(r₂² − r₁²))` — xem [RainRenderer §2](../07-render/RainRenderer.md). Ở đây thiên về gần người chơi lại là điều **mong muốn**.

### Tần suất

```
  SPAWN_INTERVAL = 6 s,  MAX_MONSTERS = 4
```

Thử spawn mỗi 6 giây, tối đa 4 con cùng lúc. Kết hợp với các tầng lọc, mật độ thực tế thấp hơn nhiều.

---

## 8. Cháy nắng

```java
private static final int SUNBURN_LIGHT = 12;

if (!night && world.skyLightAt(x, y, z) >= SUNBURN_LIGHT) monster.burn(delta);

// trong Monster:
public void burn(float delta) {
    burnTimer += delta;
    while (burnTimer >= 1f) { burnTimer -= 1f; takeHit(BURN_DAMAGE); }   // 4 máu
}
```

```
  Thời gian bốc hơi = MAX_HEALTH / BURN_DAMAGE = 20 / 4 = 5 giây
```

> Comment: *"sau khoảng năm giây là bốc hơi — đúng như zombie Minecraft gặp bình minh."*

`SUNBURN_LIGHT = 12` cao hơn `MAX_SPAWN_LIGHT = 7` ⇒ có vùng đệm `8..11` nơi quái **không spawn nhưng cũng không cháy** — bóng râm dưới tán cây.

Dùng `while` cho `burnTimer` — cùng lý do với [PlayerStats](../10-gameplay/PlayerStats.md) §2.

---

## 9. `Monster.stepToward` — di chuyển & quay mặt

```java
public void stepToward(World world, float wx, float wz, float speed, float delta) {
    float dx = wx - position.x, dz = wz - position.z;
    float dist = (float) Math.sqrt(dx * dx + dz * dz);
    if (dist > 1e-4f) {
        float step = Math.min(dist, speed * delta);        // ← chống vọt quá
        position.x += dx / dist * step;
        position.z += dz / dist * step;
        yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
    }
    snapToGround(world);
}
```

### Chuẩn hoá & giới hạn bước

```
  d⃗ = (wx − x, wz − z)
  step = min(‖d⃗‖, v·Δt)          ← không bao giờ vượt quá đích
  p⃗ ← p⃗ + (d⃗/‖d⃗‖) · step
```

`min(dist, speed*delta)` ngăn **vọt quá** (overshoot) khi `delta` lớn — nếu không, quái sẽ dao động qua lại quanh điểm đích.

`dist > 1e-4f` tránh chia cho 0.

### Góc quay mặt

```java
yaw = Math.toDegrees(Math.atan2(dx, dz));
```

Chú ý thứ tự **`atan2(dx, dz)`** chứ không phải `atan2(dz, dx)` chuẩn toán học. Lý do: trong hệ toạ độ game, `yaw = 0` nghĩa là nhìn theo `+Z`, và góc tăng khi quay về `+X`.

```
  atan2(x, z) = góc từ trục +Z quay về trục +X
```

`atan2` (hai tham số) xử lý đúng **cả 4 góc phần tư**, khác `atan(x/z)` chỉ cho `(−90°, 90°)` và chia 0 khi `z = 0`.

### `snapToGround` — bám mặt đất

```java
private void snapToGround(World world) {
    int top = Math.min(worldHeight - 1, (int) Math.floor(position.y) + 1);
    for (int y = top; y > 0; y--) {
        if (world.blockAt(x, y - 1, z).isCollidable() && !world.blockAt(x, y, z).isCollidable()) {
            position.y = y;
            return;
        }
    }
}
```

Quái vật **không dùng bộ vật lý Bullet** — chúng "dán" vào mặt đất bằng cách quét từ `y + 1` xuống tìm bề mặt đầu tiên.

**Ưu điểm:** rẻ (`O(y)` thay vì mô phỏng vật lý), leo dốc mượt, không bao giờ rơi khỏi thế giới.
**Nhược:** không có trọng lực thật — quái không rơi xuống hố, chỉ trượt theo địa hình.

Bắt đầu từ `y + 1` cho phép **leo lên 1 khối** mỗi bước.

---

## 10. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `IdleState.update` | `O(1)` — 1 phép tính khoảng cách |
| `ChaseState.update` | `O(1)` thường, `O(A*)` = 22 000 khi tính lại (2 lần/s) |
| `AttackState.update` | `O(1)` |
| `FuseState.update` | `O(1)` |
| `snapToGround` | `O(worldHeight)` xấu nhất, thường vài bước |
| `explode` | `O(reach³)` = 343 ô |
| `trySpawn` | `O(worldHeight)` cho `groundHeight` |
| **Tổng mỗi khung hình** | `O(MAX_MONSTERS)` = `O(4)` |

---

## 11. Chủ đề DSA / Toán thể hiện

- **State pattern** — 4 trạng thái, chuyển tiếp tự quyết.
- **Trễ Schmitt** (hysteresis) chống rung trạng thái.
- **A\* có nhịp** — đánh đổi độ chính xác lấy CPU.
- **Voxel hoá hình cầu** cho vụ nổ.
- **Suy giảm tuyến tính theo khoảng cách** cho sát thương.
- **Lấy mẫu trên vành khăn** (toạ độ cực).
- **Xác suất có điều kiện nhiều tầng** cho luật spawn.
- **`atan2`** và tránh chia 0.
- **Giới hạn bước** chống vọt quá.
- **Bit parity** (`& 1`) cho hiệu ứng nhấp nháy.
- **Tách biệt trách nhiệm** — state không biết về mạng.

---

## 12. Liên kết

- Tìm đường: [AStarPathFinder.md](AStarPathFinder.md)
- Nhắm đánh quái: [MeleeAim.md](MeleeAim.md)
- Hoạt hình: [WalkCycle.md](../07-render/WalkCycle.md)
- Ban đêm: [DayNightCycle.md](../07-render/DayNightCycle.md)
- Máu người chơi: [PlayerStats.md](../10-gameplay/PlayerStats.md)
