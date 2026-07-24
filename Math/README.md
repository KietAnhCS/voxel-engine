# Math — Toàn bộ thuật toán & công thức toán của Voxel Engine

Tài liệu này quét **toàn bộ mã nguồn** của project và tổng hợp mọi thuật toán, công thức toán học, cấu trúc dữ liệu và design pattern được dùng. Mỗi file mã nguồn có nội dung toán học đáng kể đều có một trang `.md` riêng, phân tích:

- **Bài toán** cần giải và vì sao cách ngây thơ không đủ
- **Công thức** đầy đủ, kèm chứng minh hoặc suy dẫn
- **Bảng giá trị** cụ thể với các hằng số thật trong code
- **Độ phức tạp** thời gian & bộ nhớ
- **Chủ đề DSA** mà đoạn code đó thể hiện

**50 tài liệu**, chia theo 10 nhóm.

---

## 📑 Mục lục

### 1. Nhiễu & sinh địa hình — [`01-noise-terrain/`](01-noise-terrain/)

| Tài liệu | File nguồn | Nội dung chính |
|---|---|---|
| [SimplexNoise](01-noise-terrain/SimplexNoise.md) | `engine/generation/SimplexNoise.java` | Lưới tam giác, hằng số skew `F=(√(n+1)−1)/n`, gradient 12 hướng, hàm suy giảm `(r²−d²)⁴`, fBm |
| [TerrainNoise](01-noise-terrain/TerrainNoise.md) | `game/terrain/TerrainNoise.java` | 14 trường nhiễu, seed nguyên tố, ridge `1−\|fBm\|`, nhiễu bất đẳng hướng |
| [BiomeSource](01-noise-terrain/BiomeSource.md) | `terrain/biome/BiomeSource.java` | Cây quyết định 4 tham số, kernel blend 3×3, smoothstep khoét sông hồ |
| [Biome](01-noise-terrain/Biome.md) | `terrain/biome/Biome.java` | Template Method, Chain of Responsibility, xác suất tích luỹ |
| [Biome-heights](01-noise-terrain/Biome-heights.md) | 12 file `*Biome.java` | 12 công thức độ cao, phân tích `R²` vs `R³`, hàm `min(0,·)` |
| [Deterministic](01-noise-terrain/Deterministic.md) | `terrain/Deterministic.java` | Fibonacci hashing, `fmix64` MurmurHash3, hiệu ứng tuyết lở |
| [TerrainShaper](01-noise-terrain/TerrainShaper.md) | `terrain/TerrainShaper.java` + 5 stage | Facade, Chain of Responsibility, Composite, Null Object |

### 2. Hang động, công trình — [`03-caves/`](03-caves/)

| Tài liệu | File nguồn | Nội dung chính |
|---|---|---|
| [PerlinWormCarver](03-caves/PerlinWormCarver.md) | `terrain/carve/PerlinWormCarver.java` | Đệ quy có chặn, random walk tương quan, bộ lọc IIR `τ=12`, toạ độ cầu |
| [SpaghettiCaveCarver](03-caves/SpaghettiCaveCarver.md) | `terrain/carve/SpaghettiCaveCarver.java` | **Giao hai mặt zero** `dim = 3−1−1 = 1`, xấp xỉ Taylor |
| [CheeseCaveCarver](03-caves/CheeseCaveCarver.md) | `terrain/carve/CheeseCaveCarver.java` | Phân ngưỡng trường vô hướng, z-score, fade-out tuyến tính |
| [RavineCarver](03-caves/RavineCarver.md) | `terrain/carve/RavineCarver.java` | Ellipsoid, hàm bao sin, tách seed bằng salt |
| [CarveScope](03-caves/CarveScope.md) | `terrain/carve/CarveScope.java` | Voxel hoá ellipsoid, memoization (nhanh 39×), cắt tỉa 2D |
| [SurfaceGuard](03-caves/SurfaceGuard.md) | `terrain/carve/SurfaceGuard.java` | Hàm bậc thang, vì sao nhiễu hơn random độc lập |
| [CaveSeeds](03-caves/CaveSeeds.md) | `terrain/carve/CaveSeeds.java` | Tái tạo tất định thay chia sẻ trạng thái liên luồng |
| [StructureCarver](03-caves/StructureCarver.md) | `terrain/structure/*.java` | Stratified sampling, `floorDiv`, tổng bình phương số lẻ, voxel hoá vòng tròn |

