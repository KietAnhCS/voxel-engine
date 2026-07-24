# Hud & MinecraftUi — hệ toạ độ giao diện

**Files:**
- `core/src/com/voxel/game/play/Hud.java`
- `core/src/com/voxel/game/play/MinecraftUi.java`

---

## 1. Hệ đơn vị "GUI pixel"

```java
public static final float SCALE = 3f;

private static float px(float guiPixels) {
    return guiPixels * MinecraftUi.SCALE;
}
```

```
  1 GUI pixel = 3 màn hình pixel
```

Mọi kích thước trong code viết theo **đơn vị Minecraft gốc**, chỉ nhân `SCALE` khi vẽ:

| Đại lượng | GUI px | Màn hình px |
|---|---|---|
| Thanh nhanh rộng | 182 | **546** |
| Thanh nhanh cao | 22 | 66 |
| Một ô | 20 | 60 |
| Khung chọn | 24 | 72 |
| Biểu tượng tim/đùi gà | 9 | 27 |
| Thanh XP cao | 5 | 15 |

Cùng tư tưởng với hằng `U` của [HumanoidFigure](../07-render/HumanoidFigure.md): **tách đơn vị thiết kế khỏi đơn vị hiển thị**.

### Vì sao có ích?

1. **Đối chiếu trực tiếp với ảnh chụp Minecraft** — comment trong `MinecraftUi` ghi rõ mọi màu và kích thước đều "đo trực tiếp từ hai ảnh chụp Minecraft thật".
2. **Đổi tỉ lệ = đổi một hằng số** — muốn giao diện to hơn chỉ cần `SCALE = 4`.
3. **Số nguyên** — mọi kích thước GUI là số nguyên nên nhân với `SCALE` nguyên cho toạ độ nguyên ⇒ **không có điểm ảnh mờ** (không cần lọc bilinear).

### Chiều rộng thanh nhanh

```java
private static final float BAR_WIDTH = Inventory.HOTBAR_SIZE * SLOT + 2f;   // 9 × 20 + 2 = 182
```

`+2` là **viền 1 px mỗi bên**. Con số 182 khớp chính xác texture gốc của Minecraft.

---

## 2. Căn giữa màn hình

```java
float barX = Math.round((width - px(BAR_WIDTH)) * 0.5f);
```

```
              W − 546
  x = round( ───────── )
                 2
```

`Math.round` là chi tiết quan trọng: nếu `W` lẻ, `(W − 546)/2` cho số **.5** ⇒ toạ độ không nguyên ⇒ texture bị **lấy mẫu giữa hai điểm ảnh** ⇒ viền mờ nhoè.

Làm tròn đảm bảo mọi cạnh rơi đúng vào biên điểm ảnh — **pixel-perfect**.

Comment trong `Hud` xác nhận: *"Làm tròn như InventoryScreen: thanh nhanh nằm giữa màn hình lẻ vẫn sắc nét."*

---

## 3. Khung chọn trượt mượt

```java
private static final float SELECT_SLIDE_SPEED = 18f;

float target = inventory.selected();
if (animatedSlot < 0f || Math.abs(target - animatedSlot) > Inventory.HOTBAR_SIZE / 2f) {
    animatedSlot = target;                    // bám thẳng
} else {
    animatedSlot += (target - animatedSlot) * Math.min(1f, delta * SELECT_SLIDE_SPEED);
}
```

### Lerp hàm mũ

```
  a ← a + (target − a) · min(1, 18·Δt)
```

```
        1
  τ =  ────  ≈ 0.056 giây
        18
```

Nhanh hơn nội suy mạng ([RemotePlayer](../11-net/RemotePlayer.md), `τ = 0.1`) vì đây là phản hồi trực tiếp cho thao tác người chơi — độ trễ phải nhỏ.

### Hai trường hợp bám thẳng

**(a) `animatedSlot < 0`** — lần đầu, chưa có giá trị. Khởi tạo `animatedSlot = −1` làm sentinel.

