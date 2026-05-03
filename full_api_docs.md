# Tổng hợp API Hệ thống Vé Tàu (VeTau-v1) - Cập nhật mới nhất

Tài liệu này tổng hợp toàn bộ các API đang hoạt động, đã tối ưu cho việc kết nối giữa Frontend và Backend.

---

## 1. Nhóm Xác thực & Người dùng (Authentication)

Tất cả API nhóm này đều bắt đầu bằng `/auth`. Cần Bearer Token cho các API yêu cầu bảo mật.

### 1.1 Đăng ký & Đăng nhập
*   `POST /auth/register`: Đăng ký.
*   `POST /auth/login`: Đăng nhập (Trả về `accessToken`, `refreshToken`).

### 1.2 Thông tin cá nhân & Vé của tôi
*   `GET /auth/me`: Lấy thông tin User profile từ token.
*   `GET /auth/my-tickets`: Danh sách vé đã đặt của User (tự động lấy từ token).

---

## 2. Nhóm Tra cứu chuyến đi (Trips)

### 2.1 Tìm kiếm nâng cao (Advanced Search)
*   **Method**: `GET`
*   **URL**: `/trips/search`
*   **Query Params**:
    - `departure` (String, Required): Tên ga đi.
    - `arrival` (String, Required): Tên ga đến.
    - `date` (YYYY-MM-DD, Required): Ngày đi.
    - `trainTypes` (List, Optional): Loại tàu (Ví dụ: `SE,TN`). 
    - `minPrice`, `maxPrice` (Number, Optional): Khoảng giá.
*   **Example**: `/trips/search?departure=Saigon&arrival=Hanoi&date=2026-04-25&trainTypes=SE&minPrice=1000000`/trips/search?departure=Saigon&arrival=Hanoi&date=2026-04-25&trainTypes=SE&minPrice=1000000`

### 2.2 Chi tiết chuyến đi (Sơ đồ ghế theo Toa)
*   **Method**: `GET`
*   **URL**: `/trips/{id}`
*   **Description**: Trả về dữ liệu chuyến đi, trong đó quan trọng nhất là mảng `carriages`.
*   **Cấu trúc dữ liệu**:
```json
{
  "tripId": 1,
  "carriages": [
    {
      "carriageNumber": "T1",
      "carriageTypeName": "4-Berth VIP",
      "seats": [
        { "id": 101, "seatNumber": "01", "status": "AVAILABLE", "price": 1250000 },
        { "id": 102, "seatNumber": "02", "status": "HOLD", "price": 1250000 }
      ]
    }
  ]
}
```

---

## 3. Nhóm Nghiệp vụ Đặt vé (Booking)

Tuân thủ luồng 3 bước trên FE.

### 3.1 Bước 1: Giữ chỗ (POST `/bookings`)
FE gọi ngay sau khi khách chọn ghế xong và nhấn "Continue Booking".
```json
{
    "tripId": 1,
    "ticketIds": [101, 102]
}
```

### 3.2 Bước 2: Điền thông tin hành khách (PUT `/bookings/{id}`)
FE gọi sau khi khách điền xong form thông tin người đi.
```json
{
    "passengers": [
        { "ticketId": 101, "name": "Nguyên Văn A", "idCard": "123456" },
        { "ticketId": 102, "name": "Trần Thị B", "idCard": "654321" }
    ]
}
```

### 3.3 Bước 3: Xác nhận thanh toán (POST `/bookings/{id}/confirm-payment`)
FE gọi sau khi nhận được callback thanh toán thành công từ Gateway (hoặc Mock trong quá trình phát triển).
*   **Trạng thái Booking**: Chuyển từ `PENDING` sang `CONFIRMED`.
*   **Trạng thái Ghế**: Chuyển từ `HOLD` sang `BOOKED`.
*   **Redis**: Xóa key giữ chỗ `seat:{tripId}:{seatId}`.
*   **Kafka**: Gửi event `payment-confirmed` sang bước tiếp theo.

### 3.4 Bước 4: Thông báo (Tự động qua Kafka)
Sau khi thanh toán thành công, hệ thống tự động xử lý:
*   **Kafka Consumer** (`PaymentNotificationConsumer`) lắng nghe topic `payment-confirmed`.
*   Gửi **Email/SMS xác nhận vé** cho khách hàng (hiện tại là Mock, sẵn sàng tích hợp JavaMailSender).
*   Nếu đơn hàng quá hạn (15p không thanh toán), hệ thống tự động gửi **thông báo hủy đơn**.

```
Quy trình tổng thể:
  Chọn ghế → Hold (Redis 15p) → Tạo Booking PENDING → Thanh toán 
  → CONFIRMED → Xóa Redis Lock → Gửi Notification (Kafka)
  
  Nếu quá hạn 15p:
  PENDING → EXPIRED/CANCELLED → Giải phóng ghế → Gửi Notification hủy
