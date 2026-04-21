# Học SQL · Kafka · Redis qua DVDRental

File này dùng **code thật trong project** để giải thích từng khái niệm.  
Không có ví dụ giả — mọi đoạn code đều đang chạy trong container của bạn.

---

## Mục lục

1. [Kết nối DBeaver](#1-kết-nối-dbeaver)
2. [SQL](#2-sql)
3. [Kafka](#3-kafka)
4. [Offset Explorer — Kafka GUI](#4-offset-explorer--kafka-gui)
5. [Redis](#5-redis)
6. [Another Redis Desktop Manager — Redis GUI](#6-another-redis-desktop-manager--redis-gui)

---

## 1. Kết nối DBeaver

DBeaver là GUI miễn phí để kết nối database, chạy SQL và xem schema trực quan.  
Tải tại: https://dbeaver.io/download/ (chọn **Community Edition**)

---

### 1.1 Tạo kết nối PostgreSQL

> Đảm bảo project đang chạy (`./docker.sh start`) trước khi kết nối.

**Bước 1** — Mở DBeaver, bấm nút **New Database Connection** (icon phích cắm + dấu cộng, góc trên trái).

**Bước 2** — Chọn **PostgreSQL** → bấm **Next**.

**Bước 3** — Điền thông tin kết nối:

| Trường | Giá trị |
|--------|---------|
| Host | `localhost` |
| Port | `5433` |
| Database | `dvdrental` |
| Username | `postgres` |
| Password | `postgres` |

> Port là **5433** (không phải 5432 mặc định) vì container map `5433 → 5432` trong `docker-compose.yml`.

**Bước 4** — Bấm **Test Connection**. Lần đầu DBeaver sẽ hỏi tải driver PostgreSQL → bấm **Download**.

**Bước 5** — Thấy thông báo "Connected" → bấm **Finish**.

---

### 1.2 Khám phá Schema

Sau khi kết nối, mở rộng cây bên trái:

```
dvdrental
└── Schemas
    └── public
        └── Tables
            ├── actor
            ├── category
            ├── customer
            ├── film
            ├── film_actor
            ├── film_category
            ├── inventory
            ├── payment
            ├── rental
            ├── staff
            └── store
```

**Xem cấu trúc bảng:** Click phải vào bảng → **View Table** → tab **Columns** hiện tên cột, kiểu dữ liệu, nullable.

**Xem dữ liệu mẫu:** Click phải → **View Data** → DBeaver tự chạy `SELECT * FROM ... LIMIT 200`.

**Xem quan hệ (ERD):** Click phải vào schema `public` → **View Diagram** — DBeaver vẽ sơ đồ quan hệ toàn bộ database.

---

### 1.3 Chạy SQL trong DBeaver

**Mở SQL Editor:** Bấm **SQL Editor** trên toolbar (hoặc `F3`) → chọn kết nối `dvdrental`.

Paste thử câu query sau rồi bấm **Execute** (`Ctrl+Enter` hoặc nút tam giác):

```sql
SELECT f.title, COUNT(r.rental_id) AS total_rentals
FROM film f
JOIN inventory i ON i.film_id = f.film_id
JOIN rental r    ON r.inventory_id = i.inventory_id
GROUP BY f.film_id, f.title
ORDER BY total_rentals DESC
LIMIT 10;
```

Kết quả hiện ngay phía dưới. Có thể export ra CSV bằng cách click phải vào kết quả → **Export**.

**Phím tắt hay dùng:**

| Phím | Tác dụng |
|------|---------|
| `Ctrl+Enter` | Chạy câu query tại con trỏ |
| `Ctrl+Shift+Enter` | Chạy toàn bộ script |
| `Ctrl+Space` | Gợi ý tên bảng / cột (autocomplete) |
| `Ctrl+/` | Comment / uncomment dòng |
| `Ctrl+Shift+F` | Format (indent) SQL tự động |
| `F3` | Mở SQL Editor mới |
| `Alt+X` | Explain (xem query plan) |

---

### 1.4 Xem Query Execution Plan

Query plan cho biết PostgreSQL **sẽ làm gì** để chạy câu SQL — hữu ích khi query chậm.

Trong SQL Editor, bấm **Explain** (hoặc `Alt+X`):

```sql
EXPLAIN ANALYZE
SELECT f.title, COUNT(r.rental_id) AS total_rentals
FROM film f
JOIN inventory i ON i.film_id = f.film_id
JOIN rental r    ON r.inventory_id = i.inventory_id
GROUP BY f.film_id, f.title
ORDER BY total_rentals DESC
LIMIT 10;
```

DBeaver hiện kết quả dạng bảng. Các cột cần chú ý:

| Cột | Ý nghĩa |
|-----|---------|
| **Node Type** | Loại thao tác: `Seq Scan` (scan toàn bảng), `Index Scan` (dùng index), `Hash Join` (join bằng hash) |
| **Actual Rows** | Số hàng thực tế được xử lý |
| **Actual Total Time** | Thời gian thực tế (ms) |
| **Cost** | Chi phí ước tính (đơn vị tương đối, không phải ms) |

`Seq Scan` trên bảng lớn + cost cao = cần xem xét thêm index.

---

### 1.5 Tips DBeaver hữu ích

**Xem foreign key:** Trong tab **Columns** của một bảng, cột có icon chìa khóa = FK. Hover để thấy trỏ đến bảng nào.

**Navigate giữa các bảng:** Trong kết quả query, `Ctrl+Click` vào một giá trị ID → DBeaver tự mở bảng liên quan và highlight dòng đó (nếu có FK).

**Lưu query hay dùng:** `File → Save As` trong SQL Editor → lưu thành file `.sql` để dùng lại.

**Mở nhiều editor:** `F3` mỗi lần mở một tab SQL mới — có thể chạy song song nhiều query.

**Filter nhanh trong kết quả:** Click vào cột header → gõ giá trị → DBeaver filter ngay mà không cần chạy lại SQL.

---

## 2. SQL

### Schema cần biết trước

```
customer ──< rental >── inventory >── film
                                       │
payment ──────────────────────────── rental
film ──< film_category >── category
film ──< film_actor    >── actor
```

Mở psql để thực hành trực tiếp:

```bash
./docker.sh db
```

---

### 1.1 JOIN — Lấy phim đang có hàng tại Store 1

File: `springboot/.../repository/FilmRepository.java`

```sql
SELECT DISTINCT f.*
FROM film f
JOIN inventory i ON i.film_id = f.film_id
WHERE i.store_id = 1
  AND i.inventory_id NOT IN (
      SELECT r.inventory_id FROM rental r WHERE r.return_date IS NULL
  );
```

**Đọc từng bước:**
- `film JOIN inventory` → một phim có nhiều bản vật lý (inventory), nối để biết bản nào ở store nào
- `NOT IN (SELECT ...)` → loại bỏ các bản đang được thuê (`return_date IS NULL` = chưa trả)
- `DISTINCT` → một phim có nhiều bản, không muốn trùng hàng

**Thử biến thể:**
```sql
-- Đếm số bản còn lại mỗi phim tại Store 1
SELECT f.title, COUNT(i.inventory_id) AS copies_available
FROM film f
JOIN inventory i ON i.film_id = f.film_id
WHERE i.store_id = 1
  AND i.inventory_id NOT IN (
      SELECT inventory_id FROM rental WHERE return_date IS NULL
  )
GROUP BY f.film_id, f.title
ORDER BY copies_available DESC;
```

---

### 1.2 GROUP BY + Aggregate — Top phim được thuê nhiều nhất

File: `springboot/.../repository/ReportRepository.java`

```sql
SELECT f.film_id, f.title, COUNT(r.rental_id) AS rental_count
FROM film f
JOIN inventory i ON i.film_id = f.film_id
JOIN rental r    ON r.inventory_id = i.inventory_id
GROUP BY f.film_id, f.title
ORDER BY rental_count DESC
LIMIT 10;
```

**Cách đọc:** Đếm số lượt thuê (`COUNT`) của từng phim. Mỗi `rental` là một lượt thuê cụ thể của một bản vật lý — phải đi qua `inventory` để về `film`.

**Thử biến thể:**
```sql
-- Doanh thu trung bình mỗi phim
SELECT f.title,
       COUNT(r.rental_id)  AS total_rentals,
       SUM(p.amount)       AS total_revenue,
       AVG(p.amount)       AS avg_payment
FROM film f
JOIN inventory i ON i.film_id   = f.film_id
JOIN rental r    ON r.inventory_id = i.inventory_id
JOIN payment p   ON p.rental_id = r.rental_id
GROUP BY f.film_id, f.title
ORDER BY total_revenue DESC
LIMIT 10;
```

---

### 1.3 DATE_PART — Doanh thu theo tháng

File: `springboot/.../repository/ReportRepository.java`

```sql
SELECT
    DATE_PART('month', payment_date) AS month,
    SUM(amount)                       AS revenue,
    COUNT(*)                          AS transaction_count
FROM payment
WHERE DATE_PART('year', payment_date) = 2007
GROUP BY month
ORDER BY month;
```

**`DATE_PART(field, date)`** trích xuất một phần của timestamp.  
Kết quả trả về `month=2, revenue=8351.84, transaction_count=2016` — đây đúng là `MonthlyRevenueDto` mà trang Reports hiển thị.

**Thử biến thể:**
```sql
-- Doanh thu theo quý
SELECT
    DATE_PART('quarter', payment_date) AS quarter,
    SUM(amount)                         AS revenue
FROM payment
WHERE DATE_PART('year', payment_date) = 2007
GROUP BY quarter
ORDER BY quarter;
```

---

### 1.4 Window Function — Xếp hạng khách hàng

File: `springboot/.../repository/ReportRepository.java`

```sql
SELECT
    c.customer_id,
    c.first_name || ' ' || c.last_name       AS name,
    SUM(p.amount)                             AS total_spent,
    COUNT(DISTINCT r.rental_id)               AS total_rentals,
    RANK() OVER (ORDER BY SUM(p.amount) DESC) AS rank
FROM customer c
JOIN payment p ON p.customer_id = c.customer_id
JOIN rental  r ON r.customer_id = c.customer_id
GROUP BY c.customer_id, name
ORDER BY total_spent DESC
LIMIT 10;
```

**`RANK() OVER (...)`** là window function — nó tính thứ hạng mà **không** thu gọn hàng như `GROUP BY`.  
- `GROUP BY` thu gọn nhiều hàng thành 1
- `OVER (ORDER BY ...)` tính thêm một cột mới trên tập kết quả đã có

**Thử biến thể:**
```sql
-- DENSE_RANK vs RANK: khi 2 người bằng điểm, RANK bỏ qua số tiếp theo, DENSE_RANK thì không
SELECT name, total_spent,
       RANK()       OVER (ORDER BY total_spent DESC) AS rank,
       DENSE_RANK() OVER (ORDER BY total_spent DESC) AS dense_rank
FROM (
    SELECT c.first_name || ' ' || c.last_name AS name, SUM(p.amount) AS total_spent
    FROM customer c JOIN payment p ON p.customer_id = c.customer_id
    GROUP BY c.customer_id
) t
LIMIT 20;
```

---

### 1.5 Full-text Search — Tìm phim theo từ khóa

File: `springboot/.../repository/FilmRepository.java`

```sql
SELECT f.*
FROM film f
WHERE to_tsvector('english', f.title || ' ' || COALESCE(f.description, ''))
      @@ plainto_tsquery('english', 'dinosaur');
```

**Khái niệm:**
- `to_tsvector(...)` — chuyển chuỗi thành danh sách từ gốc (token), bỏ stop words ("the", "a"...)
- `plainto_tsquery('english', 'dinosaur')` — chuyển từ khóa tìm kiếm thành query token
- `@@` — toán tử khớp: "tsvector có khớp với tsquery không?"

Khác với `LIKE '%dinosaur%'`: full-text search hiểu "run" = "running" = "ran"; tốc độ nhanh hơn khi có index.

**Thử:**
```sql
-- So sánh kết quả LIKE vs full-text
SELECT title FROM film WHERE title ILIKE '%love%';
SELECT title FROM film
WHERE to_tsvector('english', title) @@ plainto_tsquery('english', 'love');
```

---

### 1.6 Phát hiện thuê quá hạn (Interval Arithmetic)

File: `springboot/.../repository/RentalRepository.java`

```sql
SELECT r.*
FROM rental r
JOIN inventory i ON i.inventory_id = r.inventory_id
JOIN film f      ON f.film_id = i.film_id
WHERE r.return_date IS NULL
  AND r.rental_date + CAST((f.rental_duration || ' days') AS interval) < NOW();
```

**`rental_duration`** là số ngày được phép thuê lưu trong bảng `film`.  
`rental_date + interval '3 days'` = hạn trả. Nếu hạn trả < NOW() → quá hạn.

```sql
-- Thử ngay: xem các rental còn đang mượn, kèm hạn trả và số ngày trễ
SELECT
    r.rental_id,
    f.title,
    r.rental_date,
    r.rental_date + (f.rental_duration || ' days')::interval AS due_date,
    EXTRACT(day FROM NOW() - (r.rental_date + (f.rental_duration || ' days')::interval)) AS days_overdue
FROM rental r
JOIN inventory i ON i.inventory_id = r.inventory_id
JOIN film f      ON f.film_id = i.film_id
WHERE r.return_date IS NULL
  AND r.rental_date + (f.rental_duration || ' days')::interval < NOW()
ORDER BY days_overdue DESC;
```

---

## 3. Kafka

### Kafka là gì?

Kafka là một **message broker** — một dịch vụ trung gian nhận tin nhắn từ nơi này và chuyển đến nơi khác, **không đồng bộ**.

Dùng **hàng đợi bình thường** (ví dụ Redis Queue) cũng được, nhưng Kafka thêm:
- **Lưu trữ vĩnh viễn** — tin nhắn không mất sau khi consumer đọc xong
- **Nhiều consumer cùng đọc** một topic độc lập nhau
- **Replay** — consumer mới có thể đọc lại từ đầu

### Các khái niệm cốt lõi

| Khái niệm | Là gì | Trong project này |
|-----------|-------|-------------------|
| **Topic** | Kênh phân loại tin nhắn | `rental.created`, `rental.returned`, `payment.processed` |
| **Producer** | Bên gửi tin | Spring Boot (Java) |
| **Consumer** | Bên nhận tin | Django (Python) |
| **Message/Event** | Nội dung tin nhắn | JSON: `{rentalId, customerId, filmId, ...}` |
| **Broker** | Server Kafka | Container `dvdrental-kafka` |
| **Partition** | Phân vùng của topic (song song hóa) | Mỗi topic có 1 partition trong project này |
| **Offset** | Vị trí của message trong partition | Consumer nhớ đã đọc đến đâu |
| **Consumer Group** | Nhóm consumer, mỗi partition chỉ giao cho 1 consumer trong group | `dvdrental-group` |

---

### 2.1 Flow trong project

```
User tạo rental trên UI
        │
        ▼
POST /api/rentals  (Spring Boot)
        │
        ├─► Lưu vào PostgreSQL (rental table)
        │
        └─► Gửi Kafka event: topic "rental.created"
                    │
                    ▼
            Kafka Broker (container)
                    │
                    ▼
            Django consumer (Python thread)
                    │
                    └─► Lưu vào PostgreSQL (activity_log table)
                              │
                              ▼
                    Hiển thị trang Activity (React)
```

**Tại sao không ghi thẳng vào activity_log từ Spring Boot?**  
Vì Spring Boot và Django là 2 service độc lập — chúng không share database session. Kafka là "hợp đồng" giữa chúng: Spring Boot chỉ cần biết "tôi gửi event lên topic này", Django chỉ cần biết "tôi đọc topic này". Hai bên không phụ thuộc nhau.

---

### 2.2 Producer (Spring Boot gửi event)

**Khai báo topic** — `springboot/.../config/KafkaConfig.java`:
```java
public static final String TOPIC_RENTAL_CREATED = "rental.created";

@Bean
public NewTopic rentalCreatedTopic() {
    return TopicBuilder.name(TOPIC_RENTAL_CREATED).partitions(1).replicas(1).build();
}
```

**Gửi event** — `springboot/.../kafka/producer/EventProducer.java`:
```java
public void sendRentalCreated(RentalCreatedEvent event) {
    kafkaTemplate.send(
        KafkaConfig.TOPIC_RENTAL_CREATED,   // topic
        String.valueOf(event.getRentalId()), // key (dùng để route vào partition)
        event                               // value (sẽ được serialize thành JSON)
    );
}
```

**Nơi gọi** — `springboot/.../service/RentalService.java`, sau khi lưu DB:
```java
rental = rentalRepository.save(rental);  // 1. Lưu PostgreSQL trước

eventProducer.sendRentalCreated(new RentalCreatedEvent(  // 2. Rồi mới gửi Kafka
    rental.getRentalId(),
    customer.getCustomerId(),
    inventory.getFilm().getFilmId(),
    inventory.getInventoryId(),
    rental.getRentalDate()
));
```

**Event payload** trông như thế này khi lên Kafka:
```json
{
  "rentalId": 16053,
  "customerId": 42,
  "filmId": 103,
  "inventoryId": 500,
  "rentalDate": "2026-04-21T10:30:00"
}
```

---

### 2.3 Consumer (Django nhận event)

File: `django-consumer/activity/consumers/kafka_consumer.py`

```python
def run_consumer():
    consumer = Consumer({
        'bootstrap.servers': settings.KAFKA_BOOTSTRAP_SERVERS,
        'group.id': settings.KAFKA_GROUP_ID,   # "dvdrental-group"
        'auto.offset.reset': 'earliest',        # đọc từ đầu nếu chưa có offset
    })
    consumer.subscribe(settings.KAFKA_TOPICS)  # ['rental.created', 'rental.returned', 'payment.processed']

    while True:
        msg = consumer.poll(1.0)               # chờ tối đa 1 giây
        if msg is None:
            continue
        data = json.loads(msg.value())
        handle_event(msg.topic(), data)        # lưu vào ActivityLog

def handle_event(topic, data):
    ActivityLog.objects.create(
        event_type=topic,           # "rental.created"
        customer_id=data.get('customerId'),
        film_id=data.get('filmId'),
        rental_id=data.get('rentalId'),
        payload=data,               # toàn bộ JSON lưu vào cột JSONField
    )
```

Consumer chạy trong **background thread** ngay khi Django khởi động — nó loop vô tận, cứ có message mới là xử lý.

---

### 2.4 Quan sát Kafka thực tế

**Xem các topic đang có:**
```bash
./docker.sh kafka
# hoặc
docker exec dvdrental-kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

**Đọc tất cả message trong topic `rental.created`:**
```bash
docker exec dvdrental-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic rental.created \
  --from-beginning
```

**Mở tab khác, tạo một rental trên UI** (`http://localhost:5173/rentals`), rồi quay lại terminal — bạn sẽ thấy JSON message xuất hiện ngay lập tức.

**Xem consumer group đã đọc đến offset nào:**
```bash
docker exec dvdrental-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group dvdrental-group
```

---

### 2.5 Offset và "at-least-once delivery"

Kafka không xóa message sau khi consumer đọc. Thay vào đó, consumer **commit offset** — ghi nhớ "tôi đã đọc đến message số X".

Nếu Django crash giữa chừng trước khi commit offset → khi restart, nó đọc lại từ offset cũ → message có thể được xử lý 2 lần.

Đây là **at-least-once delivery** — message được xử lý ít nhất 1 lần, có thể nhiều hơn. Project này chấp nhận điều đó (ghi duplicate vào activity_log không ảnh hưởng nghiêm trọng).

---

## 4. Offset Explorer — Kafka GUI

Offset Explorer (trước đây tên Kafka Tool) cho phép xem toàn bộ Kafka bằng giao diện đồ họa thay vì dòng lệnh.  
Tải tại: https://www.kafkatool.com/download.html (chọn bản phù hợp Windows)

---

### 4.1 Kết nối vào Kafka của project

> Đảm bảo project đang chạy (`./docker.sh start`) trước khi mở Offset Explorer.

**Bước 1** — Mở Offset Explorer → bấm **Add new connection** (icon `+` góc trên trái).

**Bước 2** — Tab **Properties**, điền:

| Trường | Giá trị |
|--------|---------|
| Cluster name | `dvdrental` (đặt tên tuỳ ý) |
| Kafka Cluster Type | `Standard` |
| Bootstrap servers | `localhost:9092` |

**Bước 3** — Tab **Advanced** → bỏ trống (không cần security).

**Bước 4** — Bấm **Test** → thấy "Connected successfully" → **Add**.

---

### 4.2 Khám phá Topics

Sau khi kết nối, mở rộng cây bên trái:

```
dvdrental
└── Topics
    ├── rental.created
    ├── rental.returned
    └── payment.processed
```

**Xem messages:** Click vào topic `rental.created` → tab **Messages** → bấm nút tìm kiếm (kính lúp).  
Mỗi hàng là một message, cột quan trọng:

| Cột | Ý nghĩa |
|-----|---------|
| **Offset** | Vị trí message trong partition (tăng dần, không bao giờ giảm) |
| **Key** | `rentalId` — dùng để quyết định message vào partition nào |
| **Value** | JSON payload: `{rentalId, customerId, filmId, ...}` |
| **Timestamp** | Thời điểm message được ghi vào Kafka |

**Thử thực hành:** Mở trang `http://localhost:5173/rentals` → tạo một rental → quay lại Offset Explorer → bấm refresh → thấy message mới xuất hiện ở cuối danh sách.

---

### 4.3 Xem Consumer Group và Lag

**Consumer Group** cho biết Django đã đọc đến đâu.

Trong cây bên trái: **Consumers** → `dvdrental-group` → chọn topic.

| Cột | Ý nghĩa |
|-----|---------|
| **Current Offset** | Offset Django đã đọc và commit |
| **Log End Offset** | Offset mới nhất trong topic |
| **Lag** | `Log End - Current` — số message chưa được xử lý |

**Lag = 0** → Django đã xử lý hết, hoạt động bình thường.  
**Lag > 0** → Django đang chậm hơn producer, hoặc Django đang down.

**Thử thực hành:** Dừng Django (`./docker.sh stop` rồi chỉ start lại springboot + react), tạo vài rental → quan sát Lag tăng dần. Start lại Django → Lag về 0.

---

### 4.4 Đọc lại message từ offset cũ

Offset Explorer cho phép đọc message từ bất kỳ offset nào — hữu ích khi muốn "replay" event cũ.

Tab **Messages** → ô **Offset** → nhập offset muốn đọc từ → bấm tìm kiếm.  
Đây là tính năng mà queue thông thường (như Redis Queue) không có: message không mất sau khi consumer đọc.

---

## 5. Redis

### Redis là gì?

Redis là một **in-memory key-value store** — lưu dữ liệu trong RAM thay vì đĩa cứng, nên đọc/ghi cực nhanh (micro-giây vs mili-giây của PostgreSQL).

### Redis là gì?

Redis là một **in-memory key-value store** — lưu dữ liệu trong RAM thay vì đĩa cứng, nên đọc/ghi cực nhanh (micro-giây vs mili-giây của PostgreSQL).

Trong project này Redis đóng vai trò **cache** — lưu kết quả query đắt tiền để lần sau không phải chạy lại SQL.

---

### 5.1 Vấn đề Redis giải quyết

Query "top phim được thuê nhiều nhất" phải scan toàn bộ bảng `rental` (hàng chục nghìn dòng), JOIN nhiều bảng, rồi GROUP BY. Chạy mỗi lần tốn ~50ms.

Nếu 1000 user cùng vào trang Reports → 1000 query giống hệt nhau → database quá tải.

**Giải pháp:** Chạy query 1 lần, lưu kết quả vào Redis với thời hạn 30 phút. 999 request sau đọc từ Redis (~0.1ms), không đụng đến PostgreSQL.

---

### 5.2 Cache-aside Pattern (đang dùng trong project)

```
Request đến
    │
    ▼
Kiểm tra Redis:
    ├─ HIT  → trả về ngay (không chạy SQL)
    └─ MISS → chạy SQL → lưu kết quả vào Redis → trả về
```

Spring Boot implement pattern này qua annotation, không cần code thủ công:

File: `springboot/.../service/FilmService.java`
```java
@Cacheable(value = "film", key = "#id")   // value = tên cache, key = Redis key
public FilmDetailDto getFilmById(int id) {
    // Đoạn này CHỈ chạy khi cache miss
    Film film = filmRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Film not found: " + id));
    return new FilmDetailDto(film);
}
```

Spring tự động:
1. Trước khi vào method: kiểm tra Redis key `film::1`
2. Nếu có → deserialize JSON → trả về, **bỏ qua method hoàn toàn**
3. Nếu không có → chạy method → lưu kết quả vào `film::1` → trả về

---

### 5.3 Cache Eviction — Xóa cache khi dữ liệu thay đổi

Khi khách thuê phim, danh sách "phim còn hàng" thay đổi. Cache cũ sẽ sai → phải xóa.

File: `springboot/.../service/RentalService.java`
```java
@CacheEvict(value = "film:available", allEntries = true)  // xóa toàn bộ cache "film:available"
public RentalDto createRental(RentalRequest request) {
    // ... lưu rental vào DB ...
    // Sau khi method kết thúc, Spring tự xóa cache
}
```

`allEntries = true` vì không biết store nào bị ảnh hưởng — xóa hết cho chắc. Lần request tiếp theo sẽ cache lại.

---

### 5.4 TTL — Thời gian sống của cache

File: `springboot/.../config/CacheConfig.java`
```java
Map<String, RedisCacheConfiguration> perCacheConfig = Map.of(
    "film",                defaultConfig.entryTtl(Duration.ofMinutes(10)),  // chi tiết phim: 10 phút
    "film:available",      defaultConfig.entryTtl(Duration.ofMinutes(2)),   // tồn kho: 2 phút (thay đổi thường)
    "report:top-films",    defaultConfig.entryTtl(Duration.ofMinutes(30)),  // báo cáo: 30 phút
    "report:top-customers",defaultConfig.entryTtl(Duration.ofMinutes(15)),
    "report:category",     defaultConfig.entryTtl(Duration.ofMinutes(5))
);
```

TTL ngắn hơn = dữ liệu tươi hơn, nhưng hit rate thấp hơn.  
TTL dài hơn = ít query DB hơn, nhưng có thể hiển thị dữ liệu cũ.

---

### 5.5 Quan sát Redis thực tế (CLI)

**Mở Redis CLI:**
```bash
./docker.sh redis
# hoặc
docker exec -it dvdrental-redis redis-cli
```

**Các lệnh cơ bản:**
```
# Xem tất cả key đang có
KEYS *

# Xem key theo pattern
KEYS film::*
KEYS report:*

# Xem TTL còn lại (giây) của một key
TTL film::1

# Xem nội dung của key (JSON đã serialize)
GET film::1

# Xóa một key thủ công
DEL film::1

# Đếm tổng số key
DBSIZE
```

**Thử nghiệm cache hit/miss:**
1. Mở `http://localhost:5173/cache` (trang Cache Inspector)
2. Nhập Film ID `1`, bấm **Probe** → thấy `source: database` (cache miss)
3. Bấm **Probe** lần 2 → thấy `source: cache` + TTL còn lại
4. Bấm **Evict** → xóa key khỏi Redis
5. Bấm **Probe** lần 3 → lại thấy `source: database`

---

### 5.6 Key format trong project

Spring Data Redis lưu key theo dạng `<cacheName>::<key>`:

| Redis Key | Được tạo khi | TTL |
|-----------|-------------|-----|
| `film::1` | GET `/api/films/1` | 10 phút |
| `film::42` | GET `/api/films/42` | 10 phút |
| `film:available::1` | GET `/api/films/available?storeId=1` | 2 phút |
| `film:available::2` | GET `/api/films/available?storeId=2` | 2 phút |
| `report:top-films::10` | GET `/api/reports/films/top?limit=10` | 30 phút |
| `report:revenue:monthly::2007` | GET `/api/reports/revenue/monthly?year=2007` | 30 phút |

---

### 5.7 Redis vs PostgreSQL — khi nào dùng gì?

| | PostgreSQL | Redis |
|--|-----------|-------|
| Lưu trữ | Đĩa cứng (bền) | RAM (mất khi restart nếu không config persistence) |
| Tốc độ đọc | ~1-50ms | ~0.1ms |
| Truy vấn phức tạp | JOIN, GROUP BY, Window Function | Không hỗ trợ |
| Dữ liệu chính | ✅ Luôn dùng | ❌ Không phù hợp |
| Kết quả query đắt | ❌ Chạy lại mỗi lần | ✅ Cache lại |
| Session / rate limit | ❌ | ✅ |

---

## Luồng đầy đủ khi tạo một Rental

Đây là tổng hợp tất cả 3 công nghệ hoạt động cùng nhau:

```
1. User bấm "Create Rental" trên UI
        │
        ▼
2. POST /api/rentals  →  RentalService.createRental()
        │
        ├─ 3. SQL INSERT INTO rental ...  (PostgreSQL)
        │
        ├─ 4. @CacheEvict "film:available"  →  Redis DEL film:available::*
        │       (vì tồn kho thay đổi, cache cũ không còn đúng)
        │
        └─ 5. kafkaTemplate.send("rental.created", event)  →  Kafka Broker
                        │
                        ▼
6. Django consumer poll() nhận message
        │
        └─ 7. SQL INSERT INTO activity_activitylog ...  (PostgreSQL riêng)

8. Trang Activity tự refresh mỗi 10s → gọi Django API → hiển thị event mới
```

---

## 6. Another Redis Desktop Manager — Redis GUI

Another Redis Desktop Manager (ARDM) là GUI miễn phí, open source để xem và thao tác Redis trực quan.  
Tải tại: https://github.com/qishibo/AnotherRedisDesktopManager/releases (chọn file `.exe` mới nhất)

---

### 6.1 Kết nối vào Redis của project

> Đảm bảo project đang chạy trước khi mở ARDM.

**Bước 1** — Mở ARDM → bấm **New Connection**.

**Bước 2** — Điền thông tin:

| Trường | Giá trị |
|--------|---------|
| Host | `127.0.0.1` |
| Port | `6379` |
| Password | *(để trống)* |
| Name | `dvdrental` (tuỳ ý) |

**Bước 3** — Bấm **Test Connection** → **OK** → **Connect**.

---

### 6.2 Khám phá Cache của project

Sau khi kết nối, ARDM hiển thị tất cả key theo dạng cây. Bên trái có thanh tìm kiếm — nhập pattern để lọc:

| Pattern nhập | Hiển thị |
|-------------|---------|
| `film::*` | Tất cả chi tiết phim đã cache |
| `film:available::*` | Danh sách phim còn hàng theo store |
| `report:*` | Tất cả cache báo cáo |

**Click vào một key** → bên phải hiển thị:
- **Value**: JSON đẹp, dễ đọc
- **TTL**: số giây còn lại trước khi key tự xóa
- **Type**: `string` (Spring Cache dùng string để lưu JSON)

---

### 6.3 Quan sát cache hit/miss trực tiếp

**Thử thực hành — xem cache được tạo:**

1. Vào ARDM, nhìn xem có key `film::1` chưa (nếu có, bấm **DEL** để xóa đi)
2. Vào `http://localhost:5173/films` → click **Detail** trên bất kỳ phim nào có ID = 1
3. Quay lại ARDM → bấm **Refresh** (F5) → key `film::1` xuất hiện với TTL ~600s

**Thử thực hành — xem cache bị evict:**

1. Vào `http://localhost:5173/rentals` → tạo một rental mới
2. Quay lại ARDM → tìm key `film:available::*` → tất cả đã bị xóa
3. Vào Films → chọn Store → ARDM refresh → key `film:available::1` hoặc `::2` xuất hiện lại

Đây chính là `@CacheEvict` trong `RentalService.java` hoạt động — tạo rental → Spring tự xóa toàn bộ cache available films.

---

### 6.4 Các thao tác hay dùng trong ARDM

| Thao tác | Cách làm |
|---------|---------|
| Xóa một key | Click vào key → nút **DEL** góc trên phải |
| Xóa theo pattern | Click phải vào nhóm key → **Delete Keys** |
| Xem TTL | Hiển thị ngay dưới tên key khi click vào |
| Refresh | `F5` hoặc nút Refresh trên toolbar |
| Tìm key | Thanh search bên trái, hỗ trợ `*` wildcard |
| Xem raw value | Tab **Raw** thay vì **Viewer** |

---

## Tóm tắt lệnh hay dùng

```bash
# Xem log realtime
./docker.sh logs springboot
./docker.sh logs django

# Vào PostgreSQL
./docker.sh db

# Vào Redis CLI
./docker.sh redis

# Đọc message Kafka realtime
docker exec dvdrental-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic rental.created

# Xem status containers
./docker.sh ps
```
