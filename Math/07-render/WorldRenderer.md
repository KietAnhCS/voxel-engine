# WorldRenderer — frustum culling & pipeline đa luồng

**File:** `core/src/com/voxel/engine/render/WorldRenderer.java`

---

## 1. Độ ưu tiên — bình phương khoảng cách

```java
private int priorityOf(Chunk chunk) {
    int half = config.chunkSize() / 2;
    float dx = chunk.originX() + half - camera.position.x;
    float dz = chunk.originZ() + half - camera.position.z;
    return (int) (dx * dx + dz * dz);
}
```

```
  p = (x_tâm − x_cam)² + (z_tâm − z_cam)²
```

### Ba tối ưu trong ba dòng

1. **Không lấy căn bậc hai.** Vì `√` là hàm **đơn điệu tăng**, so sánh `d₁² < d₂²` tương đương `d₁ < d₂`. Bỏ `sqrt` tiết kiệm ~15 chu kỳ CPU mỗi lần so sánh trong heap.

2. **Ép về `int`.** So sánh số nguyên nhanh hơn số thực và tránh vấn đề `NaN` trong [ChunkHeap](../06-datastructures/ChunkHeap.md).

3. **Chỉ dùng khoảng cách ngang** (bỏ `y`). Chunk trải suốt chiều cao thế giới nên khoảng cách dọc vô nghĩa.

**Cảnh báo tràn số:** `dx² + dz²` với `dx = dz = 30 000` cho `1.8 × 10⁹` — vẫn dưới `2³¹ − 1 ≈ 2.1 × 10⁹`. An toàn trong phạm vi thế giới hợp lý.

---

## 2. Frustum culling

```java
BoundingBox bounds = translucent ? set.translucentBounds(section) : set.solidBounds(section);
if (bounds == null || !camera.frustum.boundsInFrustum(bounds)) continue;
```

### Frustum là gì

**Khối chóp cụt thị giác** — vùng không gian camera nhìn thấy, giới hạn bởi **6 mặt phẳng**:

```
                    far plane
                  ╱──────────╲
                ╱              ╲      top / bottom
              ╱                  ╲    left / right
            ╱────────────────────╲   near / far
           near plane
```

### Kiểm tra hộp bao vs frustum

Mỗi mặt phẳng biểu diễn bởi phương trình:

```
  n·p + d = 0
```

Điểm `p` nằm **phía trong** nếu `n·p + d > 0`.

Với hộp bao AABB, chỉ cần kiểm tra **đỉnh xa nhất theo hướng `−n`** (positive vertex test):

```
  Nếu tồn tại mặt phẳng mà TẤT CẢ 8 đỉnh hộp đều nằm ngoài
     ⟹ hộp nằm hoàn toàn ngoài frustum ⟹ loại
```

libGDX cài đặt bằng cách kiểm tra 8 đỉnh với 6 mặt phẳng = `O(48)` phép tích vô hướng — nhưng thoát sớm ngay khi tìm được một mặt phẳng loại được hộp.

### Hiệu quả

Với góc nhìn `67°` và tỉ lệ màn hình `16:9`, frustum chiếm khoảng:

```
  Ω_frustum / 4π  ≈  0.18
```

⇒ **~82 % số section bị loại** trước khi gửi lệnh vẽ. Đây là tối ưu hiệu quả nhất trong toàn bộ pipeline vẽ.

### Vì sao culling theo SECTION chứ không theo CHUNK?

Chunk cao 128 khối. Người chơi đứng dưới đất nhìn ngang chỉ thấy vài section ở giữa. Culling theo section (16 khối) loại thêm được ~60 % phần còn lại.

```
  Chunk-level culling:   ~18 % chunk được vẽ
  Section-level culling: ~18 % × 40 % ≈ 7 % khối lượng
```

---

## 3. Pipeline đa luồng

```
  ┌─────────────┐  submit   ┌───────────┐  poll   ┌──────────────┐
  │ World       │──────────►│ Scheduler │────────►│ mesh workers │
  │ (đổi khối)  │           │ (heap)    │         │ (N luồng)    │
  └─────────────┘           └───────────┘         └──────┬───────┘
                                                          │ build()
                                                          ▼
  ┌─────────────┐  drain    ┌────────────────┐   submit  ┌──────────────┐
  │ luồng VẼ    │◄──────────│ RenderCommand  │◄──────────│ GeometryData │
  │ (upload GPU)│  (3/frame)│ Queue          │           └──────────────┘
  └─────────────┘           └────────────────┘
```

### Vì sao phải tách?

