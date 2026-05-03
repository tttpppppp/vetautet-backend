# Tài liệu Chi tiết Cơ sở dữ liệu VeTau-v1

Tài liệu này mô tả chi tiết các bảng, mối quan hệ và chức năng của từng thành phần trong cơ sở dữ liệu hệ thống đặt vé tàu trực tuyến.

---

## 1. Nhóm Quản trị & Bảo mật (RBAC & Auth)

### Bảng: `users`
*   **Chức năng**: Lưu trữ thông tin định danh người dùng.
*   **Mối quan hệ**: 
    - Liên kết n-n với `roles` qua bảng trung gian `user_roles`.
    - Có nhiều `bookings` và `refresh_tokens`.

### Bảng: `roles`
*   **Chức năng**: Định nghĩa các vai trò trong hệ thống (ADMIN, STAFF, CUSTOMER).
*   **Mối quan hệ**: Liên kết n-n với `permissions`.

### Bảng: `permissions`
*   **Chức năng**: Lưu trữ các quyền hạn cụ thể (ví dụ: `TICKET_BOOK`, `TRIP_MANAGE`).
*   **Mối quan hệ**: Liên kết với `roles` để xác định vai trò nào được làm gì.

### Bảng: `refresh_tokens`
*   **Chức năng**: Quản lý phiên đăng nhập và hỗ trợ cơ chế Token Rotation để chống hacker chiếm đoạt tài khoản.

---

## 2. Nhóm Cấu hình Tàu & Lịch trình (Core Trains)

### Bảng: `stations`
*   **Chức năng**: Danh sách các ga tàu trên toàn quốc (Ga Hà Nội, Ga Vinh, Ga Sài Gòn...).

### Bảng: `trains`
*   **Chức năng**: Thông tin các đoàn tàu (Số hiệu tàu SE1, SE3...).

### Bảng: `carriage_types`
*   **Chức năng**: Phân loại toa tàu (Ghế mềm, Ghế cứng, Giường nằm...).

### Bảng: `carriages`
*   **Chức năng**: Các toa cụ thể của một đoàn tàu. Lưu sơ đồ ghế bằng định dạng JSON.
*   **Mối quan hệ**: Thuộc về 1 `train` và có 1 `carriage_type`.

### Bảng: `seats`
*   **Chức năng**: Danh sách các ghế vật lý trong mỗi toa.

### Bảng: `trips`
*   **Chức năng**: Lịch trình cụ thể của một đoàn tàu di chuyển giữa 2 ga trong một khung giờ nhất định.

---

## 3. Nhóm Nghiệp vụ Đặt vé (Booking Logic)

### Bảng: `tickets`
*   **Chức năng**: Quản lý trạng thái thực tế của ghế cho mỗi chuyến đi.
*   **Logic quan trọng**: Lưu trạng thái `AVAILABLE`, `HOLD` (giữ chỗ 15p), hoặc `BOOKED`.

### Bảng: `ticket_prices`
*   **Chức năng**: Cấu hình giá vé dựa trên loại toa cho từng chuyến đi cụ thể.

### Bảng: `bookings`
*   **Chức năng**: Lưu trữ thông tin đơn hàng, tổng tiền và tiền hành thanh toán.
*   **Trạng thái**: `PENDING`, `PAID`, `CANCELLED`, `REFUNDED`.

### Bảng: `booking_details`
*   **Chức năng**: Chi tiết từng vé trong một đơn hàng, gắn tên hành khách và số CCCD/Hộ chiếu.

### Bảng: `payments`
*   **Chức năng**: Nhật ký thanh toán (VNPAY, Momo, Bank) liên kết với Đơn hàng.

---

## 4. Nhóm Khuyến mãi & Cấu hình (Flash Sale & Config)

### Bảng: `activities`
*   **Chức năng**: Quản lý các sự kiện mở bán đặc biệt (Ví dụ: Đợt bán vé Tết).

### Bảng: `activity_items`
*   **Chức năng**: Các sản phẩm khuyến mãi trong sự kiện, hỗ trợ cơ chế "giá sốc" và đánh dấu stock đã cache (Redis).

### Bảng: `system_configs`
*   **Chức năng**: Lưu tham số hệ thống (Thời gian giữ ghế, số vé tối đa) để admin có thể đổi mà không cần sửa code.

---

## Sơ đồ quan hệ chính

1.  **Người dùng** ➡️ **Đơn hàng** ➡️ **Chi tiết đơn hàng** ➡️ **Vé**.
2.  **Vé** = **Chuyến đi** + **Ghế ngồi**.
3.  **Chuyến đi** = **Tàu** + **Ga đi/đến** + **Thời gian**.
4.  **Tàu** ➡️ **Toa tàu** ➡️ **Ghế ngồi**.

---

## 5. Ví dụ Luồng Xử lý Thực tế: Đặt vé & Thanh toán

Dưới đây là mô tả cách các bảng tương tác khi người dùng thực hiện đặt vé:

### Bước 1: Tìm kiếm chuyến đi
*   Hệ thống truy vấn bảng `trips` kết hợp với `stations` để tìm lịch trình.
*   Kiểm tra bảng `ticket_prices` để hiển thị giá tham khảo cho từng loại toa.

### Bước 2: Chọn ghế và Giữ chỗ (HOLD)
*   Người dùng chọn một ghế trống từ bảng `seats`.
*   Hệ thống cập nhật bảng **`tickets`**: 
    - Chuyển `status` thành `HOLD`.
    - Thiết lập `hold_expired_at` = Hiện tại + 15 phút.
    - *Lúc này, người khác sẽ không thể chọn ghế này.*

### Bước 3: Tạo đơn hàng (Booking)
*   Một bản ghi mới được tạo trong bảng **`bookings`** với trạng thái `PENDING`.
*   Thông tin hành khách được lưu vào bảng **`booking_details`**, liên kết trực tiếp với `ticket_id` vừa được giữ.

### Bước 4: Thanh toán
*   Người dùng thanh toán qua cổng điện tử. Một bản ghi được ghi vào bảng **`payments`**.
*   **Nếu thành công**:
    - Cập nhật `bookings.status` ➡️ `PAID`.
    - Cập nhật `tickets.status` ➡️ `BOOKED`.
*   **Nếu thất bại/Hết hạn**:
    - Hệ thống (Scheduler) quét bảng `tickets` dựa trên `hold_expired_at`.
    - Cập nhật `tickets.status` quay lại ➡️ `AVAILABLE`.
    - Cập nhật `bookings.status` ➡️ `CANCELLED`.