### 3. Trang trí — [`04-decor/`](04-decor/)

| Tài liệu | File nguồn | Nội dung chính |
|---|---|---|
| [DecorationContext](04-decor/DecorationContext.md) | `terrain/decor/DecorationContext.java` | **Nội suy song tuyến tính**, ease Hermite `3t²−2t³`, value noise |
| [ScatterDecorator](04-decor/ScatterDecorator.md) | `terrain/decor/ScatterDecorator.java` | Điều biến xác suất, hàm dốc bị kẹp, Builder pattern |
| [TreeShapes](04-decor/TreeShapes.md) | `terrain/decor/{Oak,Birch,Pine,Cactus}Shape.java` | Máy trạng thái váy tầng thông, salt động, voxel hoá nửa cầu |

### 4. Thế giới, ánh sáng, chất lỏng — [`05-world/`](05-world/)

| Tài liệu | File nguồn | Nội dung chính |
|---|---|---|
| [LightEngine](05-world/LightEngine.md) | `engine/world/LightEngine.java` | **BFS trên lưới 3D** `O(V+E)`, đóng gói 2 nibble, copy-on-write |
| [ChunkStorage](05-world/ChunkStorage.md) | `engine/world/ChunkStorage.java` | Làm phẳng 3D→1D bằng bit, `skyFloor`, cập nhật tăng dần |
| [FluidSimulator](05-world/FluidSimulator.md) | `engine/world/FluidSimulator.java` | **Automat tế bào**, chứng minh hội tụ, đóng gói `long` 26/12/26 bit |
| [ChunkScheduler](05-world/ChunkScheduler.md) | `engine/world/ChunkScheduler.java` | Stack + Heap + HashSet, lập lịch ưu tiên tuyệt đối |

### 5. Cấu trúc dữ liệu — [`06-datastructures/`](06-datastructures/)

| Tài liệu | File nguồn | Nội dung chính |
|---|---|---|
| [ChunkHeap](06-datastructures/ChunkHeap.md) | `engine/world/ChunkHeap.java` | **Binary min-heap**, siftUp/siftDown, tối ưu "hole" |
| [MergeSort](06-datastructures/MergeSort.md) | `engine/util/MergeSort.java` | **Chia để trị**, định lý thợ, tính ổn định, lai insertion sort |
| [IntQueue](06-datastructures/IntQueue.md) | `engine/util/IntQueue.java` | **Mảng vòng**, phân tích khấu hao, tránh autoboxing |
| [ChunkKey](06-datastructures/ChunkKey.md) | `engine/util/ChunkKey.java` | Song ánh `int×int → long`, bẫy mở rộng dấu |
| [Direction](06-datastructures/Direction.md) | `engine/util/Direction.java` | Bảng tra 6 mặt, tích có hướng `u×v=n`, xấp xỉ Lambert |

### 6. Dựng lưới & vẽ hình — [`07-render/`](07-render/)

