# BẢNG MA TRẬN KIỂM THỬ TOÀN BỘ API HỆ THỐNG MILKTEA (API TEST MATRIX)

Tài liệu kiểm kê chi tiết toàn bộ bề mặt API của ứng dụng **MilkTea Monolith Spring Boot MVC**, bao gồm phân quyền vai trò (Role-Based Access Control), phạm vi chi nhánh (Branch Scoping), yêu cầu bảo mật CSRF / Idempotency và kết quả thực thi thực tế trên môi trường runtime (`localhost:8080`).

---

## 1. Tóm tắt kết quả kiểm nghiệm API trên môi trường thực tế

- **Tổng số ca kiểm thử tự động**: 41 ca kiểm thử end-to-end
- **Số ca ĐẠT (PASSED)**: 41 / 41 (100%)
- **Số ca THẤT BẠI (FAILED)**: 0
- **Môi trường thực thi**:
  - Máy chủ: Java 21, Spring Boot 4.1.1, Apache Tomcat 11.0.24, cổng HTTP 8080.
  - Cơ sở dữ liệu: Microsoft SQL Server 2022 / 2025 (`localhost:1433`, CSDL `milktea`).
  - Bộ kiểm thử: `scripts/test_full_suite.ps1` (kết quả lưu tại `docs/api_test_results.json`).

---

## 2. Danh mục chi tiết ma trận API & Trạng thái nghiệm thu

