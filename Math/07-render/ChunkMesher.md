# ChunkMesher — dựng lưới tam giác từ voxel

**File:** `core/src/com/voxel/engine/render/ChunkMesher.java`

Biến mảng `32 768` khối thành lưới tam giác cho GPU. Nguyên tắc cốt lõi: **chỉ vẽ mặt nào không bị hàng xóm che**.

---

## 1. Face culling — vì sao độ phức tạp là DIỆN TÍCH chứ không phải THỂ TÍCH

Một chunk đặc hoàn toàn có `16 × 16 × 128 = 32 768` khối × 6 mặt = **196 608 mặt**. Nhưng gần như toàn bộ bị che bởi hàng xóm.

```java
public boolean occludes(int x, int y, int z, Block source) {
    if (y < 0) return true;                                   // dưới đáy: coi như đặc
    if (y >= config.worldHeight()) return false;              // trên trời: hở
    Block block = blockAt(x, y, z);
    if (block.isAir()) return false;
    if (block.geometry().occludesNeighbours()) return true;   // khối đặc che hàng xóm
    return block == source;                                   // cùng loại → không vẽ mặt chung
}
```

Chỉ mặt tiếp giáp không khí mới được sinh ⇒

```
  Số mặt sinh ra ∝ DIỆN TÍCH BỀ MẶT, không phải THỂ TÍCH
```

Với chunk địa hình điển hình: `16 × 16 = 256` mặt trên + vài trăm mặt vách ≈ **1 000–3 000 mặt** thay vì 196 608. **Giảm ~99 %.**

### `block == source` — mẹo cho nước và kính

Hai ô nước kề nhau **không vẽ mặt chung** — nếu vẽ, sẽ thấy các mặt trong suốt chồng lên nhau tối sầm và gây lỗi sắp xếp độ sâu. Cùng nguyên tắc với kính trong Minecraft.

---

## 2. Chia section — cắt chunk thành 8 lát

```java
public static final int SECTION_HEIGHT = 16;
int sections = config.worldHeight() / SECTION_HEIGHT;         // 128 / 16 = 8
```

Mỗi chunk sinh **8 mesh riêng**, mỗi mesh cao 16 khối.

### Lợi ích

| | Một mesh cả chunk | 8 mesh section |
|---|---|---|
| Sửa 1 khối ở `y = 5` | dựng lại toàn bộ 32 768 ô | dựng lại **4 096 ô** (1/8) |
| Frustum culling | tất-cả-hoặc-không-gì | bỏ được các lát ngoài tầm nhìn |
| Kích thước 1 mesh | có thể vượt giới hạn 65 536 đỉnh | luôn an toàn |

**Giới hạn 65 536 đỉnh:** chỉ số đỉnh trong OpenGL ES dùng `short` 16-bit. Một section tối đa `4 096 × 6 mặt × 4 đỉnh = 98 304` đỉnh — về lý thuyết vẫn có thể tràn, nhưng face culling khiến thực tế chỉ vài nghìn.

---

## 3. Hai lượt quét — tách hình va chạm khỏi hình trang trí

```java
emitSection(section, size, true);                 // lượt 1: khối CẢN
int collisionIndexCount = solid.indexCount();     // ← ghi lại RANH GIỚI
emitSection(section, size, false);                // lượt 2: khối ĐI XUYÊN QUA (cỏ, hoa)
```

> Comment: *"Nhờ vậy phần đầu của solid mesh chính xác là hình dạng va chạm, phần còn lại chỉ để vẽ. `collisionIndexCount` ghi lại ranh giới đó."*

### Bố trí bộ đệm

```
  solid mesh:  [───── khối cản (va chạm) ─────][── cỏ, hoa (chỉ vẽ) ──]
               0                    collisionIndexCount           indexCount
```

**Một mảng, hai mục đích:**
- **Vẽ:** dùng toàn bộ `[0, indexCount)`.
- **Va chạm:** chỉ đọc `[0, collisionIndexCount)` — người chơi đi xuyên qua cỏ, không đi xuyên qua đá.

Tiết kiệm hẳn một bộ đệm hình học riêng cho vật lý (~50 % bộ nhớ mesh).

### Ba đích ghi (`target`)

```java
if (block.isLiquid()) {
    if (collidableOnly) continue;
    target = fluid;                                    // mesh TRONG SUỐT
} else {
    if (block.isCollidable() != collidableOnly) continue;
    target = solid;
}
```

