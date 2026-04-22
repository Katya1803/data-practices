# Hands-on: Tự viết lại Spring Boot API

## Mục tiêu

Xóa toàn bộ code trong `springboot/src/` rồi tự viết lại từ đầu dựa theo đặc tả bên dưới.  
Không cần nhìn lại code cũ — dùng file này làm tài liệu duy nhất.

## Cấu trúc project cần tạo

```
springboot/src/main/java/com/dvdrental/
├── config/
│   ├── CacheConfig.java        — cấu hình Redis cache, TTL từng cache
│   ├── KafkaConfig.java        — khai báo 3 Kafka topic
│   └── WebConfig.java          — cấu hình CORS
├── controller/
│   ├── CustomerController.java
│   ├── FilmController.java
│   ├── RentalController.java
│   ├── PaymentController.java
│   └── ReportController.java
├── service/
│   ├── CustomerService.java
│   ├── FilmService.java
│   ├── RentalService.java
│   ├── PaymentService.java
│   └── ReportService.java
├── repository/
│   ├── CustomerRepository.java
│   ├── FilmRepository.java
│   ├── RentalRepository.java
│   ├── PaymentRepository.java
│   ├── InventoryRepository.java
│   ├── StaffRepository.java
│   └── ReportRepository.java
├── model/                      — JPA entities (Film, Customer, Rental, Payment, ...)
├── dto/                        — xem chi tiết từng DTO bên dưới
├── kafka/
│   ├── event/                  — RentalCreatedEvent, RentalReturnedEvent, PaymentProcessedEvent
│   └── producer/
│       └── EventProducer.java
└── exception/
    └── GlobalExceptionHandler.java
```

## Wrapper chung cho mọi response

Mọi API đều trả về `ApiResponse<T>`:

```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

Khi lỗi:
```json
{
  "success": false,
  "data": null,
  "message": "Film not found: 999"
}
```

## Wrapper cho response phân trang

Các API trả danh sách có phân trang dùng `PageResponse<T>`:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 1000,
  "totalPages": 50
}
```

Query params phân trang: `?page=0&size=20`

---

## Nhóm 1 — Films

### `GET /api/films`

Lấy danh sách phim, hỗ trợ lọc theo `category` và `rating`, có phân trang.

**Query params:**

| Tên | Kiểu | Bắt buộc | Mô tả |
|-----|------|----------|-------|
| `category` | String | Không | Tên thể loại, VD: `Action` |
| `rating` | String | Không | Xếp hạng MPAA: `G`, `PG`, `PG-13`, `R`, `NC-17` |
| `page` | int | Không | Mặc định `0` |
| `size` | int | Không | Mặc định `20` |

