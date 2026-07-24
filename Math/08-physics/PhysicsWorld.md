# PhysicsWorld & PlayerBody — va chạm bằng Bullet

**Files:**
- `core/src/com/voxel/engine/physics/PhysicsWorld.java`
- `core/src/com/voxel/engine/physics/PlayerBody.java`

Dùng thư viện **Bullet Physics** (qua libGDX binding) thay vì tự viết va chạm AABB.

---

## 1. Kiến trúc Bullet — 4 thành phần

```java
collisionConfiguration = new btDefaultCollisionConfiguration();
dispatcher             = new btCollisionDispatcher(collisionConfiguration);
broadphase             = new btAxisSweep3(min, max);
solver                 = new btSequentialImpulseConstraintSolver();
dynamicsWorld          = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
```

| Thành phần | Vai trò | Thuật toán |
|---|---|---|
| **Broadphase** | Lọc nhanh các cặp *có thể* va chạm | Sweep and Prune |
| **Dispatcher** | Kiểm tra chính xác từng cặp | GJK / EPA |
| **Solver** | Giải hệ ràng buộc, tính lực đẩy | Sequential Impulse |
| **DynamicsWorld** | Điều phối, tích phân chuyển động | — |

### 1.1 Broadphase — Sweep and Prune

```java
broadphase = new btAxisSweep3(
        new Vector3(-4096, -4096, -4096),
        new Vector3( 4096,  4096,  4096));
```

**Bài toán:** với `n` vật thể, kiểm tra mọi cặp là `O(n²)`. Với 5 000 section va chạm ⇒ `1.25 × 10⁷` phép mỗi khung hình — không khả thi.

**Sweep and Prune:** chiếu hộp bao của mọi vật lên **từng trục toạ độ**, sắp xếp các điểm đầu/cuối. Hai vật chỉ có thể va chạm nếu khoảng chiếu của chúng **chồng lấn trên cả 3 trục**.

```
  trục X:  [──A──]  [───B───]        A, B không chồng ⟹ loại ngay
                 [──C──]              A, C chồng ⟹ kiểm tra tiếp trục Y, Z
```

Vì vật thể **di chuyển ít giữa hai khung hình**, danh sách đã sắp gần như không đổi ⇒ dùng insertion sort để cập nhật, chi phí khấu hao gần `O(n)`.

```
  O(n²)  →  O(n log n) lần đầu, O(n + k) các lần sau
```

với `k` = số cặp thực sự chồng lấn.

`btAxisSweep3` yêu cầu **giới hạn thế giới cố định** (`WORLD_EXTENT = 4096`) vì nó dùng chỉ số 16-bit cho các điểm đầu/cuối. Thế giới `8192 × 8192 × 8192` khối — thừa đủ.

### 1.2 Narrowphase — GJK

Thuật toán **Gilbert–Johnson–Keerthi** xác định hai khối lồi có giao nhau không, bằng cách tìm xem **hiệu Minkowski** của chúng có chứa gốc toạ độ không:

```
  A ∩ B ≠ ∅   ⟺   0 ∈ (A ⊖ B) = { a − b : a ∈ A, b ∈ B }
```

GJK dựng dần một **đơn hình** (simplex — tam giác/tứ diện) bao lấy gốc, hội tụ trong vài vòng lặp. Độ phức tạp thực nghiệm `O(1)` cho khối lồi đơn giản.

### 1.3 Solver — Sequential Impulse

Giải hệ ràng buộc va chạm bằng **phương pháp lặp Gauss–Seidel**: xử lý từng điểm tiếp xúc một, áp xung lực, lặp lại vài vòng cho tới khi hội tụ. Nhanh hơn giải hệ tuyến tính đầy đủ, đủ chính xác cho game.

---

## 2. Trọng lực

```java
dynamicsWorld.setGravity(new Vector3(0f, -26f, 0f));
```

```
  g = 26 khối/s²
```

**So sánh với thực tế:** `g_Trái Đất = 9.81 m/s²`. Nếu 1 khối = 1 m thì game có trọng lực **gấp 2.65 lần**.

### Vì sao?

Trọng lực thật khiến nhân vật **rơi chậm và bay xa** — cảm giác "trôi trên mặt trăng". Game platformer luôn tăng trọng lực để chuyển động **dứt khoát**.

