# Tài liệu Luồng Bảo mật (Security Flow) - VeTau Project

Tài liệu này mô tả chi tiết cách thức hoạt động của hệ thống Authentication và Authorization trong dự án theo kiến trúc DDD.

---

## 1. Tổng quan Công nghệ
- **Framework**: Spring Security 6.x
- **Cơ chế**: Stateless (Không dùng Session)
- **Token**: JSON Web Token (JWT) - Access Token & Refresh Token (Bao gồm `userId` claim)
- **Mã hóa**: BCrypt (độ mạnh 10)
- **Phân quyền**: RBAC (Role-Based Access Control)
- **Mapping**: MapStruct (Chuyển đổi DTO - Domain - Entity)

---

## 2. Các API Bảo mật
| Method | Endpoint | Quyền truy cập | Mô tả |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/register` | `permitAll()` | Đăng ký tài khoản mới |
| `POST` | `/auth/login` | `permitAll()` | Đăng nhập và nhận bộ Token đôi |
| `POST` | `/auth/refresh` | `permitAll()` | Sử dụng Refresh Token để đổi Access Token mới (Cơ chế xoay vòng) |
| `POST` | `/auth/logout` | `permitAll()` | Vô hiệu hóa Refresh Token bên phía Server |
| `GET` | `/auth/me` | `authenticated()` | Lấy thông tin Profile (Minh họa Map Domain -> DTO) |

---

## 3. Tính năng Cao cấp: Refresh Token Rotation & Hacker Detection

Để đảm bảo an toàn, hệ thống thực hiện cơ chế **Xoay vòng Token (Rotation)**:
1. Mỗi khi dùng Refresh Token để lấy Access Token mới, hệ thống sẽ hủy Token cũ và cấp một Refresh Token hoàn toàn mới cho Client.
2. **Hacker Detection**: Nếu một Refresh Token đã bị hủy (`revoked = true`) mà vẫn được gửi lên -> Hệ thống xác định đây là hành vi tấn công (Sử dụng lại Token bị đánh cắp).
3. **Phản ứng**: Xóa toàn bộ các Refresh Token hiện có của User đó trong Database, yêu cầu đăng nhập lại từ đầu trên mọi thiết bị.

---

## 4. Cấu trúc Model 3 Tầng (DDD)
Dự án tách biệt hoàn toàn dữ liệu qua các lớp:
1. **Persistence Entity (`UserEntity`)**: Làm việc với Database (Infrastructure).
2. **Domain Entity (`User`)**: Chứa logic nghiệp vụ thuần túy (Domain), không chứa annotation JPA.
3. **DTO (`UserResponse`)**: Dữ liệu sau khi chọn lọc để trả về cho Client (Application).

Việc chuyển đổi được thực hiện tự động qua **MapStruct** (`PersistenceMapper`, `UserMapper`).

---

## 5. Xử lý Lỗi chuẩn JSON
Mọi Exceptions (Sai mật khẩu, Token hết hạn, Dữ liệu không hợp lệ...) đều được bắt qua `GlobalExceptionHandler` và trả về cấu trúc lỗi tiếng Việt thân thiện:
```json
{
  "timestamp": "2026-04-10T03:30:00",
  "status": 400,
  "error": "Business Logic Error",
  "message": "Sai tài khoản hoặc mật khẩu",
  "path": "/auth/login",
  "validationErrors": null
}
```