**(b) `|target − animatedSlot| > 4.5`** — phát hiện **cuộn vòng**.

```
  HOTBAR_SIZE / 2 = 9 / 2 = 4.5
```

Cuộn từ ô 8 sang ô 0: `|0 − 8| = 8 > 4.5` ⇒ **bám thẳng**, không trượt.

> Comment: *"Lần đầu, hoặc cuộn vòng (ô 8 → ô 0): bám thẳng, không trượt ngang hết cả thanh."*

Không có kiểm tra này, khung chọn sẽ **lướt ngang qua toàn bộ 8 ô** — trông rất lạ khi người chơi chỉ cuộn chuột một nấc.

Đây là bài toán **khoảng cách trên vòng tròn**, cùng bản chất với `shortestAngle` của [RemotePlayer](../11-net/RemotePlayer.md) — nhưng ở đây chọn cách đơn giản hơn: phát hiện và bỏ qua hoạt ảnh.

### Bù lệch 1 pixel

```java
float selectX = x - edge + px(animatedSlot * SLOT);
float size = px(24f);
```

Khung chọn rộng **24** GUI px, ô rộng **20** GUI px. Để tâm khung trùng tâm ô:

```
  lệch = (24 − 20) / 2 = 2 GUI px
```

Nhưng ô bắt đầu **sau viền 1 px** của thanh, nên bù thêm `−1`:

```
  x_khung = x_thanh − 1 + slot × 20
```

> Comment: *"Tâm khung 24 phải trùng tâm Ô 20 (ô bắt đầu sau viền 1px): lùi 1px GUI sang trái, nếu không khung sẽ LỆCH 1px về bên phải so với ô đang chọn."*

Loại lỗi lệch 1 pixel này rất khó phát hiện nhưng mắt nhận ra ngay là "có gì đó sai".

---

## 4. Hàng biểu tượng — thuật toán nửa trái tim

```java
private void drawIconRow(SpriteBatch batch, Texture empty, Texture half, Texture full,
                         int value, boolean fromRight, float x, float y) {
    for (int i = 0; i < ICONS; i++)                      // LƯỢT 1: nền rỗng
        batch.draw(empty, iconX(x, i, fromRight), y, size, size);

    for (int i = 0; i < ICONS; i++) {                    // LƯỢT 2: đầy / nửa
        int slot = value - i * 2;
        Texture fill = slot >= 2 ? full : slot == 1 ? half : null;
        if (fill != null) batch.draw(fill, iconX(x, i, fromRight), y, size, size);
    }
}
```

### Công thức

Mỗi biểu tượng chứa **2 đơn vị** (20 máu = 10 trái tim):

```
  slot_i = value − 2i

              ⎧ FULL    nếu slot_i ≥ 2
  icon_i =    ⎨ HALF    nếu slot_i = 1
              ⎩ (nền)   nếu slot_i ≤ 0
```

### Ví dụ `value = 15` (7.5 trái tim)

| `i` | `slot = 15 − 2i` | Vẽ |
|---|---|---|
| 0 | 15 | FULL |
| 1 | 13 | FULL |
| ... | ... | FULL |
| 6 | 3 | FULL |
| **7** | **1** | **HALF** |
| 8 | −1 | (chỉ nền) |
| 9 | −3 | (chỉ nền) |

⇒ 7 trái tim đầy + 1 nửa = **7.5** ✔

### Vì sao vẽ hai lượt?

> Comment: *"vẽ hết các ô NỀN rỗng (`_empty`) trước, rồi dán bản đầy (`_full`) hoặc nửa (`_half`) lên trên — nhờ vậy ô nửa vẫn thấy được phần nền tối còn lại."*

Ảnh `_half` chỉ vẽ **nửa trái tim bên trái**, nửa phải **trong suốt**. Nếu không có lớp nền, nửa phải sẽ lộ ra thế giới phía sau thay vì phần trái tim xám.

Đây cũng chính là cách Minecraft làm.

### `fromRight` — hướng vơi

