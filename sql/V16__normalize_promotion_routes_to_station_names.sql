USE vetautet;

UPDATE `promotions`
SET `route` = 'Ga Sai Gon -> Ga Ha Noi'
WHERE `code` = 'TETSUMVAY';

UPDATE `promotions`
SET `route` = 'Ga Ha Noi -> Vinh'
WHERE `code` = 'SVRAIL';

UPDATE `promotions`
SET `route` = 'Ga Da Nang -> Ga Sai Gon'
WHERE `code` = 'GIADINH';

UPDATE `promotions`
SET `route` = 'Ga Sai Gon -> Ga Da Nang'
WHERE `code` = 'BIENXANH';
