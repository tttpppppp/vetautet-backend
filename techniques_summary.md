# Kỹ Thuật Đã Áp Dụng Trong Dự Án

Tài liệu này liệt kê ngắn gọn các kỹ thuật chính đang có trong dự án `VeTau-v1` và tác dụng của từng kỹ thuật.

## Tổng số kỹ thuật chính

Hiện tại có **12 kỹ thuật / hướng kỹ thuật chính**.

## 1. DDD phân tầng

- **Mục đích**: tách `controller`, `application`, `domain`, `infrastructure`.
- **Tác dụng**:
  - code dễ bảo trì hơn
  - tách business ra khỏi framework
  - dễ thay DB / Redis / Kafka hơn

## 2. Distributed lock bằng Redisson

- **Mục đích**: khóa theo tài nguyên khi nhiều request cùng đặt chỗ.
- **Tác dụng**:
  - tránh 2 người cùng giữ cùng 1 ghế
  - giảm race condition ở booking/cancel

## 3. Redis seat hold với TTL

- **Mục đích**: giữ ghế tạm thời trong Redis theo thời gian sống.
- **Tác dụng**:
  - giảm query DB liên tục
  - tự động hết hạn giữ chỗ nếu người dùng không thanh toán

## 4. Local cache trong app

- **Mục đích**: cache nóng ngay trong RAM của instance app.
- **Tác dụng**:
  - đọc nhanh hơn Redis
  - giảm tải Redis ở các endpoint public
  - phù hợp cho dữ liệu đọc lặp lại nhiều

## 5. Redis distributed cache

- **Mục đích**: cache dùng chung giữa nhiều instance backend.
- **Tác dụng**:
  - dữ liệu cache không bị giới hạn trong 1 node app
  - hỗ trợ chia sẻ trạng thái hold/cache giữa nhiều backend

## 6. Chống cache stampede

- **Mục đích**: khi cache miss, chỉ cho 1 request xuống DB.
- **Tác dụng**:
  - tránh nhiều request cùng đập DB
  - ổn định hơn dưới tải cao
- **Cách làm**:
  - local check
  - Redis check
  - lock
  - check lần 2
  - miss thật mới query DB

## 7. Kafka xử lý bất đồng bộ

- **Mục đích**: đẩy phần xử lý chậm sang background.
- **Tác dụng**:
  - request HTTP trả nhanh hơn
  - tách booking/payment/notification/email khỏi request path
  - hệ thống chịu tải tốt hơn

## 8. Saga / compensation

- **Mục đích**: khi flow nhiều bước bị lỗi, hệ thống có hướng bù trừ.
- **Tác dụng**:
  - giữ dữ liệu nhất quán hơn
  - lỗi payment hoặc confirm booking không làm hệ thống lệch trạng thái quá lâu
  - có thể phát event `FAILED / COMPENSATED`

## 9. Flash-sale gate bằng Redis Lua

- **Mục đích**: kiểm tra và trừ stock atomic ngay tại Redis.
- **Tác dụng**:
  - request thua bị loại ngay
  - chỉ request thắng mới vào Kafka / DB
  - tránh oversell
  - chịu tải tốt hơn rất nhiều so với check DB trực tiếp

## 10. Benchmark order async

- **Mục đích**: tạo luồng test riêng cho order tải cao.
- **Tác dụng**:
  - đo được khả năng nhận request
  - đo được khả năng xử lý async qua Kafka
  - tách khỏi flow booking thật để benchmark an toàn

## 11. Scale order theo tháng

- **Mục đích**: tách bản ghi order / booking theo `YYYYMM`.
- **Tác dụng**:
  - dữ liệu lịch sử dễ quản lý hơn
  - truy vấn order theo thời gian mới -> cũ rõ ràng hơn
  - hỗ trợ hướng scale dữ liệu dài hạn

## 12. Monitoring + observability

- **Mục đích**: theo dõi app khi chạy thật và khi benchmark.
- **Thành phần**:
  - Spring Actuator
  - Prometheus
  - Grafana
- **Tác dụng**:
  - xem health
  - theo dõi JVM / latency / throughput
  - hỗ trợ phân tích bottleneck

## 13. Redis Sentinel (HA / failover)

- **Mục đích**: Redis master chết thì slave có thể được promote lên master.
- **Tác dụng**:
  - giảm single point of failure
  - app không phụ thuộc 1 Redis node duy nhất
  - tăng tính sẵn sàng của hệ thống

## 14. Realtime update

- **Mục đích**: cập nhật thay đổi booking/seat/status về phía client theo thời gian thực.
- **Tác dụng**:
  - giao diện đồng bộ nhanh hơn
  - người dùng thấy trạng thái ghế / booking thay đổi ngay

## Ghi chú ngắn

- **Local cache**: giúp đọc nhanh.
- **Redis cache**: chia sẻ cache giữa nhiều instance.
- **Redis slave + Sentinel**: chủ yếu để HA / failover, không phải kỹ thuật chính để tăng tốc stock gate.
- **Kafka**: giúp tách tải và xử lý async.
- **Redis Lua**: giúp chặn request thua ngay ở cổng vào.
- **Saga**: giúp bù trừ khi flow nhiều bước bị lỗi.

## 3 kỹ thuật đáng giá nhất để trình bày

Nếu cần chọn ít nhưng chất lượng để thuyết trình, nên nhấn mạnh:

1. **Redis Lua flash-sale gate**
2. **Kafka async + compensation**
3. **Redis Sentinel + failover**