**Response** `ApiResponse<PageResponse<FilmDto>>`:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "filmId": 1,
        "title": "Academy Dinosaur",
        "description": "A Epic Drama of a Feminist And a Mad Scientist...",
        "releaseYear": 2006,
        "rentalDuration": 6,
        "rentalRate": 0.99,
        "length": 86,
        "replacementCost": 20.99,
        "rating": "PG"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1000,
    "totalPages": 50
  },
  "message": null
}
```

---

### `GET /api/films/{id}`

Lấy chi tiết một phim theo ID, bao gồm ngôn ngữ, danh sách thể loại và diễn viên.  
Kết quả được cache trong Redis (cache name: `film`, key: `id`, TTL: 10 phút).

**Path param:** `id` — film ID

**Response** `ApiResponse<FilmDetailDto>`:

```json
{
  "success": true,
  "data": {
    "filmId": 1,
    "title": "Academy Dinosaur",
    "description": "A Epic Drama of a Feminist And a Mad Scientist...",
    "releaseYear": 2006,
    "language": "English",
    "rentalDuration": 6,
    "rentalRate": 0.99,
    "length": 86,
    "replacementCost": 20.99,
    "rating": "PG",
    "categories": ["Documentary"],
    "cast": [
      { "actorId": 1, "firstName": "Penelope", "lastName": "Guiness" }
    ]
  },
  "message": null
}
```

---

### `GET /api/films/search`

Tìm kiếm phim bằng full-text search trên `title` và `description`.  
Trả về danh sách (không phân trang).

**Query params:**

| Tên | Kiểu | Bắt buộc | Mô tả |
|-----|------|----------|-------|
| `keyword` | String | Có | Từ khóa tìm kiếm |

**Response** `ApiResponse<List<FilmDto>>`:

```json
{
  "success": true,
  "data": [
    {
      "filmId": 1,
      "title": "Academy Dinosaur",
      "description": "...",
      "releaseYear": 2006,
      "rentalDuration": 6,
      "rentalRate": 0.99,
      "length": 86,
      "replacementCost": 20.99,
      "rating": "PG"
    }
  ],
  "message": null
}
```

---

### `GET /api/films/available`

Lấy danh sách phim còn hàng tại một store (chưa được thuê).  
Kết quả được cache trong Redis (cache name: `film:available`, key: `storeId`, TTL: 2 phút).  
Cache bị xóa toàn bộ mỗi khi có rental được tạo hoặc trả.

**Query params:**

| Tên | Kiểu | Bắt buộc | Mô tả |
|-----|------|----------|-------|
| `storeId` | int | Có | ID của store (1 hoặc 2) |

**Response** `ApiResponse<List<FilmDto>>`:

```json
{
  "success": true,
  "data": [
    {
      "filmId": 3,
      "title": "Adaptation Holes",
      "description": "...",
      "releaseYear": 2006,
      "rentalDuration": 7,
      "rentalRate": 2.99,
      "length": 50,
      "replacementCost": 18.99,
      "rating": "NC-17"
    }
  ],
  "message": null
}
```

---

### `GET /api/films/{id}/cast`

Lấy danh sách diễn viên của một phim.

**Path param:** `id` — film ID

**Response** `ApiResponse<List<ActorDto>>`:

```json
{
  "success": true,
  "data": [
    { "actorId": 1,  "firstName": "Penelope", "lastName": "Guiness" },
    { "actorId": 10, "firstName": "Christian", "lastName": "Gable" }
  ],
  "message": null
}
```

---

## Nhóm 2 — Customers

### `GET /api/customers`

Lấy danh sách khách hàng, có phân trang.  
Kết quả từng customer được cache (cache name: `film`, key: `customer:{id}`, TTL: 10 phút).

**Query params:** `page`, `size` (mặc định `size=20`)

**Response** `ApiResponse<PageResponse<CustomerDto>>`:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "customerId": 1,
        "firstName": "Mary",
        "lastName": "Smith",
        "email": "mary.smith@sakilacustomer.org",
        "storeId": 1,
        "active": true
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 599,
    "totalPages": 30
  },
  "message": null
}
```

---

### `GET /api/customers/{id}`

Lấy thông tin một khách hàng theo ID.  
Cache trong Redis (cache name: `film`, key: `customer:{id}`, TTL: 10 phút).

**Response** `ApiResponse<CustomerDto>` — cùng cấu trúc một phần tử như trên.

---

### `GET /api/customers/{id}/rentals`

Lấy lịch sử thuê phim của khách hàng, sắp xếp theo ngày thuê mới nhất, có phân trang.

**Path param:** `id` — customer ID  
**Query params:** `page`, `size` (mặc định `size=20`)