| Loại khối | Lượt 1 (`collidableOnly = true`) | Lượt 2 |
|---|---|---|
| Đá, đất (cản) | → `solid` | bỏ qua |
| Cỏ, hoa (không cản) | bỏ qua | → `solid` |
| Nước (chất lỏng) | bỏ qua | → `fluid` |

`fluid` được vẽ ở **pass riêng** sau khi vẽ xong mọi thứ đặc, với `glDepthMask(false)` — quy tắc chuẩn để alpha blending đúng.

---

## 4. Cache UV — tra bảng thay vì tìm kiếm

```java
private final Map<String, float[]> uvCache = new HashMap<String, float[]>();

private void cacheRegion(TextureAtlas atlas, String name) {
    if (name == null || uvCache.containsKey(name)) return;
    TextureAtlas.AtlasRegion region = atlas.findRegion(name);
    if (region == null) throw new IllegalArgumentException("missing atlas region " + name);
    uvCache.put(name, new float[]{
        region.getU(),  region.getV2(),      // góc (0,0)
        region.getU2(), region.getV2(),      // góc (1,0)
        region.getU2(), region.getV(),       // góc (1,1)
        region.getU(),  region.getV()        // góc (0,1)
    });
}
```

`atlas.findRegion(name)` là **tìm kiếm tuyến tính** qua danh sách vùng — `O(n)` với `n` = số texture. Gọi nó cho **mỗi mặt** của **mỗi khối** = hàng triệu lần/giây.

Nạp trước vào `HashMap` trong constructor ⇒ tra cứu `O(1)`.

### Vì sao `V2` ở trên, `V` ở dưới?

Toạ độ texture của OpenGL có gốc ở **góc dưới trái**, nhưng ảnh bitmap có gốc ở **góc trên trái**. libGDX trả `v` = mép trên, `v2` = mép dưới. Thứ tự `{v2, v2, v, v}` **lật ảnh theo chiều dọc** cho đúng chiều.

---

## 5. `quad` — sinh 4 đỉnh từ hệ trục địa phương

```java
public void quad(float x, float y, float z, Direction face, float width, float height, ...) {
    float px = x + face.originX();       // gốc mặt
    float py = y + face.originY();
    float pz = z + face.originZ();

    float ux = face.tangentX()   * width;    // vector cạnh 1
    float vx = face.bitangentX() * height;   // vector cạnh 2
    ...
    P0 = (px,        py,        pz       );
    P1 = (px+ux,     py+uy,     pz+uz    );
    P2 = (px+ux+vx,  py+uy+vy,  pz+uz+vz );
    P3 = (px+vx,     py+vy,     pz+vz    );
}
```

### Công thức

```
  P(i, j) = O + i·(w·u) + j·(h·v),     (i,j) ∈ {(0,0), (1,0), (1,1), (0,1)}
```

Đây là **tham số hoá mặt phẳng** bằng một điểm gốc và hai vector tiếp tuyến — chi tiết các con số ở [Direction.md](../06-datastructures/Direction.md).

Thứ tự `(0,0) → (1,0) → (1,1) → (0,1)` đi **ngược chiều kim đồng hồ nhìn từ ngoài** ⇒ GPU nhận diện đúng mặt trước, back-face culling hoạt động.

Tham số `width`, `height` cho phép **một quad phủ nhiều khối** (chuẩn bị cho greedy meshing) và cho **mặt nước thấp hơn 1 khối** (xem [LiquidGeometry](Geometries.md)).

---

## 6. `billboard` — cỏ, hoa hình chữ X

```java
public void billboard(float x, float y, float z, float offsetX, float offsetZ, boolean alongX, ...) {
    float x0 = x + (alongX ? 1f : 0f) + offsetX;
    float z0 = z + offsetZ;
    float x1 = x + (alongX ? 0f : 1f) + offsetX;
    float z1 = z + 1f + offsetZ;
    // quad 1: (x0,z0) → (x1,z1)
    // quad 2: (x1,z1) → (x0,z0)     ← ĐẢO NGƯỢC
}
```

### Vì sao vẽ 2 quad ngược chiều nhau?

Một mặt phẳng chỉ hiển thị từ **một phía** khi bật back-face culling. Cỏ phải nhìn thấy từ **mọi hướng** ⇒ vẽ thêm một quad với thứ tự đỉnh đảo ngược.

`alongX` chọn đường chéo `(0,0)→(1,1)` hay `(1,0)→(0,1)`. Hai `CrossGeometry` gọi `billboard` hai lần với `alongX = true/false` ⇒ tạo hình **chữ X** khi nhìn từ trên xuống.

