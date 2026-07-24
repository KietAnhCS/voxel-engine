# PerlinWormCarver — hang động dạng đường hầm

**File:** `core/src/com/voxel/game/terrain/carve/PerlinWormCarver.java`

**Ý tưởng:** thả một "con giun" bò trong lòng đất. Mỗi bước nó tiến về phía trước 1 khối và khoét rỗng quanh mình một quả cầu. Hướng đi không random hoàn toàn mà bị **nhiễu Simplex lái**, nên đường hầm uốn lượn mượt chứ không giật cục.

---

## 1. Bài toán liên chunk

Một đường hầm dài 70–180 bước ⇒ trải qua nhiều chunk. Nhưng mỗi chunk sinh **độc lập, đa luồng**. Làm sao hang chạy liền mạch?

### Giải pháp: tái tạo tất định

```java
for (offsetX = −6..6)
  for (offsetZ = −6..6)
      spawnSystem(scope, chunkX + offsetX, chunkZ + offsetZ, chunkSize);
```

Khi sinh chunk `(cx, cz)`, ta **duyệt lại toàn bộ 13×13 = 169 chunk lân cận**, tái tạo *chính xác* những con giun của chúng (cùng seed ⇒ cùng đường đi), nhưng chỉ ghi ra phần rơi vào chunk hiện tại.

```
  CHUNK_RADIUS = 6  ⇒  vùng quét = 13 × 13 = 169 chunk = 208 × 208 khối
```

**Vì sao bán kính 6?** Đường hầm dài nhất: `steps ≤ 180` bước × ~1 khối/bước = 180 khối ≈ 11 chunk. Bán kính 6 chunk (96 khối) bắt được đa số; lớn hơn thì chi phí tăng **bình phương**.

> Comment trong code ghi rõ: *"tăng lên là thời gian sinh chunk tăng bình phương"*.

---

## 2. Xác suất sinh

```java
Random random = new Random(CaveSeeds.forChunk(seed, cx, cz, 1));
if (random.nextInt(SPAWN_RARITY) != 0) return;      // SPAWN_RARITY = 5
int tunnels = 1 + random.nextInt(3);                // 1..3 đường hầm
```

```
  P(chunk có hệ thống hang) = 1/5 = 0.2
  E[số hầm | có hang]       = (1+2+3)/3 = 2
  E[số hầm mỗi chunk]       = 0.2 × 2 = 0.4
```

Trong vùng quét 169 chunk: `E[số hầm] = 169 × 0.4 ≈ 67.6` đường hầm được tái tạo mỗi lần sinh 1 chunk (phần lớn không chạm chunk hiện tại và bị `touches()` loại sớm).

---

## 3. Tham số khởi tạo một đường hầm

```java
boolean climber = random.nextInt(4) == 0;          // 25% là "hầm nổi"

startY  = climber ? 36 + rand(14)                  // [36, 49]
                  : 9 + rand(30);                  // [9, 38]
yaw     = rand() · 2π;                             // hướng ngang, đều trên [0, 2π)
pitch   = climber ? 0.10 + rand()·0.15             // [0.10, 0.25] rad — luôn đi LÊN
                  : (rand() − 0.5) · 0.4;          // [−0.2, 0.2] rad — gần ngang
radius  = 1.9 + rand()·1.9;                        // [1.9, 3.8] khối
steps   = 70 + rand(110);                          // [70, 179] bước
```

**"Climber" (25 %)** bắt đầu cao và luôn ngóc lên `pitch > 0` ⇒ khi đi qua vùng `SurfaceGuard` cho phép, nó **trổ miệng ra sườn đồi** thành cửa hang. 75 % còn lại chạy sâu trong lòng đất.

---

## 4. Vòng lặp bò — trái tim thuật toán

```java
for (int step = 0; step < steps; step++) {
    double t = step * 0.06;

    heading += noise.worm(t, wobble) * 0.35;
    climb    = climb * 0.92 + noise.worm(t, wobble + 128.0) * 0.14;
    climb    = clamp(climb, −0.55, +0.55);

    cursorX += cos(heading) * cos(climb);
    cursorZ += sin(heading) * cos(climb);
    cursorY += sin(climb) * 0.9;
    ...
}
```

### 4.1 Lái hướng bằng nhiễu — bước ngẫu nhiên tương quan

```
  θ(k+1) = θ(k) + 0.35 · worm(0.06k, w)
```

