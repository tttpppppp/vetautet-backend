# vetautet

Backend Spring Boot cho hệ thống đặt vé tàu trực tuyến.

## Local Run

Khởi động MySQL và Redis bằng Docker Compose:

```powershell
docker compose -f environment/docker-compose-dev.yml up -d db redis
```

Chạy backend local khi chưa bật Kafka:

```powershell
cd vetautet-start
mvn -q org.springframework.boot:spring-boot-maven-plugin:3.4.4:run -Dspring-boot.run.arguments=--vetautet.kafka.listeners.enabled=false
```

API base URL:

```text
http://localhost:8080/api/v1
```

## Required Environment Variables

Set các biến này khi dùng tính năng tương ứng:

```text
JWT_SECRET
GOOGLE_CLIENT_ID
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET
MOMO_PARTNER_CODE
MOMO_ACCESS_KEY
MOMO_SECRET_KEY
VNPAY_TMN_CODE
VNPAY_HASH_SECRET
TICKET_QR_SECRET
```
