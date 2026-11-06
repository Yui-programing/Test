# Server Module

Module **Server** là trung tâm xử lý của hệ thống.

## Trách nhiệm

- Lắng nghe kết nối TCP từ nhiều Client.
- Xử lý đồng thời bằng thread pool / ExecutorService.
- Nhận request và điều phối theo action.
- Xử lý authentication.
- Xử lý tìm kiếm bài hát và ca sĩ.
- Gọi external API và chuyển dữ liệu về DTO nội bộ.
- Thực hiện Fuzzy Search.
- Quản lý Playlist và Search History thông qua tầng dữ liệu.
- Sử dụng Cache để giảm số lần gọi API.
- Xử lý timeout, request lỗi và client disconnect.

## Luồng xử lý

```text
Client Request
     ↓
TCP Server
     ↓
Request Dispatcher
     ↓
Service
     ├── External API
     ├── Cache
     └── Repository / Database
     ↓
Response
     ↓
Client
```

## Phụ trách

**Đạt – Server Lead**
