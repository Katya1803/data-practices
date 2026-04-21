# DVDRental — Hướng dẫn học SQL · Kafka · Redis

## Khởi động

```bash
./docker.sh run       # lần đầu hoặc sau khi sửa code (~5-10 phút)
./docker.sh start     # lần sau (không rebuild, ~1-2 phút)
```

Mở browser: **http://localhost:5173**

### Các lệnh thường dùng

```bash
./docker.sh stop               # dừng tất cả (giữ data)
./docker.sh reset              # xóa sạch data, restore lại từ đầu
./docker.sh restart springboot # restart 1 service
./docker.sh rebuild django     # rebuild + restart 1 service
./docker.sh logs springboot    # xem log realtime (Ctrl+C để thoát, containers vẫn chạy)
./docker.sh ps                 # kiểm tra trạng thái containers
./docker.sh db                 # mở psql shell
./docker.sh redis              # mở redis-cli
./docker.sh kafka              # list kafka topics
```

---

## 1. SQL — PostgreSQL

### Kết nối database

```bash
./docker.sh db
# hoặc kết nối từ bên ngoài: host=localhost port=5433 user=postgres db=dvdrental
```

### Sơ đồ các bảng chính

```
customer ──< rental >── inventory >── film >── film_category >── category
                │                               │
             payment                         film_actor >── actor
```

---

### 1.1 SELECT cơ bản

```sql
-- Lấy 10 phim đầu tiên
SELECT film_id, title, rental_rate, rating FROM film LIMIT 10;

-- Lọc theo rating
SELECT title, rating, length FROM film WHERE rating = 'PG' ORDER BY length DESC;

-- Đếm phim theo rating
SELECT rating, COUNT(*) AS total FROM film GROUP BY rating ORDER BY total DESC;
```

### 1.2 JOIN nhiều bảng

```sql
-- Khách hàng + số lần thuê + tổng tiền
SELECT
    c.first_name || ' ' || c.last_name AS name,
    COUNT(r.rental_id)                  AS rentals,
    COALESCE(SUM(p.amount), 0)          AS total_paid
FROM customer c
LEFT JOIN rental  r ON r.customer_id = c.customer_id
LEFT JOIN payment p ON p.customer_id = c.customer_id
GROUP BY c.customer_id
ORDER BY total_paid DESC
LIMIT 10;
```

> **Tại sao LEFT JOIN?** Nếu dùng INNER JOIN, khách hàng chưa thuê lần nào sẽ bị loại khỏi kết quả.

### 1.3 Subquery & EXISTS

```sql
-- Tìm phim hiện có hàng trong kho (chưa bị thuê hết)
SELECT DISTINCT f.title
FROM film f
WHERE EXISTS (
    SELECT 1 FROM inventory i
    WHERE i.film_id = f.film_id
      AND i.inventory_id NOT IN (
          SELECT inventory_id FROM rental WHERE return_date IS NULL
      )
);
```

### 1.4 Window Function

```sql
-- Xếp hạng phim trong từng thể loại theo số lần thuê
SELECT
    f.title,
    c.name AS category,
    COUNT(r.rental_id) AS rentals,
    RANK() OVER (PARTITION BY c.name ORDER BY COUNT(r.rental_id) DESC) AS rank_in_category
FROM film f
JOIN film_category fc ON fc.film_id    = f.film_id
JOIN category      c  ON c.category_id = fc.category_id
JOIN inventory     i  ON i.film_id     = f.film_id
JOIN rental        r  ON r.inventory_id = i.inventory_id
GROUP BY f.film_id, c.name
ORDER BY category, rank_in_category;
```

### 1.5 Full-Text Search (tsvector)

```sql
-- Tìm phim có "action" hoặc "love" trong description
SELECT title, description
FROM film
WHERE to_tsvector('english', description) @@ to_tsquery('english', 'action | love');

-- Tìm theo nhiều từ (AND)
SELECT title FROM film
WHERE to_tsvector('english', description) @@ to_tsquery('english', 'epic & drama');
```

> Cột `fulltext` trong bảng `film` là tsvector được index sẵn — nhanh hơn `LIKE '%keyword%'` rất nhiều.