Kiểm chứng bằng độ cao nhảy: với xung lực `v₀`, độ cao tối đa

```
        v₀²
  h =  ─────
        2g
```

Muốn nhảy cao 1.25 khối (đủ lên 1 khối) với `g = 26`:

```
  v₀ = √(2 × 26 × 1.25) = √65 ≈ 8.06 khối/s
```

Thời gian lên đỉnh: `t = v₀/g = 8.06/26 ≈ 0.31` s. Tổng thời gian nhảy ~0.62 s — nhanh gọn, đúng nhịp Minecraft.

Với `g = 9.81`, cùng độ cao cần `v₀ = 4.95` và thời gian nhảy **1.01 s** — lê thê.

---

## 3. Tích phân chuyển động — bước cố định

```java
public void step(float delta) {
    dynamicsWorld.stepSimulation(delta, 4, 1f / 120f);
}
```

| Tham số | Giá trị | Ý nghĩa |
|---|---|---|
| `timeStep` | `delta` | Thời gian thực trôi qua |
| `maxSubSteps` | **4** | Tối đa 4 bước con mỗi lần gọi |
| `fixedTimeStep` | **1/120 s** | Mỗi bước con dài 8.33 ms |

### Vì sao cần bước cố định?

Tích phân số (Euler, Verlet) **phụ thuộc bước thời gian**. Nếu `delta` thay đổi theo FPS:

- 30 FPS: `delta = 33` ms ⇒ vật rơi 1 bước lớn ⇒ có thể **xuyên qua** tường mỏng (tunneling).
- 144 FPS: `delta = 7` ms ⇒ kết quả khác hẳn.

⇒ **Vật lý không tất định**, người chơi FPS cao nhảy khác người chơi FPS thấp.

Bước cố định `1/120` s làm mọi máy chạy **cùng một chuỗi tính toán**, chỉ khác số bước con mỗi khung hình:

| FPS | `delta` | Số bước con |
|---|---|---|
| 30 | 33.3 ms | 4 |
| 60 | 16.7 ms | 2 |
| 120 | 8.3 ms | 1 |
| 144 | 6.9 ms | 0–1 |

Phần dư được **tích luỹ** (accumulator) cho lần gọi sau.

### `maxSubSteps = 4` — chống spiral of death

Nếu máy quá chậm (`delta = 500` ms) thì cần 60 bước con ⇒ tốn nhiều thời gian hơn ⇒ `delta` khung sau còn lớn hơn ⇒ **vòng xoáy tử thần**.

Chặn ở 4 bước = `4/120 = 33.3` ms mô phỏng mỗi lần gọi. Khi máy chậm hơn thế, thời gian trong game **trôi chậm lại** (slow motion) thay vì treo hẳn.

---

## 4. Hình dạng va chạm — BVH triangle mesh

```java
btBvhTriangleMeshShape shape = new btBvhTriangleMeshShape(parts);
btRigidBody body = new btRigidBody(0f, null, shape, Vector3.Zero);
```

### Khối lượng 0 = vật thể tĩnh

```java
new btRigidBody(0f, ...)
```

Trong Bullet, `mass = 0` nghĩa là **khối lượng vô hạn** ⇒ vật không bao giờ di chuyển, không chịu trọng lực, không cần tích phân. Đúng cho địa hình.

### BVH — Bounding Volume Hierarchy

Một section chunk có hàng nghìn tam giác. Kiểm tra tia/hộp với từng tam giác là `O(n)`.

**BVH** là cây nhị phân các hộp bao:

```
                [hộp bao toàn bộ]
                 ╱             ╲
        [hộp trái]             [hộp phải]
         ╱      ╲               ╱      ╲
    [tam giác] [tg]        [tg]      [tg]
```

Truy vấn: nếu tia không cắt hộp cha ⇒ **loại cả cây con**.

```
  O(n)  →  O(log n)
```

**Chi phí dựng cây:** `O(n log n)` — đây chính là "~3 ms" mà [WorldRenderer](../07-render/WorldRenderer.md) nhắc tới trong `UPLOAD_BUDGET_PER_FRAME`.

### Chỉ dùng phần va chạm của mesh

```java
parts.add(new MeshPart("collision", mesh, 0,
        Math.min(indexCount, mesh.getNumIndices()), GL20.GL_TRIANGLES));
```

