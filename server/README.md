# Voxel Server — backend lưu tài khoản & thế giới

Backend Spring Boot + PostgreSQL cho game voxel. Game LibGDX gọi vào đây để **đăng ký / đăng nhập**
và **lưu / tải thế giới** (seed + vị trí người chơi + các block đã đặt/phá).

Đây **không phải web** — không có trang HTML nào. Chỉ là REST API cho game gọi. Màn đăng nhập nằm
ngay trong game LibGDX.

## Cách chạy

### 1. Bật backend (PostgreSQL + server) bằng Docker

```bash
# từ thư mục gốc voxel-engine
docker compose up --build
```

Lần đầu sẽ tải Maven + Spring Boot nên hơi lâu. Khi thấy dòng `Started VoxelServerApplication`
là server đã sẵn sàng ở `http://localhost:8080`.

Dữ liệu Postgres được giữ trong volume `voxel-db-data`, tắt máy không mất thế giới.

Tắt: `Ctrl+C`, hoặc `docker compose down` (thêm `-v` nếu muốn xoá luôn dữ liệu).

### 2. Chạy game

```bash
./gradlew.bat :desktop:run
```

Game mở ra màn **đăng nhập**. Bấm **Đăng ký** cho tài khoản mới, hoặc **Đăng nhập** nếu đã có.
Vào game xây nhà, đi lại — tự lưu mỗi 20 giây và lưu lần cuối khi thoát (phím **Q**).
Lần sau đăng nhập lại đúng tài khoản đó, thế giới và vị trí hiện lại y như cũ.

## API

| Method | Đường dẫn            | Cần đăng nhập | Việc                                   |
|--------|----------------------|:-------------:|----------------------------------------|
| POST   | `/api/auth/register` | không         | Tạo tài khoản → trả JWT                 |
| POST   | `/api/auth/login`    | không         | Đăng nhập → trả JWT                     |
| GET    | `/api/world`         | có (Bearer)   | Tải thế giới (tạo mới nếu lần đầu)      |
| PUT    | `/api/world`         | có (Bearer)   | Lưu vị trí người chơi + các block đã sửa |

Địa chỉ server game gọi lấy từ biến môi trường `VOXEL_SERVER_URL` (mặc định `http://localhost:8080`).

## Cấu trúc

- `auth/` — tài khoản, băm mật khẩu BCrypt, cấp/kiểm JWT.
- `world/` — thế giới (seed + trạng thái người chơi) và các block đã sửa.
- `config/` — chặn request kiểm tra JWT trước khi vào `/api/world`.
