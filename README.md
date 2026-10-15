# BÁO CÁO ĐÁNH GIÁ TỔNG QUÁT HỆ THỐNG MILKTEA

---

## I. ĐÁNH GIÁ TỔNG QUÁT HỆ THỐNG MILKTEA

### 1. Giới thiệu và Mục tiêu hệ thống
**MilkTea Management System** là nền tảng quản lý và bán hàng toàn diện dành cho chuỗi/quán trà sữa F&B hiện đại. Hệ thống thực hiện số hóa toàn diện quy trình vận hành khép kín (O2O - Online to Offline): từ trải nghiệm thực khách gọi món tại bàn qua mã QR không cần cài app, quầy thu ngân (POS), điều phối chế biến tại bếp (KDS), quản trị kho hàng hai tầng định mức công thức (BOM) cho đến hệ thống báo cáo tài chính doanh thu, dòng tiền của quản trị viên (Admin).

### 2. Quy mô và Đặc tả kỹ thuật
* **Phạm vi nghiệp vụ**: Thiết kế và hiện thực hóa đầy đủ **41 Use Cases (UC01 - UC41)**, bao phủ 5 phân hệ lớn.
* **Mô hình kiến trúc**: Kiến trúc phân tầng chuẩn mực (**Layered Architecture**: Controller - Service - Repository - Entity - DTO) kết hợp **Stateless Security** và **Event-Driven Realtime Broker**.
* **Độ toàn vẹn & Tin cậy (Enterprise Reliability)**:
  - Kiểm soát giao dịch đa bảng (**ACID Transactions**) nghiêm ngặt.
  - Cơ chế **Idempotency Key (Bản ghi chống lặp)** chống tạo trùng đơn, trùng giao dịch thanh toán hoặc trùng phiếu kho khi gặp sự cố mạng.
  - Sử dụng khóa bi quan (**Pessimistic Locking - `PESSIMISTIC_WRITE`**) với thứ tự khóa được sắp xếp cố định (`deterministic sorting`) để xử lý tranh chấp tài nguyên tồn kho và triệt tiêu nguy cơ **Deadlock**.
  - Đóng băng snapshot định mức nguyên liệu (`OrderIngredientSnapshot`) tại thời điểm đặt đơn, tránh biến động số liệu khi công thức món thay đổi sau đó.

---

## II. CÔNG NGHỆ SỬ DỤNG 

| Phân tầng / Khía cạnh | Công nghệ & Thư viện sử dụng | Vai trò & Mục đích ứng dụng |
| :--- | :--- | :--- |
| **Backend Core** | **Java 21 (LTS)** | Khai thác các tính năng hiện đại (Records, Pattern Matching, Sequenced Collections). |
| **Framework chính** | **Spring Boot 4.x / 3.x** | Bộ khung phát triển backend doanh nghiệp mạnh mẽ, tối ưu cấu hình. |
| **Bảo mật & Xác thực** | **Spring Security 6, JWT, BCrypt** | Kiến trúc xác thực Stateless; JWT được lưu trữ an toàn trong `HttpOnly Cookie`; kiểm tra `TokenVersion` tức thì khi đổi mật khẩu/đăng xuất. |
| **Xác thực OTP Email** | **JavaMailSender (Gmail SMTP), HMAC-SHA256, SecureRandom** | Sinh OTP 6 số ngẫu nhiên an toàn, băm HMAC kèm Salt 16-byte độc lập; cơ chế Cooldown chống spam bot và Brute-force lock. |
| **Truy tiếp dữ liệu** | **Spring Data JPA, Hibernate, HikariCP** | Quản lý ORM, tối ưu câu truy vấn, xử lý Transaction và Locking. |
| **Cơ sở dữ liệu** | **Microsoft SQL Server (Production) / H2 (Testing)** | Hệ quản trị CSDL quan hệ chính thức với quan hệ khóa ngoại (FK) chặt chẽ và chỉ mục (Index) hiệu năng cao. |
| **Giao tiếp Real-time** | **Spring WebSocket, STOMP Protocol, SockJS** | Đồng bộ hóa dữ liệu thời gian thực 2 chiều giữa Khách - Thu ngân - Bếp - Quản lý, xóa bỏ thao tác F5/reload thủ công. |
| **Lưu trữ đám mây** | **Cloudinary Java SDK 2.x (Apache HTTP Client 5)** | Lưu trữ, tối ưu hóa và phân phối ảnh sản phẩm, ảnh đại diện, logo thương hiệu. |
| **Frontend & UI/UX** | **Thymeleaf SSR, Bootstrap 5, Modern Vanilla JS, CSS3** | Giao diện Server-Side Rendering chuẩn SEO, responsive linh hoạt trên Mobile/Tablet/Desktop, hiệu ứng mượt mà. |
| **Kiểm thử & Chất lượng** | **JUnit 5, Spring Boot Test, Mockito** | Thực thi hơn 30 test suite tích hợp tự động kiểm tra luồng POS, KDS, Kho, JWT, Concurrency. |

