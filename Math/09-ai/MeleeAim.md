# MeleeAim — ngắm đánh gần bằng tia vs hộp bao

**File:** `core/src/com/voxel/game/combat/MeleeAim.java`

Bắn một tia từ mắt người chơi, coi mỗi sinh vật là một hộp chữ nhật, giữ lại mục tiêu **chạm tia gần nhất**.

---

## 1. Ba bước sử dụng

```java
aim.aimFrom(eyePosition, lookDirection, REACH);   // 1. bắt đầu
for (Monster m : monsters)  aim.consider(m);      // 2. xét từng ứng viên
for (RemotePlayer p : players) aim.consider(p);   // 3. cả người chơi khác
Attackable victim = aim.target();                  // 4. đọc kết quả
```

Đây là mẫu **accumulator / visitor**: object giữ trạng thái "tốt nhất hiện tại", mỗi ứng viên được so sánh và có thể thay thế.

---

## 2. Hộp bao nhân vật

```java
private static final float HALF_WIDTH = 0.35f;
private static final float HEIGHT = 1.85f;

min.set(feet.x - HALF_WIDTH, feet.y,          feet.z - HALF_WIDTH);
max.set(feet.x + HALF_WIDTH, feet.y + HEIGHT, feet.z + HALF_WIDTH);
```

```
  AABB = [x−0.35, x+0.35] × [y, y+1.85] × [z−0.35, z+0.35]
```

| Kích thước | Giá trị |
|---|---|
| Rộng | `0.70` khối |
| Cao | `1.85` khối |
| Gốc | **bàn chân** (`feet.y`) |

### So sánh với hộp va chạm thật

| | Va chạm ([PlayerBody](../08-physics/PhysicsWorld.md)) | Ngắm đánh |
|---|---|---|
| Hình | Capsule `r = 0.35`, cao 1.75 | AABB `0.70 × 1.85` |
| Chiều cao | 1.75 | **1.85** |

> Comment: *"cao hơn nhân vật một chút cho dễ trúng"*

Hộp ngắm **cao hơn 0.1 khối** (~5.7 %) tạo vùng đệm — người chơi nhắm hơi cao vẫn trúng. Đây là **hitbox forgiving**, kỹ thuật phổ biến để game "cảm giác đúng" thay vì "chính xác toán học".

Dùng AABB thay capsule vì `Intersector.intersectRayBounds` rẻ hơn nhiều so với giao tia–capsule.

---

## 3. Giao tia với AABB — thuật toán slab

```java
if (!Intersector.intersectRayBounds(ray, box, point)) return;
```

libGDX cài đặt **slab method**:

### Nguyên lý

AABB là **giao của 3 cặp mặt phẳng song song** ("slab"):

```
  Slab X: x ∈ [minX, maxX]
  Slab Y: y ∈ [minY, maxY]
  Slab Z: z ∈ [minZ, maxZ]
```

Với tia `P(t) = o + t·d`, tính khoảng tham số `t` mà tia nằm trong từng slab:

```
        min_i − o_i              max_i − o_i
  t₁ = ─────────────  ,   t₂ = ─────────────
             d_i                     d_i
```

(hoán đổi nếu `d_i < 0`)

Tia cắt hộp khi **giao của 3 khoảng khác rỗng**:

```
  t_gần = max( t₁ˣ, t₁ʸ, t₁ᶻ )
  t_xa  = min( t₂ˣ, t₂ʸ, t₂ᶻ )

  cắt  ⟺  t_gần ≤ t_xa  ∧  t_xa ≥ 0
```

**Điểm chạm:** `P(t_gần)`.

### Trực quan (2D)

```
        slab Y
     ┌───────────┐
     │  ╱        │        Khoảng t của slab X: [a, b]
  ───┼─╱─────────┼───     Khoảng t của slab Y: [c, d]
     │╱          │        Giao [max(a,c), min(b,d)] ≠ ∅ ⟹ cắt
     ╱           │
    ╱└───────────┘
   tia    slab X
```

**Độ phức tạp:** `O(1)` — 6 phép chia, 4 phép so sánh. Không có vòng lặp, không `sqrt`.

`d_i = 0` (tia song song với slab) được xử lý bằng `±∞` từ phép chia số thực IEEE-754 — không cần rẽ nhánh đặc biệt.

---

## 4. Giữ mục tiêu gần nhất

```java
public void aimFrom(Vector3 origin, Vector3 direction, float reach) {
    ray.origin.set(origin);
    ray.direction.set(direction).nor();
    bestDistance = reach;              // ← khởi tạo bằng TẦM VỚI
    best = null;
}

public void consider(Attackable candidate) {
    ...
    float distance = point.dst(ray.origin);
    if (distance < bestDistance) { bestDistance = distance; best = candidate; }
}
```

### Mẹo thiết kế: `bestDistance = reach`

Khởi tạo `bestDistance` bằng **tầm với** thay vì `+∞` khiến điều kiện `distance < bestDistance` **đồng thời** làm hai việc:

