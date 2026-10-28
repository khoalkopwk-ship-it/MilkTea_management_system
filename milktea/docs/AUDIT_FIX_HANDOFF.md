# BÁO CÁO NGHIỆM THU TỔNG THỂ DỰ ÁN MILKTEA MULTI-BRANCH (PROMPT 1 & 2)

## THÔNG TIN NGHIỆM THU
- **Dự án**: MilkTea Chain Management System (Hệ thống chuỗi trà sữa đa chi nhánh).
- **Phạm vi vận hành**: **Chỉ phục vụ tại quán** (In-store Only: `TABLE` qua quét QR tại bàn ăn và `COUNTER` tại quầy thu ngân POS). Không giao nhận / đặt online từ xa.
- **R19 Status**: `OUT_OF_SCOPE` (Theo chỉ thị tuyệt đối tại Prompt 1 & Prompt 2).
- **Kiến trúc**: Spring Boot MVC Monolith (Java 21, Thymeleaf, Bootstrap, JPA, SQL Server).
- **Kết quả kiểm thử tự động**: **148/148 tests passed (100% SUCCESS)**.

---

## 1. KẾT QUẢ ĐỐI SOÁT VÀ SỬA LỖI 20 NHÓM RỦI RO (R01 - R21, TRỪ R19)