```java
private float iconX(float barX, int i, boolean fromRight) {
    return fromRight ? iconFromRight(barX, i) : barX + px(i * ICON_STEP);
}
```

| Hàng | Hướng | Vơi từ |
|---|---|---|
| Trái tim | trái → phải | **phải** |
| Đùi gà | phải → trái | **trái** |

Hai hàng vơi về **phía đối diện nhau**, đối xứng qua tâm màn hình — chi tiết thẩm mỹ của Minecraft.

---

## 5. Thanh kinh nghiệm

```java
float filled = barWidth * Math.max(0f, Math.min(1f, stats.progress()));

ui.rect(batch, XP_EMPTY, x, y, barWidth, height);              // nền
ui.rect(batch, Color.BLACK, x, y, barWidth, px(1f));           // viền dưới
if (filled > 0f) {
    ui.rect(batch, XP_GREEN,      x, y + px(1f), filled, height - px(1f));   // thân
    ui.rect(batch, XP_GREEN_DARK, x, y,          filled, px(1f));            // gờ dưới
}
```

```
  w_đầy = 546 × clamp(progress, 0, 1)
```

`progress ∈ [0, 1]` từ [PlayerStats](PlayerStats.md) §6 — phần trăm tới cấp tiếp theo. `clamp` phòng vệ nếu `progress` vượt biên do lỗi làm tròn.

**Ba sắc màu** tạo cảm giác nổi khối: xanh sáng ở thân, xanh đậm ở gờ dưới, đen ở viền — kỹ thuật đổ bóng 1 pixel kinh điển của pixel art.

### Viền chữ 8 hướng

```java
font.setColor(Color.BLACK);
for (int dx = -1; dx <= 1; dx++)
    for (int dy = -1; dy <= 1; dy++)
        if (dx != 0 || dy != 0)
            font.draw(batch, text, textX + dx * 2f, textY + dy * 2f);
font.setColor(XP_GREEN);
font.draw(batch, text, textX, textY);
```

Vẽ chữ đen **8 lần** lệch quanh vị trí gốc (bỏ `(0,0)`), rồi vẽ chữ màu ở giữa.

```
  ╔═══╦═══╦═══╗
  ║ ● ║ ● ║ ● ║      ● = bản đen lệch
  ╠═══╬═══╬═══╣      ○ = bản màu chính giữa
  ║ ● ║ ○ ║ ● ║
  ╠═══╬═══╬═══╣      Tổng: 9 lần vẽ
  ║ ● ║ ● ║ ● ║
  ╚═══╩═══╩═══╝
```

Tạo **viền đen bao quanh** giúp số cấp đọc được trên mọi nền. Rẻ hơn dùng shader outline (9 lệnh vẽ text vs 1 pass shader phụ), và với một con số ngắn thì không đáng kể.

Bước lệch `2f` là **màn hình pixel** (không nhân `px`) — viền dày 2 px thật.

---

## 6. Bong bóng khí

```java
float ratio = stats.air() / PlayerStats.MAX_AIR;
int full = (int) Math.ceil(ratio * ICONS);
```

```
  n_đầy = ⌈ 10 · air / 15 ⌉
```

**Dùng `ceil` chứ không `floor`:** khi `air` vừa tụt xuống dưới 15, `ratio = 0.99` ⇒ `ceil(9.9) = 10` ⇒ vẫn hiện đủ 10 bong bóng. Với `floor` thì bong bóng cuối biến mất ngay lập tức.

Bong bóng cuối chỉ vỡ khi `air` thực sự về 0:

```
  air = 0  ⟹  ceil(0) = 0 bong bóng
  air = 0.1 ⟹ ceil(0.067) = 1 bong bóng
```

⇒ Người chơi luôn thấy **ít nhất 1 bong bóng** cho tới khoảnh khắc hết hơi. Đúng cảnh báo trực quan.

### Chỉ hiện khi cần

```java
if (stats.showAir()) drawBubbles(batch, barX);

// trong PlayerStats:
public boolean showAir() { return air < MAX_AIR - 0.01f; }
```

