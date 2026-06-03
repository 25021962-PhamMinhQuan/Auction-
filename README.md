#  Auction App

Ứng dụng đấu giá trực tuyến theo mô hình Client–Server, cho phép người dùng đăng ký tài khoản, đăng vật phẩm lên sàn, tham gia đấu giá theo thời gian thực và quản lý giao dịch. Hệ thống hỗ trợ ba vai trò: Admin, Seller và Bidder, với giao diện đồ họa JavaFX đầy đủ tính năng.

---

##  Công nghệ & Môi trường

| Thành phần        | Chi tiết |
|---|---|
| Ngôn ngữ          | Java 21 |
| Giao diện         | JavaFX 21.0.6 + FXML |
| Build tool        | Apache Maven 3.x |
| Database          | PostgreSQL (Supabase Cloud) |
| Connection pool   | HikariCP 5.1.0 |
| Lưu trữ ảnh       | Supabase Storage (REST API qua OkHttp) |
| Bảo mật mật khẩu  | jBCrypt |
| Logging           | SLF4J + Logback |
| UI bổ sung        | JavaFX 21.0.6 + FXML|
| Triển khai server | Docker + GitHub Actions CI/CD lên VPS |

### Yêu cầu cài đặt

- **JDK 21** (khuyến nghị Eclipse Temurin): https://adoptium.net
- **Maven 3.6+**: https://maven.apache.org/download.cgi
- **Git**

> Không cần cài PostgreSQL cục bộ — cơ sở dữ liệu đã được host trên Supabase. Đảm bảo máy có kết nối Internet khi chạy.

---

##  Cấu trúc thư mục

```
Auction-/
├── src/main/java/org/example/
│   ├── Main.java                   # Khởi tạo tài khoản admin (chạy một lần)
│   ├── controller/                 # AuthController, AuctionController
│   ├── coordinator/                # BiddingCoordinator – điều phối luồng đấu giá
│   ├── domain/                     # Entity: User (Admin/Seller/Bidder), Item, Auction, BidTransaction
│   ├── factory/                    # ServiceFactory (Singleton), ItemFactory + Strategy tạo item
│   ├── manager/                    # AuctionManager, AutoBidManager
│   ├── observer/                   # AuctionObserver, AuctionNotifier, BidderClient
│   ├── repository/                 # Interface + impl (DAO: User, Item, Auction, Bid, Deposit)
│   ├── server/
│   │   ├── AuctionServer.java      # Entry point của Server (port 2501)
│   │   ├── AuctionClient.java      # Socket client phía GUI
│   │   └── ClientHandler.java      # Xử lý từng kết nối client trên server
│   ├── service/                    # AuctionService, UserService, ItemService, DepositService
│   ├── uicontroller/               # JavaFX controllers: Login, Register, MainScreen, AdminScreen, ...
│   └── util/                       # AutoBid, IDGenerator, ThemeManager, LanguageManager, SupabaseStorage
├── src/main/resources/
│   ├── org/example/view/           # Các file FXML (login, mainscreen, adminscreen, ...)
│   ├── i18n/                       # messages_en.properties, messages_vi.properties
│   ├── css/                        # style.css (dark), light.css
│   ├── config.properties           # Cấu hình DB và Supabase
│   └── fonts/, *.png, *.jpg        # Tài nguyên giao diện
├── Dockerfile                      # Build Docker image cho server
├── .github/workflows/deploy.yml    # CI/CD pipeline
└── pom.xml
```

### Các design pattern áp dụng

- Singleton — ServiceFactory: quản lý khởi tạo service duy nhất toàn ứng dụng
- Factory + Strategy — ItemFactory + các CreationStrategy: tạo item theo từng danh mục
- Observer — AuctionNotifier / AuctionObserver: thông báo real-time khi có bid mới
- DAO/Repository — tách biệt logic truy cập dữ liệu khỏi business logic

---

##  Cấu hình trước khi chạy

### Tạo file config.properties

File này không được commit lên Git. Sau khi clone repo về, tạo thủ công file tại đường dẫn:

```
src/main/resources/config.properties
```

Sau đó copy toàn bộ nội dung sau vào file (vui lòng liên hệ nhóm để nhận credentials kết nối database và storage của dự án):

```properties
db.url=jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require
db.username=username
db.password=password
db.driver=org.postgresql.Driver

supabase.url=https://affpxxlcjinbsbtatwjd.supabase.co
supabase.key=sb_publishable_uZ14G8N_9fNvfeCkINjO5A_pwHVSMh3
supabase.bucket=item-images
supabase.bucket.avatar=Avatar-imange
```

> Hoặc tạo nhanh bằng lệnh terminal:
>
> **Linux / macOS**
> ```bash
> cat > src/main/resources/config.properties << 'EOF'
> db.url=jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require
> db.username=username
> db.password=password
> db.driver=org.postgresql.Driver
>
> supabase.url=https://affpxxlcjinbsbtatwjd.supabase.co
> supabase.key=sb_publishable_uZ14G8N_9fNvfeCkINjO5A_pwHVSMh3
> supabase.bucket=item-images
> supabase.bucket.avatar=Avatar-imange
> EOF
> ```
>
> **Windows (PowerShell)**
> ```powershell
> @"
> db.url=jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require
> db.username=username
> db.password=password
> db.driver=org.postgresql.Driver
>
> supabase.url=https://affpxxlcjinbsbtatwjd.supabase.co
> supabase.key=sb_publishable_uZ14G8N_9fNvfeCkINjO5A_pwHVSMh3
> supabase.bucket=item-images
> supabase.bucket.avatar=Avatar-imange
> "@ | Out-File -FilePath src\main\resources\config.properties -Encoding utf8
> ```