| Mã | Nhóm lỗi / Rủi ro | Trạng thái | Bằng chứng & Vị trí code xử lý |
|---|---|:---:|---|
| **R01** | Cashier phân quyền lỏng, không check branch assignment | **RESOLVED** | [PaymentService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/payment/PaymentService.java#L104-L124): Validate qua `StaffBranchRepository` active assignment trước idempotency replay. |
| **R02** | Bếp nấu sơ chế / kết ca không chặn chéo chi nhánh | **RESOLVED** | [InventoryService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/inventory/InventoryService.java): Validate `targetBranchId`, ép theo branch bếp trưởng, publish event đúng channel. |
| **R03** | Cookie lưu QR table 3 ngày gây lẫn phiên bàn | **RESOLVED** | [CustomerPageController.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/controller/CustomerPageController.java#L78-L86): Đặt `cookie.setMaxAge(-1)` chỉ giữ trong session browser, tránh lẫn lộn session giữa các ngày. |
| **R04** | API kho không lọc theo branch của actor | **RESOLVED** | [InventoryController.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/controller/InventoryController.java): Overload `listPreparationBatches`, `getShiftSettlementPreview`, fail-closed nếu actor non-admin thiếu branch. |
| **R05** | Guest đặt bàn chuyển sang Customer mất token bàn | **RESOLVED** | [JwtAuthenticationFilter.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/security/JwtAuthenticationFilter.java#L94-L115): Trích xuất `X-Table-Session-Token` và cookie kể cả khi đã login Customer. |
| **R06** | Hoàn tiền và gán bàn quầy thiếu kiểm tra chi nhánh | **RESOLVED** | [RefundService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/cancellation/RefundService.java#L162-L188) & [OrderService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/order/OrderService.java#L470-L485): Cashier phải thuộc đúng branch của đơn. |
| **R07** | Kết ca tính sai tồn cuối (lấy book thay vì retained) | **RESOLVED** | [InventoryService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/inventory/InventoryService.java#L305-L330): Tồn cuối = `retainedQuantity` (`actual - discarded`), movement HAO_HUT khớp `discarded`. |
| **R08** | Manager không có assignment được cấp quyền ngầm hoặc tạo Admin | **RESOLVED** | [AdminController.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/controller/AdminController.java#L45-L58): `resolveManagerBranchId` ném 403 `ACCESS_DENIED`. [AdminService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/admin/AdminService.java): Chặn Manager tạo Admin. |
| **R09** | Giao diện Quầy & Bếp thiếu nhãn chi nhánh và query branch | **RESOLVED** | [cashier/index.html](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/resources/templates/cashier/index.html), [kitchen/orders.html](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/resources/templates/kitchen/orders.html), [cashier.js](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/resources/static/js/cashier/cashier.js), [kitchen.js](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/resources/static/js/kitchen/kitchen.js): Data attributes & branch context. |
| **R10** | Replay Idempotency bỏ qua kiểm tra quyền Cashier | **RESOLVED** | [PaymentService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/payment/PaymentService.java#L92-L125): Kiểm tra chi nhánh và quyền thu ngân TRƯỚC replay idempotency key. |
| **R11** | Danh sách món mới nhất thiếu thứ tự deterministic | **RESOLVED** | [CatalogService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/catalog/CatalogService.java): Sắp xếp `createdAt DESC, id DESC`. |
| **R12** | Quản lý hủy đơn và hoàn tiền chưa đồng bộ kho | **RESOLVED** | [CancellationService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/cancellation/CancellationService.java): Đơn chưa chế biến không trừ kho; hủy đơn giải phóng voucher. |
| **R13** | CSRF token missing hoặc fail trên form submit | **RESOLVED** | [api.js](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/resources/static/js/common/api.js): Tự động trích xuất `_csrf` meta tag gửi qua Header `X-CSRF-TOKEN`. |
| **R14** | Trừ kho mẻ sơ chế không atomic | **RESOLVED** | [InventoryService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/inventory/InventoryService.java): Trừ nguyên liệu thô và tăng nguyên liệu sơ chế trong cùng `@Transactional`. |
| **R15** | Áp dụng Voucher tranh chấp lượt cuối & đơn 0đ lỗi | **RESOLVED** | [VoucherService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/voucher/VoucherService.java#L180-L215): Atomic `incrementUsageCount`, kiểm tra quota khách, đổi voucher release cũ; [PaymentService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/payment/PaymentService.java): Cho phép hóa đơn 0đ. |
| **R16** | Đếm sai OTP và race condition khi verify mã | **RESOLVED** | [EmailVerificationOtpRepository.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/repository/account/EmailVerificationOtpRepository.java), [OtpAttemptRecorder.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/auth/OtpAttemptRecorder.java): `incrementAttemptsAtomically` (REQUIRES_NEW) và `consumeOtpAtomically`. |
| **R17** | WebSocket realtime broadcast nhầm chi nhánh | **RESOLVED** | [WebSocketConfig.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/config/WebSocketConfig.java): Kênh `/topic/branches/{branchId}/*` có authorization filter fail-closed. |
| **R18** | Upload review images mất khi mock/không có Cloudinary | **RESOLVED** | [ProductImageService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/media/ProductImageService.java#L330-L345) & [WebMvcConfig.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/config/WebMvcConfig.java): Ghi file cục bộ `uploads/reviews/` và map qua URL `/images/reviews/**`. |
| **R19** | Vòng đời đóng cơ sở / shutdown chi nhánh | **OUT_OF_SCOPE** | Tuyệt đối loại khỏi phạm vi theo chỉ dẫn Prompt 1 & Prompt 2. |
| **R20** | Cho phép Topping làm món uống chính hoặc category sai | **RESOLVED** | [CatalogService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/catalog/CatalogService.java) & [OrderService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/order/OrderService.java): Chặn topping chọn làm sản phẩm chính, kiểm tra danh mục active. |
| **R21** | Sai lệch báo cáo hợp nhất khi lọc theo cơ sở | **RESOLVED** | [ReportService.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/main/java/vn/edu/ute/milktea/service/report/ReportService.java): Lọc chính xác theo `branchId`, tính đúng doanh thu sau giảm giá/voucher và trừ hoàn tiền. |

---

## 2. CÁC LUỒNG NGHIỆP VỤ ĐÃ CHẠY THẬT (VERIFIED WORKFLOWS)

1. **Luồng Khách hàng Quét QR Bàn (`TABLE`)**:
   - URL: `/menu?table=QR_CN01_BAN01`.
   - Hiển thị badge: `Chi nhánh: MilkTea Quán 01 - Võ Văn Ngân · Bàn 01`.
   - Xem menu, chọn size, mức đường/đá, thêm topping.
   - Khi chưa gửi đơn: không tự động mở phiên hay tạo đơn rác.
   - Bấm gửi đơn đầu: tự động mở phiên bàn `CO_KHACH`, tạo đơn `#1011` trạng thái `CHO_XAC_NHAN`.
   - Trả lời qua cookie session-only và token header an toàn.

2. **Luồng Quầy Thu Ngân Bán Hàng (`COUNTER`)**:
   - URL: `/cashier` (Đăng nhập `cashier@milktea.vn`).
   - Server trích xuất `branchId = 2` (CN01) từ phân công nhân sự trong CSDL.
   - Lập đơn tại quầy, áp voucher `CHAOMUNG` hoặc `CN01SPECIAL`, tính đúng giảm giá.
   - Xác nhận đơn bàn và quầy: đơn chuyển sang `CHO_CHE_BIEN`. Tồn kho **chưa** bị trừ.

3. **Luồng Màn Hình Bếp KDS (`KITCHEN`)**:
   - URL: `/kitchen/orders` (Đăng nhập `kitchen@milktea.vn`).
   - Nhận đơn realtime qua `/topic/branches/2/kitchen`.
   - Bấm "Bắt đầu làm" $\to$ `DANG_CHE_BIEN`.
   - Bấm "Hoàn thành" $\to$ `HOAN_THANH`: Hệ thống tự động trừ kho Bếp CN01 theo đúng định mức nguyên liệu đã chụp (`DinhMucDonHang`). Ghi sổ `LichSuKho` loại `XUAT`.

4. **Luồng Thanh Toán & Đóng Bàn**:
   - Thu ngân chọn đơn, bấm "Thanh toán".
   - Hỗ trợ `CASH` hoặc `BANK_TRANSFER` (VietQR). Hóa đơn chuyển `DA_THANH_TOAN`.
   - Cho phép hóa đơn 0 VND khi voucher giảm 100%.
   - Thu ngân bấm "Đóng phiên bàn": Bàn trở về `TRONG`, thu hồi session token.

5. **Luồng Quản Trị Viên & Quản Lý Cơ Sở (`ADMIN` & `MANAGER`)**:
   - `ADMIN` (`admin@milktea.vn`): Xem báo cáo toàn chuỗi, lọc từng cơ sở, quản lý Kho Tổng (`KHOTONG`), cấu hình công thức sơ chế và danh mục.
   - `MANAGER` (`manager1@milktea.vn`): Chỉ xem và quản lý trong phạm vi CN01. Bị chặn 403 nếu truy cập chi nhánh khác hoặc tạo tài khoản Admin.

---

## 3. CÁC SCRIPT ĐỐI SOÁT CƠ SỞ DỮ LIỆU ĐÃ BAN HÀNH
Vị trí: `database/validation/`
1. [verify_demo_integrity.sql](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/database/validation/verify_demo_integrity.sql): Kiểm tra 100% tính toàn vẹn chi nhánh, nhân sự, QR bàn, công thức món và sơ chế.
2. [verify_inventory_reconciliation.sql](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/database/validation/verify_inventory_reconciliation.sql): Đối soát số dư tồn kho không âm, khớp biến động lịch sử kho với số dư thực tế, kiểm tra trừ kho đơn hoàn thành.
3. [verify_financial_reconciliation.sql](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/database/validation/verify_financial_reconciliation.sql): Đối soát công thức hóa đơn, khớp tiền thực thu thanh toán, doanh thu từng chi nhánh và số lượt dùng voucher.

---

## 4. BẢNG ĐỐI SOÁT TỒN KHO - TÀI CHÍNH - XẾP HẠNG MÓN
*(Tham chiếu kịch bản chuẩn trong `database/sample_data_reference.md`)*

### 4.1. Đối soát Tồn kho sau Đơn Hàng Mẫu #1011 (Bàn 01 CN01)
| Nguyên Liệu | Vị Trí Kho | Tồn Trước Đơn | Tiêu Hao Đơn #1011 | Tồn Thực Tế Sau Chế Biến | Trạng Thái |
|---|:---:|:---:|:---:|:---:|:---:|
| Nước Cốt Trà Đen | BEP (CN01) | 8.500 ml | -150 ml | **8.350 ml** | HỢP LỆ |
| Trân Châu Nấu Chín | BEP (CN01) | 4.500 g | -50 g | **4.450 g** | HỢP LỆ |
| Nước Cốt Trà Lài | BEP (CN01) | 5.000 ml | -120 ml | **4.880 ml** | HỢP LỆ |
| Nước Đường Nấu | BEP (CN01) | 6.000 ml | -45 ml | **5.955 ml** | HỢP LỆ |
| Ly Nhựa 500ml | BEP (CN01) | 500 cái | -2 cái | **498 cái** | HỢP LỆ |
| Ống Hút Trà Sữa | BEP (CN01) | 1.000 cái | -2 cái | **998 cái** | HỢP LỆ |

### 4.2. Đối soát Doanh Thu & Hóa Đơn Đơn #1011
| Khoản mục | Giá trị Tính toán | Ghi chú nghiệp vụ |
|---|:---:|---|
| Tiền hàng (Subtotal) | 73.000 ₫ | 1x Trà sữa truyền thống (M) 32k + Trân châu 6k + 1x Trà đào cam sả (M) 35k |
| Giảm giá cửa hàng | 0 ₫ | |
| Giảm giá Voucher (`CN01SPECIAL`) | -15.000 ₫ | Giảm trực tiếp 15k cho đơn $\ge$ 45k tại CN01 |
| **Tổng tiền thanh toán (Payable)** | **58.000 ₫** | Khớp chính xác `TongTien` trong `HoaDon` |
| Thực thu (Payment) | 58.000 ₫ | Ghi nhận phương thức `BANK_TRANSFER` (VietQR) |
| Chênh lệch đối soát | **0 ₫** | **Khớp 100% không lệch 1 đồng** |

---

## 5. KẾT QUẢ TEST COMMANDS VÀ CHỨNG CHỈ XÁC THỰC
Lệnh kiểm thử đã chạy:
```powershell
.\mvnw.cmd test
```
Kết quả thực tế từ console:
```text
[INFO] Results:
[INFO] Tests run: 148, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  7.229 s
[INFO] ------------------------------------------------------------------------
```
Toàn bộ các test suite cốt lõi đều đạt:
- `BranchAccessControlAuditTest`: PASS (Kiểm soát chi nhánh fail-closed).
- `InventoryRealUnitTest` & `InventoryAdminManagementTest`: PASS (Kho 2 tầng, mẻ sơ chế, kết ca).
- `Phase5VoucherAndDiscountsTest` & `RefundAndVoucherAuditTest`: PASS (Voucher concurrency & hoàn tiền).
- `AuthEmailVerificationFlowTest`: PASS (OTP email, đếm sai nguyên tử, chống brute-force).
- `InStoreOnlyScopeAcceptanceTest`: PASS (Chỉ phục vụ TABLE/COUNTER, gỡ bỏ hoàn toàn ONLINE).
- `WebSocketSecurityConfigTest`: PASS (Bảo mật kênh WebSocket phân nhánh).

---

## 6. KIỂM THỬ TRÌNH DUYỆT & GIAO DIỆN ĐỒNG NHẤT (UI/VIEWPORT)
- Đã kiểm tra responsive trên các kích thước: **360px, 390px (Mobile), 768px (Tablet), 1366px (Desktop)**.
- Giao diện TABLE (`customer/menu.html`, `customer/cart.html`): Nhãn chi nhánh và bàn ăn luôn cố định, hỗ trợ chạm vuốt trên mobile, không hiện form giao hàng.
- Giao diện Thu ngân POS (`cashier/index.html`): Mở bán nhanh, modal thu tiền tiền mặt / VietQR hiển thị rõ ràng, tích hợp phím tắt.
- Giao diện Bếp (`kitchen/orders.html`): Bảng trạng thái thẻ đơn trực quan, đổi màu theo tiến độ, âm thanh thông báo đơn mới qua WebSocket.
- CSRF Token và Cookie: Tự động đính kèm `X-CSRF-TOKEN` trên tất cả các request AJAX POST/PUT/DELETE.

---

## 7. TÀI LIỆU DỮ LIỆU MẪU & KỊCH BẢN DEMO
Tài liệu đặc tả chi tiết: [database/sample_data_reference.md](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/database/sample_data_reference.md).
Bao gồm:
- Danh mục chi nhánh, 9 tài khoản nhân sự và khách hàng chuẩn.
- Sơ đồ 22 bàn ăn với mã QR định danh độc lập (`QR_CN01_BAN01` đến `QR_CN02_BAN10`).
- 5 kịch bản demo mẫu từ Admin toàn chuỗi, Quản lý cơ sở, Khách hàng đặt bàn, Bếp KDS trừ kho đến Thu ngân đóng bàn và Khách hàng đánh giá 5 sao.

---

## 8. DANH SÁCH RỦI RO CÒN MỞ / BLOCKED
- **R19 (Đóng chi nhánh / Vòng đời cơ sở)**: Đã được gắn cờ `OUT_OF_SCOPE` theo yêu cầu tuyệt đối của Prompt 1 & Prompt 2. Hệ thống tập trung tối đa cho mô hình chuỗi đang hoạt động tại quán.
- **Biến môi trường Cloudinary**: Nếu không cấu hình `CLOUDINARY_*`, hệ thống tự động kích hoạt chế độ fallback lưu ảnh review vào `uploads/reviews/` trên đĩa máy chủ và phục vụ qua `/images/reviews/**` mà không bị gián đoạn hay mất ảnh khi khởi động lại.
