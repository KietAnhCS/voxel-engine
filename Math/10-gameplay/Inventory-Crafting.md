# Inventory & Crafting — mảng cố định và ghép công thức

**Files:**
- `core/src/com/voxel/game/play/Inventory.java`
- `core/src/com/voxel/game/play/Crafting.java`
- `core/src/com/voxel/game/play/ItemStack.java`

---

## 1. Bố cục túi đồ

```java
public static final int HOTBAR_SIZE = 9;
public static final int STORAGE_ROWS = 3;
public static final int SIZE = HOTBAR_SIZE + STORAGE_ROWS * HOTBAR_SIZE;   // 9 + 27 = 36

private final ItemStack[] slots = new ItemStack[SIZE];
```

```
  ô  0 ..  8  : thanh nhanh (hotbar) — thứ đang cầm trên tay
  ô  9 .. 35  : kho chứa — chỉ hiện khi bấm E
```

```
  SIZE = 9 + 3×9 = 36
```

Đúng bố cục Minecraft: 4 hàng × 9 cột.

### Vì sao mảng cố định chứ không phải `List`?

| | `ItemStack[36]` | `ArrayList<ItemStack>` |
|---|---|---|
| Truy cập ô `i` | **`O(1)`** | `O(1)` |
| Ô trống | `null` tại đúng vị trí | phải thêm object placeholder |
| Vị trí ổn định | **có** — ô 5 luôn là ô 5 | mất khi `remove()` dồn mảng |
| Bộ nhớ | 36 tham chiếu | + overhead của List |

Giao diện túi đồ cần **vị trí cố định** (người chơi nhớ đồ để ở ô nào) ⇒ mảng thưa với `null` là mô hình đúng.

---

## 2. `add` — thuật toán hai lượt

```java
public boolean add(Block block) {
    for (int i = 0; i < SIZE; i++) {                       // LƯỢT 1: dồn chồng
        ItemStack stack = slots[i];
        if (stack != null && stack.isSameBlock(block) && stack.room() > 0) {
            stack.add(1);
            return true;
        }
    }
    for (int i = 0; i < SIZE; i++) {                       // LƯỢT 2: ô trống
        if (slots[i] == null) {
            slots[i] = ItemStack.of(block);
            return true;
        }
    }
    return false;                                           // túi đầy
}
```

### Vì sao phải hai lượt riêng biệt?

Nếu gộp thành một vòng lặp:

```java
// ✗ SAI
for (i) {
    if (slots[i] == null) { slots[i] = new ItemStack(block); return true; }
    if (cùng loại && còn chỗ) { stack.add(1); return true; }
}
```

Túi có ô 0 trống, ô 3 chứa 10 viên đá. Nhặt thêm đá ⇒ tạo chồng **mới** ở ô 0 thay vì dồn vào ô 3 ⇒ túi đầy nhanh vô lý, người chơi phải tự dồn.

**Hai lượt** đảm bảo **luôn ưu tiên dồn chồng** — đúng hành vi Minecraft.

**Độ phức tạp:** `O(n)` với `n = 36`, xấu nhất `2n = 72` phép so sánh. Comment trong code nói rõ đây là đánh đổi có chủ ý: `O(n)` để **không tốn thêm bộ nhớ** cho bảng băm `Map<Block, List<Integer>>`.

Với `n = 36`, `O(n)` là ~72 phép — nhanh hơn cả chi phí băm.

---

## 3. `scroll` — cuộn vòng bằng `floorMod`

```java
public void scroll(int amount) {
    selected = Math.floorMod(selected + amount, HOTBAR_SIZE);
}
```

```
  selected ← (selected + amount) mod 9        (modulo DƯƠNG)
```

### Vì sao `Math.floorMod` chứ không phải `%`?

```java
  (0 - 1) %        9  = −1      ✗ chỉ số âm → ArrayIndexOutOfBounds
  Math.floorMod(-1, 9) =  8      ✔ cuộn vòng về cuối
```

`%` của Java giữ dấu số bị chia; `floorMod` luôn trả kết quả **cùng dấu với số chia** (dương).

Cuộn chuột lên từ ô 0 ⇒ nhảy về ô 8. Cuộn xuống từ ô 8 ⇒ về ô 0.

Cùng bài toán với [`Math.floorDiv`](../03-caves/StructureCarver.md) trong phân ô công trình, và với chuẩn hoá modulo dương của [DayNightCycle](../07-render/DayNightCycle.md).

### `select` — kẹp thay vì cuộn

```java
public void select(int index) {
    this.selected = Math.max(0, Math.min(HOTBAR_SIZE - 1, index));
}
```

