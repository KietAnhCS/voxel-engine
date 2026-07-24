# ChunkKey — gộp toạ độ chunk thành khoá 64-bit

**File:** `core/src/com/voxel/engine/util/ChunkKey.java`

---

## 1. Mã hoá

```java
public static long of(int x, int z) {
    return ((long) x << 32) | (z & 0xFFFFFFFFL);
}
public static int x(long key) { return (int) (key >> 32); }
public static int z(long key) { return (int) key; }
```

### Bố trí bit

```
  bit: 63 ─────────────── 32 │ 31 ─────────────── 0
      ┌──────────────────────┼──────────────────────┐
      │       x (32 bit)     │       z (32 bit)     │
      └──────────────────────┴──────────────────────┘
```

**Song ánh (bijection):** mỗi cặp `(x, z) ∈ int × int` cho **đúng một** `long` khác nhau, và ngược lại. Số cặp là `2³² × 2³² = 2⁶⁴` — vừa khít miền giá trị của `long`.

⇒ **Không bao giờ đụng khoá (zero collision)**.

---

## 2. Ba chi tiết kỹ thuật

### 2.1 `(long) x << 32` — ép kiểu TRƯỚC khi dịch

```java
((long) x << 32)     ✔ đúng
((long) (x << 32))   ✗ SAI — x << 32 trên int là dịch 0 bit!
```

Trong Java, toán tử dịch trên `int` chỉ dùng **5 bit thấp** của số dịch: `x << 32` ≡ `x << (32 & 31)` = `x << 0` = `x`. Phải ép sang `long` trước để dịch mới hoạt động (khi đó dùng 6 bit thấp).

### 2.2 `z & 0xFFFFFFFFL` — chặn mở rộng dấu

Khi ép `int` âm sang `long`, Java **mở rộng dấu**:

```java
int z = -1;
(long) z  =  0xFFFFFFFFFFFFFFFF     // 64 bit đều là 1
```

Nếu `OR` thẳng, 32 bit cao của `x` bị **ghi đè hết thành 1** ⇒ mọi `(x, −1)` cho cùng một khoá. Mask `& 0xFFFFFFFFL` **xoá 32 bit cao**, chỉ giữ lại phần `z` thật.

Chú ý hậu tố `L` — nếu viết `0xFFFFFFFF` (kiểu `int`) thì nó chính là `−1`, và `z & −1 = z`, vô tác dụng.

### 2.3 Giải mã

```java
public static int x(long key) { return (int) (key >> 32); }    // dịch có dấu → khôi phục x âm
public static int z(long key) { return (int) key; }             // ép int cắt 32 bit cao
```

`(int) key` tự động **cắt bỏ** 32 bit cao và giữ nguyên bit dấu ở vị trí 31 ⇒ `z` âm được khôi phục đúng.

Với `x`, dùng `>>` (dịch **có dấu**) chứ không phải `>>>` — bit 63 chính là bit dấu của `x`.

---

## 3. Vì sao không dùng `Map<Point, Chunk>`?

Bảng chunk bị tra cứu **hàng vạn lần mỗi giây** (mỗi lần đọc/ghi khối, mỗi lần vẽ, mỗi bước vật lý).

| | `HashMap<Point, Chunk>` | `HashMap<Long, Chunk>` (ChunkKey) |
|---|---|---|
| Cấp phát khoá | **1 object `Point` mỗi lần tra** | `Long` (cache −128..127, thường vẫn cấp phát) |
| `hashCode()` | phải tự viết, dễ đụng khoá | dựng sẵn, phân tán tốt |
| `equals()` | so 2 trường + kiểm kiểu | 1 phép so sánh `long` |
| Bộ nhớ mỗi khoá | 16 B header + 8 B dữ liệu | 16 B |
| Đụng khoá | tuỳ chất lượng `hashCode` | **0 với `LongMap`** |

Với `HashMap<Long, ...>` vẫn còn autoboxing; tối ưu triệt để là dùng `LongMap` của libGDX (bảng băm khoá nguyên thuỷ) — khi đó **hoàn toàn không cấp phát**.

### Ví dụ hashCode tự viết dễ sai

```java
// ✗ Đụng khoá nghiêm trọng
public int hashCode() { return x * 31 + z; }
// (0, 31) và (1, 0) → cùng hash 31
```

`ChunkKey` tránh hẳn vấn đề này vì nó **không băm** — nó là một **song ánh hoàn hảo**.

---

## 4. So sánh với các sơ đồ đóng gói khác trong project

| Lớp | Đóng gói | Bit | Ghi chú |
|---|---|---|---|
| `ChunkKey` | `(x, z)` → `long` | 32 + 32 | Song ánh hoàn hảo |
| `ChunkStorage.index` | `(x, y, z)` → `int` | 4 + 7 + 4 | Chỉ trong 1 chunk |
| `FluidSimulator.pack` | `(x, y, z)` → `long` | 26 + 12 + 26 | Toạ độ thế giới |
| `LightEngine` light byte | `(sky, block)` → `byte` | 4 + 4 | 2 giá trị nhỏ |

Cùng một tư tưởng: **thay object bằng số nguyên đóng gói** để tránh cấp phát và tăng tốc cache.

---

## 5. Độ phức tạp

| Thao tác | Chi phí |
|---|---|
| `of(x, z)` | `O(1)` — 1 dịch + 1 mask + 1 OR |
| `x(key)`, `z(key)` | `O(1)` — 1 dịch / 1 ép kiểu |
| Bộ nhớ | 8 byte / khoá, **không cấp phát object** |

---

## 6. Chủ đề DSA thể hiện

- **Song ánh (bijection)** giữa `int × int` và `long` — hàm băm hoàn hảo (perfect hash).
- **Đóng gói trường bit** & mở rộng dấu.
- **Bẫy ngôn ngữ:** `<<` trên `int` chỉ dùng 5 bit thấp; ép kiểu mở rộng dấu.
- **Tránh cấp phát object** trong đường dẫn nóng.

---

## 7. Liên kết

- Anh em đóng gói: [ChunkStorage.md](../05-world/ChunkStorage.md), [FluidSimulator.md](../05-world/FluidSimulator.md) §4
