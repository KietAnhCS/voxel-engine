# CaveSeeds — băm toạ độ chunk thành seed

**File:** `core/src/com/voxel/game/terrain/carve/CaveSeeds.java`

---

## 1. Vai trò

```java
public static long forChunk(long worldSeed, int chunkX, int chunkZ, int salt) {
    long hash = worldSeed;
    hash ^= chunkX * 0x9E3779B97F4A7C15L;
    hash ^= chunkZ * 0xC2B2AE3D27D4EB4FL;
    hash ^= salt   * 0x165667B19E3779F9L;
    hash ^= hash >>> 33;
    hash *= 0xFF51AFD7ED558CCDL;
    hash ^= hash >>> 33;
    hash *= 0xC4CEB9FE1A85EC53L;
    hash ^= hash >>> 33;
    return hash;
}
```

Thuật toán **giống hệt** [`Deterministic.unit`](../01-noise-terrain/Deterministic.md) (trộn hằng số vàng + finalizer MurmurHash3 `fmix64`), chỉ khác:

| | `Deterministic.unit` | `CaveSeeds.forChunk` |
|---|---|---|
| Đầu vào | toạ độ **khối** `(x, z)` | toạ độ **chunk** `(cx, cz)` |
| Đầu ra | `float ∈ [0, 1)` | `long` 64-bit đầy đủ |
| Dùng cho | quyết định xác suất trực tiếp | làm **seed** cho `new Random(...)` |

Trả về nguyên `long` vì `Random` cần đủ 64 bit entropy để sinh chuỗi dài (một đường hầm rút hàng trăm số ngẫu nhiên).

---

## 2. Vì sao cần?

### Vấn đề đồng bộ liên chunk

Một đường hầm dài 180 khối trải qua ~11 chunk. Mỗi chunk sinh **độc lập, trên luồng riêng**, không giao tiếp với nhau. Làm sao 11 chunk cùng vẽ ra **đúng một** đường hầm?

### Giải pháp

```java
Random random = new Random(CaveSeeds.forChunk(seed, sourceChunkX, sourceChunkZ, 1));
```

Mọi chunk khi tái tạo con giun của chunk nguồn `(sx, sz)` đều gọi hàm này với **cùng tham số** ⇒ **cùng seed** ⇒ `Random` sinh **cùng chuỗi số** ⇒ **cùng đường đi**.

```
  Chunk A tái tạo giun của (5, 3)  ─┐
  Chunk B tái tạo giun của (5, 3)  ─┼─►  cùng seed  ►  cùng quỹ đạo
  Chunk C tái tạo giun của (5, 3)  ─┘
```

Mỗi chunk chỉ ghi phần rơi vào phạm vi của mình (nhờ [CarveScope](CarveScope.md) kẹp biên) ⇒ ghép lại thành một hầm liền mạch.

**Đây là kỹ thuật "tái tạo tất định" (deterministic regeneration)** — thay vì chia sẻ dữ liệu giữa các luồng (cần khoá, cần đồng bộ), ta *tính lại* từ đầu. Tốn CPU nhưng **không cần khoá, không cần thứ tự, song song hoá hoàn hảo**.

---

## 3. Vai trò của `salt`

| Carver | `salt` |
|---|---|
| `PerlinWormCarver` | 1 |
| `RavineCarver` | 2 |

Hai carver đều gọi `forChunk(seed, cx, cz, ·)` với **cùng** `(seed, cx, cz)`. Nếu không có `salt`, chúng nhận **cùng một seed** ⇒ khe nứt và đường hầm sẽ luôn xuất hiện cùng chỗ, cùng hướng — lộ liễu ngay.

`salt` khác nhau đi qua finalizer `fmix64` cho hai giá trị **độc lập thống kê** hoàn toàn.

---

## 4. Chất lượng băm — vì sao không dùng cách đơn giản

### Cách sai thường gặp

```java
long seed = worldSeed + chunkX * 341873128712L + chunkZ * 132897987541L;   // Minecraft cũ
```

Hoặc tệ hơn:

```java
long seed = worldSeed + chunkX * 31 + chunkZ;    // ✗ RẤT XẤU
```

Cách thứ hai khiến `(cx, cz) = (0, 31)` và `(1, 0)` cho **cùng seed** ⇒ hai chunk khác nhau sinh hệ hang y hệt.

### Cách đúng ở đây

Ba lớp bảo vệ:

1. **Nhân hằng số vàng** — `0x9E3779B97F4A7C15 = ⌊2⁶⁴/φ⌋`, `φ` = tỉ lệ vàng. Phân tán bit tối ưu (Fibonacci hashing).
2. **XOR thay vì cộng** — tránh hiệu ứng nhớ (carry) làm hai đầu vào khác nhau triệt tiêu.
3. **Finalizer `fmix64`** — 3 vòng `xor-shift + multiply` đảm bảo **hiệu ứng tuyết lở**: đổi 1 bit đầu vào ⇒ mỗi bit đầu ra đổi với xác suất ≈ 0.5.

Kết quả: hai chunk cạnh nhau `(5, 3)` và `(5, 4)` cho hai seed **hoàn toàn không liên quan** ⇒ hệ hang của chúng độc lập, không lặp mẫu.

---

## 5. Độ phức tạp

| | |
|---|---|
| Thời gian | `O(1)` — ~12 lệnh CPU |
| Bộ nhớ | `O(1)` — không cấp phát |
| Thread-safe | ✔ pure function, không trạng thái |
| Tất định | ✔ mọi lúc, mọi máy, mọi thứ tự luồng |

Được gọi `(2·6+1)² = 169` lần mỗi chunk (perlin worm) + `121` lần (ravine) = **290 lần/chunk** ≈ 3 500 lệnh CPU — không đáng kể.

---

## 6. Chủ đề DSA thể hiện

- **Hàm băm không gian** & hiệu ứng tuyết lở.
- **Fibonacci hashing** (hằng số vàng).
- **Tái tạo tất định** thay cho chia sẻ trạng thái giữa luồng.
- **Salt** để tách không gian seed.

---

## 7. Liên kết

- Anh em cùng thuật toán: [Deterministic.md](../01-noise-terrain/Deterministic.md)
- Người dùng: [PerlinWormCarver.md](PerlinWormCarver.md), [RavineCarver.md](RavineCarver.md)
