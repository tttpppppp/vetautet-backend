# Luong Xu Ly Flash Sale Benchmark Order

## Muc tieu

Tai lieu nay ghi lai luong xu ly benchmark order theo kieu flash sale da ap dung vao repo hien tai.

Muc tieu cua luong nay:

- Khong de 50.000 request cung dap vao DB
- Chi cho phep so request thang toi da bang stock
- Day phan xu ly cham sang Kafka + consumer
- Khong oversell

Ket qua test sach gan nhat:

- Stock: `1000`
- Total requests: `50000`
- Success: `1000`
- Out of stock: `49000`
- Server error: `0`
- Oversell check: `OK (1000 <= 1000)`
- Throughput HTTP: `525.51 req/s`
- Latency p95: `33.64 ms`
- Async DB result:
  - `Queue rows = 1000`
  - `Created rows = 1000`
  - `Pending rows = 0`
  - `Failed rows = 0`

## Tong quan luong xu ly

```text
Client
-> POST /api/v1/benchmark/test-orders/async
-> Redis Lua gate (giam stock atomic)
-> Neu het stock: reject ngay
-> Neu con stock: luu row PENDING vao test_orders
-> Publish Kafka event
-> Tra response ngay cho client

Kafka consumer
-> Nhan event
-> Doc row theo requestId
-> Chuyen status PENDING -> CREATED
-> Neu loi: cong stock Redis lai, danh dau FAILED
```

## Cac thanh phan chinh

### 1. Redis stock gate

Interface:

- [BenchmarkOrderStockGateway.java](D:/Java/train_spring/VeTau-v1/vetautet-domain/src/main/java/com/vetautet/domain/gateway/BenchmarkOrderStockGateway.java)

Redis implementation:

- [RedisBenchmarkOrderStockGateway.java](D:/Java/train_spring/VeTau-v1/vetautet-infrastructure/src/main/java/com/vetautet/infrastructure/gateway/RedisBenchmarkOrderStockGateway.java)

Redis key:

```text
benchmark:order:stock:{ticketRef}
```

Lua script tra ve:

- `1`: giam stock thanh cong
- `0`: het stock
- `-1`: chua prepare stock

Y nghia:

- Redis la cua gate nhanh
- request thua bi loai tai cong vao
- DB khong can xu ly 50.000 request

### 2. API benchmark

Controller:

- [BenchmarkController.java](D:/Java/train_spring/VeTau-v1/vetautet-controller/src/main/java/com/vetautet/controller/resource/BenchmarkController.java)

Endpoints:

- `POST /api/v1/benchmark/test-orders/prepare?ticketRef=23&stock=1000`
- `DELETE /api/v1/benchmark/test-orders/reset?ticketRef=23`
- `POST /api/v1/benchmark/test-orders/async`
- `GET /api/v1/benchmark/test-orders/stats?ticketRef=23`

### 3. Request path

Service:

- [BenchmarkOrderAppServiceImpl.java](D:/Java/train_spring/VeTau-v1/vetautet-application/src/main/java/com/vetautet/application/service/benchmark/impl/BenchmarkOrderAppServiceImpl.java)

Luot xu ly:

1. Validate request
2. Giam stock Redis bang Lua
3. Neu `OUT_OF_STOCK` thi tra ngay
4. Neu thang:
   - tao `requestId`
   - luu row `PENDING` vao `test_orders`
   - publish Kafka event
5. Neu send Kafka fail:
   - cong stock Redis lai
   - update row `FAILED`

### 4. Kafka topic

Config:

- [KafkaConfig.java](D:/Java/train_spring/VeTau-v1/vetautet-infrastructure/src/main/java/com/vetautet/infrastructure/config/KafkaConfig.java)

Topic benchmark:

```text
benchmark-order-created
```

Hien tai topic nay duoc tao voi:

```java
.partitions(6)
```

### 5. Kafka consumer

Consumer:

- [BenchmarkOrderConsumer.java](D:/Java/train_spring/VeTau-v1/vetautet-infrastructure/src/main/java/com/vetautet/infrastructure/messaging/BenchmarkOrderConsumer.java)

Luot xu ly:

1. Nhan event tu Kafka
2. Tim row theo `requestId`
3. Neu da `CREATED` thi bo qua
4. Neu chua:
   - update status thanh `CREATED`
5. Neu loi:
   - cong stock Redis lai
   - update `FAILED`

## Tai sao khong oversell

Ly do khong oversell nam o Redis Lua gate:

- chi request nao giam duoc stock Redis moi duoc vao Kafka
- stock giam atomic trong Redis
- khi stock ve `0`, cac request sau bi reject ngay

Voi bai test:

- `stock = 1000`
- `request = 50000`

thi business result dung phai la:

- `1000` success
- `49000` out_of_stock

Va luong hien tai da cho ket qua nay.

## Vi sao can row PENDING truoc khi publish Kafka

Row `PENDING` trong `test_orders` dang dong vai tro queue state tam thoi.

No dung de:

- track moi request async
- debug request nao da vao queue
- consumer co diem de update thanh `CREATED` hoac `FAILED`
- de tranh mat dau vet request

No giong vai tro `order_queue` trong cac he thong async lon, chi khac la hien tai ta dang dung chung bang `test_orders` de benchmark nhanh.

## Cach chay lai benchmark

### 1. Start backend

```powershell
mvn -f D:\Java\train_spring\VeTau-v1\vetautet-start\pom.xml org.springframework.boot:spring-boot-maven-plugin:run
```

### 2. Chay k6

```powershell
powershell -ExecutionPolicy Bypass -File D:\Java\train_spring\VeTau-v1\scripts\load-test\run-benchmark-test-orders-k6.ps1 `
  -K6Path "C:\Program Files\k6\k6.exe" `
  -ApiPath "/api/v1/benchmark/test-orders/async" `
  -TotalUsers 50000 `
  -Vus 10 `
  -Stock 1000 `
  -TicketRef 23 `
  -Quantity 1 `
  -Amount 100000
```

Script se tu dong:

1. reset du lieu benchmark
2. prepare stock Redis
3. ban 50.000 request
4. in summary
5. check async DB sau test

## Co can tang Kafka partition khong

### Cau tra loi ngan

**Chua can tang partition ngay luc nay.**

Hien tai:

- topic `benchmark-order-created` da co `6` partition
- nhung consumer hien chi co **1 `@KafkaListener` thread**
- nen thuc te ta **chua dung het 6 partition de xu ly song song**

File consumer hien tai:

- [BenchmarkOrderConsumer.java](D:/Java/train_spring/VeTau-v1/vetautet-infrastructure/src/main/java/com/vetautet/infrastructure/messaging/BenchmarkOrderConsumer.java)

Hien `@KafkaListener` khong co `concurrency`.

### Dieu nay co nghia gi

Kafka partition chi phat huy khi:

- co nhieu consumer instance trong cung group
hoac
- `@KafkaListener(concurrency = "...")`

Neu van chi 1 thread consumer, tang tu `6 -> 12` partition thuong khong tao buoc nhay lon.

### Producer key hien tai

Trong:

- [BenchmarkOrderAppServiceImpl.java](D:/Java/train_spring/VeTau-v1/vetautet-application/src/main/java/com/vetautet/application/service/benchmark/impl/BenchmarkOrderAppServiceImpl.java)

producer dang send voi key:

```java
String kafkaKey = request.getUserRef() != null ? request.getUserRef().toString() : requestId;
kafkaTemplate.send(BENCHMARK_ORDER_CREATED_TOPIC, kafkaKey, event);
```

Nghia la:

- neu `userRef` thay doi theo request thi event co the duoc phan tan qua nhieu partition
- day la dieu tot

### Thu tu uu tien toi uu

Neu muon nang throughput consumer, thu tu hop ly la:

1. **Them consumer concurrency**
   - vi du `@KafkaListener(..., concurrency = "3")`
2. Do lai throughput
3. Neu consumer da can het 6 partition moi nghi tang them partition

### Khi nao nen tang partition

Nen tang partition khi:

- consumer concurrency da tang
- producer key da phan tan deu
- va monitoring cho thay consumer lag van lon
- hoac moi partition da qua nong

### Khuyen nghi hien tai

Voi benchmark hien tai, toi khuyen:

- giu `6` partition
- tang consumer concurrency len `3` truoc
- do lai

Ly do:

- day la thay doi nho
- dung voi thiet ke Kafka hien co
- de xac dinh xem nut nghen nam o consumer thread hay khong

## Huong di tiep theo

Sau khi benchmark nay on dinh, ta co the ap dung cung pattern sang flow ve tau that:

1. Gate nhanh bang Redis
2. Request thang moi vao Kafka
3. Consumer xu ly booking that
4. Loi thi compensate

Nhung voi domain ve tau, phai quyet dinh ro:

- gate theo seat/ticket hien tai
hoac
- them inventory so hoc tong hop de gate nhanh hon

Do la bai toan tiep theo, khong nen tron vao benchmark flow nay.
