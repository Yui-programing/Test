# 🎵 Music Finder Network

Ứng dụng tra cứu thông tin bài hát và ca sĩ theo mô hình **Client–Server** bằng Java.

Client giao tiếp với Server qua **TCP Socket**. Toàn bộ xử lý nghiệp vụ, truy cập cơ sở dữ liệu, gọi API bên ngoài, cache và tìm kiếm gần đúng được thực hiện phía Server. Client tập trung vào giao diện JavaFX, hiển thị dữ liệu và phát media từ liên kết do Server cung cấp.

## Kiến trúc tổng quan

```text
JavaFX Client
     │
     │ TCP Socket / JSON
     ▼
Java TCP Server
     ├── Authentication
     ├── Search / External API
     ├── Playlist / History
     ├── Cache / Statistics
     └── PostgreSQL Database
```

## Cấu trúc thư mục dự kiến

```text
music-finder-network/
│
├── client/
│   ├── README.md
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/musicfinder/client/
│           │       ├── controller/
│           │       ├── network/
│           │       ├── service/
│           │       ├── model/
│           │       └── util/
│           │
│           └── resources/
│               ├── view/
│               ├── css/
│               └── assets/
│
├── server/
│   ├── README.md
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── java/
│               └── com/musicfinder/server/
│                   ├── network/
│                   ├── handler/
│                   ├── service/
│                   ├── repository/
│                   ├── api/
│                   ├── cache/
│                   ├── config/
│                   └── util/
│
├── shared/
│   ├── README.md
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── java/
│               └── com/musicfinder/shared/
│                   ├── dto/
│                   ├── request/
│                   ├── response/
│                   └── enums/
│
├── database/
│   ├── README.md
│   ├── schema.sql
│   ├── seed.sql
│   └── migrations/
│
├── docs/
│   ├── README.md
│   ├── architecture/
│   ├── protocol/
│   ├── database/
│   └── testing/
│
├── .gitignore
├── pom.xml
└── README.md

## Chức năng chính

- Đăng ký, đăng nhập và cập nhật thông tin người dùng.
- Tra cứu bài hát theo tên.
- Tra cứu ca sĩ Việt Nam.
- Hiển thị thông tin bài hát/ca sĩ và phát audio/video từ link hỗ trợ.
- Quản lý playlist cá nhân.
- Lưu và xem lịch sử tra cứu.
- Server xử lý đồng thời nhiều client.
- Cache kết quả để giảm số lần gọi API.
- Fuzzy Search để gợi ý khi người dùng nhập sai.
- Thống kê bài hát/ca sĩ được tra cứu nhiều.

## Công nghệ dự kiến

- Java 21
- JavaFX
- TCP Socket
- Maven
- JSON / Jackson
- PostGreSql
- JDBC
- Docker
- Git & GitHub

## Phân công

| Thành viên | Vai trò | Phụ trách chính |
|---|---|---|
| Đông | Client Lead | JavaFX GUI, Socket Client, Media Player |
| Đạt | Server Lead | TCP Server, Multi-thread, Authentication, Search/API, Fuzzy Search |
| Hiệp | Data & Integration Lead | Database, Repository, Playlist/History, Cache/Statistics, Deploy |

## Nguyên tắc kiến trúc

1. Client không truy cập database trực tiếp.
2. Client không gọi external API trực tiếp.
3. Business logic được xử lý ở Server.
4. Client và Server giao tiếp thông qua TCP protocol đã thống nhất.
5. DTO và protocol dùng chung đặt trong module `shared`.
6. Các thành viên phát triển trên branch riêng và merge qua Pull Request.

## Git workflow đề xuất

```text
main
├── feature/client-...
├── feature/server-...
└── feature/database-...
```



## Trạng thái

Project đang trong giai đoạn phát triển cho môn **Lập trình mạng**.