Ngưỡng `−0.01` (không phải `< MAX_AIR`) tránh nhấp nháy do sai số dấu phẩy động khi `air` vừa hồi đầy.

---

## 7. Hệ thống màu

```java
public static final Color PANEL_BG    = rgb(0x2E2C37);
public static final Color SLOT_BG     = rgb(0x1B1A21);
public static final Color SELECTION   = rgb(0xEF51D5);   // magenta
public static final Color XP_GREEN    = rgb(0x80FF20);
public static final Color XP_EMPTY    = rgb(0x1B2B0B);
```

Bảng màu "tối + điểm nhấn tím" học từ gói **Better Modded GUI**. Mọi màu là hằng số `static final` — sửa một chỗ đổi toàn giao diện.

### Vẽ hình chữ nhật bằng 1 điểm ảnh trắng

```java
public final Texture white;    // 1×1 pixel trắng
```

`ui.rect(batch, color, x, y, w, h)` = vẽ texture 1×1 kéo giãn ra `w × h` với `batch.setColor(color)`.

**Ưu điểm:**
- Một texture duy nhất cho **mọi** hình chữ nhật ⇒ SpriteBatch không phải đổi texture ⇒ **gộp được tất cả vào một lệnh vẽ** (batching).
- Sắc nét ở mọi độ phân giải (màu đơn, không có chi tiết để mờ).
- Tốn 4 byte bộ nhớ.

Nếu dùng `ShapeRenderer`, mỗi lần chuyển giữa `SpriteBatch` và `ShapeRenderer` phải `end()`/`begin()` ⇒ hàng chục lệnh vẽ riêng lẻ.

---

## 8. Bảng hằng số

| Hằng | GUI px | Màn hình px |
|---|---|---|
| `SCALE` | — | 3 |
| `BAR_WIDTH` | 182 | 546 |
| `BAR_HEIGHT` | 22 | 66 |
| `SLOT` | 20 | 60 |
| Khung chọn | 24 | 72 |
| `ICON` | 9 | 27 |
| `ROW_HEALTH_Y` | 28 | 84 |
| `ROW_AIR_Y` | 38 | 114 |
| `XP_BAR_Y` | 22 | 66 |
| `XP_BAR_HEIGHT` | 5 | 15 |
| `SELECT_SLIDE_SPEED` | 18 | `τ ≈ 0.056` s |
| `ICONS` | 10 | biểu tượng mỗi hàng |

---

## 9. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `drawHotbar` | `O(9)` ô |
| `drawIconRow` | `O(2 × 10)` = 20 lệnh vẽ |
| `drawXpBar` | `O(1)` + 9 lệnh vẽ chữ |
| `drawSelection` | `O(1)` |
| **Tổng mỗi khung hình** | ~80 lệnh vẽ, gộp thành ít lệnh GPU |

---

## 10. Chủ đề Toán / Đồ hoạ thể hiện

- **Hệ đơn vị quy đổi** (GUI px → screen px).
- **Làm tròn để pixel-perfect** — tránh lấy mẫu giữa điểm ảnh.
- **Lerp hàm mũ** cho hoạt ảnh khung chọn.
- **Phát hiện cuộn vòng** bằng ngưỡng nửa chu vi.
- **Thuật toán nửa biểu tượng** `slot = value − 2i`.
- **Vẽ hai lượt** để lộ nền ở biểu tượng nửa.
- **`ceil` vs `floor`** — chọn hướng làm tròn theo ngữ nghĩa.
- **Vùng chết** (`MAX_AIR − 0.01`) chống nhấp nháy.
- **Outline 8 hướng** cho chữ.
- **Batching bằng texture 1×1**.

---

## 11. Liên kết

- Nguồn dữ liệu: [PlayerStats.md](PlayerStats.md), [Inventory-Crafting.md](Inventory-Crafting.md)
- Hoạt ảnh tương tự: [RemotePlayer.md](../11-net/RemotePlayer.md)