### 1.6 Interval & Date

```sql
-- Phim đang overdue (chưa trả, quá hạn rental_duration)
SELECT
    c.first_name || ' ' || c.last_name   AS customer,
    f.title,
    r.rental_date,
    r.rental_date + CAST((f.rental_duration || ' days') AS interval) AS due_date,
    NOW() - (r.rental_date + CAST((f.rental_duration || ' days') AS interval)) AS overdue_by
FROM rental r
JOIN customer  c ON c.customer_id   = r.customer_id
JOIN inventory i ON i.inventory_id  = r.inventory_id
JOIN film      f ON f.film_id       = i.film_id
WHERE r.return_date IS NULL
  AND r.rental_date + CAST((f.rental_duration || ' days') AS interval) < NOW();
```

### 1.7 Aggregate + DATE_PART

```sql
-- Doanh thu theo tháng năm 2007
SELECT
    DATE_PART('year',  payment_date)::int AS year,
    DATE_PART('month', payment_date)::int AS month,
    COUNT(*)                              AS transactions,
    SUM(amount)                           AS revenue
FROM payment
WHERE DATE_PART('year', payment_date) = 2007
GROUP BY year, month
ORDER BY month;
```

> Dataset dvdrental chỉ có dữ liệu năm **2005–2007**. Dùng year=2007 để có kết quả đầy đủ nhất.

### Bài tập SQL

```sql
-- 1. Top 5 thể loại phim được thuê nhiều nhất
-- 2. Khách hàng thuê phim nhiều nhất nhưng trả tiền ít nhất
-- 3. Tìm phim chưa được thuê lần nào
-- 4. Tính số ngày trung bình mỗi lần thuê theo thể loại
-- 5. Store nào có doanh thu cao hơn?
```

---

## 2. Kafka — Event Streaming

### Kiến trúc trong project

```
React UI
   │
   ▼
Spring Boot (Producer)
   │  POST /api/rentals            → topic: rental.created
   │  PUT  /api/rentals/:id/return → topic: rental.returned
   │  POST /api/payments           → topic: payment.processed
   ▼
Kafka Broker (kafka:29092)
   ▼
Django Consumer (Consumer Group: django-consumer-group)
   │
   ▼
PostgreSQL — dvdrental_activity (bảng activity_activitylog)
```

---

### 2.1 Xem topics và messages

```bash
# List tất cả topics đang có
./docker.sh kafka

# Đọc toàn bộ message từ đầu (--from-beginning)
docker exec dvdrental-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic rental.created \
  --from-beginning

# Chỉ xem message mới nhất (không --from-beginning = chờ message mới)
docker exec dvdrental-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic rental.created
```

### 2.2 Kiểm tra Consumer Group (LAG)

LAG = số message Kafka đã có nhưng consumer chưa đọc. **LAG=0 là tốt.**

```bash
# Xem tất cả consumer groups
docker exec dvdrental-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --list

# Xem chi tiết LAG của Django consumer
docker exec dvdrental-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group django-consumer-group
```

Output mẫu:
```
TOPIC            PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
rental.created   0          5               5               0    ← Django đã đọc hết
rental.returned  0          3               3               0
payment.processed 0         2               2               0
```

> Nếu Django đang down, LAG sẽ tăng dần. Khi Django khởi động lại, nó đọc lại từ offset cũ — **không mất message**.

### 2.3 Tự gửi message test

```bash
# Mở producer shell, gõ message rồi Enter
docker exec -it dvdrental-kafka kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic rental.created

# Gõ vào:
{"rentalId":999,"customerId":1,"filmTitle":"TEST FILM","staffId":1}
```

Sau đó kiểm tra Django đã nhận chưa:
```bash
# Xem log Django
./docker.sh logs django

# Xem trong DB
docker exec dvdrental-postgres psql -U postgres -d dvdrental_activity \
  -c "SELECT * FROM activity_activitylog ORDER BY created_at DESC LIMIT 3;"
```

### 2.4 Thực nghiệm: Django restart không mất message