**Response** `ApiResponse<PageResponse<RentalDto>>`:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "rentalId": 16050,
        "rentalDate": "2005-08-23T14:31:19",
        "returnDate": "2005-08-27T15:31:19",
        "customerId": 1,
        "customerName": "Mary Smith",
        "inventoryId": 367,
        "filmId": 80,
        "filmTitle": "Blanket Beverly",
        "storeId": 1
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 32,
    "totalPages": 2
  },
  "message": null
}
```

---

### `GET /api/customers/{id}/stats`

Lấy thống kê của một khách hàng: tổng số lượt thuê, tổng tiền đã chi, tiền trung bình mỗi lần thuê.

**Response** `ApiResponse<CustomerStatsDto>`:

```json
{
  "success": true,
  "data": {
    "customerId": 148,
    "firstName": "Eleanor",
    "lastName": "Hunt",
    "totalRentals": 46,
    "totalSpent": 216.54,
    "avgPayment": 4.71
  },
  "message": null
}
```

---

### `GET /api/customers/overdue`

Lấy danh sách khách hàng hiện đang có ít nhất một lượt thuê quá hạn (chưa trả, đã qua rental_duration).

**Response** `ApiResponse<List<CustomerDto>>` — cùng cấu trúc CustomerDto.

---

## Nhóm 3 — Rentals

### `GET /api/rentals/active`

Lấy danh sách các lượt thuê đang hoạt động (chưa trả), có phân trang.

**Query params:** `page`, `size` (mặc định `size=20`)

**Response** `ApiResponse<PageResponse<RentalDto>>`:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "rentalId": 16052,
        "rentalDate": "2026-04-22T09:00:00",
        "returnDate": null,
        "customerId": 42,
        "customerName": "Bob Williams",
        "inventoryId": 500,
        "filmId": 103,
        "filmTitle": "Bucket Brotherhood",
        "storeId": 1
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 183,
    "totalPages": 10
  },
  "message": null
}
```

---

### `GET /api/rentals/overdue`

Lấy danh sách tất cả lượt thuê quá hạn (chưa trả và đã vượt quá rental_duration của phim).  
Trả về toàn bộ danh sách, không phân trang.

**Response** `ApiResponse<List<RentalDto>>` — cùng cấu trúc RentalDto, `returnDate` luôn là `null`.

---

### `POST /api/rentals`

Tạo một lượt thuê mới. Validate inventory còn trống (chưa được thuê).  
Sau khi lưu DB, publish Kafka event `rental.created`.  
Đồng thời xóa toàn bộ cache `film:available`.  
Trả về HTTP `201 Created`.

**Request body:**

```json
{
  "inventoryId": 500,
  "customerId": 42,
  "staffId": 1
}
```

**Response** `ApiResponse<RentalDto>`:

```json
{
  "success": true,
  "data": {
    "rentalId": 16053,
    "rentalDate": "2026-04-22T09:15:00",
    "returnDate": null,
    "customerId": 42,
    "customerName": "Bob Williams",
    "inventoryId": 500,
    "filmId": 103,
    "filmTitle": "Bucket Brotherhood",
    "storeId": 1
  },
  "message": null
}
```

**Lỗi khi inventory đã được thuê:**

```json
{
  "success": false,
  "data": null,
  "message": "Inventory 500 is already rented out"
}
```

---

### `PUT /api/rentals/{id}/return`

Đánh dấu một lượt thuê là đã trả bằng cách set `return_date = NOW()`.  
Sau khi lưu DB, publish Kafka event `rental.returned` kèm số ngày đã thuê.  
Đồng thời xóa toàn bộ cache `film:available`.

**Path param:** `id` — rental ID

**Response** `ApiResponse<RentalDto>` — giống cấu trúc trên, `returnDate` được điền:

```json
{
  "success": true,
  "data": {
    "rentalId": 16053,
    "rentalDate": "2026-04-22T09:15:00",
    "returnDate": "2026-04-25T14:00:00",
    "customerId": 42,
    "customerName": "Bob Williams",
    "inventoryId": 500,
    "filmId": 103,
    "filmTitle": "Bucket Brotherhood",
    "storeId": 1
  },
  "message": null
}
```

**Lỗi khi rental đã được trả rồi:**

```json
{
  "success": false,
  "data": null,
  "message": "Rental 16053 has already been returned"
}
```

---

## Nhóm 4 — Payments

### `GET /api/payments/customer/{customerId}`

Lấy lịch sử thanh toán của một khách hàng, có phân trang.

**Path param:** `customerId`  
**Query params:** `page`, `size` (mặc định `size=20`)