1. Lọc mục tiêu ngoài tầm với (`distance ≥ reach` ⇒ bỏ qua).
2. Giữ mục tiêu gần nhất.

Không cần kiểm tra riêng `if (distance > reach) return;`.

### Vì sao phải gần nhất?

> Comment: *"Nhờ vậy nếu có cả quái vật lẫn người chơi khác chồng lên nhau thì chỉ con đứng trước ăn đòn."*

Nếu chỉ lấy mục tiêu **đầu tiên** chạm tia, thứ tự duyệt danh sách quyết định ai bị đánh — phi lý và không tất định. Lấy gần nhất là đúng vật lý: vật thể phía trước **che** vật thể phía sau.

### `ray.direction.nor()` — bắt buộc

Chuẩn hoá hướng nhìn để `t` trong `P(t) = o + t·d` **bằng đúng khoảng cách thực**. Không chuẩn hoá, `t` sẽ tỉ lệ với `1/‖d‖` và phép so sánh với `reach` sai.

Thực ra `point.dst(ray.origin)` tính khoảng cách trực tiếp nên không phụ thuộc chuẩn hoá — nhưng chuẩn hoá vẫn cần cho `Intersector` hoạt động đúng.

---

## 5. Tái sử dụng object

```java
private final Ray ray = new Ray();
private final BoundingBox box = new BoundingBox();
private final Vector3 point = new Vector3();
private final Vector3 min = new Vector3();
private final Vector3 max = new Vector3();
```

Tất cả là trường `final` khởi tạo một lần. `consider()` được gọi cho **mỗi sinh vật, mỗi lần click** — không cấp phát byte nào.

`box.set(min, max)` ghi đè hộp cũ thay vì `new BoundingBox(...)`.

---

## 6. Interface `Attackable`

```java
public interface Attackable {
    Vector3 feet();
    void takeHit(int damage);
    String displayName();
}
```

`MeleeAim` chỉ cần `feet()`. Nhờ interface, cùng một bộ ngắm phục vụ:

| Cài đặt | Ghi chú |
|---|---|
| `Monster` | Quái vật |
| `RemotePlayer` | Người chơi khác (PvP) |

Đây là **đa hình** đúng nghĩa: thêm loại thực thể mới chỉ cần cài `Attackable`, không sửa `MeleeAim`.

---

## 7. So sánh với VoxelRaycaster

| | [VoxelRaycaster](../08-physics/VoxelRaycaster.md) | MeleeAim |
|---|---|---|
| Mục tiêu | Khối trong lưới | Thực thể tự do |
| Thuật toán | **DDA** duyệt ô | **Slab** giao AABB |
| Số đối tượng | Vô hạn (lưới ngầm) | Vài chục |
| Độ phức tạp | `O(L)` với `L` = tầm | `O(n)` với `n` = số thực thể |
| Kết quả | Ô + mặt bị đâm | Thực thể + khoảng cách |

**Vì sao khác nhau?** Khối nằm trên **lưới đều** ⇒ DDA khai thác cấu trúc đó. Thực thể ở **vị trí tuỳ ý** ⇒ phải kiểm tra từng cái.

Khi số thực thể lớn (hàng nghìn), có thể thêm **spatial hash** hoặc **BVH** để giảm `O(n)` xuống `O(log n)` — nhưng với `MAX_MONSTERS = 4` thì không cần.

---

## 8. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `HALF_WIDTH` | 0.35 | Hộp rộng 0.7 khối |
| `HEIGHT` | 1.85 | Cao hơn nhân vật 0.1 để dễ trúng |
| `reach` | tham số | Tầm với cánh tay |

---

## 9. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `aimFrom` | `O(1)` |
| `consider` | `O(1)` — 6 chia + 4 so sánh + 1 `dst` |
| Tổng một lần đánh | `O(n)` với `n` = số ứng viên |
| Bộ nhớ | `O(1)` — 5 object tái dùng |

---

## 10. Chủ đề Toán / DSA thể hiện

- **Slab method** — giao tia với AABB, `O(1)`.
- **Tham số hoá tia** `P(t) = o + t·d`.
- **Giao của các khoảng** `[max của min, min của max]`.
- **Mẫu accumulator** — giữ tốt nhất hiện tại.
- **Khởi tạo thông minh** (`bestDistance = reach`) gộp hai điều kiện.
- **Hitbox forgiving** — thiết kế game vs chính xác toán học.
- **Đa hình qua interface** (`Attackable`).
- **Xử lý `±∞` của IEEE-754** thay rẽ nhánh.

---

## 11. Liên kết

- Ngắm khối: [VoxelRaycaster.md](../08-physics/VoxelRaycaster.md)
- Mục tiêu quái: [MonsterAI.md](MonsterAI.md)
- Hộp va chạm thật: [PhysicsWorld.md](../08-physics/PhysicsWorld.md)
