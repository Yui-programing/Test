# Database Module

Module **Database** quản lý thiết kế dữ liệu và tài liệu liên quan đến PostgreSQL.

## Dữ liệu chính

Dự kiến hệ thống lưu:

- User accounts
- Playlists
- Playlist items
- Search history
- API cache / metadata cần thiết

## Trách nhiệm

- Thiết kế ERD.
- Quản lý schema database.
- Tạo dữ liệu seed phục vụ test/demo.
- Quy định khóa chính, khóa ngoại và constraint.
- Hỗ trợ repository phía Server.
- Chuẩn bị cấu hình database cho môi trường local/deploy.

## Nguyên tắc

Client không được truy cập database trực tiếp. Mọi thao tác dữ liệu phải đi theo luồng:

```text
Client
  ↓ TCP
Server
  ↓
Service
  ↓
Repository
  ↓
PostgreSQL
```

## Phụ trách chính

**Hiệp – Data & Integration Lead**

## Database Đề Xuất
# Database

## 1. Tổng quan

Database được thiết kế cho ứng dụng nghe nhạc, hỗ trợ các chức năng chính:

* Quản lý người dùng.
* Quản lý bài hát và nghệ sĩ.
* Quản lý playlist cá nhân.
* Lưu lịch sử tìm kiếm của người dùng.
* Cache kết quả tìm kiếm.
* Thống kê từ khóa tìm kiếm.

Database sử dụng các bảng chính:

| Table               | Description                         |
| ------------------- | ----------------------------------- |
| `USERS`             | Lưu thông tin tài khoản người dùng  |
| `ARTISTS`           | Lưu thông tin nghệ sĩ               |
| `SONGS`             | Lưu thông tin bài hát               |
| `SONG_ARTISTS`      | Bảng liên kết bài hát và nghệ sĩ    |
| `PLAYLISTS`         | Lưu playlist của người dùng         |
| `PLAYLIST_SONGS`    | Bảng liên kết playlist và bài hát   |
| `SEARCH_HISTORY`    | Lưu lịch sử tìm kiếm của người dùng |
| `SEARCH_CACHE`      | Cache kết quả tìm kiếm              |
| `SEARCH_STATISTICS` | Thống kê số lần tìm kiếm từ khóa    |

---

## 2. Database Relationship

### Users → Playlists

Một `USER` có thể sở hữu nhiều `PLAYLIST`.

```text
USERS 1 ──────── N PLAYLISTS
```

Quan hệ được thể hiện thông qua:

```text
PLAYLISTS.user_id → USERS.user_id
```

---

### Users → Search History

Một `USER` có thể có nhiều bản ghi lịch sử tìm kiếm.

```text
USERS 1 ──────── N SEARCH_HISTORY
```

Quan hệ:

```text
SEARCH_HISTORY.user_id → USERS.user_id
```

---

### Playlists ↔ Songs

Một playlist có thể chứa nhiều bài hát và một bài hát có thể xuất hiện trong nhiều playlist.

Đây là quan hệ **Many-to-Many**, được xử lý thông qua bảng trung gian `PLAYLIST_SONGS`.

```text
PLAYLISTS 1 ──────── N PLAYLIST_SONGS N ──────── 1 SONGS
```

`PLAYLIST_SONGS` còn lưu:

* `position`: vị trí bài hát trong playlist.
* `added_at`: thời điểm bài hát được thêm vào playlist.

---

### Songs ↔ Artists

Một bài hát có thể có nhiều nghệ sĩ và một nghệ sĩ có thể tham gia nhiều bài hát.

Đây cũng là quan hệ **Many-to-Many**, được xử lý thông qua `SONG_ARTISTS`.

```text
SONGS 1 ──────── N SONG_ARTISTS N ──────── 1 ARTISTS
```

Bảng `SONG_ARTISTS` sử dụng khóa chính kết hợp:

```text
(song_id, artist_id)
```

---

# 3. Table Details

## 3.1 USERS

Lưu thông tin tài khoản và thông tin cơ bản của người dùng.

| Column          | Type           | Constraint       | Description             |
| --------------- | -------------- | ---------------- | ----------------------- |
| `user_id`       | `BIGSERIAL`    | PK               | ID người dùng           |
| `username`      | `VARCHAR(50)`  | UNIQUE, NOT NULL | Tên đăng nhập           |
| `password_hash` | `VARCHAR(255)` | NOT NULL         | Mật khẩu đã được mã hóa |
| `full_name`     | `VARCHAR(100)` |                  | Họ và tên               |
| `email`         | `VARCHAR(150)` | UNIQUE           | Email                   |
| `avatar_url`    | `TEXT`         |                  | URL ảnh đại diện        |
| `created_at`    | `TIMESTAMP`    |                  | Thời điểm tạo tài khoản |
| `updated_at`    | `TIMESTAMP`    |                  | Thời điểm cập nhật      |

---

## 3.2 ARTISTS

Lưu thông tin nghệ sĩ.