| Tài liệu | File nguồn | Nội dung chính |
|---|---|---|
| [ChunkMesher](07-render/ChunkMesher.md) | `engine/render/ChunkMesher.java` | **Face culling** `O(V)→O(S)`, bố trí bộ đệm hai vùng |
| [FaceLighting](07-render/FaceLighting.md) | `block/geometry/FaceLighting.java` | **Ambient occlusion**, `0.82^(15−L)`, đóng gói `skyShare` vào alpha |
| [Geometries](07-render/Geometries.md) | `block/geometry/{Cube,Cross,Liquid}.java` | Flyweight, `ThreadLocal`, spatial hashing, chống z-fighting |
| [Shaders](07-render/Shaders.md) | `assets/data/shaders/*` | Sương mù `e^(−k²z²)`, vibrance BT.601, elip gió, alpha test |
| [WorldRenderer](07-render/WorldRenderer.md) | `engine/render/WorldRenderer.java` | **Frustum culling**, pipeline đa luồng, ngân sách khung hình |
| [DayNightCycle](07-render/DayNightCycle.md) | `engine/render/DayNightCycle.java` | Chuyển động tròn, hàm dốc kẹp, lerp nhiều đoạn liên tục |
| [SkyRenderer](07-render/SkyRenderer.md) | `engine/render/SkyRenderer.java` | **Billboard cầu**, Gram–Schmidt, góc nhìn, chuẩn `L∞` |
| [CloudLayer](07-render/CloudLayer.md) | `engine/render/CloudLayer.java` | fBm rời rạc 2 octave, dịch hệ toạ độ, làm mờ parabol |
| [RainRenderer](07-render/RainRenderer.md) | `engine/render/RainRenderer.java` | **Lấy mẫu đều trên đĩa** `r=R√u`, billboard trụ, hạt không trạng thái |
| [WeatherSystem](07-render/WeatherSystem.md) | `engine/render/WeatherSystem.java` | Một biến điều khiển 8 hệ thống, suy tầm nhìn `d½=3√(ln2)/k` |
| [WalkCycle](07-render/WalkCycle.md) | `engine/render/WalkCycle.java` | Pha theo **quãng đường**, EMA `τ=0.125`, tần số vô tỉ |
| [HumanoidFigure](07-render/HumanoidFigure.md) | `engine/render/HumanoidFigure.java` | **Liên hợp ma trận** `T·R·T⁻¹`, lệch pha chi, Flyweight |

### 7. Vật lý — [`08-physics/`](08-physics/)

| Tài liệu | File nguồn | Nội dung chính |
|---|---|---|
| [VoxelRaycaster](08-physics/VoxelRaycaster.md) | `physics/VoxelRaycaster.java` | **DDA (Amanatides & Woo)**, `‖d‖₁ ≤ √3`, không phép chia trong vòng lặp |
| [PhysicsWorld](08-physics/PhysicsWorld.md) | `physics/{PhysicsWorld,PlayerBody}.java` | Sweep and Prune, BVH, GJK, bước cố định 1/120, capsule |
| [PlayerController](08-physics/PlayerController.md) | `input/PlayerController.java` | Chuẩn hoá chéo, `arcsin` trích pitch, **khoá gimbal** |
| [MovementStates](08-physics/MovementStates.md) | `physics/state/*.java` | State pattern, `h=v₀²/2g`, vì sao tắt trọng lực dưới nước |

### 8. Trí tuệ nhân tạo — [`09-ai/`](09-ai/)

| Tài liệu | File nguồn | Nội dung chính |
|---|---|---|
| [AStarPathFinder](09-ai/AStarPathFinder.md) | `game/mob/AStarPathFinder.java` | **A\***, heuristic admissible & consistent, lazy deletion, đường lui |
| [MonsterAI](09-ai/MonsterAI.md) | `game/mob/{Monster,*State}.java` | State pattern, **trễ Schmitt**, vụ nổ cầu, xác suất spawn nhiều tầng |
| [MeleeAim](09-ai/MeleeAim.md) | `game/combat/MeleeAim.java` | **Slab method** giao tia–AABB, accumulator, hitbox forgiving |

### 9. Lối chơi — [`10-gameplay/`](10-gameplay/)

| Tài liệu | File nguồn | Nội dung chính |
|---|---|---|
| [PlayerStats](10-gameplay/PlayerStats.md) | `game/play/PlayerStats.java` | Bộ tích luỹ ngưỡng, độc lập FPS, XP `n²+6n`, sát thương rơi |
| [Inventory-Crafting](10-gameplay/Inventory-Crafting.md) | `game/play/{Inventory,Crafting}.java` | Mảng thưa, thuật toán hai lượt, `floorMod`, rút gọn trạng thái |
| [Hud](10-gameplay/Hud.md) | `game/play/{Hud,MinecraftUi}.java` | Hệ đơn vị GUI, pixel-perfect, `slot=value−2i`, batching 1×1 |

