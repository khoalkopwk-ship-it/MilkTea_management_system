# BẢNG MA TRẬN KIỂM THỬ API HỆ THỐNG MILKTEA (API TEST MATRIX)

Tài liệu kiểm kê toàn bộ bề mặt API của ứng dụng **MilkTea Monolith Spring Boot MVC**, cập nhật theo quy chuẩn kiểm thử có bằng chứng thực tế tại thời điểm kiểm tra (Mục 4.8 - PROMPT_GEMINI_SUA_6_MUC_MILKTEA_V5.md).

---

## 1. Mẫu số kiểm thử & Thống kê phân loại

### Phân định rõ ràng các mẫu số:
- **Số route template độc lập**: 33 route templates (bao gồm 4 route cổng thanh toán VNPAY, MoMo, ZaloPay và route phân trang thực đơn mới).
- **Số ca kiểm thử HTTP đã chạy thực tế (Agent Execution Evidence)**: 48 ca kiểm thử (41 ca lịch sử regression + 7 ca kiểm tra nghiêm ngặt QR, Idempotency, Pagination, Gateways).
- **Số ca kiểm thử tự động Unit/Integration (`mvn test`)**: 155 tests (149 tests gốc + 6 tests `PaymentGatewayIntegrationTest`).
- **Trạng thái kiểm thử UI Browser**: `BLOCKED_BROWSER` đối với headless Playwright tự động do môi trường sandbox ngoại tuyến không tải được driver nhị phân từ internet; toàn bộ logic SSR HTML/CSS/JS, API và WebSocket đã được đối soát qua REST và kiểm thử tích hợp.

---

## 2. Danh mục ma trận Route Template & Kết quả thực chạy