##  Câu lệnh chạy chương trình

### 1. Build toàn bộ dự án

```bash
# Linux / macOS
./mvnw clean package -DskipTests

# Windows (Command Prompt / PowerShell)
mvnw.cmd clean package -DskipTests

# Hoặc nếu đã cài Maven global (mọi hệ điều hành)
mvn clean package -DskipTests
```

Sau khi build thành công, hai artifact được tạo ra trong thư mục `target/`:
- `auction-server.jar` — fat JAR chứa toàn bộ dependencies của server
- `Project-C-1.0-SNAPSHOT.jar` — JAR của client (chạy qua Maven plugin)

---

##  Hướng dẫn chạy Server & Client

### Bước 1 — Chạy Server (Nếu chạy localhost còn nếu dùng server chung thì skip sang bước 2)

> Server cần khởi động trước khi mở bất kỳ client nào.

**Cách A: Chạy trực tiếp bằng JAR (sau khi đã build)

```bash
# Linux / macOS / Windows (Git Bash, PowerShell, cmd)
java -jar target/auction-server.jar
```

Kết quả mong đợi:
```
==Auction Server started on port 2501 ==
```

**Cách B: Chạy bằng Docker** (yêu cầu Docker đã cài)

```bash
# Build image
docker build -t auction-server .

# Chạy container
docker run -d --name auction-server -p 2501:2501 auction-server
```

**Cách C: Chạy qua Maven** (dùng khi phát triển)

```bash
# Linux / macOS
mvn exec:java -Dexec.mainClass="org.example.server.AuctionServer"

# Windows
mvn exec:java "-Dexec.mainClass=org.example.server.AuctionServer"
```

---

### Bước 2 — Chạy Client (GUI)

> Có thể mở nhiều cửa sổ client cùng lúc để test đa người dùng.

```bash
# Linux / macOS / Windows — chạy JavaFX GUI qua Maven plugin
mvn compile exec:java -Dexec.mainClass="org.example.uicontroller.Launcher"
```

Nếu muốn truyền tham số JVM thủ công (ví dụ khi không dùng Maven plugin):

```bash
# Linux / macOS
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web,javafx.swing \
     -cp target/Project-C-1.0-SNAPSHOT.jar \
     org.example.uicontroller.Launcher

# Windows (PowerShell)
java --module-path "C:\path\to\javafx-sdk\lib" `
     --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web,javafx.swing `
     -cp target\Project-C-1.0-SNAPSHOT.jar `
     org.example.uicontroller.Launcher
```

> **Khuyến nghị:** Dùng `mvn javafx:run` để Maven tự xử lý module path.

---

### Thứ tự khởi động tóm tắt

```
1. Khởi động Server        →  java -jar target/auction-server.jar
2. Mở Client (người dùng)  →  mvn javafx:run          (cửa sổ 1: Seller/Bidder)
3. Mở thêm Client          →  mvn javafx:run          (cửa sổ 2, 3, ... để test)
```

---

##  Danh sách chức năng đã hoàn thành

### Xác thực & Người dùng
- [x] Đăng ký tài khoản (Seller / Bidder), kiểm tra độ mạnh mật khẩu
- [x] Đăng nhập / đăng xuất, ngăn đăng nhập trùng session
- [x] Chỉnh sửa hồ sơ cá nhân, tải ảnh đại diện lên Supabase Storage
- [x] Quản lý số dư tài khoản, gửi yêu cầu nạp tiền

### Vật phẩm (Item)
- [x] Đăng vật phẩm mới với 6 danh mục: Art, Electronics, Estate, Fashions, Vehicles, Others
- [x] Tải ảnh vật phẩm lên Supabase Storage
- [x] Chỉnh sửa, huỷ vật phẩm chưa được đấu giá
- [x] Xem danh sách vật phẩm của bản thân

### Đấu giá
- [x] Tạo phiên đấu giá với thời gian bắt đầu/kết thúc
- [x] Đấu giá thủ công (manual bid) theo thời gian thực qua Socket
- [x] Đặt giá tự động (auto-bid) với mức tối đa
- [x] Xem danh sách phiên Upcoming và Ongoing
- [x] Lọc phiên đấu giá theo danh mục
- [x] Tìm kiếm phiên đấu giá với gợi ý tự động (autocomplete)
- [x] Xem lịch sử các lần đặt giá trong một phiên
- [x] Thông báo real-time khi có bid mới (Observer pattern)
- [x] Tự động đóng phiên khi hết thời gian (ScheduledExecutorService)
- [x] Xem danh sách phiên đã thắng

### Admin
- [x] Quản lý người dùng (xem, khoá tài khoản)
- [x] Duyệt / từ chối yêu cầu nạp tiền
- [x] Quản lý vật phẩm và phiên đấu giá
- [x] Xem thống kê hệ thống

### Giao diện & Tiện ích
- [x] Hỗ trợ đa ngôn ngữ: Tiếng Anh (EN) và Tiếng Việt (VI)
- [x] Chế độ giao diện Dark / Light theme
- [x] Crop ảnh đại diện trực tiếp trong ứng dụng
- [x] CI/CD tự động deploy server lên VPS qua GitHub Actions + Docker

---

##  Tài liệu & Demo

| Tài liệu            | Liên kết |
|---------------------|---|
| Báo cáo PDF/Video Demo | `https://drive.google.com/drive/folders/1ALsKwwGGeHWdnbVTiTglqe8gwkGA6LHp?usp=drive_link` |


---