Đây **không phải** random walk thường. `worm()` là nhiễu Simplex 1D liên tục, nên `worm(0.06k)` và `worm(0.06(k+1))` gần bằng nhau ⇒ **góc quay giữa hai bước liên tiếp gần giống nhau** ⇒ đường cong trơn (khả vi), không zic-zắc.

Bước lấy mẫu `Δt = 0.06`: một chu kỳ nhiễu (`λ ≈ 1`) trải qua `1/0.06 ≈ 17` bước ⇒ hầm đổi hướng rõ rệt sau mỗi ~17 khối.

Biên độ `0.35 rad ≈ 20°` mỗi bước là **cực đại**; trung bình nhỏ hơn nhiều.

### 4.2 Góc dốc — bộ lọc thông thấp (low-pass filter)

```
  φ(k+1) = 0.92 · φ(k) + 0.14 · worm(0.06k, w+128)
```

Đây là **bộ lọc IIR bậc 1** (exponential moving average) với hệ số nhớ `α = 0.92`.

**Hằng số thời gian:**

```
  τ = −1 / ln(0.92) ≈ 12 bước
```

Nghĩa là hầm cần ~12 bước để "quên" độ dốc cũ ⇒ độ cao thay đổi từ tốn, không có bậc thang đột ngột.

**Giá trị ổn định (steady state)** nếu nhiễu là hằng `n`:

```
  φ* = 0.14n / (1 − 0.92) = 1.75n
```

Với `n ∈ [−1, 1]` ⇒ `φ* ∈ [−1.75, 1.75]` rad — vượt xa giới hạn, nên phải kẹp:

```java
climb = clamp(climb, −0.55, +0.55)   // ±31.5°
```

`0.55 rad ≈ 31.5°` là độ dốc tối đa — dốc hơn nữa người chơi không đi được.

### 4.3 Bước tiến — toạ độ cầu

```
  Δx = cos θ · cos φ
  Δz = sin θ · cos φ
  Δy = sin φ · 0.9
```

Đây chính là **vector đơn vị trong hệ toạ độ cầu** (spherical coordinates) với:
- `θ` = azimuth (góc phương vị, quay quanh trục Y)
- `φ` = elevation (góc nâng)

Kiểm tra chuẩn hoá:

```
  |Δ|² = cos²θcos²φ + sin²θcos²φ + 0.81sin²φ
       = cos²φ(cos²θ + sin²θ) + 0.81sin²φ
       = cos²φ + 0.81sin²φ
       = 1 − 0.19sin²φ
```

Với `|φ| ≤ 0.55`: `sin²φ ≤ 0.273` ⇒ `|Δ| ∈ [0.973, 1]`. Gần như đúng 1 khối mỗi bước.

**Hệ số `0.9` ở `Δy`** cố ý làm hầm "bẹt" hơn một chút — thoải theo phương ngang, dễ đi.

### 4.4 Nảy khi chạm trần/đáy

```java
if (cursorY < MIN_WORM_Y) { cursorY = MIN_WORM_Y; climb =  |climb|; }   // 7
if (cursorY > MAX_WORM_Y) { cursorY = MAX_WORM_Y; climb = −|climb|; }   // 54
```

**Phản xạ gương** (specular reflection): giữ nguyên độ lớn góc dốc, đảo dấu. Hầm chạm đáy thì bật lên, chạm trần thì chúi xuống — giống tia sáng phản xạ.

`MAX_WORM_Y = 54` cố ý đặt **gần mặt đất** (seaLevel 48, mặt đất thường 54–60) để hầm có cơ hội trổ miệng; việc chặn hay không do `SurfaceGuard` quyết định.

### 4.5 Thu nhỏ hai đầu — hàm sin

```java
double taper = Math.sin(Math.PI * (step + 1.0) / (steps + 1.0));
double stepRadius = radius * (0.55 + 0.75 * taper);
```

```
              ⎛    π(k+1)  ⎞
  taper(k) = sin⎜ ────────── ⎟  ∈ (0, 1]
              ⎝    n+1     ⎠

  r(k) = r₀ · (0.55 + 0.75 · taper(k))
```

| Vị trí | `taper` | Hệ số bán kính |
|---|---|---|
| Đầu hầm (`k=0`) | ≈ 0 | 0.55 |
| Giữa hầm (`k=n/2`) | 1.0 | **1.30** |
| Cuối hầm (`k=n−1`) | ≈ 0 | 0.55 |

Đường hầm **phình ở giữa, thắt hai đầu** — trông tự nhiên như hang thật, và quan trọng hơn: hai đầu hẹp lại rồi tắt hẳn nên không để lại "lỗ cụt vuông vức".

