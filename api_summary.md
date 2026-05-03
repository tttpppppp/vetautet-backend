# Tổng hợp API Hệ thống (VeTau-v1)

### 1. Nhóm Xác thực & Người dùng (Authentication)
*   `POST /auth/register` - Đăng ký tài khoản.
*   `POST /auth/login` - Đăng nhập.
*   `GET /auth/me` - Lấy thông tin cá nhân (từ Token).
*   `PUT /auth/profile` - Cập nhật thông tin cá nhân (từ Token).
*   `GET /auth/my-tickets` - Danh sách vé của tôi (từ Token).
*   `POST /auth/refresh` - Làm mới Token.
*   `POST /auth/logout` - Đăng xuất.

### 2. Nhóm Tra cứu chuyến đi (Trips)
*   `GET /trips` - Lấy toàn bộ danh sách chuyến đi.
*   `GET /trips/{id}` - Chi tiết chuyến đi và sơ đồ ghế.
*   `GET /trips/search` - Tìm kiếm chuyến đi nâng cao.

### 3. Nhóm Nghiệp vụ Đặt vé (Booking)
*   `POST /bookings` - Bước 1: Giữ chỗ (Hold seats + Redis lock 15p).
*   `PUT /bookings/{id}` - Bước 2: Cập nhật thông tin hành khách.
*   `POST /bookings/{id}/confirm-payment` - Bước 3: Xác nhận thanh toán → CONFIRMED → Xóa Redis lock.
*   Bước 4: Hệ thống tự động gửi **Notification** qua Kafka (Email/SMS) + lưu DB.

### 3.5 Thông báo (Notifications)
*   `GET /notifications` - Danh sách thông báo của tôi.
*   `GET /notifications/unread-count` - Số thông báo chưa đọc.
*   `PUT /notifications/{id}/read` - Đánh dấu đã đọc.
*   `PUT /notifications/read-all` - Đánh dấu tất cả đã đọc.

### 4. Nhóm Quản trị (Admin)
**Quản lý Chuyến đi:**
*   `GET /admin/trips` - Lấy danh sách chuyến đi.
*   `POST /admin/trips` - Tạo chuyến đi mới.
*   `PUT /admin/trips/{id}` - Cập nhật chuyến đi.
*   `DELETE /admin/trips/{id}` - Xóa chuyến đi.

**Quản lý Tàu (Trains):**
*   `GET /admin/trains` - Danh sách tàu.
*   `POST /admin/trains` - Thêm tàu mới.
*   `PUT /admin/trains/{id}` - Cập nhật tàu.
*   `DELETE /admin/trains/{id}` - Xóa tàu.

**Quản lý Ghế/Vé (Tickets/Seats):**
*   `GET /admin/tickets/trip/{tripId}` - Danh sách ghế của một chuyến đi.
*   `PUT /admin/tickets/{id}` - Cập nhật giá/trạng thái ghế.
*   `DELETE /admin/tickets/{id}` - Xóa ghế.

**Quản lý Đặt vé (Bookings):**
*   `GET /admin/bookings` - Danh sách toàn bộ đơn hàng.
*   `GET /admin/bookings/{id}` - Chi tiết đơn hàng.
*   `PUT /admin/bookings/{id}/status` - Cập nhật trạng thái đơn hàng.
*   `DELETE /admin/bookings/{id}` - Xóa đơn hàng.

**Quản lý Ga tàu:**
*   `GET /admin/stations` - Danh sách ga.
*   `GET /admin/stations/{id}` - Chi tiết ga.
*   `POST /admin/stations` - Thêm ga mới.
*   `PUT /admin/stations/{id}` - Cập nhật thông tin ga.
*   `DELETE /admin/stations/{id}` - Xóa ga.

**Quản lý Người dùng:**
*   `GET /admin/users` - Danh sách người dùng.
*   `PUT /admin/users/{id}` - Cập nhật thông tin & phân quyền User.
*   `GET /admin/users/roles` - Danh sách các Role có sẵn.
*   `DELETE /admin/users/{id}` - Xóa người dùng.

### 5. Khác
*   `GET /hello` - Check health hệ thống.
*   `POST /test-kafka` - Test gửi tin nhắn vào Kafka.
