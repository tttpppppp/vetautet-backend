# Tài liệu API Đặt Vé (Booking API Specification)

Tài liệu này hướng dẫn các Payload mà Frontend cần gửi lên và dữ liệu nhận về cho các API vừa mới được cập nhật.

---

## 1. Xem chi tiết chuyến đi (Trip/Tour Details)

API này dùng để lấy thông tin chi tiết một chuyến đi bao gồm danh sách toàn bộ ghế để người dùng chọn.

*   **Endpoint**: `GET /trips/{id}`
*   **Authentication**: Không yêu cầu (Public)
*   **Response Payload**:

```json
{
    "id": 1,
    "trainCode": "SE1",
    "departureStation": "Ga Hà Nội",
    "arrivalStation": "Ga Sài Gòn",
    "departureTime": "2024-04-18T19:00:00",
    "arrivalTime": "2024-04-19T23:00:00",
    "duration": 28,
    "status": "SCHEDULED",
    "seats": [
        {
            "id": 101,
            "trainCode": "SE1",
            "departureStation": "Ga Hà Nội",
            "arrivalStation": "Ga Sài Gòn",
            "departureTime": "2024-04-18T19:00:00",
            "seatNumber": "A01",
            "price": 500000.00,
            "status": "AVAILABLE"
        },
        {
            "id": 102,
            "trainCode": "SE1",
            "departureStation": "Ga Hà Nội",
            "arrivalStation": "Ga Sài Gòn",
            "departureTime": "2024-04-18T19:00:00",
            "seatNumber": "A02",
            "price": 500000.00,
            "status": "BOOKED"
        }
    ]
}
```

**Lưu ý**: Frontend nên lọc danh sách `seats` có `status === 'AVAILABLE'` để hiển thị các ghế có thể chọn.

---

## 2. Tạo đơn hàng đặt vé (Create Booking)

API này dùng để giữ chỗ các ghế mà người dùng đã chọn (tối đa 4 ghế).

*   **Endpoint**: `POST /bookings`
*   **Authentication**: Yêu cầu Token (Header: `Authorization: Bearer <token>`)
*   **Phân quyền**: User phải có role `CUSTOMER`.

### Request Payload (FE gửi lên)

```json
{
    "tripId": 1,
    "ticketIds": [101, 103, 105],
    "passengerName": "Nguyễn Thành Chung"
}
```

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `tripId` | Long | Có | ID của chuyến đi. |
| `ticketIds` | List<Long> | Có | Danh sách ID của các ghế (vé) muốn đặt. **Tối đa 4 ID**. |
| `passengerName` | String | Có | Tên người đại diện đặt vé. |

### Response Payload (Kết quả trả về)

**Success (200 OK)**:
```json
{
    "bookingId": 5,
    "status": "PENDING",
    "totalPrice": 1500000.00,
    "expiredAt": "2024-04-17T02:00:00"
}
```

*   `bookingId`: Dùng để thực hiện thanh toán ở bước sau.
*   `expiredAt`: Thời hạn để người dùng thanh toán. Sau thời gian này vé sẽ tự động bị hủy (giải phóng ghế).

**Error Cases**:
*   `400 Bad Request`: Gửi quá 4 ghế hoặc danh sách ghế trống.
*   `401 Unauthorized`: Chưa đăng nhập.
*   `403 Forbidden`: Không có quyền đặt vé.
*   `500 Internal Server Error`: Ghế đã có người khác nhanh tay giữ chỗ trước (`status != AVAILABLE`).

---

## 3. Quy trình đề xuất cho Frontend

1.  Gọi `GET /trips/{id}` để hiển thị sơ đồ ghế.
2.  Người dùng click chọn ghế (FE nên lưu danh sách `id` của ghế).
3.  Khi nhấn "Tiếp tục/Đặt vé", kiểm tra nếu `selectedSeats.length > 4` thì báo lỗi.
4.  Gọi `POST /bookings` với danh sách ID đã chọn.
5.  Nếu thành công, chuyển hướng người dùng tới trang Thanh toán (Payment) với `bookingId`.
