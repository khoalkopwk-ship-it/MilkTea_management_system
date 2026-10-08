# MilkTea — kiến trúc và hợp đồng dùng chung

## 1. Stack bắt buộc

Một ứng dụng Spring Boot, Maven, Spring MVC, Thymeleaf, Bootstrap, JavaScript, Spring Data JPA/Hibernate, Spring Security/JWT, Spring WebSocket/STOMP và Cloudinary. Dùng một SQL Server hoặc MySQL hoặc PostgreSQL làm nguồn dữ liệu chính; SQL Server là lựa chọn mặc định khi khởi tạo mới theo đồ án hiện tại. Không bắt buộc chạy/kiểm thử cả ba DB nếu nhóm chưa yêu cầu hỗ trợ cả ba.

Số phiên bản Java/Spring/dependency lấy từ `pom.xml` và wrapper có thật, kiểm tra tài liệu tương thích trước khi khóa; không mang Java 21/Spring Boot 3.2+ hay một số version bất kỳ từ dự án gốc sang như yêu cầu đã chốt. Dùng starter phù hợp phiên bản Boot thực dùng, không trộn BOM. Một database và một instance ứng dụng đủ cho đồ án.

## 2. Luồng kỹ thuật và package

HTTP → Controller (Page/Api) → Service → Repository → DB; Service trả DTO/ViewModel → Controller → Thymeleaf hoặc JSON. WebSocket chỉ phát thông báo sau commit; không thay HTTP command.

Root đề xuất `vn.edu.ute.milktea`; nếu code đã dùng `vn.edu.ute.utetra` thì giữ root hiện có nhất quán, không tạo hai ứng dụng scan chồng nhau.

| Package cấp ngoài | Package nghiệp vụ bên trong / trách nhiệm |
|---|---|
| controller | auth, account, customer, catalog, table, order, payment, cancellation, inventory, reporting, settings; mỗi domain có PageController/ApiController khi cần |
| service | Các domain tương ứng; nghiệp vụ, quyền resource, transaction |
| repository | account, catalog, recipe, table, order, payment, cancellation, inventory, settings, audit; reporting query nếu cần |
| entity | account, catalog, recipe, table, order, payment, cancellation, inventory, settings, audit |
| dto | Chia theo domain, rồi request/response nếu hữu ích |
| config | WebMvc, WebSocket, Cloudinary, Properties wiring |
| security | JWT, current actor, QR/session/counter access, STOMP interceptors |
| notification | Event publisher/after-commit dispatch |
| common | ApiResponse, ErrorResponse, exceptions, helpers nhỏ |

Tổ chức theo tầng ở ngoài, nghiệp vụ ở trong. Không bắt tất cả Service có interface + Impl; không tạo Entity cho reporting/UI chỉ để đủ tầng. Giỏ guest quầy không có Entity; giỏ phiên bàn có lưu hệ thống để phục hồi. Lân review schema chung không viết mọi Repository; chủ nghiệp vụ chịu trách nhiệm xuyên suốt các tầng. Các tên thành viên là đầu mối kiến trúc, không phải lịch giao việc mới.

## 3. HTTP và envelope

API prefix `/api/v1`, endpoint cụ thể xem [milktea-openapi.yaml](milktea-openapi.yaml). GET không mở phiên/đổi bàn/tạo đơn/thu tiền. Page route: `/`, `/?table=05&qrCode=...`, `/auth/*`, `/customer/*`, `/cashier`, `/kitchen`, `/admin/*`; HTML không bọc ApiResponse.

Success JSON: `{"success":true,"data":...,"meta":null}`. List phân trang: data là mảng và meta là `{page,size,totalElements,totalPages}`. Error: `{"success":false,"error":{"code":"...","message":"...","fieldErrors":{}},"traceId":"..."}`. Không trả raw Entity, hash, tokenVersion hay QR secret trong DTO không được phép.

| HTTP | Ý nghĩa |
|---|---|
| 200 | Đọc/sửa/action có body |
| 201 | Tạo resource |
| 204 | Xóa/ngừng hoạt động chủ đích không body |
| 400 | Payload/validation cú pháp sai |
| 401 | Credential thiếu/sai/hết hạn ở route được bảo vệ |
| 403 | Sai role/phạm vi hoặc CSRF |
| 404 | Resource không tồn tại |
| 409 | Sai trạng thái/phiên cũ/version/idempotency conflict/thiếu tồn |
| 413 | File vượt giới hạn |
| 415 | Loại file/media không hỗ trợ |
| 500 | Lỗi ngoài dự kiến, không lộ stack/SQL |
| 503 | Tích hợp ngoài tạm lỗi |