**Response** `ApiResponse<PageResponse<PaymentDto>>`:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "paymentId": 32100,
        "customerId": 42,
        "rentalId": 16050,
        "amount": 2.99,
        "paymentDate": "2026-04-20T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 25,
    "totalPages": 2
  },
  "message": null
}
```

---

### `POST /api/payments`

Tạo một giao dịch thanh toán cho rental.  
Sau khi lưu DB, publish Kafka event `payment.processed`.  
Trả về HTTP `201 Created`.

**Request body:**

```json
{
  "rentalId": 16053,
  "customerId": 42,
  "staffId": 1,
  "amount": 4.99
}
```

**Response** `ApiResponse<PaymentDto>`:

```json
{
  "success": true,
  "data": {
    "paymentId": 32101,
    "customerId": 42,
    "rentalId": 16053,
    "amount": 4.99,
    "paymentDate": "2026-04-22T09:30:00"
  },
  "message": null
}
```

---

## Nhóm 5 — Reports

Tất cả report API đều được cache trong Redis. Kết quả không thay đổi liên tục nên TTL dài.

### `GET /api/reports/films/top`

Top phim được thuê nhiều nhất.  
Cache name: `report:top-films`, key: `limit`, TTL: 30 phút.

**Query params:**

| Tên | Kiểu | Mặc định | Mô tả |
|-----|------|----------|-------|
| `limit` | int | 10 | Số lượng phim muốn lấy |

**Response** `ApiResponse<List<TopFilmDto>>`:

```json
{
  "success": true,
  "data": [
    { "filmId": 103, "title": "Bucket Brotherhood", "rentalCount": 34 },
    { "filmId": 738, "title": "Rocketeer Mother",   "rentalCount": 33 }
  ],
  "message": null
}
```

---

### `GET /api/reports/revenue/monthly`

Doanh thu theo từng tháng trong một năm.  
Cache name: `report:revenue:monthly`, key: `year`, TTL: 30 phút.

**Query params:**

| Tên | Kiểu | Mặc định | Mô tả |
|-----|------|----------|-------|
| `year` | int | 2005 | Năm cần lấy doanh thu |

**Response** `ApiResponse<List<MonthlyRevenueDto>>`:

```json
{
  "success": true,
  "data": [
    { "month": 2, "revenue": 8351.84,  "transactionCount": 2016 },
    { "month": 3, "revenue": 23886.56, "transactionCount": 5644 },
    { "month": 4, "revenue": 28559.46, "transactionCount": 6754 }
  ],
  "message": null
}
```

---

### `GET /api/reports/customers/top`

Top khách hàng chi tiêu nhiều nhất, kèm xếp hạng.  
Cache name: `report:top-customers`, key: `limit`, TTL: 15 phút.

**Query params:**

| Tên | Kiểu | Mặc định | Mô tả |
|-----|------|----------|-------|
| `limit` | int | 10 | Số lượng khách hàng muốn lấy |

**Response** `ApiResponse<List<TopCustomerDto>>`:

```json
{
  "success": true,
  "data": [
    {
      "customerId": 148,
      "name": "Eleanor Hunt",
      "totalSpent": 9731.30,
      "totalRentals": 46,
      "rank": 1
    },
    {
      "customerId": 526,
      "name": "Karl Seal",
      "totalSpent": 9386.10,
      "totalRentals": 45,
      "rank": 2
    }
  ],
  "message": null
}
```

---

### `GET /api/reports/rentals/by-category`

Thống kê số lượt thuê và doanh thu theo từng thể loại phim.  
Cache name: `report:category`, TTL: 5 phút.

**Response** `ApiResponse<List<CategoryRentalDto>>`:

```json
{
  "success": true,
  "data": [
    { "category": "Sports",    "totalRentals": 1179, "totalRevenue": 4892.19 },
    { "category": "Animation", "totalRentals": 1166, "totalRevenue": 4245.31 },
    { "category": "Action",    "totalRentals": 1112, "totalRevenue": 3951.18 }
  ],
  "message": null
}
```

---

## Kafka Events

Ba sự kiện được publish sau khi write vào DB thành công.

### `rental.created` — topic: `rental.created`

```json
{
  "rentalId": 16053,
  "customerId": 42,
  "filmId": 103,
  "inventoryId": 500,
  "rentalDate": "2026-04-22T09:15:00"
}
```

### `rental.returned` — topic: `rental.returned`

```json
{
  "rentalId": 16053,
  "customerId": 42,
  "filmId": 103,
  "returnDate": "2026-04-25T14:00:00",
  "daysRented": 3
}
```

### `payment.processed` — topic: `payment.processed`

```json
{
  "paymentId": 32101,
  "rentalId": 16053,
  "customerId": 42,
  "amount": 4.99,
  "paymentDate": "2026-04-22T09:30:00"
}
```

---

## Nhóm 6 — Realtime Stats (Kafka Consumer)

### Thiết kế consumer

Tạo một **Kafka Consumer** trong Spring Boot lắng nghe 3 topic (`rental.created`, `rental.returned`, `payment.processed`) và cập nhật counter real-time vào Redis.

Consumer group: `springboot-stats-group` — **khác** với Django's `dvdrental-group`, nên cả hai nhận đầy đủ message độc lập nhau.

**Redis keys** được dùng để lưu counter theo ngày:

| Key | Tăng khi nào | Redis command |
|-----|-------------|---------------|
| `stats:rentals:{date}` | Nhận event `rental.created` | `INCR` |
| `stats:returns:{date}` | Nhận event `rental.returned` | `INCR` |
| `stats:payments:{date}` | Nhận event `payment.processed` | `INCR` |
| `stats:revenue:{date}` | Nhận event `payment.processed` | `INCRBYFLOAT` |

**Lưu ý triển khai:**
- Producer dùng `spring.json.add.type.headers=false` → consumer không đọc được type header → dùng `StringDeserializer` và parse JSON thủ công bằng `ObjectMapper`
- Cần tạo một `ConcurrentKafkaListenerContainerFactory` riêng (đặt tên `statsListenerContainerFactory`) dùng `StringDeserializer`, không dùng factory mặc định của Spring Boot
- `auto.offset.reset=earliest` → khi lần đầu consumer group join, nó đọc lại toàn bộ event lịch sử

---

### `GET /api/stats/realtime`

Đọc các counter từ Redis và trả về thống kê hoạt động trong một ngày.

**Query params:**

| Tên | Kiểu | Bắt buộc | Mô tả |
|-----|------|----------|-------|
| `date` | String (ISO: `yyyy-MM-dd`) | Không | Mặc định là ngày hôm nay |

**Response** `ApiResponse<RealtimeStatsDto>`:

```json
{
  "success": true,
  "data": {
    "date": "2026-04-22",
    "rentalsCreated": 5,
    "rentalsReturned": 3,
    "paymentsProcessed": 4,
    "totalRevenue": 17.96
  },
  "message": null
}
```

Nếu chưa có event nào trong ngày:

```json
{
  "success": true,
  "data": {
    "date": "2026-04-22",
    "rentalsCreated": 0,
    "rentalsReturned": 0,
    "paymentsProcessed": 0,
    "totalRevenue": 0
  },
  "message": null
}
```

---

## Gợi ý thứ tự tự học

1. Setup project Spring Boot với đúng dependencies (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-cache`, `spring-kafka`, `spring-boot-starter-data-redis`)
2. Tạo JPA entities từ schema PostgreSQL
3. Viết `ApiResponse` và `PageResponse` wrapper
4. Implement **Films API** trước — không có write, không có Kafka, dễ test nhất
5. Implement **Customers API** — thêm cache `@Cacheable`
6. Implement **Rentals API** — thêm write, `@CacheEvict`, Kafka producer
7. Implement **Payments API** — tương tự Rentals nhưng đơn giản hơn
8. Implement **Reports API** — thuần SQL aggregate, cache dài
9. Thêm `GlobalExceptionHandler` để trả lỗi đúng format `ApiResponse`
10. Implement **Realtime Stats Consumer** — `@KafkaListener`, custom `ContainerFactory`, Redis INCR