```bash
# Bước 1 — Dừng Django
docker compose stop django

# Bước 2 — Tạo vài rental (Kafka giữ message, không ai đọc)
curl -s -X POST http://localhost:8080/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"customerId":10,"inventoryId":50,"staffId":1}'

# Kiểm tra LAG đang tăng
docker exec dvdrental-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group django-consumer-group
# → LAG > 0

# Bước 3 — Khởi động lại Django
docker compose start django

# Bước 4 — Django tự đọc lại từ offset cũ
./docker.sh logs django
# → thấy log xử lý các message bị missed

# Bước 5 — LAG về 0
docker exec dvdrental-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group django-consumer-group
```

### 2.5 Xem offset details

```bash
# Xem tất cả partition và offset của topic
docker exec dvdrental-kafka kafka-run-class.sh kafka.tools.GetOffsetShell \
  --bootstrap-server localhost:9092 \
  --topic rental.created
```

### Các khái niệm chính

| Khái niệm | Trong project | Ý nghĩa |
|---|---|---|
| **Topic** | `rental.created` | Kênh chứa message, persistent trên disk |
| **Producer** | Spring Boot `KafkaTemplate` | Gửi event sau mỗi giao dịch |
| **Consumer** | Django `confluent_kafka.Consumer` | Đọc và xử lý bất đồng bộ |
| **Group ID** | `django-consumer-group` | Kafka track offset riêng cho mỗi group |
| **Offset** | Số thứ tự message trong partition | Consumer commit offset sau khi xử lý xong |
| **LAG** | `LOG-END-OFFSET - CURRENT-OFFSET` | Bao nhiêu message chưa được xử lý |

---

## 3. Redis — Caching

### 3.1 Kết nối và xem cache

```bash
./docker.sh redis

# Xem tất cả key đang được cache
KEYS *

# Xem giá trị (JSON được serialize)
GET "film::1"

# Xem TTL còn lại (giây), -1 = không hết hạn, -2 = không tồn tại
TTL "film::1"

# Xem loại dữ liệu
TYPE "film::1"

# Đếm số key
DBSIZE
```

### 3.2 Thực nghiệm Cache Miss → Cache Hit

```bash
# Bước 1 — Xóa toàn bộ cache
./docker.sh redis
FLUSHALL

# Bước 2 — Gọi API lần đầu (cache MISS → query DB)
# Mở tab khác, xem log Spring Boot
./docker.sh logs springboot
# Gọi API:
curl http://localhost:8080/api/films/1
# → Log sẽ có: SELECT * FROM film WHERE film_id = 1

# Bước 3 — Gọi lại (cache HIT → không query DB)
curl http://localhost:8080/api/films/1
# → Log KHÔNG có SELECT nào

# Bước 4 — Kiểm tra key đã được tạo trong Redis
./docker.sh redis
KEYS *
# → thấy "film::1"
TTL "film::1"
# → còn ~598 giây (TTL = 10 phút)
```

### 3.3 Thực nghiệm Cache Eviction

Khi tạo rental mới, Spring Boot tự xóa cache `film:available` vì data đã thay đổi.

```bash
# Bước 1 — Load cache film:available
curl "http://localhost:8080/api/films/available?storeId=1"
./docker.sh redis
KEYS "film:available*"
# → thấy key "film:available::1"

# Bước 2 — Tạo rental (Spring sẽ @CacheEvict film:available)
curl -s -X POST http://localhost:8080/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"inventoryId":1,"staffId":1}'

# Bước 3 — Cache đã bị xóa
./docker.sh redis
KEYS "film:available*"
# → không còn key nào
```

### 3.4 Thực nghiệm TTL hết hạn

```bash
# Set TTL ngắn để test nhanh (trong redis-cli)
./docker.sh redis

# Xem TTL hiện tại của report cache
KEYS "report*"
TTL "report:top-films::10"

# Chờ TTL hết (hoặc set expire thủ công để test nhanh)
EXPIRE "report:top-films::10" 5    # hết hạn sau 5 giây
TTL "report:top-films::10"         # → 5
# Chờ 6 giây...
EXISTS "report:top-films::10"      # → 0 (đã bị xóa tự động)

# Gọi API lại → cache MISS → query DB → tạo cache mới
curl http://localhost:8080/api/reports/films/top?limit=10
```