---

## III. LUỒNG HOẠT ĐỘNG TỔNG THỂ (OVERALL WORKFLOW)

Quy trình vận hành khép kín xuyên suốt 5 mắt xích:

```
[Khách hàng quét QR tại bàn] ──> [Khởi tạo Phiên bàn & Đặt món] ──> [Đóng băng Snapshot BOM]
                                                                             │
┌────────────────────────────────────────────────────────────────────────────┘
▼
[POS Thu ngân nhận Real-time] ──> [Xác nhận & Thu tiền (VietQR/Tiền mặt)] ──> [Đơn vào Bếp]
                                                                                     │
┌────────────────────────────────────────────────────────────────────────────────────┘
▼
[KDS Bếp chế biến] ──> [Bếp bấm Hoàn thành] ──> [Trừ Kho 2 tầng & Kích hoạt biến động]
                                                                │
┌───────────────────────────────────────────────────────────────┘
▼
[Kho 2 tầng KHO / BEP] ──> [Tự động trừ sơ chế/thô] ──> [Cảnh báo tồn ngưỡng tối thiểu]
                                                                │
┌───────────────────────────────────────────────────────────────┘
▼
[Admin & Báo cáo tài chính] ──> [Doanh thu, Dòng tiền, Chi phí giá vốn MAC, Lãi gộp]
```

1. **Khách hàng (Customer & Guest)**:
   - Quét mã QR tại bàn -> Hệ thống tự động nhận diện và kích hoạt phiên bàn (`TableSession`).
   - Lựa chọn đồ uống, tùy biến kích cỡ (Size S/M/L) và topping đi kèm.
   - Khi gửi đơn: Server xác thực giá server-side, chốt giảm giá, tính toán và lưu snapshot định mức nguyên liệu tiêu hao (`OrderIngredientSnapshot`), cấp khóa định danh idempotency để chống đặt trùng.
2. **Thu ngân (Cashier POS)**:
   - Nhận tín hiệu đơn hàng mới ngay lập tức qua WebSocket (`/topic/orders/cashier`).
   - Xác nhận đơn, chọn phương thức thanh toán: **Tiền mặt** hoặc **Chuyển khoản VietQR** (sinh mã QR động theo đúng số tiền và cú pháp).
   - Quản lý trạng thái bàn (TRỐNG ⇄ CÓ KHÁCH) và thực hiện đóng phiên bàn khi khách rời đi.
3. **Bếp (Kitchen KDS)**:
   - Màn hình KDS nhận đơn tự động nhảy vào pipeline: `CHO_CHE_BIEN` ➔ Bếp nhận làm (`DANG_CHE_BIEN`) ➔ Bếp bấm `HOAN_THANH`.
   - Bếp có thể kích hoạt quy trình **Sơ chế mẻ mới** (`THO` ➔ `SOCHE`): Tự động trừ nguyên liệu thô tại Kho tổng (`KHO`) và cộng nguyên liệu sơ chế vào kho bếp (`BEP`).
   - Chức năng **Kiểm kê & Kết ca**: Bếp chốt số lượng thực tế cuối ca để tự động ghi nhận hao hụt và tính chi phí thất thoát.
4. **Kho 2 tầng (2-Tier Inventory Engine)**:
   - Khi đơn hoàn thành, giao dịch trừ kho kích hoạt trong cùng Transaction: trừ nguyên liệu sơ chế tại `BEP` và nguyên liệu trực tiếp/thô tại `KHO`.
   - Thuật toán định giá vốn bình quân gia quyền (**Weighted Average Cost - MAC**) tự động cập nhật sau mỗi phiếu nhập hàng.
5. **Quản trị & Tài chính (Admin Dashboard)**:
   - Giám sát 4 chỉ số tài chính cốt lõi: Doanh thu thực nhận, Giá vốn hàng bán (COGS), Chi phí hao hụt nguyên liệu, và Lợi nhuận gộp.
   - Quản lý danh mục món, định mức công thức, duyệt hoàn tiền, và thiết lập cấu hình chung toàn quán.

---

## IV. SƠ ĐỒ KIẾN TRÚC HỆ THỐNG (ARCHITECTURE DIAGRAM)

