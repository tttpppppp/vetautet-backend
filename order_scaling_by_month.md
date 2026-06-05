# Order Scaling By Month

## 1. Muc tieu

Tai lieu nay chot huong luu tru `booking/order` theo thang, giong tu duy cac san thuong mai dien tu ban rut gon:

- Du lieu don hang duoc chia theo thoi gian
- Moi thang mot bang rieng
- `order_number` chua thong tin ngay thang nam
- Tim 1 don hang thi route thang vao bang thang tuong ung
- Query lich su 12 thang thi doc tu bang moi nhat den bang cu nhat

Phan nay ap dung cho du lieu `booking/order history`, khong ap dung cho `trip`, `station`, `seat layout`, `ticket price`.

## 2. Pham vi ap dung

Ap dung cho:

- `bookings`
- Neu can, mo rong tuong tu cho `payments`
- Cac API lich su dat ve cua user
- Cac API tim kiem don hang theo `order_number`

Khong ap dung cho:

- `trip` hien hanh
- `seat inventory` dang ban real-time
- Redis cache

## 3. Y tuong tong quat

Thay vi de toan bo don hang trong 1 bang lon:

```text
bookings
```

he thong tach thanh cac bang theo thang:

```text
bookings_202601
bookings_202602
bookings_202603
...
```

Moi ban ghi don hang se duoc luu vao bang cua thang tao don.

## 4. Quy tac dat ten bang

Format:

```text
bookings_YYYYMM
```

Vi du:

- `bookings_202605`
- `bookings_202606`

Co the ap dung tuong tu cho bang thanh toan:

- `payments_202605`
- `payments_202606`

## 5. Quy tac ma don hang

`order_number` phai chua ngay thang nam de co the route dung bang.

De xuat:

```text
ORD-YYYYMMDD-XXXXXX
```

Vi du:

```text
ORD-20260508-000123
```

Hoac neu muon ngan hon:

```text
250508000123
```

Trong tai lieu nay, uu tien format de doc:

```text
ORD-20260508-000123
```

## 6. Loi ich cua ma don hang co thoi gian

Khi nhin vao `order_number`, backend co the:

1. Tach `YYYYMM`
2. Xac dinh bang can query, vi du `bookings_202605`
3. Query dung 1 bang

Nhu vay, bai toan `findByOrderNumber()` khong can quet 12 bang.

## 7. Cau truc bang thang

Moi bang thang co cung schema.

Vi du:

```sql
CREATE TABLE bookings_202605 (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    trip_id BIGINT NOT NULL,
    total_price DECIMAL(18,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    payment_status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_bookings_user_created (user_id, created_at DESC),
    INDEX idx_bookings_status_created (status, created_at DESC),
    INDEX idx_bookings_trip_created (trip_id, created_at DESC)
);
```

Khuyen nghi giu schema giua cac bang thang dong nhat.

## 8. Flow tao don hang

Khi user dat ve:

1. Lay thoi diem hien tai, vi du `2026-05-08`
2. Xac dinh bang dich: `bookings_202605`
3. Sinh `order_number`, vi du `ORD-20260508-000123`
4. Ghi don hang vao `bookings_202605`

Pseudo-code:

```java
YearMonth month = YearMonth.now();
String tableName = "bookings_" + month.format(DateTimeFormatter.ofPattern("yyyyMM"));
String orderNumber = orderNumberGenerator.generate(now);

bookingMonthlyRepository.save(tableName, booking);
```

## 9. Flow tim 1 don hang theo ma don

Khi can tim chinh xac 1 don:

1. Nhan `order_number`
2. Parse ra `YYYYMM`
3. Route toi bang thang
4. Query theo `order_number`

Pseudo-code:

```java
String month = extractMonth(orderNumber); // 202605
String tableName = "bookings_" + month;
return bookingMonthlyRepository.findByOrderNumber(tableName, orderNumber);
```

Truong hop nay rat nhanh vi:

- Khong scan 12 bang
- Khong can union
- Query trung 1 bang duy nhat

## 10. Flow lay lich su don hang 12 thang

Khi can lay danh sach don hang cua user:

1. Xac dinh 12 thang gan nhat
2. Query tu bang moi nhat den bang cu nhat
3. Moi bang lay du lieu theo `user_id`
4. Gop ket qua theo thu tu `created_at DESC`
5. Ap `limit/offset` tai service layer

Huong don gian:

```text
bookings_202605 -> bookings_202604 -> bookings_202603 -> ...
```

Vi du SQL tung bang:

```sql
SELECT * FROM bookings_202605
WHERE user_id = ?
ORDER BY created_at DESC
LIMIT ?, ?;
```

Neu thang moi khong du du lieu, tiep tuc doc sang bang cu hon.

## 11. Danh doi can chap nhan

Huong nay don gian de hieu, nhung co nhung danh doi ro rang:

- `findByOrderNumber()` nhanh
- Query recent data nhanh hon vi doc bang moi
- Archive du lieu cu de lam

Nhung:

- `limit/offset` xuyen nhieu bang phuc tap hon
- `count(*)` tong 12 thang phai cong nhieu bang
- Admin search nhieu dieu kien se kho hon
- Migration schema phai dam bao dong bo tren cac bang thang

## 12. Tai sao van chon huong nay

Du an nay chon theo logic:

- Don hang tang theo thoi gian
- Phan lon truy van la don moi
- Tim 1 don hang bang ma don la use case quan trong
- Muon hoc theo tu duy cua cac he thong lon, ban rut gon

Nen day la mot huong hop ly cho giai doan hien tai.

## 13. Gioi han cua giai phap

Giai phap nay chua phai phien ban day du cua he thong lon.

He thong lon thuong co them:

- metadata/index bang nho de dinh vi du lieu
- routing service rieng
- job tao bang thang tu dong
- archive / cold storage
- search pipeline rieng cho admin

Tai repo nay, chung ta moi ap dung phien ban toi gian:

- chia bang theo thang
- ma don hang chua thoi gian
- query tu bang moi den bang cu

## 14. Khuyen nghi trien khai trong du an nay

De trien khai on dinh, nen lam theo thu tu:

1. Chot format `order_number`
2. Tao repository dong cho `bookings_YYYYMM`
3. Tao helper parse thang tu `order_number`
4. Tao service `findByOrderNumber()`
5. Tao service `getOrdersByUser()` doc 12 bang gan nhat
6. Tao SQL template de sinh bang thang moi

## 15. Huong DDD de ap dung

Huong nay van co the giu cau truc DDD:

- `domain`
  - `Booking`
  - `BookingRepository`
- `application`
  - `BookingAppService`
- `infrastructure`
  - `BookingMonthlyRepositoryImpl`
  - `BookingTableRouter`
  - `BookingOrderNumberParser`

Infrastructure se chiu trach nhiem:

- route dung bang thang
- map giua DB va domain
- che giau logic chia bang khoi `application`

`application` va `domain` khong can biet chi tiet ten bang vat ly.

## 16. Ket luan

Kien truc nay chot theo logic:

- Moi thang 1 bang `bookings_YYYYMM`
- `order_number` chua `YYYYMMDD`
- Tim 1 don thi parse thang tu ma don va query dung 1 bang
- Lay danh sach 12 thang thi doc tu moi den cu

Day la phien ban "Shopee-style ban rut gon", phu hop de hoc va trien khai giai doan hien tai cua du an.
