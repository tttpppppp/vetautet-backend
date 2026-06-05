# Redis Sentinel Setup

## Muc tieu

Them Redis master/slave + Sentinel vao repo nay de:

- demo high availability
- demo failover tu dong
- giai thich dung vai tro cua Sentinel trong do an

Luu y:

- Sentinel **khong lam app nhanh hon**
- Sentinel chi giai quyet:
  - Redis master chet
  - tu dong promote slave len master moi
  - client reconnect sang master moi

## Kien truc hien tai

Trong [docker-compose-dev.yml](D:/Java/train_spring/VeTau-v1/environment/docker-compose-dev.yml), stack Redis da duoc doi thanh:

- `pre-event-redis` -> master, expose `6319`
- `pre-event-redis-slave-1` -> slave, expose `6320`
- `pre-event-redis-slave-2` -> slave, expose `6321`
- `pre-event-redis-sentinel-1` -> sentinel, expose `26379`
- `pre-event-redis-sentinel-2` -> sentinel, expose `26380`
- `pre-event-redis-sentinel-3` -> sentinel, expose `26381`

Sentinel monitor master name:

```text
mymaster
```

Config files:

- [sentinel-1.conf](D:/Java/train_spring/VeTau-v1/environment/redis/sentinel-1.conf)
- [sentinel-2.conf](D:/Java/train_spring/VeTau-v1/environment/redis/sentinel-2.conf)
- [sentinel-3.conf](D:/Java/train_spring/VeTau-v1/environment/redis/sentinel-3.conf)

## Spring Boot config

File:

- [application.yml](D:/Java/train_spring/VeTau-v1/vetautet-start/src/main/resources/application.yml)

Da ho tro 2 mode:

### 1. Single Redis mode

Mac dinh:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6319}
```

### 2. Sentinel mode

Bat bang env:

```text
REDIS_SENTINEL_MASTER=mymaster
REDIS_SENTINEL_NODES=localhost:26379,localhost:26380,localhost:26381
```

Khi do:

- Spring Data Redis se dung Sentinel de tim master
- Redisson cung se dung Sentinel

File Java:

- [RedisConfig.java](D:/Java/train_spring/VeTau-v1/vetautet-infrastructure/src/main/java/com/vetautet/infrastructure/config/RedisConfig.java)

Logic:

- neu khong co `spring.data.redis.sentinel.*` -> `useSingleServer()`
- neu co -> `useSentinelServers()`

## Cach chay stack

### Dung compose hien tai

```powershell
docker compose -f D:\Java\train_spring\VeTau-v1\environment\docker-compose-dev.yml up -d redis redis-slave-1 redis-slave-2 redis-sentinel-1 redis-sentinel-2 redis-sentinel-3
```

Hoac dung full stack:

```powershell
docker compose -f D:\Java\train_spring\VeTau-v1\environment\docker-compose-dev.yml up -d
```

## Cach chay app voi Sentinel

PowerShell:

```powershell
$env:REDIS_SENTINEL_MASTER="mymaster"
$env:REDIS_SENTINEL_NODES="localhost:26379,localhost:26380,localhost:26381"
mvn -f D:\Java\train_spring\VeTau-v1\vetautet-start\pom.xml org.springframework.boot:spring-boot-maven-plugin:run
```

Neu muon quay ve single Redis:

```powershell
Remove-Item Env:REDIS_SENTINEL_MASTER -ErrorAction SilentlyContinue
Remove-Item Env:REDIS_SENTINEL_NODES -ErrorAction SilentlyContinue
```

## Cach verify replication

### Kiem tra master

```powershell
docker exec pre-event-redis redis-cli INFO replication
```

Ky vong:

- `role:master`
- co `connected_slaves:2`

### Kiem tra slave

```powershell
docker exec pre-event-redis-slave-1 redis-cli INFO replication
docker exec pre-event-redis-slave-2 redis-cli INFO replication
```

Ky vong:

- `role:slave`
- `master_link_status:up`

### Kiem tra Sentinel

```powershell
docker exec pre-event-redis-sentinel-1 redis-cli -p 26379 SENTINEL master mymaster
```

## Demo failover

### 1. Kill master

```powershell
docker stop pre-event-redis
```

### 2. Kiem tra Sentinel

```powershell
docker exec pre-event-redis-sentinel-1 redis-cli -p 26379 SENTINEL master mymaster
```

Ky vong:

- mot slave duoc promote len master moi

### 3. Kiem tra app

- app van doc/ghi Redis duoc
- stock gate benchmark van chay
- local cache va booking hold van khong bi mat logic

## Khi nao nen doc tu slave

Co the doc tu slave cho:

- cache public read-heavy
- trip list
- station list
- categories
- report / analytics

## Khi nao khong duoc doc tu slave

Khong duoc dung slave cho:

- Redis Lua stock gate
- hold seat
- booking ownership
- idempotency nong

Ly do:

- slave co replication lag
- doc stock cu se sai logic

## Co can them partition, cluster hay khong

Sentinel chi giai quyet:

- HA
- failover

Sentinel **khong** giai quyet:

- chia du lieu
- scale ngang du lieu Redis

Neu sau nay can:

- du lieu qua mot node
- key space qua lon
- throughput write qua cao

thi do moi la bai toan Redis Cluster, khong phai Sentinel.

## Ket luan

Trong do an nay:

- them Sentinel la hop ly
- no cho thay ban hieu HA va failover
- nhung stock gate benchmark van phai doc/ghi master

Noi ngan:

```text
Sentinel = song sot khi Redis master chet
Cluster  = chia du lieu / scale ngang
```