`indexCount` chính là `collisionIndexCount` mà [ChunkMesher](../07-render/ChunkMesher.md) ghi lại — ranh giới giữa khối cản và cỏ/hoa.

```
  mesh:  [───── khối cản ─────][── cỏ, hoa ──]
         0            indexCount        numIndices
```

⇒ **Dùng chung một mesh cho cả vẽ lẫn va chạm**, người chơi đi xuyên qua cỏ. Tiết kiệm hoàn toàn một bộ hình học riêng cho vật lý.

### Ma trận thế giới

```java
body.setWorldTransform(new Matrix4().setToTranslation(chunk.originX(), 0f, chunk.originZ()));
```

Mesh được dựng theo **toạ độ địa phương của chunk** (`0..15`). Ma trận dịch đưa nó về đúng vị trí thế giới — không phải cộng offset vào từng đỉnh.

`friction = 0.4` — ma sát vừa phải, người chơi không trượt như trên băng.

---

## 5. Nhóm lọc va chạm (collision filtering)

```java
dynamicsWorld.addRigidBody(body,
        (short) StaticFilter,                              // nhóm CỦA NÓ
        (short) (CharacterFilter | DefaultFilter));        // nhóm nó VA CHẠM VỚI
```

Bullet dùng **mặt nạ bit**: hai vật va chạm khi

```
  (groupA & maskB) ≠ 0   ∧   (groupB & maskA) ≠ 0
```

| Vật | Group | Mask |
|---|---|---|
| Địa hình | `StaticFilter` | `CharacterFilter \| DefaultFilter` |
| Người chơi | `CharacterFilter` | `StaticFilter \| DefaultFilter` |

Kiểm tra: `StaticFilter & (StaticFilter\|Default) ≠ 0` ✔ và `CharacterFilter & (Character\|Default) ≠ 0` ✔ ⇒ người chơi va chạm địa hình.

**Địa hình vs địa hình:** `StaticFilter & (Character|Default) = 0` ✗ ⇒ hai section **không bao giờ** kiểm tra với nhau. Với 5 000 section, đây là tối ưu khổng lồ — loại bỏ `5000²/2 = 1.25 × 10⁷` cặp ngay ở broadphase.

---

## 6. PlayerBody — kinematic character controller

```java
this.shape = new btCapsuleShape(0.35f, 1.05f);
this.ghost = new btPairCachingGhostObject();
this.controller = new btKinematicCharacterController(ghost, shape, 0.55f);
```

### Hình con nhộng (capsule)

```
  radius = 0.35,  height = 1.05  (phần trụ)

  Tổng chiều cao = 1.05 + 2 × 0.35 = 1.75 khối     ✔ khớp HumanoidFigure.HEIGHT
  Đường kính     = 0.70 khối
```

```
        ╭───╮      ← chỏm cầu bán kính 0.35
        │   │
        │   │  1.05  ← phần trụ
        │   │
        ╰───╯      ← chỏm cầu bán kính 0.35
```

### Vì sao capsule chứ không phải hộp?

| | Hộp (AABB) | Capsule |
|---|---|---|
| Đi qua góc tường | **kẹt** ở cạnh sắc | **trượt** qua mượt |
| Leo bậc thang | giật cục | mượt |
| Chi phí GJK | thấp | **thấp hơn** (chỉ 1 đoạn thẳng + bán kính) |
| Xoay | phải cập nhật AABB | **bất biến** khi xoay quanh trục dọc |

Bề mặt cong của capsule khiến vật thể **tự trượt** khỏi cạnh thay vì mắc kẹt — lý do mọi game FPS đều dùng capsule cho nhân vật.

### `stepHeight = 0.55`

Người chơi **tự động bước lên** bậc cao tới `0.55` khối mà không cần nhảy.

Chọn `0.55` (hơn nửa khối) để: leo được bậc **nửa khối** (slab) nhưng **không** tự leo lên khối đầy (1.0) — phải nhảy. Đúng quy tắc Minecraft.

### Ghost object

`btPairCachingGhostObject` **phát hiện** va chạm nhưng **không phản ứng vật lý**. Character controller tự xử lý: đọc danh sách vật chạm, tính vector trượt, dịch chuyển thủ công.

