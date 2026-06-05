USE vetautet;

UPDATE stations SET name = 'Ga Hà Nội', location = 'Hà Nội' WHERE code = 'HAN';
UPDATE stations SET name = 'Ga Đà Nẵng', location = 'Đà Nẵng' WHERE code = 'DAN';
UPDATE stations SET name = 'Ga Sài Gòn', location = 'TP. Hồ Chí Minh' WHERE code = 'SGN';
UPDATE stations SET name = 'Ga Vinh', location = 'Nghệ An' WHERE code = 'VIN';
UPDATE stations SET name = 'Ga Huế', location = 'Thừa Thiên Huế' WHERE code = 'HUE';
UPDATE stations SET name = 'Ga Nha Trang', location = 'Khánh Hòa' WHERE code = 'NTR';

UPDATE trains SET description = 'Tàu hỏa Thống Nhất Bắc Nam' WHERE code = 'SE1';
UPDATE trains SET description = 'Tàu nhanh chất lượng cao' WHERE code = 'SE3';
UPDATE trains SET description = 'Tàu nhanh Bắc Nam bổ sung' WHERE code = 'SE5';
UPDATE trains SET description = 'Tàu thống nhất đêm bổ sung' WHERE code = 'TN1';
UPDATE trains SET description = 'Tàu địa phương kết nối đô thị' WHERE code LIKE 'SUB%';

UPDATE carriage_types SET name = 'Ghế mềm điều hòa', description = 'Chỗ ngồi bọc da thoải mái' WHERE code = 'SOFT_SEAT';
UPDATE carriage_types SET name = 'Ghế cứng', description = 'Chỗ ngồi tiết kiệm' WHERE code = 'HARD_SEAT';
UPDATE carriage_types SET name = 'Giường nằm khoang 6', description = 'Khoang 6 giường nằm tầng' WHERE code = 'SLEEPER_6';
UPDATE carriage_types SET name = 'Giường nằm khoang 4', description = 'Khoang 4 giường nằm chất lượng cao' WHERE code = 'SLEEPER_4';

UPDATE carriages SET name = REPLACE(name, 'Ghe Mem', 'Ghế mềm');
UPDATE carriages SET name = REPLACE(name, 'Ghe mem', 'Ghế mềm');
UPDATE carriages SET name = REPLACE(name, 'Ghe Cung', 'Ghế cứng');
UPDATE carriages SET name = REPLACE(name, 'Ghe cung', 'Ghế cứng');
UPDATE carriages SET name = REPLACE(name, 'Giuong Nam', 'Giường nằm');
UPDATE carriages SET name = REPLACE(name, 'Giuong nam', 'Giường nằm');
UPDATE carriages SET name = REPLACE(name, 'Giuong 4', 'Giường 4');
UPDATE carriages SET name = REPLACE(name, 'Giuong 6', 'Giường 6');

UPDATE promotions
SET title = 'Vé Tết sum vầy',
    description = 'Ưu đãi sớm cho các chuyến tàu Tết trên trục Bắc - Nam.',
    conditions = 'Áp dụng cho đơn từ 2 vé, đặt trước ngày khởi hành tối thiểu 7 ngày.',
    route = 'Ga Sài Gòn -> Ga Hà Nội'
WHERE code = 'TETSUMVAY';

UPDATE promotions
SET title = 'Sinh viên lên tàu',
    description = 'Tiết kiệm cho sinh viên khi đặt vé ghế ngồi hoặc giường nằm.',
    conditions = 'Cần xuất trình thẻ sinh viên còn hiệu lực khi lên tàu.',
    route = 'Ga Hà Nội -> Vinh'
WHERE code = 'SVRAIL';

UPDATE promotions
SET title = 'Đi về tiết kiệm hơn',
    description = 'Giảm trực tiếp cho hành trình khứ hồi trong cùng một đơn.',
    conditions = 'Áp dụng khi đặt tối thiểu 1 vé chiều đi và 1 vé chiều về.'
WHERE code = 'KHUTHOI';

UPDATE promotions
SET title = 'Thanh toán online cuối tuần',
    description = 'Miễn phí dịch vụ khi thanh toán bằng ví điện tử hoặc thẻ nội địa.',
    conditions = 'Áp dụng từ thứ Sáu đến Chủ nhật cho đơn thanh toán online.'
WHERE code = 'PAYONLINE';

UPDATE promotions
SET title = 'Gia đình chọn khoang',
    description = 'Ưu đãi cho nhóm gia đình đặt khoang 4 hoặc khoang 6.',
    conditions = 'Đơn từ 4 hành khách, áp dụng cho khoang 4 hoặc khoang 6.',
    route = 'Ga Đà Nẵng -> Ga Sài Gòn'
WHERE code = 'GIADINH';

UPDATE promotions
SET title = 'Tuyến biển miền Trung',
    description = 'Giảm mạnh cho tuyến Sài Gòn - Đà Nẵng trong mùa du lịch.',
    conditions = 'Áp dụng cho vé từ thứ Hai đến thứ Năm, không cộng dồn ưu đãi.',
    route = 'Ga Sài Gòn -> Ga Đà Nẵng'
WHERE code = 'BIENXANH';

UPDATE promotions
SET title = 'Chốt vé tháng 6',
    description = 'Ưu đãi ngắn ngày cho các chuyến còn nhiều ghế trống.',
    conditions = 'Số lượng mã có hạn, áp dụng cho đơn từ 300.000đ.'
WHERE code = 'MAYLAST';

UPDATE promotions
SET title = 'Nhóm bạn mùa hè',
    description = 'Càng đông càng tiết kiệm cho nhóm từ 6 hành khách.',
    conditions = 'Áp dụng cho đơn từ 6 vé cùng chuyến, cùng hạng ghế.'
WHERE code = 'NHOMHE';