Bấm phím số `1..9` ⇒ **kẹp** (clamp), không cuộn. Bấm phím 9 khi chỉ có 9 ô là ô cuối, không vòng về đầu.

Hai hành vi khác nhau cho hai cách nhập liệu khác nhau — chi tiết nhỏ nhưng đúng trực giác.

---

## 4. Crafting — công thức không phân biệt hình dạng

```java
public static final int GRID = 4;                  // lưới 2×2

recipes.add(new Recipe(blocks.wood,        1, blocks.planks,    4));
recipes.add(new Recipe(blocks.birchWood,   1, blocks.planks,    4));
recipes.add(new Recipe(blocks.sand,        4, blocks.sandstone, 1));
recipes.add(new Recipe(blocks.cobblestone, 4, blocks.brick,     1));
```

### Bảng công thức

| Nguyên liệu | Cần | Ra | Số lượng | Tỉ lệ |
|---|---|---|---|---|
| Gỗ sồi | 1 | Ván | 4 | **1 : 4** |
| Gỗ bạch dương | 1 | Ván | 4 | 1 : 4 |
| Cát | 4 | Sa thạch | 1 | **4 : 1** |
| Đá cuội | 4 | Gạch | 1 | 4 : 1 |

Hai loại: **chia nhỏ** (gỗ → ván, ×4) và **nén lại** (cát → sa thạch, ÷4).

### Thuật toán ghép — "shapeless"

```java
private void refresh() {
    result = null;
    Block only = null;
    int count = 0;

    for (ItemStack stack : grid) {
        if (stack == null || stack.isEmpty()) continue;
        if (only != null && only != stack.block()) return;   // ← trộn 2 loại ⟹ không khớp
        only = stack.block();
        count += stack.count();
    }
    if (only == null) return;

    for (Recipe recipe : recipes) {
        if (recipe.ingredient == only && count >= recipe.amount) {
            result = new ItemStack(recipe.result, recipe.resultCount);
            return;
        }
    }
}
```

### Rút gọn bài toán

Công thức **shapeless** (không phân biệt vị trí) ⇒ trạng thái lưới 2×2 rút gọn thành **hai số**:

```
  (loại khối duy nhất,  tổng số lượng)
```

Nếu lưới chứa **từ 2 loại trở lên** ⇒ thoát ngay (`return`), không công thức nào khớp.

So sánh với công thức **có hình dạng** (shaped, như cây cuốc trong Minecraft): phải so khớp **ma trận 2D** với phép tịnh tiến và lật gương — phức tạp hơn nhiều.

### So sánh tham chiếu `!=` và `==`

```java
if (only != null && only != stack.block()) return;
if (recipe.ingredient == only && ...)
```

`Blocks` giữ các **singleton bất biến** ⇒ so sánh tham chiếu là đúng và nhanh hơn `equals()` (1 lệnh CPU vs gọi hàm).

Đây là **Flyweight** áp dụng cho loại khối — cùng tinh thần với [Geometries](../07-render/Geometries.md).

---

## 5. Vì sao dùng `List` cho công thức?

> Comment: *"The recipe list is just a LIST scanned in order; with a few recipes `O(n)` is fast enough, and adding a new recipe does not touch the matching logic."*

| | `List<Recipe>` | `Map<Block, Recipe>` |
|---|---|---|
| Tra cứu | `O(n)` | `O(1)` |
| Nhiều công thức cùng nguyên liệu | **được** | phải dùng `Map<Block, List<Recipe>>` |
| Thêm công thức | 1 dòng | 1 dòng + xử lý va chạm khoá |
| `n = 4` | ~4 phép | băm + tra bảng ≈ tương đương |

Với `n = 4`, quét tuyến tính **nhanh hơn** băm. Đây là ví dụ tốt về việc **không tối ưu sớm**: `O(n)` với `n` nhỏ đánh bại `O(1)` có hằng số lớn.

### Thứ tự quét có ý nghĩa

`for (Recipe recipe : recipes)` trả về công thức **khớp đầu tiên**. Nếu có hai công thức cùng nguyên liệu (ví dụ `4 cát → 1 sa thạch` và `8 cát → 3 sa thạch`), thứ tự khai báo quyết định cái nào thắng.

---

## 6. `take` — tiêu thụ nguyên liệu

```java
public ItemStack take() {
    if (result == null) return null;
    ItemStack taken = result.copy();
    int need = needed(taken.block());
    for (int i = 0; i < GRID && need > 0; i++) {
        ItemStack stack = grid[i];
        while (stack != null && !stack.isEmpty() && need > 0) {
            stack.shrink();
            need--;
            if (stack.isEmpty()) { grid[i] = null; stack = null; }
        }
    }
    refresh();
    return taken;
}
```