### 10. Mạng — [`11-net/`](11-net/)

| Tài liệu | File nguồn | Nội dung chính |
|---|---|---|
| [RemotePlayer](11-net/RemotePlayer.md) | `game/net/RemotePlayer.java` | **Nội suy hàm mũ** `τ=0.1`, cung góc ngắn nhất, `volatile` |

---

## 🎓 Tra cứu theo chủ đề DSA

### Cấu trúc dữ liệu

| Cấu trúc | Nơi dùng | Tài liệu |
|---|---|---|
| **Mảng động / khấu hao** | Hàng đợi BFS ánh sáng | [IntQueue](06-datastructures/IntQueue.md) |
| **Mảng vòng (ring buffer)** | Hàng đợi BFS | [IntQueue](06-datastructures/IntQueue.md) |
| **Ngăn xếp (LIFO)** | Việc dựng mesh gấp | [ChunkScheduler](05-world/ChunkScheduler.md) |
| **Hàng đợi (FIFO)** | Tương tác chuột | [PlayerController](08-physics/PlayerController.md) |
| **Binary heap / hàng đợi ưu tiên** | Thứ tự nạp chunk, A* | [ChunkHeap](06-datastructures/ChunkHeap.md), [AStarPathFinder](09-ai/AStarPathFinder.md) |
| **Bảng băm (HashMap/HashSet)** | Chống trùng, `closed` set của A* | [ChunkScheduler](05-world/ChunkScheduler.md), [AStarPathFinder](09-ai/AStarPathFinder.md) |
| **Mảng thưa** | Túi đồ 36 ô | [Inventory-Crafting](10-gameplay/Inventory-Crafting.md) |
| **Mảng song song** | Heap, MergeSort, vật lý | [ChunkHeap](06-datastructures/ChunkHeap.md), [MergeSort](06-datastructures/MergeSort.md) |
| **BVH (cây hộp bao)** | Va chạm địa hình | [PhysicsWorld](08-physics/PhysicsWorld.md) |
| **Đồ thị ẩn** | Lưới ánh sáng, lưới đi bộ | [LightEngine](05-world/LightEngine.md), [AStarPathFinder](09-ai/AStarPathFinder.md) |

### Thuật toán

| Thuật toán | Độ phức tạp | Tài liệu |
|---|---|---|
| **BFS** (lan ánh sáng) | `O(V+E)` | [LightEngine](05-world/LightEngine.md) |
| **A\*** (tìm đường) | `O(E log V)` | [AStarPathFinder](09-ai/AStarPathFinder.md) |
| **Merge sort** | `O(n log n)` ổn định | [MergeSort](06-datastructures/MergeSort.md) |
| **Insertion sort** | `O(n²)`, nhanh khi `n` nhỏ | [MergeSort](06-datastructures/MergeSort.md) |
| **Fisher–Yates shuffle** | `O(n)` | [SimplexNoise](01-noise-terrain/SimplexNoise.md) |
| **DDA / Bresenham 3D** | `O(L)` | [VoxelRaycaster](08-physics/VoxelRaycaster.md) |
| **Slab method** (tia–AABB) | `O(1)` | [MeleeAim](09-ai/MeleeAim.md) |
| **Sweep and Prune** | `O(n+k)` | [PhysicsWorld](08-physics/PhysicsWorld.md) |
| **Automat tế bào** | `O(k)`/vòng | [FluidSimulator](05-world/FluidSimulator.md) |
| **Đệ quy có chặn** | `O(steps·r³)` | [PerlinWormCarver](03-caves/PerlinWormCarver.md) |
| **Frustum culling** | `O(n)` × 48 | [WorldRenderer](07-render/WorldRenderer.md) |
| **Face culling** | `O(V)` → `O(S)` | [ChunkMesher](07-render/ChunkMesher.md) |

### Kỹ thuật tối ưu

