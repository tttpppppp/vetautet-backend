USE vetautet;

UPDATE stations SET name = 'Ga Ha Noi', location = 'Ha Noi' WHERE code = 'HAN';
UPDATE stations SET name = 'Ga Da Nang', location = 'Da Nang' WHERE code = 'DAN';
UPDATE stations SET name = 'Ga Sai Gon', location = 'TP. Ho Chi Minh' WHERE code = 'SGN';

UPDATE trains SET description = 'Tau hoa Thong Nhat Bac Nam' WHERE code = 'SE1';
UPDATE trains SET description = 'Tau nhanh chat luong cao' WHERE code = 'SE3';

UPDATE carriage_types
SET name = 'Ghe mem dieu hoa',
    description = 'Cho ngoi boc da thoai mai'
WHERE code = 'SOFT_SEAT';

UPDATE carriage_types
SET name = 'Ghe cung',
    description = 'Cho ngoi tiet kiem'
WHERE code = 'HARD_SEAT';

UPDATE carriage_types
SET name = 'Giuong nam khoang 6',
    description = 'Khoang 6 giuong nam tang'
WHERE code = 'SLEEPER_6';

UPDATE carriage_types
SET name = 'Giuong nam khoang 4',
    description = 'Khoang 4 giuong nam chat luong cao'
WHERE code = 'SLEEPER_4';

UPDATE carriages SET name = 'Toa 1 - Ghe Mem' WHERE id = 1;
UPDATE carriages SET name = 'Toa 2 - Giuong Nam' WHERE id = 2;
UPDATE carriages SET name = 'Toa 1 - Ghe Cung' WHERE id = 3;
