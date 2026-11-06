# Client Module

Module **Client** chịu trách nhiệm giao diện người dùng và giao tiếp với Server.

## Trách nhiệm

- Xây dựng GUI bằng JavaFX.
- Đăng ký / đăng nhập từ giao diện.
- Gửi request đến Server qua TCP Socket.
- Nhận và hiển thị response từ Server.
- Hiển thị kết quả tìm kiếm bài hát và ca sĩ.
- Hiển thị playlist, lịch sử tra cứu và thông tin tài khoản.
- Phát audio/video từ URL do Server trả về.
- Xử lý loading, lỗi kết nối và trạng thái giao diện.

## Không thực hiện tại Client

- Không truy cập PostgreSQL trực tiếp.
- Không gọi Music API trực tiếp.
- Không xử lý nghiệp vụ chính.
- Không tự lưu dữ liệu playlist/history thay cho Server.

## Phụ trách

**Đông – Client Lead**

## Hướng phát triển

```text
JavaFX View
    ↓
Controller
    ↓
Client Service
    ↓
Socket Client
    ↓
TCP Server
```