Với `r₀ ∈ [1.9, 3.8]`: bán kính thực tế `r ∈ [1.05, 4.94]` khối.

---

## 5. Đệ quy — nhánh rẽ

```java
if (depth < MAX_BRANCH_DEPTH && step > 10 && random.nextInt(110) == 0) {
    digTunnel(scope, random, cursorX, cursorY, cursorZ,
              heading + (random.nextDouble() - 0.5) * 2.4,   // lệch tối đa ±1.2 rad (±69°)
              climb * 0.5,
              radius * 0.7,
              steps / 2,
              depth + 1);
}
```

### Chứng minh dừng

| Yếu tố | Giảm dần |
|---|---|
| `depth` | `+1` mỗi lần, chặn tại `MAX_BRANCH_DEPTH = 2` |
| `steps` | `÷2` mỗi cấp |
| `radius` | `×0.7` mỗi cấp |

⇒ Cây đệ quy có **chiều sâu tối đa 3 cấp** (0, 1, 2). Trường hợp cơ sở: `depth == 2` không rẽ nữa.

### Ước lượng số nhánh

Xác suất rẽ mỗi bước `p = 1/110`, số bước hợp lệ `≈ steps − 10`:

```
  E[nhánh cấp 1] = (125 − 10) / 110 ≈ 1.05
  E[nhánh cấp 2] = 1.05 × (62 − 10)/110 ≈ 0.50
  E[tổng nhánh]  ≈ 1 + 1.05 + 0.50 ≈ 2.55 đoạn hầm / hệ thống
```

Kích thước cây nhỏ và có chặn cứng ⇒ không bao giờ nổ đệ quy.

---

## 6. Độ phức tạp

### Một đường hầm

```
  O(steps · r³)
```

Mỗi bước khoét một quả cầu bán kính `r` ⇒ `(4/3)πr³` ô, nhưng vòng lặp duyệt hộp bao `(2r)³ = 8r³` ô.

Với `steps ≈ 125`, `r ≈ 2.9`: `125 × 8 × 24 ≈ 24 000` phép kiểm tra.

### Toàn bộ bước khoét hang của 1 chunk

```
  O(CHUNK_RADIUS² · steps · r³)
```

Cụ thể: `169 chunk × 0.4 hầm × 2.55 đoạn × 24 000` — **nhưng** `scope.touches()` loại bỏ sớm gần như toàn bộ:

```java
if (scope.touches(cursorX, cursorZ, stepRadius)) {
    scope.clearSphere(...);
}
```

`touches` là kiểm tra **AABB vs hình tròn** `O(1)` (4 phép so sánh). Chỉ ~1/169 số bước thực sự chạm chunk hiện tại ⇒ chi phí thực tế giảm ~169 lần.

---

## 7. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `CHUNK_RADIUS` | 6 | Bán kính quét (chunk) |
| `SPAWN_RARITY` | 5 | 1/5 chunk có hệ thống hang |
| `MAX_BRANCH_DEPTH` | 2 | Độ sâu đệ quy tối đa |
| `MAX_WORM_Y` | 54 | Trần — gần mặt đất để trổ cửa hang |
| `MIN_WORM_Y` | 7 | Đáy — trên bedrock |
| hệ số lái `heading` | 0.35 | rad/bước (max ~20°) |
| hệ số nhớ `climb` | 0.92 | `τ ≈ 12` bước |
| kẹp `climb` | ±0.55 | ±31.5° |
| `Δt` nhiễu | 0.06 | chu kỳ ~17 bước |

---

## 8. Chủ đề DSA thể hiện

- **Đệ quy có chặn** + chứng minh dừng (biến giảm `steps/2`, `depth+1`).
- **Bước ngẫu nhiên tương quan** (correlated random walk) lái bằng nhiễu.
- **Bộ lọc IIR bậc 1** / trung bình trượt hàm mũ.
- **Toạ độ cầu** & chuẩn hoá vector.
- **Sinh tất định** để đồng bộ liên chunk mà không cần giao tiếp giữa các luồng.
- **Loại bỏ sớm bằng hộp bao** (AABB culling).

---

## 9. Liên kết

- Khoét & luật mái hang: [CarveScope.md](CarveScope.md), [SurfaceGuard.md](SurfaceGuard.md)
- Seed tất định: [CaveSeeds.md](CaveSeeds.md)
- Anh em: [RavineCarver.md](RavineCarver.md), [CheeseCaveCarver.md](CheeseCaveCarver.md), [SpaghettiCaveCarver.md](SpaghettiCaveCarver.md)