| Column            | Type           | Constraint | Description              |
| ----------------- | -------------- | ---------- | ------------------------ |
| `artist_id`       | `BIGSERIAL`    | PK         | ID nghệ sĩ               |
| `external_id`     | `VARCHAR(100)` |            | ID từ hệ thống bên ngoài |
| `name`            | `VARCHAR(150)` | NOT NULL   | Tên nghệ sĩ              |
| `normalized_name` | `VARCHAR(150)` |            | Tên đã chuẩn hóa         |
| `birth_date`      | `DATE`         |            | Ngày sinh                |
| `hometown`        | `VARCHAR(255)` |            | Quê quán                 |
| `biography`       | `TEXT`         |            | Tiểu sử                  |
| `image_url`       | `TEXT`         |            | URL hình ảnh             |
| `created_at`      | `TIMESTAMP`    |            | Thời điểm tạo            |
| `updated_at`      | `TIMESTAMP`    |            | Thời điểm cập nhật       |

---

## 3.3 SONGS

Lưu thông tin các bài hát.

| Column             | Type           | Constraint | Description                       |
| ------------------ | -------------- | ---------- | --------------------------------- |
| `song_id`          | `BIGSERIAL`    | PK         | ID bài hát                        |
| `external_id`      | `VARCHAR(100)` |            | ID từ hệ thống bên ngoài          |
| `title`            | `VARCHAR(255)` | NOT NULL   | Tên bài hát                       |
| `normalized_title` | `VARCHAR(255)` |            | Tên bài hát đã chuẩn hóa          |
| `composer`         | `VARCHAR(255)` |            | Nhạc sĩ sáng tác                  |
| `duration_seconds` | `INTEGER`      |            | Thời lượng bài hát tính bằng giây |
| `audio_url`        | `TEXT`         |            | URL file âm thanh                 |
| `video_url`        | `TEXT`         |            | URL video                         |
| `thumbnail_url`    | `TEXT`         |            | URL ảnh thumbnail                 |
| `created_at`       | `TIMESTAMP`    |            | Thời điểm tạo                     |
| `updated_at`       | `TIMESTAMP`    |            | Thời điểm cập nhật                |

---

## 3.4 SONG_ARTISTS

Bảng trung gian biểu diễn quan hệ Many-to-Many giữa `SONGS` và `ARTISTS`.

| Column      | Type     | Constraint | Description |
| ----------- | -------- | ---------- | ----------- |
| `song_id`   | `BIGINT` | PK, FK     | ID bài hát  |
| `artist_id` | `BIGINT` | PK, FK     | ID nghệ sĩ  |

Khóa chính:

```text
PRIMARY KEY (song_id, artist_id)
```

---

## 3.5 PLAYLISTS

Lưu các playlist do người dùng tạo.

| Column        | Type           | Constraint | Description           |
| ------------- | -------------- | ---------- | --------------------- |
| `playlist_id` | `BIGSERIAL`    | PK         | ID playlist           |
| `user_id`     | `BIGINT`       | FK         | Người sở hữu playlist |
| `name`        | `VARCHAR(100)` | NOT NULL   | Tên playlist          |
| `description` | `VARCHAR(255)` |            | Mô tả playlist        |
| `created_at`  | `TIMESTAMP`    |            | Thời điểm tạo         |
| `updated_at`  | `TIMESTAMP`    |            | Thời điểm cập nhật    |

---

## 3.6 PLAYLIST_SONGS

Bảng trung gian biểu diễn quan hệ Many-to-Many giữa `PLAYLISTS` và `SONGS`.

| Column        | Type        | Constraint | Description                   |
| ------------- | ----------- | ---------- | ----------------------------- |
| `playlist_id` | `BIGINT`    | PK, FK     | ID playlist                   |
| `song_id`     | `BIGINT`    | PK, FK     | ID bài hát                    |
| `position`    | `INTEGER`   |            | Vị trí bài hát trong playlist |
| `added_at`    | `TIMESTAMP` |            | Thời điểm thêm bài hát        |

Khóa chính:

```text
PRIMARY KEY (playlist_id, song_id)
```

---

## 3.7 SEARCH_HISTORY

Lưu lịch sử tìm kiếm của người dùng.

| Column               | Type           | Constraint | Description              |
| -------------------- | -------------- | ---------- | ------------------------ |
| `history_id`         | `BIGSERIAL`    | PK         | ID lịch sử               |
| `user_id`            | `BIGINT`       | FK         | Người thực hiện tìm kiếm |
| `keyword`            | `VARCHAR(255)` | NOT NULL   | Từ khóa tìm kiếm         |
| `normalized_keyword` | `VARCHAR(255)` |            | Từ khóa đã chuẩn hóa     |
| `search_type`        | `VARCHAR(20)`  |            | Loại tìm kiếm            |
| `result_count`       | `INTEGER`      |            | Số lượng kết quả         |
| `searched_at`        | `TIMESTAMP`    |            | Thời điểm tìm kiếm       |

---

## 3.8 SEARCH_CACHE

Lưu cache của kết quả tìm kiếm nhằm hạn chế việc thực hiện lại các truy vấn giống nhau.