### Thuật toán "greedy" tiêu thụ

Duyệt các ô theo thứ tự, **rút cạn từng ô** cho tới khi đủ `need`:

```
  Lưới: [3 cát] [2 cát] [null] [null],  cần 4

  ô 0: 3 → 2 → 1 → 0  (need: 4→3→2→1),  ô 0 = null
  ô 1: 2 → 1          (need: 1→0),       dừng
  Còn lại: [null] [1 cát] [null] [null]
```

`&& need > 0` ở **cả hai** vòng lặp (ngoài và trong) đảm bảo dừng ngay khi đủ — không rút thừa.

`result.copy()` trả về **bản sao**; nếu trả thẳng `result` thì người chơi và lưới cùng trỏ vào một object, sửa một bên hỏng bên kia.

`refresh()` ở cuối cập nhật lại kết quả sau khi nguyên liệu giảm — có thể vẫn đủ ghép tiếp (như ví dụ trên còn 1 cát, không đủ 4 ⇒ `result = null`).

### `needed` — tra ngược

```java
private int needed(Block made) {
    for (Recipe recipe : recipes)
        if (recipe.result == made) return recipe.amount;
    return 1;
}
```

Tra **theo kết quả** thay vì theo nguyên liệu. Trả `1` làm giá trị mặc định an toàn nếu không tìm thấy.

> Điểm cần lưu ý: nếu hai công thức cho **cùng kết quả** với số lượng nguyên liệu khác nhau (gỗ sồi 1 và bạch dương 1 → đều ra ván), `needed` trả về công thức **đầu tiên**. Ở đây cả hai đều cần 1 nên không sao; nếu thêm công thức khác số lượng thì cần truyền `Recipe` thay vì tra ngược.

---

## 7. `returnAll` — trả nguyên liệu khi đóng bảng

```java
public void returnAll(Inventory inventory) {
    for (int i = 0; i < GRID; i++) {
        ItemStack stack = grid[i];
        if (stack != null)
            for (int n = 0; n < stack.count(); n++) inventory.add(stack.block());
        grid[i] = null;
    }
    refresh();
}
```

Đóng bảng chế tạo ⇒ nguyên liệu **quay về túi** thay vì biến mất. Đúng hành vi Minecraft.

Gọi `inventory.add()` từng viên một (`O(count × 36)`) thay vì thêm cả chồng — đảm bảo tận dụng chỗ trống trong các chồng có sẵn.

---

## 8. Độ phức tạp tổng hợp

| Thao tác | Chi phí |
|---|---|
| `Inventory.get/set` | **`O(1)`** |
| `Inventory.add` | `O(n)` = 72 phép xấu nhất |
| `Inventory.consume` | `O(n)` = 36 |
| `Inventory.scroll/select` | `O(1)` |
| `Crafting.refresh` | `O(GRID + R)` = `O(4 + 4)` = **8** |
| `Crafting.take` | `O(GRID × maxStack)` |
| `Crafting.returnAll` | `O(GRID × count × SIZE)` |
| Bộ nhớ | `O(SIZE + GRID)` = 40 tham chiếu |

---

## 9. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `HOTBAR_SIZE` | 9 | Ô thanh nhanh |
| `STORAGE_ROWS` | 3 | Hàng kho chứa |
| `SIZE` | 36 | Tổng ô túi |
| `GRID` | 4 | Lưới chế tạo 2×2 |
| Số công thức | 4 | |

---

## 10. Chủ đề DSA thể hiện

- **Mảng thưa với `null`** — vị trí ổn định, truy cập `O(1)`.
- **Thuật toán hai lượt** để ưu tiên dồn chồng.
- **`Math.floorMod`** cho cuộn vòng với số âm.
- **Kẹp vs cuộn vòng** — hai ngữ nghĩa cho hai cách nhập.
- **Rút gọn không gian trạng thái** — lưới 2×2 → cặp `(loại, số lượng)`.
- **Quét tuyến tính khi `n` nhỏ** — không tối ưu sớm.
- **So sánh tham chiếu với Flyweight** singleton.
- **Thuật toán tham lam** tiêu thụ nguyên liệu.
- **Sao chép phòng vệ** (`result.copy()`).

---

## 11. Liên kết

- Hiển thị: [Hud.md](Hud.md)
- Chỉ số sinh tồn: [PlayerStats.md](PlayerStats.md)