```

---

## 4. Realtime Sơ đồ ghế (WebSocket)
Hệ thống bắn event Realtime cập nhật trạng thái ghế (Hold, Booked, Available) để FE tự động đổi màu ghế UI cho những user khác đang cùng xem 1 chuyến đi, tránh việc bấm chui vào ghế vừa bị người khác giữ.

*   **Endpoint**: `ws://localhost:8081/ws`
*   **Subscribe Topic**: `/topic/trips/{tripId}/seats`
*   **Payload trả về (Event)**:
    ```json
    {
      "tripId": 1,
      "ticketId": 105,
      "seatNumber": "1A",
      "status": "HOLD" // Hoặc BOOKED, AVAILABLE
    }
    ```

---

## 5. Lưu ý quan trọng cho FE
- Hệ thống có cơ chế tự động hủy đơn hàng và giải phóng ghế sau **15 phút** nếu không thanh toán.
- Hệ thống hỗ trợ **Distributed Lock (Redisson)**: Đảm bảo không bao giờ có 2 người cùng đặt trùng 1 ghế.
- **Redis Seat Hold**: Key `seat:{tripId}:{seatId}` giúp kiểm tra nhanh ghế đang bị giữ mà không cần query DB.
- Tất cả các API POST/PUT liên quan đến Booking đều yêu cầu Header: `Authorization: Bearer <token>`.

---

## 5. Nhóm Quản trị (Admin)

Cần có Role `ADMIN` để truy cập các API này. Header: `Authorization: Bearer <token>`.

### 5.1 Quản lý Chuyến đi (Trips)
*   `GET /admin/trips`: Danh sách toàn bộ chuyến đi.
*   `POST /admin/trips`: Tạo chuyến đi mới.
*   `PUT /admin/trips/{id}`: Cập nhật thông tin chuyến đi.
*   `DELETE /admin/trips/{id}`: Xóa chuyến đi.

### 5.2 Quản lý Tàu (Trains)
*   `GET /admin/trains`: Danh sách tàu.
*   `POST /admin/trains`: Thêm tàu mới (Body: `{ "code": "SE1", "description": "Tàu Bắc Nam" }`).
*   `PUT /admin/trains/{id}`: Cập nhật thông tin tàu.
*   `DELETE /admin/trains/{id}`: Xóa tàu.

### 5.3 Quản lý Ghế/Vé cho Chuyến đi (Tickets/Seats)
*   `GET /admin/tickets/trip/{tripId}`: Danh sách toàn bộ ghế của một chuyến đi cụ thể.
*   `PUT /admin/tickets/{id}`: Cập nhật giá vé hoặc trạng thái ghế (Body: `{ "price": 1200000, "status": "AVAILABLE" }`).
*   `DELETE /admin/tickets/{id}`: Xóa ghế khỏi chuyến đi.

### 5.4 Quản lý Đặt vé (Bookings)
*   `GET /admin/bookings`: Danh sách toàn bộ đơn hàng.
*   `GET /admin/bookings/{id}`: Chi tiết đơn hàng.
*   `PUT /admin/bookings/{id}/status`: Cập nhật trạng thái đơn hàng (Ví dụ: `PAID`, `CANCELLED`).
*   `DELETE /admin/bookings/{id}`: Xóa đơn hàng.

### 5.5 Quản lý Ga tàu (Stations)
*   `GET /admin/stations`: Danh sách ga.
*   `POST /admin/stations`: Thêm ga mới.
*   `PUT /admin/stations/{id}`: Cập nhật thông tin ga.
*   `DELETE /admin/stations/{id}` - Xóa ga.

### 5.6 Quản lý Người dùng & Phân quyền (Users)
*   `GET /admin/users`: Danh sách người dùng.
*   `PUT /admin/users/{id}`: Cập nhật thông tin và phân quyền (Roles) cho User (Body: `{ "roles": ["ADMIN", "CUSTOMER"] }`).
*   `GET /admin/users/roles`: Lấy danh sách các Role hợp lệ trong hệ thống.
*   `DELETE /admin/users/{id}`: Xóa người dùng.
