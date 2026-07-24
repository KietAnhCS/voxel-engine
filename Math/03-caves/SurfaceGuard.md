# SurfaceGuard — luật mái hang & cửa hang

**File:** `core/src/com/voxel/game/terrain/carve/SurfaceGuard.java`

---

## 1. Bài toán đánh đổi

| Nếu... | Hậu quả |
|---|---|
| Cấm tuyệt đối khoét lên mặt đất | Cả thế giới **không có lấy một cửa hang** — người chơi không bao giờ tìm được đường xuống lòng đất |
| Thả cửa hoàn toàn | Mặt đất **thủng lỗ chỗ như tổ ong** — nhìn rất lộ liễu, phi tự nhiên |

**Minecraft ở giữa:** hang gần như luôn nằm kín trong lòng đất, thỉnh thoảng mới trổ một cái miệng ra sườn đồi.

---

## 2. Giải pháp: mái hang biến thiên theo nhiễu 2D

```java
public int requiredRoof(int worldX, int worldZ) {
    if (noise == null) return ROOF;                 // bản sealed()
    double value = noise.entrance(worldX, worldZ);
    if (value > OPEN) return 0;
    if (value > RIM)  return RIM_ROOF;
    return ROOF;
}
```

### Hàm bậc thang

```
             ⎧ 0    nếu  E(x,z) > 0.44        ← MIỆNG HANG: thủng hẳn ra
  roof(x,z) = ⎨ 2    nếu  E(x,z) > 0.32       ← VIỀN: mái mỏng
             ⎩ 4    ngược lại                 ← BÌNH THƯỜNG: mái dày
```

với `E(x,z) = noise.entrance(x, z)` — fBm 2D, 2 octave, `f = 0.012`.

Điều kiện khoét ở [CarveScope.clear](CarveScope.md):

```
  y + roof(x,z)  ≤  surfaceHeight(x,z)
```

- `roof = 0` ⇒ được khoét tới tận `y = surfaceHeight` ⇒ **thủng ra mặt đất**.
- `roof = 4` ⇒ ô cao nhất được khoét là `surfaceHeight − 4` ⇒ còn 4 khối đá che.

---

## 3. Vì sao dùng nhiễu chứ không phải xác suất ngẫu nhiên?

Nếu mỗi cột tự tung xúc xắc `P(mở) = 3 %`, các cột mở sẽ **rải rác đơn lẻ** — mặt đất lỗ chỗ những lỗ 1×1 khối, trông như bị sâu đục.

Nhiễu Simplex thì **liên tục theo không gian**: các cột có `E > 0.44` **kết thành mảng liền nhau**. Một mảng như vậy rộng cỡ:

```
  λ = 1/f = 1/0.012 ≈ 83 khối
```

⇒ vùng "được phép mở" là những **mảnh đất rộng vài chục khối**, đủ lớn để một đường hầm đâm xuyên qua và tạo ra **một cái cửa hang liền lạc**, chứ không phải hàng trăm lỗ kim.

### Tỉ lệ diện tích

fBm 2 octave có `σ ≈ 0.28`:

| Vùng | Điều kiện | z-score | Diện tích ≈ |
|---|---|---|---|
| Miệng hang (`roof = 0`) | `E > 0.44` | +1.57σ | **~5.8 %** |
| Viền (`roof = 2`) | `0.32 < E ≤ 0.44` | +1.14σ … +1.57σ | ~6.9 % |
| Bình thường (`roof = 4`) | `E ≤ 0.32` | — | ~87 % |

> Comment trong code ước lượng "~3 % diện tích" — cùng bậc độ lớn.

### Vì sao có mức trung gian `RIM_ROOF = 2`?

Nếu chỉ có 2 mức (0 và 4), tại biên của mảng mở sẽ có **bậc thang đột ngột 4 khối** — nhìn thấy rõ một đường viền vuông vức quanh miệng hang.

Mức trung gian tạo **chuyển tiếp 3 bậc** `4 → 2 → 0`, miệng hang loe dần ra tự nhiên.

---

## 4. Hai chế độ — Factory Method

```java
public static SurfaceGuard withEntrances(TerrainNoise noise) {
    return new SurfaceGuard(noise);         // cho phép trổ miệng
}

public static SurfaceGuard sealed() {
    return new SurfaceGuard(null);          // luôn giữ mái dày 4
}
```

`noise == null` được dùng làm **cờ chế độ** (Null Object). Constructor để `private` ⇒ chỉ tạo được qua hai factory method có tên tự mô tả.

### Ai dùng chế độ nào

| Carver | Chế độ | Lý do |
|---|---|---|
| `PerlinWormCarver` | `withEntrances` | Đường hầm nên có cửa vào |
| `RavineCarver` | `withEntrances` | Khe nứt xé toạc lên mặt đất là điểm nhấn |
| `SpaghettiCaveCarver` | `withEntrances` | Hành lang nhỏ trổ ra sườn đồi |
| **`CheeseCaveCarver`** | **`sealed`** | Bọng rỗng nằm **khắp nơi** — nếu cho mở, mặt đất sẽ rỗ như tổ ong |

Đây là điểm thiết kế then chốt: `CheeseCave` khoét ~4.8 % **toàn bộ thể tích** lòng đất, mật độ quá cao để cho phép trổ lên bề mặt.

---

## 5. Tương tác với `MAX_WORM_Y`

`PerlinWormCarver` đặt `MAX_WORM_Y = 54` — cố ý **cao gần mặt đất** (seaLevel 48, mặt đất thường 54–60).

```
  Giun được phép bò lên tới y = 54
        ↓
  Tại cột có E > 0.44  →  roof = 0  →  khoét được tới mặt đất  →  CỬA HANG
  Tại cột có E ≤ 0.32  →  roof = 4  →  dừng ở surfaceHeight − 4  →  hầm ngầm
```

> Comment trong code: *"việc giữ mái hang dày bao nhiêu là do SurfaceGuard lo, nên chỗ nào được phép thì hầm trổ miệng ra thành cửa hang, chỗ không được thì tự động dừng lại dưới lòng đất."*

Đây là **phân tách trách nhiệm** đẹp: carver chỉ lo hình dạng hang, guard chỉ lo quan hệ với bề mặt.

---

## 6. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `ROOF` | 4 | Mái dày mặc định (khối) |
| `OPEN` | 0.44 | Ngưỡng nhiễu để mở hẳn |
| `RIM` | 0.32 | Ngưỡng viền |
| `RIM_ROOF` | 2 | Mái mỏng ở viền |
| `entrance` frequency | 0.012 | Mảng mở rộng ~83 khối |

---

## 7. Chủ đề DSA / Pattern thể hiện

- **Hàm bậc thang** (step function) trên trường nhiễu liên tục.
- **Factory Method** + **Null Object** (`noise == null` = chế độ sealed).
- **Phân tách trách nhiệm** (separation of concerns): hình dạng vs ràng buộc bề mặt.
- **Tương quan không gian** — vì sao nhiễu hơn hẳn random độc lập.

---

## 8. Liên kết

- Nơi áp dụng luật: [CarveScope.md](CarveScope.md) §3
- Trường nhiễu `entrance`: [TerrainNoise.md](../01-noise-terrain/TerrainNoise.md)
- Người dùng: [PerlinWormCarver.md](PerlinWormCarver.md), [CheeseCaveCarver.md](CheeseCaveCarver.md)