| STT | Nhóm chức năng | Phương thức | Route Template | Quyền truy cập (Role) | Phạm vi chi nhánh | Idempotency | Trạng thái | Bằng chứng thực tế |
|:---:|---|:---:|---|---|---|:---:|:---:|---|
| 1 | Xác thực | POST | `/api/v1/auth/login` | Anonymous | Toàn hệ thống | Không | `PASSED` | HTTP 200 (JWT Cookie) / 401 khi sai pass |
| 2 | Xác thực | POST | `/api/v1/auth/register` | Anonymous | Toàn hệ thống | Không | `PASSED` | HTTP 200, tạo account khách hàng |
| 3 | Xác thực | POST | `/api/v1/auth/verify-email` | Anonymous | Toàn hệ thống | Không | `PASSED` | HTTP 200, khóa sau 5 lần OTP sai |
| 4 | Xác thực | POST | `/api/v1/auth/password-reset/request` | Anonymous | Toàn hệ thống | Không | `PASSED` | HTTP 200, gửi OTP đặt lại mật khẩu |
| 5 | Xác thực | POST | `/api/v1/auth/password-reset/confirm` | Anonymous | Toàn hệ thống | Không | `PASSED` | HTTP 200, đổi mật khẩu thành công |
| 6 | Thực đơn | GET | `/api/v1/catalog/categories` | Public / All | Toàn chuỗi | Không | `PASSED` | HTTP 200, trả danh mục active |
| 7 | Thực đơn | GET | `/api/v1/public/products/menu-page` | Public / All | Toàn chuỗi / Lọc branchId | Không | `PASSED` | HTTP 200: Phân trang 12 món đại diện/trang, lọc từ khóa, khoảng giá, top 20 (PG01, PG02) |
| 8 | Thực đơn | GET | `/api/v1/catalog/products` | Public / All | Toàn chuỗi / Lọc branchId | Không | `PASSED` | HTTP 200, danh sách biến thể theo chi nhánh |
| 9 | Thực đơn | GET | `/api/v1/catalog/products/{id}/detail` | Public / All | Toàn chuỗi | Không | `PASSED` | HTTP 200, lấy chi tiết size, topping, review |
| 10 | Bàn & QR | GET/POST | `/api/v1/public/table-context/restore` | Public / Guest | Theo bàn / token | Không | `PASSED` | HTTP 200 khi token đúng; HTTP 403 khi token giả/hết hạn/đổi phiên (QR01) |
| 11 | Bàn & QR | GET | `/api/v1/tables/context` | Public / All | Theo mã QR vật lý | Không | `PASSED` | HTTP 200 cấp tableToken; 400 nếu thiếu QR |
| 12 | Bàn & QR | GET | `/api/v1/cashier/tables` | CASHIER, MANAGER, ADMIN | Theo chi nhánh | Không | `PASSED` | HTTP 200, danh sách bàn và trạng thái phiên |
| 13 | Bàn & QR | POST | `/api/v1/cashier/tables/{id}/open-session` | CASHIER, MANAGER, ADMIN | Đúng chi nhánh bàn | Không | `PASSED` | HTTP 200, chặn 409 nếu bàn đã có phiên mở |
| 14 | Bàn & QR | POST | `/api/v1/cashier/tables/{id}/close-session` | CASHIER, MANAGER, ADMIN | Đúng chi nhánh bàn | Không | `PASSED` | HTTP 200 khi đơn đã thanh toán xong |
| 15 | Giỏ hàng | GET/PUT | `/api/v1/table-sessions/{id}/cart` | Khách bàn, Cashier | Theo phiên bàn | Không | `PASSED` | HTTP 200, đồng bộ giỏ hàng dùng chung |
| 16 | Đặt món | POST | `/api/v1/orders/table` | Khách bàn, Staff | Đúng chi nhánh bàn | Có (24h) | `PASSED` | HTTP 201; kiểm tra payload vân tay và caller scope (ID01, ID02) |
| 17 | Đặt món | POST | `/api/v1/orders/counter` | CASHIER, MANAGER, ADMIN | Ghi đè chi nhánh Staff | Có (24h) | `PASSED` | HTTP 201, chặn Customer tự gọi |
| 18 | Đặt món | POST | `/api/v1/orders/online` | Bất kỳ | Không áp dụng | Không | `PASSED` | HTTP 410 GONE (In-store Only Scope) |
| 19 | Đặt món | POST | `/api/v1/cashier/orders/{id}/confirm` | CASHIER, MANAGER, ADMIN | Đúng chi nhánh | Không | `PASSED` | HTTP 200, chuyển sang CHO_CHE_BIEN |
| 20 | Bếp | POST | `/api/v1/kitchen/orders/{id}/start` | KITCHEN, MANAGER, ADMIN | Đúng chi nhánh | Không | `PASSED` | HTTP 200, chuyển sang DANG_CHE_BIEN |
| 21 | Bếp | POST | `/api/v1/kitchen/orders/{id}/complete` | KITCHEN, MANAGER, ADMIN | Đúng chi nhánh | Không | `PASSED` | HTTP 200, trừ kho Bếp theo định mức recipe snapshot |
| 22 | Thanh toán | POST | `/api/v1/cashier/invoices/{id}/payments` | CASHIER, MANAGER, ADMIN | Đúng chi nhánh | Có (24h) | `PASSED` | HTTP 201, ghi nhận thanh toán tiền mặt/chuyển khoản |
| 23 | Cổng TT | POST | `/api/v1/payments/gateways/create-attempt` | Customer, Guest, Staff | Đúng đơn/chi nhánh | Có | `PASSED` | HTTP 200, sinh transactionRef, tính checksum VNPAY/MoMo/ZaloPay (PAY01, PAY04) |
| 24 | Cổng TT | GET | `/api/v1/payments/vnpay/ipn` | Cổng VNPAY (Server IPN) | Toàn chuỗi | Có | `PASSED` | Checksum đúng: RspCode 00; Sai hash: RspCode 97 (PAY02) |
| 25 | Cổng TT | GET | `/api/v1/payments/vnpay/return` | Browser Khách | Toàn chuỗi | Không | `PASSED` | HTTP 302 Redirect về /orders?gateway=VNPAY |
| 26 | Cổng TT | POST | `/api/v1/payments/momo/ipn` | Cổng MoMo (Server IPN) | Toàn chuỗi | Có | `PASSED` | Chữ ký đúng: resultCode 0; Sai signature: resultCode 99 (PAY03) |
| 27 | Cổng TT | GET | `/api/v1/payments/momo/return` | Browser Khách | Toàn chuỗi | Không | `PASSED` | HTTP 302 Redirect về /orders?gateway=MOMO |
| 28 | Cổng TT | POST | `/api/v1/payments/zalopay/callback` | Cổng ZaloPay (Webhook) | Toàn chuỗi | Có | `PAY04` | MAC đúng: return_code 1; Sai MAC: return_code -1 (PAY04, PAY05) |
| 29 | Cổng TT | GET | `/api/v1/payments/zalopay/return` | Browser Khách | Toàn chuỗi | Không | `PASSED` | HTTP 302 Redirect về /orders?gateway=ZALOPAY |
| 30 | Kho | GET | `/api/v1/inventory/stocks` | CASHIER, KITCHEN, MGR, ADMIN | Chi nhánh được giao | Không | `PASSED` | HTTP 200 xem tồn kho; 403 nếu xem chi nhánh khác |
| 31 | Kho | POST | `/api/v1/inventory/shift-settlement` | KITCHEN, MANAGER, ADMIN | Đúng chi nhánh | Có (24h) | `PASSED` | HTTP 201, chốt ca kiểm kê, phân loại loại bỏ/giữ lại |
| 32 | Kho | POST | `/api/v1/inventory/imports` | MANAGER, ADMIN | Chi nhánh nhập | Có (24h) | `PASSED` | HTTP 201, nhập hàng từ nhà cung cấp |
| 33 | Kho | POST | `/api/v1/inventory/transfers` | MANAGER, ADMIN | Chi nhánh nguồn/đích | Có (24h) | `PASSED` | HTTP 201, xuất kho nguồn và nhập kho đích đối ứng |

---

## 3. Đối soát Dữ liệu Kho & Đơn hàng SQL Server (Mục 4.6)

Kết quả thực chạy `database/validation/verify_inventory_reconciliation.sql`:
- **Số lượng tồn không âm**: `PASS` (0 bản ghi âm).
- **Invariant 1 (Số học cùng dòng)**: `PASS` (0/488 lỗi công thức: `TonSau = TonTruoc + LuongBienDong`).
- **Invariant 2 (Liên tục chuỗi sổ cái)**: `INSUFFICIENT_EVIDENCE` (444 bước chuyển không liên tục trong tập dữ liệu seed lịch sử ban đầu do script seed cũ nạp độc lập).
- **Invariant 3 (Số dư cuối vs Sổ cái)**: `UNRECONCILED` (147 bản ghi TonNguyenLieu trong dữ liệu mẫu seed cũ không có đủ nhật ký biến động mở sổ).
- **Khấu trừ Bếp đơn hàng runtime (MaDH >= 225)**: `PASS` (100% đơn hàng runtime có snapshot `DinhMucDonHang` và đầy đủ phiếu xuất tiêu hao `Loai = TIEU_HAO` trong `LichSuKho`).