ErrorCode ổn định: VALIDATION_FAILED, AUTH_INVALID_CREDENTIALS, AUTH_TOKEN_EXPIRED, ACCESS_DENIED, CSRF_INVALID, TABLE_NOT_FOUND, QR_INVALID, TABLE_SESSION_CLOSED, TABLE_SESSION_CHANGED, CART_VERSION_CONFLICT, TABLE_CLOSE_NOT_ALLOWED, PRODUCT_UNAVAILABLE, ORDER_STATE_CONFLICT, CANCELLATION_PENDING, PAYMENT_ALREADY_RECORDED, REFUND_NOT_APPROVED, INVENTORY_INSUFFICIENT, IDEMPOTENCY_CONFLICT, IMAGE_UPLOAD_FAILED, INTERNAL_ERROR. Client dùng code, không so sánh nguyên văn message.

Pagination page=0, size=20, max=100. Chỉ cho sort field có allowlist, mặc định createdAt desc. Danh sách giỏ/phiên có giới hạn cấu hình nhưng không phân trang gây mất tổng thanh toán.

## 4. Số và thời gian

ID BIGINT/Long; API truyền chuỗi số để tránh mất chính xác JavaScript. Tiền và định lượng là chuỗi decimal, Java BigDecimal, SQL DECIMAL; không float/double cho ledger. Đơn vị tiền VND, làm tròn HALF_UP tới đồng; snapshot total = sum(unitPrice*quantity) - discountAmount. DiscountPercent có hai chữ số thập phân. Định lượng hỗ trợ tới ba chữ số thập phân theo đơn vị chuẩn. API dùng Money (scale tối đa 2) và Decimal cho lượng (tối đa 3); version BIGINT truyền chuỗi Version.

Lưu UTC (Instant hoặc kiểu được DB hỗ trợ rõ); API ISO-8601 có offset. Hiển thị và kỳ báo cáo Asia/Ho_Chi_Minh. Kỳ ngày dùng khoảng [start,end), chuyển sang UTC cho query. Không áp dụng timezone máy deploy tùy ý.

## 5. Header và credential

Cookie `milktea_access_token` cho JWT tài khoản. Bearer header là lựa chọn cho REST client/Postman trong cùng chính sách kiểm tra quyền. Khi đồng thời có hai account credentials mâu thuẫn, từ chối; không âm thầm dùng token khác.

`X-Table-Token` là quyền guest trong một phiên bàn, `X-Counter-Token` là quyền guest đúng đơn quầy. Chúng không phải JWT role. QR access trước phiên dùng `X-QR-Code` theo OpenAPI; QR không cấp quyền nhân viên. Headers không được ghi log.

`X-CSRF-TOKEN` cho browser request ghi, kể cả guest. GET `/api/v1/auth/csrf` trả token để JS dùng; refresh token CSRF sau login/logout. Không yêu cầu JS đọc cookie JWT HttpOnly. `Idempotency-Key` cho các command ghi sổ/tạo đơn; `If-Match` cho giỏ theo version dạng ETag có dấu ngoặc kép (ví dụ `"2"`); version trả dưới dạng chuỗi BIGINT. Định nghĩa chi tiết trong OpenAPI.

## 6. Hợp đồng bổ sung từ báo cáo

OpenAPI v2 giữ các route mẫu và bổ sung: UC23 SettingsWrite/View cho thông tin quán/ngân hàng; UC30 PUT ngưỡng Stock theo materialId/location; UC39 incidents và additional-consumptions. Đặt bàn trả session/cart/version sau commit. Ngưỡng không nằm chung ở Material. Những route này là thiết kế triển khai; phải đồng bộ Controller/Service/tests khi thực hiện, không kết luận endpoint đã có trong source.

## 7. Tích hợp, phiên và lỗi

DB là nguồn đúng cho TableSession, SessionCart và giao dịch; gọi vùng dữ liệu phục vụ lượt khách là tạm nhưng không đồng nghĩa bộ RAM tiến trình. SessionCart bị xóa khi đóng; giao dịch và metadata phiên giữ lịch sử. Không dùng timeout đăng nhập/socket để dọn phiên bàn.

Cloudinary lưu binary ảnh, DB lưu secure_url/public_id; không lưu đơn/tiền vào Cloudinary. WebSocket/STOMP và JWT thực sự chạy, không coi đoạn mô tả là đã tích hợp. Các liên kết chính thức dưới đây phục vụ tra cứu; khi triển khai phải chọn tài liệu đúng phiên bản project và kiểm tra tương thích, không coi đường dẫn là bằng chứng đã chạy tích hợp:
- [Spring MVC và Thymeleaf](https://spring.io/guides/gs/serving-web-content/)
- [Spring Security JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring STOMP token authentication](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/authentication-token-based.html)
- [Cloudinary Java upload](https://cloudinary.com/documentation/java_image_and_video_upload)

Không thêm message broker ngoài, cache distributed, trigger/procedure nghiệp vụ bắt buộc từ đề tài khác. Bộ này là thiết kế thực hiện, không xác nhận source hiện tại đã đáp ứng.