| Việc | Luồng nào | Lý do |
|---|---|---|
| Dựng mesh (`ChunkMesher.build`) | **worker** | Thuần CPU, ~10–30 ms, song song hoá được |
| Nạp lên GPU (`new Mesh`) | **luồng vẽ** | OpenGL context chỉ thuộc một luồng |

`ChunkMesher.build` trả về `ChunkGeometryData` — **mảng float/short thuần**, không đụng OpenGL ⇒ chạy an toàn trên worker.

### `ThreadLocal<ChunkMesher>`

```java
this.meshers = new ThreadLocal<ChunkMesher>() {
    protected ChunkMesher initialValue() { return new ChunkMesher(engineConfig, registry, atlas); }
};
```

`ChunkMesher` có bộ đệm tái dùng (`MeshBuffer`, `positions[]`, `corners[]`) ⇒ **không an toàn đa luồng**. `ThreadLocal` cho mỗi worker một thể hiện riêng — cùng kỹ thuật với [Geometries](Geometries.md).

Số object = số worker (2–8), không phải số chunk.

---

## 4. Điều tiết tải (throttling)

### 4.1 Ngân sách nạp GPU

```java
private static final int UPLOAD_BUDGET_PER_FRAME = 3;
commands.drain(UPLOAD_BUDGET_PER_FRAME);
```

> Comment: *"Một lần nạp tốn ~3 ms trên luồng chính (chủ yếu là dựng cây BVH va chạm), nên đặt 6 sẽ kéo khung hình xuống ~50 fps mỗi khi hàng đợi đầy. Ba chunk/khung hình ở 60 fps vẫn là 180 chunk/giây — thừa sức theo kịp người chơi đang chạy."*

### Tính toán

```
  Chi phí = 3 chunk × 3 ms = 9 ms / khung hình
  Ngân sách khung hình 60 fps = 16.67 ms
  Còn lại cho vẽ = 7.67 ms
```

Với 6 chunk: `18 ms > 16.67 ms` ⇒ **tụt xuống dưới 60 fps**. Con số 3 được chọn chính xác từ phép tính này.

### Thông lượng

```
  3 chunk/khung × 60 khung/s = 180 chunk/giây
```

Người chơi chạy `4.3` khối/s cần khoảng `4.3/16 × (2×12+1) ≈ 6.7` chunk mới mỗi giây — dư sức đáp ứng.

### 4.2 Giới hạn tác vụ đang chạy

```java
while (dispatched < MESH_TASKS_PER_FRAME && tasksInFlight.get() < config.workerThreads() * 2) {
```

Hai chặn:
- **`MESH_TASKS_PER_FRAME = 4`** — không gửi quá 4 tác vụ mỗi khung hình.
- **`tasksInFlight < workerThreads × 2`** — hàng đợi executor không phình vô hạn.

Hệ số `× 2` cho mỗi worker một tác vụ đang chạy + một tác vụ chờ ⇒ worker không bao giờ rảnh rỗi, nhưng cũng không tích luỹ hàng trăm tác vụ lỗi thời.

`AtomicInteger` đếm không khoá:

```java
tasksInFlight.incrementAndGet();
try { ... } finally { tasksInFlight.decrementAndGet(); }
```

`finally` đảm bảo bộ đếm không rò rỉ khi tác vụ ném ngoại lệ.

---

## 5. Command pattern cho thao tác GPU

```java
public void onChunkUnloaded(final Chunk chunk) {
    scheduler.forget(chunk);
    commands.submit(new RenderCommand() {
        public void execute() {
            ChunkMeshSet set = meshSets.remove(chunk);
            if (set != null) set.dispose();
            if (collisionSink != null) collisionSink.removeChunk(chunk);
        }
    });
}
```

`onChunkUnloaded` có thể được gọi từ **bất kỳ luồng nào**, nhưng `mesh.dispose()` phải chạy trên luồng có OpenGL context.

**Command pattern** đóng gói thao tác thành object, xếp hàng, luồng vẽ `drain()` thực thi. Đây là mẫu chuẩn để chuyển việc giữa các luồng có ràng buộc context.

---

## 6. Bỏ qua nạp lại khi không đổi

```java
if (set.isUnchanged(section, data.solidKey(section), data.translucentKey(section))) continue;
```

So sánh **khoá nội dung** (content key — thường là hash hoặc kích thước) của section mới với section đang dùng. Giống nhau ⇒ **bỏ qua hoàn toàn**: không tạo `Mesh` mới, không nạp GPU, không dựng lại BVH va chạm.