**Vì sao không dùng rigid body?** Rigid body chịu xung lực ⇒ người chơi bị đẩy văng khi chạm tường, bị lật ngửa, trượt theo quán tính. Kinematic controller cho **điều khiển trực tiếp**, đúng cảm giác game FPS.

### `EYE_HEIGHT = 0.72`

Camera đặt cao `0.72` khối **so với tâm capsule**. Tâm ở `1.75/2 = 0.875` từ chân ⇒ mắt ở `0.875 + 0.72 = 1.595` khối từ mặt đất.

Tỉ lệ `1.595 / 1.75 = 91 %` chiều cao — đúng vị trí mắt người.

---

## 7. Quản lý vòng đời — chống rò rỉ bộ nhớ native

```java
private void release(btDiscreteDynamicsWorld world, int section) {
    if (bodies[section] != null) {
        world.removeRigidBody(bodies[section]);
        bodies[section].dispose();
        bodies[section] = null;
    }
    if (shapes[section] != null) {
        shapes[section].dispose();
        shapes[section] = null;
    }
}
```

Bullet là thư viện **C++ native**. Object Java chỉ là vỏ bọc trỏ tới bộ nhớ ngoài heap của JVM ⇒ **GC không thu hồi được**.

**Bắt buộc:**
1. `removeRigidBody` trước — gỡ khỏi thế giới, nếu không Bullet vẫn giữ con trỏ tới vùng nhớ đã giải phóng ⇒ **crash JVM**.
2. `dispose()` sau — giải phóng bộ nhớ native.
3. Gán `null` — cho GC thu hồi phần vỏ Java.

**Thứ tự quan trọng.** Đảo ngược sẽ gây segfault.

`SectionBodies` giữ mảng song song `bodies[]` và `shapes[]` theo section — cùng kỹ thuật mảng song song với [ChunkHeap](../06-datastructures/ChunkHeap.md).

---

## 8. Bảng hằng số

| Hằng | Giá trị | Ý nghĩa |
|---|---|---|
| `WORLD_EXTENT` | 4096 | Nửa cạnh thế giới vật lý |
| Trọng lực | −26 khối/s² | Gấp 2.65 lần Trái Đất |
| `fixedTimeStep` | 1/120 s | Bước tích phân cố định |
| `maxSubSteps` | 4 | Chống spiral of death |
| Ma sát địa hình | 0.4 | |
| Capsule radius | 0.35 | Đường kính 0.7 khối |
| Capsule height | 1.05 | Tổng cao 1.75 khối |
| `stepHeight` | 0.55 | Tự leo bậc nửa khối |
| `EYE_HEIGHT` | 0.72 | Mắt ở 91 % chiều cao |

---

## 9. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| Broadphase (SAP) | `O(n + k)` khấu hao |
| Narrowphase (GJK) | `O(1)` mỗi cặp cho khối lồi |
| Dựng BVH | `O(t log t)`, `t` = số tam giác — **~3 ms** |
| Truy vấn BVH | `O(log t)` |
| `step` | `O(subSteps × (n + k + contacts))` |
| Bộ nhớ | native, ngoài heap JVM |

---

## 10. Chủ đề DSA / Toán thể hiện

- **Sweep and Prune** — giảm `O(n²)` xuống `O(n + k)`.
- **BVH** — cây phân cấp hộp bao, truy vấn `O(log n)`.
- **GJK** & hiệu Minkowski.
- **Sequential Impulse** — Gauss–Seidel giải ràng buộc.
- **Tích phân bước cố định** & accumulator — tất định bất kể FPS.
- **Mặt nạ bit** cho lọc va chạm.
- **Hình học capsule** — vì sao mượt hơn hộp.
- **Công thức chuyển động ném** `h = v₀²/2g`.
- **Quản lý tài nguyên native** — thứ tự remove/dispose/null.

---

## 11. Liên kết

- Ranh giới va chạm trong mesh: [ChunkMesher.md](../07-render/ChunkMesher.md)
- Ngân sách nạp GPU: [WorldRenderer.md](../07-render/WorldRenderer.md)
- Chiều cao nhân vật: [HumanoidFigure.md](../07-render/HumanoidFigure.md)
- Ngắm khối: [VoxelRaycaster.md](VoxelRaycaster.md)