| Column               | Type           | Constraint       | Description                            |
| -------------------- | -------------- | ---------------- | -------------------------------------- |
| `cache_id`           | `BIGSERIAL`    | PK               | ID cache                               |
| `cache_key`          | `VARCHAR(300)` | UNIQUE, NOT NULL | Khóa cache                             |
| `keyword`            | `VARCHAR(255)` | NOT NULL         | Từ khóa tìm kiếm                       |
| `normalized_keyword` | `VARCHAR(255)` | NOT NULL         | Từ khóa đã chuẩn hóa                   |
| `search_type`        | `VARCHAR(20)`  | NOT NULL         | Loại tìm kiếm                          |
| `response_json`      | `JSONB`        | NOT NULL         | Kết quả tìm kiếm dạng JSON             |
| `created_at`         | `TIMESTAMP`    |                  | Thời điểm tạo cache                    |
| `expires_at`         | `TIMESTAMP`    | NOT NULL         | Thời điểm cache hết hạn                |
| `last_accessed_at`   | `TIMESTAMP`    |                  | Thời điểm cache được truy cập gần nhất |

---

## 3.9 SEARCH_STATISTICS

Lưu thống kê về các từ khóa tìm kiếm.

| Column               | Type           | Constraint | Description           |
| -------------------- | -------------- | ---------- | --------------------- |
| `statistic_id`       | `BIGSERIAL`    | PK         | ID thống kê           |
| `keyword`            | `VARCHAR(255)` | NOT NULL   | Từ khóa               |
| `normalized_keyword` | `VARCHAR(255)` | NOT NULL   | Từ khóa đã chuẩn hóa  |
| `search_type`        | `VARCHAR(20)`  | NOT NULL   | Loại tìm kiếm         |
| `search_count`       | `BIGINT`       |            | Số lần tìm kiếm       |
| `last_searched_at`   | `TIMESTAMP`    |            | Lần tìm kiếm gần nhất |

---

# 4. Entity Relationship Diagram

```text
                    ┌──────────────┐
                    │    USERS     │
                    └──────┬───────┘
                           │
                ┌──────────┴──────────┐
                │                     │
                ▼                     ▼
        ┌──────────────┐      ┌─────────────────┐
        │  PLAYLISTS   │      │ SEARCH_HISTORY  │
        └──────┬───────┘      └─────────────────┘
               │
               │
               ▼
        ┌────────────────┐
        │ PLAYLIST_SONGS │
        └───────┬────────┘
                │
                ▼
          ┌────────────┐
          │   SONGS    │
          └─────┬──────┘
                │
                ▼
        ┌────────────────┐
        │  SONG_ARTISTS  │
        └───────┬────────┘
                │
                ▼
          ┌────────────┐
          │  ARTISTS   │
          └────────────┘


        ┌──────────────────┐
        │   SEARCH_CACHE   │
        └──────────────────┘

        ┌──────────────────────┐
        │  SEARCH_STATISTICS   │
        └──────────────────────┘
```

---

# 5. Relationship Summary

| Relationship               | Type | Through                  |
| -------------------------- | ---- | ------------------------ |
| `USERS` → `PLAYLISTS`      | 1:N  | `PLAYLISTS.user_id`      |
| `USERS` → `SEARCH_HISTORY` | 1:N  | `SEARCH_HISTORY.user_id` |
| `PLAYLISTS` ↔ `SONGS`      | N:M  | `PLAYLIST_SONGS`         |
| `SONGS` ↔ `ARTISTS`        | N:M  | `SONG_ARTISTS`           |

> `SEARCH_CACHE` và `SEARCH_STATISTICS` được định nghĩa trong ERD nhưng sơ đồ hiện tại không thể hiện quan hệ FK trực tiếp với các bảng khác.

---

# 6. Design Notes

### Normalized fields

Các trường như:

```text
normalized_name
normalized_title
normalized_keyword
```

được sử dụng để lưu dữ liệu đã chuẩn hóa, hỗ trợ việc tìm kiếm và so sánh dữ liệu hiệu quả hơn.

### External ID

`external_id` trong `SONGS` và `ARTISTS` cho phép lưu ID tương ứng từ nguồn dữ liệu hoặc API bên ngoài.

### Search Cache

`SEARCH_CACHE.response_json` sử dụng kiểu `JSONB` để lưu trực tiếp kết quả tìm kiếm. `expires_at` xác định thời điểm dữ liệu cache không còn hiệu lực.

### Search Statistics

`SEARCH_STATISTICS` giúp thống kê mức độ phổ biến của các từ khóa thông qua:

```text
search_count
last_searched_at
```

### Composite Primary Key

Hai bảng trung gian sử dụng khóa chính kết hợp:

```text
SONG_ARTISTS
PRIMARY KEY (song_id, artist_id)
```

và:

```text
PLAYLIST_SONGS
PRIMARY KEY (playlist_id, song_id)
```

Cách thiết kế này đảm bảo một cặp quan hệ giữa hai entity không bị trùng lặp.