Tình huống thường gặp: người chơi đặt một khối ở `y = 5` ⇒ chỉ section 0 đổi, **7 section còn lại không đổi** ⇒ tiết kiệm 7/8 công việc nạp.

---

## 7. Chia hai pass vẽ

```java
private final RenderableProvider solidPass       = new Pass(false);
private final RenderableProvider translucentPass = new Pass(true);
```

Trong `VoxelEngine.drawWorld`:

```java
batch.begin(camera);
batch.render(renderer.solidPass(), environment);      // 1. ĐẶC — ghi depth
batch.end();

Gdx.gl.glDepthMask(false);                             // 2. tắt ghi depth
batch.begin(camera);
batch.render(renderer.translucentPass(), environment); // 3. TRONG SUỐT
batch.end();
Gdx.gl.glDepthMask(true);
```

### Vì sao thứ tự này bắt buộc?

Alpha blending **không giao hoán**: kết quả phụ thuộc thứ tự vẽ.

| Bước | Lý do |
|---|---|
| Đặc trước | Lấp đầy depth buffer ⇒ nước phía sau tường bị loại bởi depth test |
| Depth write TẮT cho trong suốt | Hai lớp nước chồng nhau đều phải hiển thị; nếu ghi depth, lớp vẽ trước sẽ chặn lớp sau |
| Depth **test** vẫn BẬT | Nước sau tường vẫn bị che đúng |

**Hạn chế đã biết:** hai mặt nước cùng nhìn thấy nhưng vẽ sai thứ tự sẽ blend sai. Giải pháp đúng là sắp xếp theo độ sâu — nhưng với voxel thì chi phí quá cao và sai lệch hầu như không thấy được.

---

## 8. `ConcurrentHashMap` cho `meshSets`

```java
private final Map<Chunk, ChunkMeshSet> meshSets = new ConcurrentHashMap<Chunk, ChunkMeshSet>();
```

Luồng vẽ **duyệt** map trong `getRenderables` trong khi `UploadCommand` có thể **thêm** phần tử.

`ConcurrentHashMap` cho **weakly consistent iterator**: duyệt không ném `ConcurrentModificationException`, và phần tử thêm vào giữa chừng có thể xuất hiện hoặc không — chấp nhận được (chunk mới sẽ hiện ở khung hình sau).

Với `HashMap` thường, tình huống này ném exception và crash game.

---

## 9. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `priorityOf` | `O(1)` |
| `update` | `O(4 × log n)` gửi tác vụ + `O(3 × 3ms)` nạp GPU |
| `getRenderables` | `O(số section)` × `O(48)` frustum test |
| `UploadCommand.execute` | `O(số section đổi)` × chi phí tạo `Mesh` |
| Dựng mesh (worker) | `O(size² · height)` — xem [ChunkMesher](ChunkMesher.md) |

Với 625 chunk × 8 section = **5 000 section**, mỗi khung hình duyệt hết để culling: `5 000 × 48 ≈ 240 000` phép — khoảng 0.3 ms. Chấp nhận được, và có thể tối ưu thêm bằng octree nếu cần.

---

## 10. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `UPLOAD_BUDGET_PER_FRAME` | 3 | ~9 ms/khung hình, 180 chunk/s |
| `MESH_TASKS_PER_FRAME` | 4 | Chặn số tác vụ gửi đi |
| `tasksInFlight` tối đa | `workerThreads × 2` | Worker luôn có việc, không tích luỹ |

---

## 11. Chủ đề DSA / Đồ hoạ thể hiện

- **Frustum culling** — 6 mặt phẳng, kiểm tra AABB.
- **Bỏ `sqrt`** khi chỉ cần so sánh (hàm đơn điệu).
- **Pipeline producer–consumer đa luồng**.
- **Command pattern** để chuyển việc qua ranh giới luồng.
- **`ThreadLocal`** cho object có trạng thái.
- **Điều tiết tải theo ngân sách thời gian** (frame budget).
- **`AtomicInteger`** đếm không khoá.
- **`ConcurrentHashMap`** & weakly consistent iterator.
- **Bỏ qua công việc trùng** bằng khoá nội dung.
- **Thứ tự vẽ & alpha blending không giao hoán**.

---

## 12. Liên kết

- Lập lịch: [ChunkScheduler.md](../05-world/ChunkScheduler.md), [ChunkHeap.md](../06-datastructures/ChunkHeap.md)
- Dựng mesh: [ChunkMesher.md](ChunkMesher.md)
- Va chạm: [PhysicsWorld.md](../08-physics/PhysicsWorld.md)
