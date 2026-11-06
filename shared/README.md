# Shared Module

Module **Shared** chứa các thành phần dùng chung giữa Client và Server để tránh định nghĩa protocol khác nhau ở hai phía.

## Trách nhiệm

- DTO dùng chung.
- Request model.
- Response model.
- Action / enum của TCP protocol.
- Các constant hoặc contract cần dùng ở cả Client và Server.

## Ví dụ protocol

Request:

```json
{
  "requestId": "123",
  "action": "SEARCH_SONG",
  "data": {
    "keyword": "hello"
  }
}
```

Response:

```json
{
  "requestId": "123",
  "action": "SEARCH_SONG",
  "success": true,
  "message": "OK",
  "data": []
}
```

## Nguyên tắc

Mọi thay đổi liên quan đến request/response contract cần được Client Lead và Server Lead thống nhất trước khi merge.
