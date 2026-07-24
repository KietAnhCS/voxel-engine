# HumanoidFigure — khung xương & phép quay quanh khớp

**File:** `core/src/com/voxel/engine/render/HumanoidFigure.java`

Sáu khối hộp ghép thành hình người kiểu Minecraft. Phần toán nằm ở **phép quay quanh một điểm khác gốc toạ độ**.

---

## 1. Hệ đơn vị skin

```java
public static final float HEIGHT = 1.75f;
private static final float U = HEIGHT / 32f;        // ≈ 0.0547 khối
```

Skin Minecraft dùng lưới **32 đơn vị = chiều cao nhân vật**. Hằng `U` là hệ số quy đổi:

```
  1 đơn vị skin = 1.75 / 32 = 0.0546875 khối
```

Nhờ đó mọi kích thước trong code viết theo **đơn vị skin nguyên** (dễ đối chiếu với ảnh skin), chỉ nhân `U` khi dựng mesh.

| Bộ phận | Đơn vị skin | Khối |
|---|---|---|
| Chiều cao tổng | 32 | 1.75 |
| Khớp hông (`HIP_Y`) | 12 | 0.656 |
| Khớp vai (`SHOULDER_Y`) | 24 | 1.313 |

Tỉ lệ hông/tổng = `12/32 = 0.375`, vai/tổng = `24/32 = 0.75` — đúng tỉ lệ cơ thể người.

---

## 2. Phép quay quanh khớp — conjugation

```java
private void swingLimb(int index, float degrees, float pivotY) {
    limb.localTransform.idt()
            .translate(0f, pivotY * U, 0f)      // (3) đẩy trở lại
            .rotate(Vector3.X, degrees)          // (2) xoay
            .translate(0f, -pivotY * U, 0f);     // (1) kéo khớp về gốc
    limb.calculateTransforms(true);
}
```

### Công thức ma trận

```
  M = T(+h) · R_x(θ) · T(−h)
```

Đọc **từ phải sang trái** (thứ tự áp dụng lên điểm):

1. `T(−h)` — dịch để **khớp trùng gốc toạ độ**.
2. `R_x(θ)` — xoay quanh trục X (đi qua gốc).
3. `T(+h)` — dịch trở lại vị trí cũ.

### Vì sao cần?

Ma trận quay `R_x(θ)` chỉ quay quanh **trục X đi qua gốc toạ độ**. Chân được dựng sẵn ở vị trí của nó trong lưới (bàn chân ở `y = 0`, hông ở `y = 12U`). Xoay trực tiếp ⇒ chân quay quanh **bàn chân** — sai hoàn toàn.

> Comment: *"Khúc thịt được dựng sẵn ở đúng chỗ của nó trong lưới, nên phải kéo khớp về gốc toạ độ, xoay, rồi đẩy trở lại — như vậy chân xoay quanh HÔNG chứ không quanh bàn chân."*

### Đây là phép liên hợp (conjugation)

Trong đại số, `M = T · R · T⁻¹` gọi là **liên hợp** của `R` bởi `T`. Ý nghĩa: "thực hiện `R` trong hệ toạ độ đã được `T` dịch chuyển".

Đây là mẫu **phổ quát** cho mọi phép biến đổi quanh một điểm bất kỳ:
- Quay quanh điểm `p`: `T(p) · R · T(−p)`
- Phóng to quanh điểm `p`: `T(p) · S · T(−p)`
- Phản xạ qua mặt phẳng không qua gốc: `T · M_reflect · T⁻¹`

### Kiểm chứng

Điểm khớp `q = (0, h, 0)`:

```
  T(−h)·q = (0, 0, 0)          → về gốc
  R_x(θ)·(0,0,0) = (0, 0, 0)   → gốc bất động khi xoay
  T(+h)·(0,0,0) = (0, h, 0)    → về đúng chỗ cũ  ✔
```

Khớp **đứng yên**, phần còn lại của chân quay quanh nó — đúng như bản lề.

### Ma trận quay quanh X