### 3.5 Các cache key trong project

| Key pattern | Dữ liệu | TTL |
|---|---|---|
| `film::<id>` | Chi tiết 1 phim | 10 phút |
| `film:available::<storeId>` | Phim còn hàng theo store | 2 phút |
| `report:top-films::<limit>` | Top phim thuê nhiều | 30 phút |
| `report:revenue:monthly::<year>` | Doanh thu theo tháng | 30 phút |
| `report:category::SimpleKey []` | Thuê theo thể loại | 30 phút |
| `report:top-customers::<limit>` | Top khách hàng | 30 phút |

### Cache-Aside Pattern

```
Request đến
    │
    ▼
Cache hit? ──Yes──► Trả về từ Redis (< 1ms)
    │
   No
    │
    ▼
Query PostgreSQL (~10–50ms)
    │
    ▼
Lưu vào Redis với TTL
    │
    ▼
Trả kết quả về client
```

### Các annotation Spring Cache

| Annotation | Tác dụng | Ví dụ trong project |
|---|---|---|
| `@Cacheable` | Lần đầu query DB, sau đó lấy từ cache | `getFilmById`, `getTopFilms` |
| `@CacheEvict` | Xóa cache khi data thay đổi | `createRental`, `returnRental` |
| `allEntries=true` | Xóa toàn bộ key trong cache group | `film:available` sau khi tạo rental |

---

## 4. Thực hành end-to-end

### Luồng hoàn chỉnh: Tạo rental → Kafka → Django → DB → UI

```bash
# Bước 1 — Tạo rental (hoặc dùng UI tại /rentals)
curl -s -X POST http://localhost:8080/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"customerId":5,"inventoryId":10,"staffId":1}' | python -m json.tool

# Bước 2 — Xem event trong Kafka (xuất hiện ngay lập tức)
docker exec dvdrental-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic rental.created \
  --from-beginning

# Bước 3 — Xem Django đã xử lý chưa (LAG = 0 là done)
docker exec dvdrental-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group django-consumer-group

# Bước 4 — Xem ActivityLog trong DB
docker exec dvdrental-postgres psql -U postgres -d dvdrental_activity \
  -c "SELECT id, event_type, reference_id, created_at FROM activity_activitylog ORDER BY created_at DESC LIMIT 5;"

# Bước 5 — Xem trên UI: http://localhost:5173/activity
```

---

## 5. Cheat Sheet

```bash
# === Docker ===
./docker.sh run              # build + start tất cả
./docker.sh start            # start không rebuild
./docker.sh stop             # dừng containers
./docker.sh reset            # xóa sạch data, chạy lại từ đầu
./docker.sh logs springboot  # xem log realtime
./docker.sh rebuild react    # rebuild 1 service sau khi sửa code
./docker.sh ps               # trạng thái containers

# === PostgreSQL ===
./docker.sh db
\dt                          # list tables
\d film                      # describe table
\q                           # thoát

# === Redis ===
./docker.sh redis
KEYS *                       # list tất cả keys
TTL <key>                    # xem thời gian hết hạn
FLUSHALL                     # xóa toàn bộ cache

# === Kafka ===
./docker.sh kafka            # list topics

docker exec dvdrental-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group django-consumer-group   # xem LAG

docker exec -it dvdrental-kafka kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic rental.created   # gửi message thủ công

# === Spring Boot API ===
curl http://localhost:8080/api/films?page=0&size=5
curl http://localhost:8080/api/films/1
curl http://localhost:8080/api/films/1/cast
curl "http://localhost:8080/api/films/available?storeId=1"
curl "http://localhost:8080/api/films/search?q=love"
curl http://localhost:8080/api/customers/1/stats
curl http://localhost:8080/api/customers/1/rentals
curl http://localhost:8080/api/rentals/active
curl http://localhost:8080/api/rentals/overdue
curl http://localhost:8080/api/reports/films/top?limit=10
curl http://localhost:8080/actuator/health

# === Django API ===
curl http://localhost:8000/api/activity-log
curl "http://localhost:8000/api/activity-log?page=1&page_size=10"
```