| Kỹ thuật | Hiệu quả | Tài liệu |
|---|---|---|
| **Memoization** | nhanh 39× | [CarveScope](03-caves/CarveScope.md) |
| **Bảng tra tính sẵn** | thay `Math.pow` | [FaceLighting](07-render/FaceLighting.md), [Direction](06-datastructures/Direction.md) |
| **Gộp công việc (batching)** | nhanh 4000× | [FluidSimulator](05-world/FluidSimulator.md) |
| **Cập nhật tăng dần** | `O(H)` → `O(1)` | [ChunkStorage](05-world/ChunkStorage.md) |
| **Loại bỏ sớm (AABB)** | cắt >99 % | [CarveScope](03-caves/CarveScope.md) |
| **Cắt tỉa vòng lặp** | −45 % | [TerrainShaper](01-noise-terrain/TerrainShaper.md) |
| **Tránh cấp phát / autoboxing** | −7.8 MB rác/lần | [IntQueue](06-datastructures/IntQueue.md) |
| **Bỏ `sqrt` khi chỉ so sánh** | −15 chu kỳ | [WorldRenderer](07-render/WorldRenderer.md) |
| **Bit thay chia/modulo** | `&` thay `%` | [ChunkStorage](05-world/ChunkStorage.md) |
| **Điều tiết theo ngân sách** | giữ 60 FPS | [WorldRenderer](07-render/WorldRenderer.md) |

### Đóng gói bit

| Sơ đồ | Bit | Tài liệu |
|---|---|---|
| `(x, z) → long` | 32+32 | [ChunkKey](06-datastructures/ChunkKey.md) |
| `(x, y, z) → int` (trong chunk) | 4+7+4 | [ChunkStorage](05-world/ChunkStorage.md) |
| `(x, y, z) → long` (thế giới) | 26+12+26 | [FluidSimulator](05-world/FluidSimulator.md) |
| `(x, y, z) → long` (A*) | 20+20+20 | [AStarPathFinder](09-ai/AStarPathFinder.md) |
| `(sky, block) → byte` | 4+4 | [LightEngine](05-world/LightEngine.md) |
| `(windy, skyShare) → float` | 1+ | [FaceLighting](07-render/FaceLighting.md) |

### Đa luồng

| Kỹ thuật | Tài liệu |
|---|---|
| **Copy-on-write + `volatile`** | [LightEngine](05-world/LightEngine.md), [ChunkStorage](05-world/ChunkStorage.md) |
| **`ThreadLocal`** cho object có trạng thái | [Geometries](07-render/Geometries.md), [WorldRenderer](07-render/WorldRenderer.md) |
| **Hàng đợi lock-free** | [FluidSimulator](05-world/FluidSimulator.md) |
| **Tái tạo tất định** thay chia sẻ | [CaveSeeds](03-caves/CaveSeeds.md), [StructureCarver](03-caves/StructureCarver.md) |
| **Command pattern** qua ranh giới luồng | [WorldRenderer](07-render/WorldRenderer.md) |
| **`AtomicInteger`** | [WorldRenderer](07-render/WorldRenderer.md) |
| **`synchronized`** khi cần nguyên tử đa cấu trúc | [ChunkScheduler](05-world/ChunkScheduler.md) |

---

## 🧩 Design Pattern trong project