```
  nhìn từ trên:      ╲  ╱
                      ╳        2 tấm chéo nhau
                     ╱  ╲
```

Tổng: **4 quad = 8 tam giác** cho một bụi cỏ.

`offsetX`, `offsetZ` xê dịch nhẹ vị trí (dựa trên hash toạ độ) để thảm cỏ không xếp thành lưới đều tăm tắp.

---

## 7. `blockAt` — truy cập vượt biên chunk

```java
public Block blockAt(int x, int y, int z) {
    if (y < 0 || y >= config.worldHeight()) return air;
    if (storage.contains(x, y, z)) return registry.byId(storage.blockId(x, y, z));   // nhanh
    return world.blockAt(originX + x, y, originZ + z);                                // chậm
}
```

**Đường dẫn nhanh** đọc thẳng mảng nội bộ `O(1)` không qua bảng băm. **Đường dẫn chậm** chỉ dùng cho ô sát biên (hỏi `World` → tra `HashMap` chunk).

Tỉ lệ: chunk `16×16` có `256` ô, viền chiếm `4×16 − 4 = 60` cột ⇒ ~23 % số cột chạm đường dẫn chậm, nhưng chỉ ở mặt ngoài cùng.

Trên trời (`y ≥ worldHeight`) trả `air` và `lightAt` trả `MAX_LIGHT` ⇒ mặt trên cùng của thế giới luôn được vẽ và sáng đủ.

---

## 8. Tái sử dụng bộ đệm — không cấp phát trong vòng nóng

```java
private final MeshBuffer solid = new MeshBuffer();
private final MeshBuffer fluid = new MeshBuffer();
private final Color[]   corners   = { new Color(), new Color(), new Color(), new Color() };
private final float[]   positions = new float[12];      // 4 đỉnh × 3 toạ độ
private final float[]   uvs       = new float[8];       // 4 đỉnh × 2 UV
```

Tất cả là trường `final` khởi tạo **một lần**. Hàm `quad()` được gọi hàng triệu lần nhưng **không cấp phát byte nào**.

```java
System.arraycopy(uvCache.get(textureRegion), 0, uvs, 0, 8);
```

`arraycopy` là **intrinsic** của JVM — biên dịch thành lệnh SIMD, nhanh hơn vòng lặp thủ công.

### Dọn tham chiếu sau khi dựng xong

```java
this.world = null;
this.chunk = null;
this.storage = null;
```

`ChunkMesher` sống lâu (dùng lại cho mọi chunk). Giữ tham chiếu tới `Chunk` đã dựng xong sẽ **chặn GC** thu hồi chunk đó (~64 KB). Gán `null` giải phóng ngay.

---

## 9. Độ phức tạp

| Giai đoạn | Chi phí |
|---|---|
| Quét khối | `O(size² · height)` = 32 768 × 2 lượt |
| Sinh mặt | `O(diện tích bề mặt)` ≈ 1 000–3 000 mặt |
| Mỗi mặt | `O(1)` — 4 đỉnh, 6 chỉ số, cache UV `O(1)` |
| Đổ bóng góc | `O(4 × 3)` lần `occludes` mỗi mặt — xem [FaceLighting](FaceLighting.md) |
| Bộ nhớ tạm | `O(1)` — bộ đệm tái dùng |
| Bộ nhớ kết quả | tỉ lệ số mặt sinh ra |

**Kết luận quan trọng:** thời gian dựng mesh ∝ **diện tích bề mặt**, không phải thể tích. Một chunk đá đặc (không có mặt lộ) dựng gần như tức thì.

---

## 10. Chủ đề DSA thể hiện

- **Face culling** — giảm `O(V)` xuống `O(S)`.
- **Bộ nhớ đệm tra cứu (memoization)** cho UV.
- **Phân đoạn (spatial partitioning)** — 8 section/chunk.
- **Bố trí bộ đệm hai vùng** — chia sẻ mảng giữa vẽ và va chạm.
- **Tham số hoá mặt phẳng** bằng gốc + 2 tiếp tuyến.
- **Đường dẫn nhanh/chậm** (fast path / slow path).
- **Tránh cấp phát** & dọn tham chiếu chống rò rỉ.

---

## 11. Liên kết

- Hệ trục mặt: [Direction.md](../06-datastructures/Direction.md)
- Đổ bóng góc: [FaceLighting.md](FaceLighting.md)
- Hình dạng khối: [Geometries.md](Geometries.md)
- Nguồn ánh sáng: [LightEngine.md](../05-world/LightEngine.md)
