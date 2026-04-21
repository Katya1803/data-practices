# DVDRental — Hướng dẫn học SQL · Kafka · Redis

## Khởi động

```bash
./docker.sh run       # lần đầu hoặc sau khi sửa code
./docker.sh start     # lần sau (nhanh hơn, không rebuild)
```

Mở browser: **http://localhost:5173**

### Các lệnh thường dùng

```bash
./docker.sh stop               # dừng tất cả (giữ data)
./docker.sh reset              # xóa sạch data, restore lại từ đầu
./docker.sh restart springboot # restart 1 service
./docker.sh rebuild django     # rebuild + restart 1 service
./docker.sh logs springboot    # xem log realtime
./docker.sh ps                 # kiểm tra trạng thái các container
```

### Truy cập trực tiếp

```bash
./docker.sh db      # psql shell → dvdrental
./docker.sh redis   # redis-cli
./docker.sh kafka   # list kafka topics
```

---

## 1. SQL — PostgreSQL

### Kết nối database

```bash
./docker.sh db
# hoặc
docker exec -it dvdrental-postgres psql -U postgres -d dvdrental
```

### Sơ đồ các bảng chính

```
customer ──< rental >── inventory >── film >── film_category >── category
                │
             payment
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

PostgreSQL có kiểu dữ liệu riêng cho tìm kiếm văn bản:

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
JOIN customer  c ON c.customer_id  = r.customer_id
JOIN inventory i ON i.inventory_id = r.inventory_id
JOIN film      f ON f.film_id      = i.film_id
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
   │  POST /api/rentals          → topic: rental.created
   │  PUT  /api/rentals/:id/return → topic: rental.returned
   │  POST /api/payments         → topic: payment.processed
   ▼
Kafka (broker)
   ▼
Django Consumer (Consumer)
   │
   ▼
PostgreSQL (dvdrental_activity)
```

### Quan sát Kafka trực tiếp

```bash
# List tất cả topics
./docker.sh kafka

# Đọc toàn bộ message trong topic từ đầu
docker exec dvdrental-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic rental.created \
  --from-beginning

# Xem lag của consumer group (Django đang đọc đến đâu)
docker exec dvdrental-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group django-consumer-group
```

### Tự gửi message test

```bash
docker exec -it dvdrental-kafka kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic rental.created
# Gõ JSON rồi Enter:
{"rentalId":999,"customerId":1,"filmTitle":"TEST FILM"}
```

### Các khái niệm chính

| Khái niệm | Trong project | Ý nghĩa |
|---|---|---|
| **Topic** | `rental.created` | Kênh chứa message, giống queue nhưng persistent |
| **Producer** | Spring Boot `KafkaTemplate` | Gửi event sau mỗi giao dịch |
| **Consumer** | Django `confluent_kafka.Consumer` | Đọc event, xử lý bất đồng bộ |
| **Group ID** | `django-consumer-group` | Kafka track từng group đọc đến offset nào |
| **Offset** | Số thứ tự message | Consumer lưu offset → restart không mất message |
| **LAG** | Current offset - Consumer offset | LAG=0 nghĩa là consumer đang theo kịp |

### Tại sao dùng Kafka thay vì gọi thẳng Django?

- Spring Boot không cần biết Django có tồn tại không
- Django restart → Kafka giữ message, Django đọc lại khi online
- Dễ thêm consumer khác (analytics, notification...) mà không sửa producer

---

## 3. Redis — Caching

### Kết nối Redis CLI

```bash
./docker.sh redis

# Xem tất cả key đang cache
KEYS *

# Xem TTL còn lại (giây)
TTL "film::1"

# Xóa cache thủ công để test cache miss
DEL "film::1"
FLUSHALL
```

### Các cache key trong project