| Pattern | Nơi dùng | Tài liệu |
|---|---|---|
| **Strategy** | `Biome.surfaceHeight`, `BlockGeometry`, `TreeShape` | [Biome](01-noise-terrain/Biome.md), [Geometries](07-render/Geometries.md), [TreeShapes](04-decor/TreeShapes.md) |
| **State** | Di chuyển người chơi, AI quái vật | [MovementStates](08-physics/MovementStates.md), [MonsterAI](09-ai/MonsterAI.md) |
| **Chain of Responsibility** | `TerrainStage`, chuỗi `Decorator` | [TerrainShaper](01-noise-terrain/TerrainShaper.md), [Biome](01-noise-terrain/Biome.md) |
| **Composite** | `CarverPipeline` | [TerrainShaper](01-noise-terrain/TerrainShaper.md) |
| **Facade** | `TerrainShaper` | [TerrainShaper](01-noise-terrain/TerrainShaper.md) |
| **Template Method** | `Biome.decorate` | [Biome](01-noise-terrain/Biome.md) |
| **Flyweight** | Geometry singleton, `HumanoidFigure` | [Geometries](07-render/Geometries.md), [HumanoidFigure](07-render/HumanoidFigure.md) |
| **Factory Method** | `SurfaceGuard.withEntrances/sealed` | [SurfaceGuard](03-caves/SurfaceGuard.md) |
| **Null Object** | `SkyStage`, `guard == null` | [TerrainShaper](01-noise-terrain/TerrainShaper.md), [SurfaceGuard](03-caves/SurfaceGuard.md) |
| **Builder** | `ScatterDecorator.inPatches` | [ScatterDecorator](04-decor/ScatterDecorator.md) |
| **Command** | `RenderCommandQueue` | [WorldRenderer](07-render/WorldRenderer.md) |
| **Object pool** | `ColumnSample`, `DecorationContext`, bộ đệm mesh | [TerrainShaper](01-noise-terrain/TerrainShaper.md), [ChunkMesher](07-render/ChunkMesher.md) |

---

## 📐 Bảng công thức nhanh

| Công thức | Ý nghĩa | Tài liệu |
|---|---|---|
| `F_n = (√(n+1)−1)/n` | Hằng skew Simplex | [SimplexNoise](01-noise-terrain/SimplexNoise.md) |
| `ridge = 1 − \|fBm\|` | Sống núi sắc | [TerrainNoise](01-noise-terrain/TerrainNoise.md) |
| `S(t) = 3t² − 2t³` | Smoothstep (Hermite) | [BiomeSource](01-noise-terrain/BiomeSource.md), [DecorationContext](04-decor/DecorationContext.md) |
| `dim = 3−1−1 = 1` | Giao hai mặt zero → đường cong | [SpaghettiCaveCarver](03-caves/SpaghettiCaveCarver.md) |
| `φ(k+1) = 0.92φ(k) + …` | Bộ lọc IIR, `τ = −1/ln(0.92) ≈ 12` | [PerlinWormCarver](03-caves/PerlinWormCarver.md) |
| `Σ(2k+1)² = (n+1)(2n+1)(2n+3)/3` | Số khối kim tự tháp | [StructureCarver](03-caves/StructureCarver.md) |
| `d² ≤ r² + r` | Voxel hoá vòng tròn bằng số nguyên | [StructureCarver](03-caves/StructureCarver.md) |
| `P = Σ c_ij · w_ij` | Nội suy song tuyến tính | [DecorationContext](04-decor/DecorationContext.md) |
| `L' = max(L, L_hàng xóm − att)` | Relaxation ánh sáng | [LightEngine](05-world/LightEngine.md) |
| `L' = max(sides) − 1` | Mức nước | [FluidSimulator](05-world/FluidSimulator.md) |
| `idx = (y<<8)\|(x<<4)\|z` | Làm phẳng 3D → 1D | [ChunkStorage](05-world/ChunkStorage.md) |
| `B(L) = 0.82^(15−L)` | Đường cong độ sáng | [FaceLighting](07-render/FaceLighting.md) |
| `occ = 3 − (A+B+C)` | Ambient occlusion | [FaceLighting](07-render/FaceLighting.md) |
| `F(z) = e^(−k²z²)` | Sương mù EXP2 | [Shaders](07-render/Shaders.md) |
| `Y = 0.299R + 0.587G + 0.114B` | Độ chói BT.601 | [Shaders](07-render/Shaders.md) |
| `ŝ = (cos 2πt, sin 2πt, 0)` | Vị trí mặt trời | [DayNightCycle](07-render/DayNightCycle.md) |
| `α = 2·arctan(s/d)` | Góc nhìn không đổi | [SkyRenderer](07-render/SkyRenderer.md) |
| `r = R√u` | Lấy mẫu đều trên đĩa | [RainRenderer](07-render/RainRenderer.md) |
| `Δ = 1/\|d\|`, `t += Δ` | DDA duyệt voxel | [VoxelRaycaster](08-physics/VoxelRaycaster.md) |
| `h = v₀²/2g` | Độ cao nhảy | [MovementStates](08-physics/MovementStates.md) |
| `pitch = arcsin(d_y)` | Trích góc từ vector đơn vị | [PlayerController](08-physics/PlayerController.md) |
| `f = g + h` | A* | [AStarPathFinder](09-ai/AStarPathFinder.md) |
| `h = \|Δx\|+\|Δy\|+\|Δz\|` | Heuristic Manhattan | [AStarPathFinder](09-ai/AStarPathFinder.md) |
| `XP(n) = n² + 6n` | Tổng kinh nghiệm tới cấp `n` | [PlayerStats](10-gameplay/PlayerStats.md) |
| `dmg = ⌊h_rơi − 3⌋` | Sát thương rơi | [PlayerStats](10-gameplay/PlayerStats.md) |
| `p ← p + (t−p)·min(1,kΔt)` | Nội suy hàm mũ | [RemotePlayer](11-net/RemotePlayer.md) |
| `M = T(h)·R(θ)·T(−h)` | Quay quanh khớp | [HumanoidFigure](07-render/HumanoidFigure.md) |