| Nhóm chức năng | Phương thức HTTP | Đường dẫn Endpoint & Aliases | Vai trò truy cập (Role) | Phạm vi chi nhánh (Branch Scope) | CSRF | Idempotency | Trạng thái thực tế | Ghi chú & Kết quả kiểm chứng |
|---|:---:|---|---|---|:---:|:---:|:---:|---|
| **Xác thực** | POST | `/api/v1/auth/login` | Bất kỳ (Anonymous) | Toàn hệ thống | Bỏ qua | Không | `PASSED (200/401)` | Trả về JWT token và set cookie `milktea_access_token`. Đăng nhập sai trả về HTTP 401. |
| **Xác thực** | POST | `/api/v1/auth/register` | Anonymous | Toàn hệ thống | Bỏ qua | Không | `PASSED (200)` | Đăng ký tài khoản khách hàng, sinh mã OTP xác thực email 6 chữ số. |
| **Xác thực** | POST | `/api/v1/auth/verify-email` | Anonymous | Toàn hệ thống | Bỏ qua | Không | `PASSED (200)` | Xác thực OTP kích hoạt tài khoản. Đếm số lần nhập sai nguyên tử, thu hồi sau 5 lần sai. |
| **Xác thực** | POST | `/api/v1/auth/password-reset/request` | Anonymous | Toàn hệ thống | Bỏ qua | Không | `PASSED (200)` | Yêu cầu gửi OTP đặt lại mật khẩu qua email. |
| **Xác thực** | POST | `/api/v1/auth/password-reset/confirm` | Anonymous | Toàn hệ thống | Bỏ qua | Không | `PASSED (200)` | Đặt lại mật khẩu mới sau khi xác thực OTP hợp lệ. |
| **Thực đơn (Catalog)** | GET | `/api/v1/catalog/categories` | Public / Guest / All | Toàn chuỗi | Không | Không | `PASSED (200)` | Lấy danh mục đang hoạt động (`active = true`). |
| **Thực đơn (Catalog)** | GET | `/api/v1/catalog/products` | Public / Guest / All | Toàn chuỗi / Lọc theo `branchId` | Không | Không | `PASSED (200)` | Lấy danh sách món bán lẻ theo chi nhánh. Kiểm tra chính sách sellability. |
| **Thực đơn (Catalog)** | GET | `/api/v1/catalog/search`, `/catalog/products/search` | Public / Guest / All | Toàn chuỗi | Không | Không | `PASSED (200)` | Tìm kiếm món theo từ khóa `q=` hoặc `keyword=`. |
| **Thực đơn (Catalog)** | GET | `/api/v1/catalog/top-selling` | Public / Guest / All | Toàn chuỗi | Không | Không | `PASSED (200)` | Lấy danh sách top món bán chạy nhất (tối đa 20 món). |
| **Thực đơn (Catalog)** | GET | `/api/v1/catalog/newest` | Public / Guest / All | Toàn chuỗi | Không | Không | `PASSED (200)` | Lấy danh sách món mới nhất (sắp xếp `createdAt DESC, id DESC`). |
| **Chi nhánh** | GET | `/api/v1/branches` | Public / Guest / All | Toàn chuỗi | Không | Không | `PASSED (200)` | Lấy danh sách tất cả chi nhánh và trạng thái hoạt động. |
| **Bàn & QR Code** | GET | `/api/v1/tables/context`, `/public/table-context` | Public / Guest / All | Theo bàn được quét | Không | Không | `PASSED (200/400/404)` | Thiếu mã QR trả 400 `QR_INVALID`. QR giả mạo trả 404. QR hợp lệ cấp capability token `tableToken` 24h. |
| **Bàn & QR Code** | GET | `/api/v1/cashier/tables`, `/tables` | CASHIER, MANAGER, ADMIN | Theo chi nhánh công tác | Không | Không | `PASSED (200)` | Thu ngân xem danh sách bàn, trạng thái `TRONG`/`CO_KHACH` và mã QR bàn chi nhánh mình. |
| **Bàn & QR Code** | GET | `/api/v1/table-sessions/{id}` | Khách bàn, Cashier, Manager, Admin | Theo phiên của bàn | Không | Không | `PASSED (200/403)` | Chỉ khách hàng giữ token bàn, người mở phiên hoặc nhân viên chi nhánh mới được xem. |
| **Bàn & QR Code** | POST | `/api/v1/cashier/tables/{id}/open-session` | CASHIER, MANAGER, ADMIN | Khớp chi nhánh của bàn | Bắt buộc | Không | `PASSED (200/403)` | Thu ngân chủ động mở phiên bàn cho khách trực tiếp. Bị chặn nếu bàn đã có phiên mở (`UQ_PhienBan_MaBan_Open`). |
| **Bàn & QR Code** | POST | `/api/v1/cashier/tables/{id}/close-session` | CASHIER, MANAGER, ADMIN | Khớp chi nhánh của bàn | Bắt buộc | Không | `PASSED (200/409)` | Đóng phiên bàn khi tất cả hóa đơn đã được thanh toán đủ. Thu hồi toàn bộ token phiên bàn. |
| **Giỏ hàng phiên bàn** | GET | `/api/v1/table-sessions/{id}/cart` | Khách bàn, Cashier | Theo phiên bàn | Không | Không | `PASSED (200)` | Lấy nội dung giỏ hàng dùng chung (shared cart) của phiên bàn. |
| **Giỏ hàng phiên bàn** | PUT | `/api/v1/table-sessions/{id}/cart` | Khách bàn, Cashier | Theo phiên bàn | Bắt buộc | Không | `PASSED (200)` | Cập nhật số lượng món và topping trong giỏ hàng phiên bàn. Phát WebSocket `/topic/table-sessions/{id}`. |
| **Đặt hàng (Order)** | POST | `/api/v1/orders/table`, `/customer/orders` | Khách bàn (có `tableToken`), Staff | Đúng chi nhánh của bàn | Bắt buộc | Có (24h) | `PASSED (201/403/409)` | Đơn đầu tiên mở phiên và chuyển bàn sang `CO_KHACH`. Thiếu token bàn bị chặn 403. Replay idempotency trả về đúng đơn cũ. Khác payload trả 409. |
| **Đặt hàng (Order)** | POST | `/api/v1/orders/counter`, `/cashier/orders` | CASHIER, MANAGER, ADMIN | Ép theo chi nhánh của Staff | Bắt buộc | Có (24h) | `PASSED (201/403)` | Khách hàng vãng lai tự gọi API bị chặn 403. Thu ngân tạo đơn mang về, server ghi đè chi nhánh công tác của nhân viên. |
| **Đặt hàng (Order)** | POST | `/api/v1/orders/online` | Bất kỳ | Không áp dụng | Bắt buộc | Không | `PASSED (410 GONE)` | Phạm vi in-store only: Từ chối dứt khoát 410 GONE đối với chức năng đặt hàng từ xa/shipper. |
| **Đặt hàng (Order)** | POST | `/api/v1/cashier/orders/{id}/confirm` | CASHIER, MANAGER, ADMIN | Đúng chi nhánh đơn hàng | Bắt buộc | Không | `PASSED (200/403)` | Thu ngân xác nhận đơn: chuyển từ `CHO_THANH_TOAN` / `CHO_XAC_NHAN` sang `CHO_CHE_BIEN` để bếp nấu. |
| **Bếp (Kitchen)** | POST | `/api/v1/kitchen/orders/{id}/start` | KITCHEN, MANAGER, ADMIN | Đúng chi nhánh của Bếp | Bắt buộc | Không | `PASSED (200/403)` | Bếp bắt đầu chế biến: chuyển từ `CHO_CHE_BIEN` sang `DANG_CHE_BIEN`. Nhân viên chi nhánh khác bị chặn 403. |
| **Bếp (Kitchen)** | POST | `/api/v1/kitchen/orders/{id}/complete` | KITCHEN, MANAGER, ADMIN | Đúng chi nhánh của Bếp | Bắt buộc | Không | `PASSED (200/403)` | Bếp hoàn thành món: chuyển sang `HOAN_THANH`, tự động trừ kho Bếp theo định mức recipe snapshot. Phát WebSocket `/topic/branches/{bId}/kitchen` & `stock`. |
| **Thanh toán** | POST | `/api/v1/cashier/invoices/{id}/payments`, `/payments/{id}/record` | CASHIER, MANAGER, ADMIN | Đúng chi nhánh hóa đơn | Bắt buộc | Có (24h) | `PASSED (201/403)` | Ghi nhận thanh toán tiền mặt/chuyển khoản. Cập nhật hóa đơn `DA_THANH_TOAN`. Bất biến `PhaiTra` được kiểm tra toàn vẹn. |
| **Kho (Inventory)** | GET | `/api/v1/inventory/stocks` | CASHIER, KITCHEN, MANAGER, ADMIN | Bắt buộc kiểm tra `branchId` | Không | Không | `PASSED (200/403)` | Xem tồn kho theo vị trí (`KHO`, `BEP`). Nhân viên xem chi nhánh khác bị từ chối 403 `ACCESS_DENIED`. |
| **Kho (Inventory)** | GET | `/api/v1/inventory/shift-settlement/preview`, `/shift-settlements/preview` | KITCHEN, MANAGER, ADMIN | Đúng chi nhánh của Bếp | Không | Không | `PASSED (200/403)` | Xem trước số liệu kiểm kê kết ca kho Bếp (tồn đầu, nhập, trừ món, hao hụt). Thu ngân bị chặn 403. |
| **Kho (Inventory)** | POST | `/api/v1/inventory/shift-settlement`, `/shift-settlements` | KITCHEN, MANAGER, ADMIN | Đúng chi nhánh của Bếp | Bắt buộc | Có (24h) | `PASSED (201/403)` | Ghi nhận kết ca kho Bếp, lập phiếu chốt, xử lý loại bỏ/giữ lại theo quy tắc R14. Cập nhật sổ cái liên tục. |
| **Kho (Inventory)** | POST | `/api/v1/inventory/imports` | MANAGER, ADMIN | Theo kho chi nhánh | Bắt buộc | Có (24h) | `PASSED (201/403)` | Nhập hàng từ nhà cung cấp về Kho Tổng hoặc chi nhánh. Lập phiếu nhập và tăng tồn kho tương ứng. |
| **Kho (Inventory)** | POST | `/api/v1/inventory/transfers` | MANAGER, ADMIN | Đúng 2 chi nhánh nguồn/đích | Bắt buộc | Có (24h) | `PASSED (201/403)` | Điều chuyển nguyên liệu giữa các chi nhánh: ghi nhận 2 giao dịch đối ứng xuất kho nguồn và nhập kho đích. |
| **Đánh giá (Reviews)** | POST | `/api/v1/reviews` | CUSTOMER | Không giới hạn | Bắt buộc | Không | `PASSED (200/400)` | Khách hàng đánh giá món đã dùng. Độ dài nội dung bắt buộc từ 50 đến 1000 ký tự (dưới 50 ký tự trả 400). |
| **Đánh giá (Reviews)** | GET | `/api/v1/reviews/products/{id}`, `/reviews/product/{id}` | Public / Guest / All | Toàn chuỗi | Không | Không | `PASSED (200)` | Xem các đánh giá đã được duyệt (`APPROVED`) của một món ăn/đồ uống. |
| **Yêu thích (Favorites)**| POST | `/api/v1/favorites/{productId}` | CUSTOMER | Không giới hạn | Bắt buộc | Không | `PASSED (200/403)` | Thêm/bỏ món vào danh sách yêu thích cá nhân của khách hàng. Khách chưa đăng nhập bị chặn 403. |
| **Yêu thích (Favorites)**| GET | `/api/v1/favorites` | CUSTOMER | Cá nhân tài khoản | Không | Không | `PASSED (200)` | Lấy danh sách các món yêu thích của tài khoản khách hàng hiện tại. |
| **Mã giảm giá (Vouchers)**| POST | `/api/v1/vouchers/validate` | Customer / Guest | Kiểm tra điều kiện chi nhánh | Bắt buộc | Không | `PASSED (200/400)` | Kiểm tra tính hợp lệ của mã voucher: trạng thái kích hoạt, thời hạn, giá trị tối thiểu, giới hạn lượt dùng/khách. |
| **Mã giảm giá (Vouchers)**| GET | `/api/v1/vouchers/active` | Public / Customer | Theo chi nhánh và đơn hàng | Không | Không | `PASSED (200)` | Lấy danh sách voucher khả dụng theo giá trị đơn hàng và chi nhánh áp dụng. |
| **Quản trị (Admin/Mgr)** | GET | `/api/v1/admin/reports/dashboard` | MANAGER, ADMIN | Scoped chi nhánh (Manager) / Toàn chuỗi (Admin) | Không | Không | `PASSED (200/403)` | Báo cáo doanh thu, số đơn, giảm giá. Manager chỉ xem chi nhánh mình; Admin xem toàn chuỗi. |
| **Quản trị (Admin)** | GET | `/api/v1/admin/accounts` | ADMIN | Toàn chuỗi | Không | Không | `PASSED (200/403)` | Quản trị viên xem danh sách nhân sự, phân bổ chi nhánh công tác. Manager bị chặn 403. |
| **Quản trị (Admin)** | POST | `/api/v1/admin/branches` | ADMIN | Toàn chuỗi | Bắt buộc | Không | `PASSED (201/403)` | Tạo mới chi nhánh kinh doanh trong chuỗi. Chỉ dành riêng cho quyền ADMIN. |
| **Quản trị (Admin)** | POST | `/api/v1/admin/vouchers` | ADMIN | Toàn chuỗi | Bắt buộc | Không | `PASSED (201/403)` | Thiết lập chương trình khuyến mãi, tỷ lệ giảm giá, hạn mức sử dụng và phạm vi chi nhánh. |
| **Quản trị (Admin)** | POST | `/api/v1/admin/refunds/{id}/decision`| ADMIN | Toàn chuỗi | Bắt buộc | Không | `PASSED (200/403)` | Quyết định hoàn tiền cho khách hàng: chỉ duy nhất ADMIN có thẩm quyền phê duyệt/từ chối. |

---

## 3. Kết luận nghiệm thu API
Toàn bộ các endpoint của ứng dụng đã được kiểm chứng bằng các HTTP request thực tế qua kịch bản `scripts/test_full_suite.ps1`. Hệ thống đạt sự đồng bộ tuyệt đối giữa cấu hình Spring Security, xử lý CSRF token nguyên tử, bảo vệ Idempotency 24h và phân lập quyền truy cập theo chi nhánh chuẩn xác 100%.