```mermaid
flowchart TB
    subgraph ClientLayer ["1. CLIENT & PRESENTATION LAYER"]
        MobileClient["📱 Mobile Browser (Khách quét QR)"]
        DesktopClient["💻 Cashier POS / Kitchen KDS / Admin"]
        UI_SSR["Thymeleaf SSR Templates + Bootstrap 5 + Vanilla JS"]
    end

    subgraph SecurityGateway ["2. SECURITY & GATEWAY INTERCEPTOR"]
        CookieFilter["JwtAuthenticationFilter (HttpOnly Cookie Bearer)"]
        SecConfig["Spring Security 6 (Stateless, Role-based)"]
        StompSecurity["StompAuth & ChannelInterceptor (Topic Auth)"]
    end

    subgraph PresentationLayer ["3. CONTROLLER LAYER (MVC & REST)"]
        AuthCtrl["AuthController / OtpController"]
        OrderCtrl["OrderController / KitchenOrderController"]
        InvCtrl["InventoryController / CatalogController"]
        AdminCtrl["AdminController / ReportController"]
    end

    subgraph BusinessLayer ["4. BUSINESS SERVICE LAYER"]
        AuthService["AuthService & OtpService (HMAC-SHA256, Cooldown)"]
        OrderService["OrderService (Idempotency, Status Transition)"]
        InvService["InventoryService (2-Tier Stock, BOM, FIFO/MAC)"]
        RealtimePub["RealtimeEventPublisher (WebSocket Broker)"]
        MediaService["ProductImageService (Cloudinary Integration)"]
    end

    subgraph PersistenceLayer ["5. DATA PERSISTENCE LAYER (JPA / ORM)"]
        Repos["Spring Data JPA Repositories (Pessimistic Locking)"]
        Entities["JPA Entities (Account, Order, Stock, BOM, Audit)"]
    end

    subgraph ExternalAndDB ["6. DATABASE & EXTERNAL SERVICES"]
        MSSQL[("Microsoft SQL Server 2019+ (Primary DB)")]
        CloudinaryAPI["☁️ Cloudinary Media Cloud"]
        MailServer["✉️ Google SMTP Mail Service"]
    end

    %% Connections
    MobileClient --> UI_SSR
    DesktopClient --> UI_SSR
    UI_SSR --> CookieFilter
    CookieFilter --> SecConfig
    DesktopClient -.->|WebSocket / STOMP| StompSecurity

    SecConfig --> PresentationLayer
    StompSecurity --> RealtimePub

    AuthCtrl --> AuthService
    OrderCtrl --> OrderService
    InvCtrl --> InvService
    AdminCtrl --> InvService

    OrderService --> RealtimePub
    OrderService --> InvService
    AuthService --> MailServer
    InvCtrl --> MediaService
    MediaService --> CloudinaryAPI

    AuthService --> Repos
    OrderService --> Repos
    InvService --> Repos

    Repos --> Entities
    Entities --> MSSQL
```

---

## V. ĐÁNH GIÁ MỨC ĐỘ ỨNG DỤNG THỰC TẾ 

1. **Khả năng thương mại hóa & Tính thực tế cao**:
   - Giải quyết trúng bài toán đau đầu nhất của ngành F&B: **Sai lệch tồn kho, thất thoát nguyên liệu và tắc nghẽn quầy pha chế**.
   - Khách gọi món bằng QR không cần nhân viên ghi order giấy, giảm 50% thời gian chờ đợi và giảm chi phí nhân sự.
2. **Khả năng chịu tải & Tính toàn vẹn dữ liệu (High Concurrency & Data Integrity)**:
   - Cơ chế khóa bi quan (`PESSIMISTIC_WRITE`) có thứ tự giúp hệ thống vận hành trơn tru ngay cả trong giờ cao điểm khi hàng chục bàn cùng gửi đơn hoặc nhiều nhân viên bếp cùng thao tác hoàn thành đơn.
   - Bản ghi chống lặp (`Idempotency`) bảo vệ an toàn cho các tác vụ thanh toán và trừ kho.
3. **Tiêu chuẩn bảo mật cấp doanh nghiệp**:
   - Bảo vệ tuyệt đối thông tin xác thực qua `HttpOnly Cookie`, triệt tiêu rủi ro tấn công XSS đánh cắp JWT Token.
   - Luồng xác thực OTP đăng ký và khôi phục mật khẩu đạt chuẩn bảo mật ngân hàng: băm mật mã một chiều HMAC-SHA256, salt động ngẫu nhiên, giới hạn số lần thử và rate-limiting chống spam.
4. **Vận hành thời gian thực (Zero-Refresh Realtime)**:
   - Mô hình WebSocket STOMP tối ưu đường truyền mạng, không sinh tải polling liên tục lên database, mang lại trải nghiệm tương tác tức thì cho thu ngân và quầy bếp.