| Key pattern | Dữ liệu | TTL |
|---|---|---|
| `film::<id>` | Chi tiết 1 phim | 10 phút |
| `film:available::<storeId>` | Danh sách phim còn hàng | 2 phút |
| `report:top-films::<limit>` | Top phim thuê nhiều | 30 phút |
| `report:revenue:monthly::<year>` | Doanh thu theo tháng | 30 phút |
| `report:category::SimpleKey []` | Thuê theo thể loại | 30 phút |
| `report:top-customers::<limit>` | Top khách hàng | 30 phút |

### Thực nghiệm cache

```bash
# 1. Xóa cache rồi gọi API → Spring Boot query DB (log có SELECT)
./docker.sh redis        # trong redis-cli: FLUSHALL
curl http://localhost:8080/api/films/1
./docker.sh logs springboot   # quan sát SQL query

# 2. Gọi lại → cache hit, không có DB query
curl http://localhost:8080/api/films/1

# 3. Tạo rental → Spring tự evict cache film:available
curl -X POST http://localhost:8080/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"inventoryId":1,"staffId":1}'

# 4. Kiểm tra key đã bị xóa
./docker.sh redis        # KEYS film:available*
```

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

| Annotation | Dùng ở đâu | Tác dụng |
|---|---|---|
| `@Cacheable` | `getFilmById`, `getTopFilms`... | Lần đầu query DB, các lần sau lấy từ cache |
| `@CacheEvict` | `createRental`, `returnRental` | Xóa cache khi dữ liệu thay đổi |
| `allEntries=true` | `film:available` | Xóa toàn bộ key trong cache group |

---

## 4. Thực hành end-to-end

### Tạo rental và quan sát toàn bộ luồng

```bash
# Bước 1 — Tạo rental qua UI hoặc curl
curl -X POST http://localhost:8080/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"customerId":5,"inventoryId":10,"staffId":1}'

# Bước 2 — Xem event vừa vào Kafka
docker exec dvdrental-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic rental.created --from-beginning

# Bước 3 — Kiểm tra Django đã consume chưa (LAG phải = 0)
docker exec dvdrental-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group django-consumer-group

# Bước 4 — Xem ActivityLog trong DB
docker exec dvdrental-postgres psql -U postgres -d dvdrental_activity \
  -c "SELECT * FROM activity_activitylog ORDER BY created_at DESC LIMIT 5;"

# Bước 5 — Xem trên UI: http://localhost:5173/activity
```

### Kiểm tra cache hoạt động

```bash
# Cache MISS — lần đầu, Spring Boot log có SELECT
curl http://localhost:8080/api/reports/films/top?limit=5
./docker.sh logs springboot

# Cache HIT — lần 2, không có log DB query
curl http://localhost:8080/api/reports/films/top?limit=5

# Verify trong Redis
./docker.sh redis
# KEYS report*
# TTL "report:top-films::5"
```

---

## 5. Cheat Sheet

```bash
# === Docker ===
./docker.sh run              # build + start tất cả
./docker.sh stop             # dừng
./docker.sh reset            # xóa sạch, chạy lại từ đầu
./docker.sh logs springboot  # xem log Spring Boot
./docker.sh rebuild react    # rebuild + restart React sau khi sửa UI
./docker.sh ps               # trạng thái containers

# === PostgreSQL ===
./docker.sh db               # vào psql shell
\dt                          # list tables
\d film                      # describe table film
\q                           # thoát

# === Redis ===
./docker.sh redis
KEYS *
DBSIZE
FLUSHALL

# === Kafka ===
./docker.sh kafka            # list topics
docker exec dvdrental-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --list

# === Spring Boot API ===
curl http://localhost:8080/api/films?page=0&size=5
curl http://localhost:8080/api/customers/1/stats
curl http://localhost:8080/api/rentals/active
curl http://localhost:8080/api/reports/films/top?limit=10
curl http://localhost:8080/actuator/health

# === Django API ===
curl http://localhost:8000/api/activity-log
curl http://localhost:8000/api/activity-log/recent
```