```
              ⎡ 1    0        0     ⎤
  R_x(θ)  =   ⎢ 0  cos θ   −sin θ   ⎥
              ⎣ 0  sin θ    cos θ   ⎦
```

Chọn trục X vì nhân vật nhìn theo trục Z (sau khi `rotate(Y, yaw)`) ⇒ quay quanh X là **đưa chân ra trước/ra sau**, đúng chuyển động bước đi.

---

## 3. Phối hợp tay chân

```java
float swing = cycle.legAngle();
float punch = cycle.punchAngle();
float idle  = cycle.idleSway();
float armSwing = armRaise > 0f ? swing * 0.15f : swing;

swingLimb(LEG_LEFT,   swing,                            HIP_Y);
swingLimb(LEG_RIGHT, -swing,                            HIP_Y);
swingLimb(ARM_LEFT,  -armSwing - armRaise + idle,       SHOULDER_Y);
swingLimb(ARM_RIGHT,  armSwing - punch - armRaise - idle, SHOULDER_Y);
```

### Bảng góc

| Chi | Công thức | Lệch pha |
|---|---|---|
| Chân trái | `+swing` | 0° |
| Chân phải | `−swing` | **180°** |
| Tay trái | `−swing − raise + idle` | 180° (so với chân trái) |
| Tay phải | `+swing − punch − raise − idle` | 0° (so với chân trái) |

### Quy luật chéo

```
  Tay TRÁI  ngược pha với chân TRÁI  ⟹  cùng pha với chân PHẢI
  Tay PHẢI  cùng pha với chân TRÁI
```

Đây là **dáng đi chéo** (contralateral gait) của người: chân trái bước ra thì tay phải vung ra — giữ cân bằng mô-men động lượng quanh trục dọc cơ thể.

### `idle` đối dấu hai tay

```
  Tay trái:  + idle
  Tay phải:  − idle
```

Hai tay đung đưa **ngược nhau** khi đứng yên ⇒ trông như hơi thở, không phải cả hai tay cùng lắc một chiều (trông như đang bơi).

### `armRaise` — tư thế zombie

```java
float armSwing = armRaise > 0f ? swing * 0.15f : swing;
```

Khi giơ tay (`armRaise ≈ 85°`), biên độ vung tay **giảm còn 15 %**. Zombie giơ hai tay thẳng ra trước và chỉ lắc nhẹ — nếu vẫn vung đủ biên độ 60°, tay sẽ quơ loạn xạ.

`punch` chỉ áp cho **tay phải** — tay thuận, dùng để đánh/đào.

---

## 4. Bẫy libGDX: `isAnimated = true`

```java
Node limb = instance.getNode(LIMB_NAMES[i]);
limbs[i] = limb;
if (limb != null) limb.isAnimated = true;
```

> Comment: *"BẮT BUỘC. Mặc định libGDX coi `localTransform` là do nó quản lý: mỗi lần gọi `calculateTransforms()` nó dựng lại `localTransform` từ translation/rotation/scale (đều đang đứng yên) — tức XOÁ sạch góc xoay mình vừa đặt, nên tay chân không bao giờ nhúc nhích. Bật cờ này lên là báo 'khúc này TAO tự tính, đừng động vào'."*

Trong libGDX, `Node.calculateLocalTransform()`:

```java
if (!isAnimated) localTransform.set(translation, rotation, scale);
return localTransform;
```

Ghi thẳng vào `localTransform` rồi gọi `calculateTransforms()` sẽ bị ghi đè nếu `isAnimated == false`. Đây là loại lỗi im lặng rất khó tìm — không có exception, chỉ là nhân vật đứng đơ.

---

## 5. Flyweight — một hình cho cả đám

```java
public void begin(PerspectiveCamera camera) { batch.begin(camera); }
public void pose(Vector3 feet, float yaw, WalkCycle cycle, float armRaise) { ... }
public void draw() { batch.render(instance, environment); }
public void end() { batch.end(); }
```

Cách dùng:

```java
figure.begin(camera);
for (Monster m : monsters) {
    figure.pose(m.feet(), m.yaw(), m.walkCycle());   // đổi tư thế
    figure.draw();                                     // vẽ
}
figure.end();
```

**Một `Model` + một `ModelInstance` duy nhất** phục vụ toàn bộ đàn quái vật / người chơi khác. Trước mỗi lần vẽ chỉ ghi lại `instance.transform` và 4 `localTransform`.

### So sánh bộ nhớ

| | Mỗi quái một `ModelInstance` | Flyweight |
|---|---|---|
| 50 quái vật | 50 × (~6 node + ma trận) ≈ 50 KB | **~1 KB** |
| Cấp phát khi spawn | có | **không** |
| Texture skin | dùng chung (libGDX cache) | dùng chung |

Mỗi `WalkCycle` (16 byte) vẫn thuộc về từng nhân vật riêng — đó là **trạng thái ngoại lai** (extrinsic state) của Flyweight.

### Đặt vị trí & hướng

```java
instance.transform.setToTranslation(feet).rotate(Vector3.Y, yaw);
```

```
  M_world = T(feet) · R_y(yaw)
```

Áp dụng từ phải sang trái: xoay nhân vật quanh trục dọc **tại gốc**, rồi dịch tới vị trí bàn chân. Vì mesh được dựng với bàn chân ở `y = 0`, phép dịch này đặt chân đúng mặt đất.

---

## 6. Ánh sáng theo màu

```java
public HumanoidFigure(String skinPath, float ambient) { this(skinPath, ambient, ambient, ambient); }

public HumanoidFigure(String skinPath, float red, float green, float blue) {
    environment.set(new ColorAttribute(ColorAttribute.AmbientLight, red, green, blue, 1f));
}
```

Constructor một tham số = ánh sáng trắng (quái vật để tối hơn người chơi một chút). Constructor ba tham số cho phép **nhuộm đỏ** con quái đang nhấp nháy vì trúng đòn.

`Texture.TextureFilter.Nearest` — giữ pixel skin sắc nét, không làm mờ (đúng phong cách Minecraft).

---

## 7. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `HEIGHT` | 1.75 khối | Chiều cao nhân vật, khớp hình va chạm |
| `U` | `1.75/32` ≈ 0.0547 | 1 đơn vị skin |
| `HIP_Y` | 12 U | Khớp hông (37.5 % chiều cao) |
| `SHOULDER_Y` | 24 U | Khớp vai (75 % chiều cao) |
| Hệ số vung tay khi giơ | 0.15 | Zombie chỉ lắc nhẹ |

---

## 8. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `pose` | `O(4)` — 4 lần `swingLimb`, mỗi lần 3 phép nhân ma trận 4×4 |
| `draw` | `O(1)` lệnh vẽ (6 hộp = 72 tam giác) |
| Bộ nhớ | `O(1)` — một model dùng chung |

Một phép nhân ma trận 4×4 là 64 phép nhân + 48 phép cộng. `pose` tốn `4 × 3 × 112 ≈ 1 344` phép — không đáng kể so với 72 tam giác gửi tới GPU.

---

## 9. Chủ đề Toán / Pattern thể hiện

- **Phép liên hợp ma trận** `T·R·T⁻¹` — quay quanh điểm bất kỳ.
- **Ma trận quay quanh trục toạ độ**.
- **Thứ tự nhân ma trận** (áp dụng phải → trái).
- **Hệ đơn vị quy đổi** (skin unit → block).
- **Lệch pha** để phối hợp chuyển động chéo.
- **Flyweight** — tách trạng thái nội tại/ngoại lai.
- **Bẫy API**: `isAnimated` của libGDX.

---

## 10. Liên kết

- Nguồn góc vung: [WalkCycle.md](WalkCycle.md)
- Dựng lưới hộp: `PlayerMesh.java`
- Người dùng: `PlayerModel`, `MonsterRenderer`, `RemotePlayerRenderer`