---

## 📁 Về phạm vi tài liệu

Tài liệu bao phủ **mọi file có nội dung toán học hoặc thuật toán**. Các file sau **không có** trang riêng vì chúng là dữ liệu thuần hoặc khai báo:

- **Interface & DTO**: `Command`, `Attackable`, `BlockView`, `Decorator`, `TreeShape`, `Edit`, `Hurt`, `PlayerState`, `WorldSnapshot`, `AuthDtos`, `WorldDtos`
- **Cấu hình & registry**: `EngineConfig`, `GameSettings`, `BlockRegistry`, `Blocks`, `Block`, `ItemStack`
- **Backend Spring Boot**: `AuthService`, `JwtService`, `WorldSocketHandler`, các `Controller`/`Repository` (logic CRUD & JWT chuẩn, không có thuật toán riêng)
- **Giao diện**: `LoginScreen`, `SettingsScreen`, `InventoryScreen`, `CommandConsole` (bố cục UI, phần toán chung đã nằm ở [Hud](10-gameplay/Hud.md))
- **Vỏ bọc mỏng**: `PlayerModel`, `ViewMode`, `MeshBuffer`, `ChunkGeometryData`, `RenderCommandQueue`, `WorldEventBus`

Ba lớp biome subclass và bốn lớp tree shape được gom vào [Biome-heights](01-noise-terrain/Biome-heights.md) và [TreeShapes](04-decor/TreeShapes.md) vì mỗi file chỉ chứa một công thức ngắn — đặt cạnh nhau trong bảng so sánh dễ hiểu hơn 16 trang rời rạc.

---

## 🔍 Cách đọc

**Nếu bạn muốn hiểu toàn bộ pipeline sinh thế giới**, đọc theo thứ tự:
[SimplexNoise](01-noise-terrain/SimplexNoise.md) → [TerrainNoise](01-noise-terrain/TerrainNoise.md) → [BiomeSource](01-noise-terrain/BiomeSource.md) → [TerrainShaper](01-noise-terrain/TerrainShaper.md) → [PerlinWormCarver](03-caves/PerlinWormCarver.md) → [ScatterDecorator](04-decor/ScatterDecorator.md)

**Nếu bạn muốn hiểu pipeline vẽ hình**:
[ChunkStorage](05-world/ChunkStorage.md) → [LightEngine](05-world/LightEngine.md) → [ChunkMesher](07-render/ChunkMesher.md) → [FaceLighting](07-render/FaceLighting.md) → [Shaders](07-render/Shaders.md) → [WorldRenderer](07-render/WorldRenderer.md)

**Nếu bạn ôn thi DSA**, ba tài liệu đặc nhất:
[AStarPathFinder](09-ai/AStarPathFinder.md) · [LightEngine](05-world/LightEngine.md) · [ChunkHeap](06-datastructures/ChunkHeap.md)
