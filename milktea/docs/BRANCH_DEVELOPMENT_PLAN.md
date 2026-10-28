# BRANCH DEVELOPMENT PLAN — KẾ HOẠCH PHÁT TRIỂN HỆ THỐNG CHUỖI MILKTEA

## 1. Mục tiêu & Phạm vi dự án
Chuyển đổi hệ thống MilkTea một quán hiện có thành hệ thống chuỗi trà sữa cùng thương hiệu (Brand-wide Centralized Administration & Independent Branch Operations), tích hợp đầy đủ tính năng khách hàng (Khách tại bàn qua QR, khách trực tuyến, menu/tìm kiếm/lọc, top danh sách, yêu thích, đánh giá & bình luận có kiểm duyệt, voucher).

---

## 2. Các nguyên tắc bất biến (Invariants)
1. **Kiến trúc cốt lõi**: Giữ nguyên Spring Boot MVC monolith (Java 21, Thymeleaf, Bootstrap) và duy nhất 1 database SQL Server. Không chuyển sang Microservices hay SPA.
2. **Dữ liệu & Lịch sử**: Không xóa trắng (drop/recreate) database đang có dữ liệu thật. Mọi thay đổi cấu trúc dữ liệu phải thông qua kịch bản Migration có đối soát trước/sau và backup.
3. **Phân quyền Backend**: Backend là chốt chặn bảo mật tuyệt đối cho chi nhánh, vai trò, quyền sở hữu đơn/giỏ, giá, tồn kho và voucher. Không tin tưởng client.
4. **Bảo toàn quy trình nghiệp vụ**: Giữ luồng giỏ hàng, chế biến tại bếp, sơ chế, hao hụt, kết ca và luồng tiền hiện có, gắn phạm vi chi nhánh nhất quán.

---

## 3. Phân chia giai đoạn triển khai (Phased Roadmap)

### Giai đoạn 0: Khảo sát hiện trạng & Chốt thiết kế (Hiện tại)
- [x] Đọc cấu trúc source code, cấu hình, schema `1_schema.sql`, dữ liệu thực tế tại database SQL Server localhost.
- [x] Khảo sát chi tiết 10 tiêu chuẩn kỹ thuật (Bảo mật, CSRF, OTP, Bàn/Phiên/Giỏ, Đơn/Tiền/Kho, Cloudinary, WebSocket, Danh mục, Voucher).
- [x] Lập bộ tài liệu quy tắc chuẩn (`BUSINESS_RULES.md`, `REQUIREMENTS_TRACEABILITY.md`, `BRANCH_DEVELOPMENT_PLAN.md`, `IMPLEMENTATION_PROGRESS.md`, `MIGRATION_GUIDE.md`, các entry points cho agent).
- [x] Trình bày bảng đối chiếu hiện trạng và các quyết định cần thống nhất.

### Giai đoạn 1: Củng cố nền tảng hiện tại (Foundation Hardening) — HOÀN TẤT
- [x] **SEC-02.1 CSRF**: Cấu hình Spring Security hỗ trợ CSRF chuẩn với `CookieCsrfTokenRepository` (hoặc header `X-CSRF-TOKEN` đồng bộ cho API mutation).
- [x] **SEC-02.2 OTP Transactions**: Sửa `OtpService` để ghi nhận `attempts` và `revokedAt` trong transaction độc lập (`REQUIRES_NEW` hoặc không rollback khi ném ngoại lệ sai mã), ngăn chặn tấn công brute-force OTP.
- [x] **SEC-02.3 Idempotency**: Tích hợp bảng `BanGhiChongLap` (`RequestDedupRecord`) cho tạo đơn, thanh toán, hoàn tiền và ghi kho. Kiểm tra hash payload, scope và actor, xử lý xung đột idempotency rõ ràng.
- [x] **SEC-02.4 Quyền sở hữu giỏ & phiên**: Chặn việc client truyền `sessionId` tùy tiện trong `CartController` và `TableController` mà không có token chứng minh quyền sở hữu.
- [x] **MEDIA-01 Cloudinary Lifecycle**: Đảm bảo xóa ảnh cũ chỉ thực hiện **sau khi commit database thành công** (qua `TransactionSynchronizationManager.registerSynchronization`), tránh xóa nhầm ảnh khi DB rollback.
- [x] Kiểm thử tự động chứng minh: sai quyền bị chặn, OTP sai được ghi nhận, gửi lại trùng key không nhân bản giao dịch.

### Giai đoạn 2: Nền tảng dữ liệu đa chi nhánh (Branch Data Foundation) — HOÀN TẤT
- [x] **BR-01 Mô hình Chi nhánh & Nhân sự**:
  - Tạo bảng `ChiNhanh` (`Branch`): `MaCN`, `TenCN`, `DiaChi`, `SoDienThoai`, `GioMoCua`, `GioDongCua`, `HoatDong`.
  - Tạo bảng `NhanVienChiNhanh` (`StaffBranch`): phân công `MaTK` - `MaCN`.
  - Thêm vai trò `MANAGER` vào enum `Role` và các ràng buộc liên quan.
- [x] **BR-02 Sản phẩm theo chi nhánh**:
  - Tạo bảng `MonUongChiNhanh` (`BranchProduct`): `MaCN`, `MaMon`, `KinhDoanh` (BIT), `ConPhucVu` (BIT - tạm hết hay còn hàng).
  - Không tạo trùng cặp `(MaCN, MaMon)`.
- [x] **BR-03 Giao dịch & Kho theo chi nhánh**:
  - Bổ sung cột `MaCN` vào: `Ban`, `DonHang`, `PhieuNhap`, `MeSoChe`, `KetCaKho`, `LichSuKho`.
  - Cập nhật ràng buộc: `UQ_Ban_TenBan` đổi thành `UQ_Ban_ChiNhanh_Ten` (`MaCN`, `TenBan`).
  - Thay đổi khóa chính `TonNguyenLieu`: `PK (MaCN, MaNL, ViTri)`.
- [x] **BR-04 Migration dữ liệu cũ**:
  - Kịch bản migration tạo chi nhánh mặc định `CN01` ("MilkTea Quán 01 - Võ Văn Ngân").
  - Backfill dữ liệu hiện hữu vào `CN01`, bảo toàn 100% số lượng đơn, hóa đơn, thanh toán, kho và phiên bàn.
  - Đối soát kiểm tra dữ liệu trước và sau migration.

### Giai đoạn 3: Vận hành đa chi nhánh (Branch Operations) — HOÀN TẤT
- [x] **FLOW-01 & FLOW-02 QR bàn & Menu theo cơ sở**:
  - Bàn QR mang ngữ cảnh chi nhánh cụ thể. Mở menu hiển thị đúng tên cơ sở và trạng thái phục vụ của chi nhánh đó.
  - Hỗ trợ luồng khách trực tuyến: chọn chi nhánh nhận hàng, xác thực menu và giỏ hàng theo cơ sở được chọn.
  - Hỗ trợ nguồn đơn `ONLINE` bên cạnh `TABLE` và `COUNTER`.
- [x] **OPS-01 Thu ngân theo chi nhánh**:
  - Giao diện và API thu ngân chỉ xử lý bàn, phiên, đơn, khoản thu của chi nhánh được phân công.
- [x] **OPS-02 Bếp & Kho theo chi nhánh**:
  - Màn hình bếp chỉ nhận đơn và tiêu hao nguyên liệu KHO/BẾP của chi nhánh được phân công.
- [x] **RT-01 WebSocket theo chi nhánh**:
  - Chuyển đổi topic sang `/topic/branches/{branchId}/cashier`, `/topic/branches/{branchId}/kitchen`, `/topic/branches/{branchId}/stock`.
  - Kiểm tra quyền `SUBSCRIBE` chặt chẽ theo phân công nhân viên và chi nhánh.

### Giai đoạn 4: Trải nghiệm khách hàng (Customer Experience) — HOÀN TẤT
- [x] **CUS-01 Menu, Tìm kiếm & Lọc**:
  - Tìm kiếm theo tên món, lọc theo danh mục, khoảng giá tại backend.
  - Phân trang chuẩn tại backend với kích thước trang tối đa (50) và thứ tự sắp xếp ổn định.
- [x] **CUS-03 Trang chi tiết sản phẩm**:
  - Trang chi tiết `/products/{id}` đầy đủ: thông tin, size, topping, trạng thái bán tại chi nhánh, số lượng, thêm giỏ.
  - Lịch sử sản phẩm đã xem và ngữ cảnh bàn/chi nhánh được duy trì an toàn.
- [x] **CUS-05 Yêu thích (Wishlist)**:
  - Bảng `SanPhamYeuThich` (`ProductFavorite`): tài khoản khách chung toàn chuỗi, không trùng `(MaTK, MaMon)`.
  - API thêm/bỏ yêu thích, trang danh sách yêu thích có phân trang.
- [x] **CUS-06 Đánh giá, Bình luận & Media**:
  - Bảng `DanhGia` (`ProductReview`) và `DanhGiaMedia` (`ReviewMedia`): gắn với `ChiTietDonHang`.
  - Chỉ tài khoản `CUSTOMER` sở hữu đơn đã hoàn thành và đã thanh toán mới được đánh giá.
  - Điểm 1-5 sao, bình luận >= 50 ký tự sau trim, kiểm tra MIME và magic bytes media.
  - Kiểm duyệt: `MANAGER` ẩn đánh giá thuộc chi nhánh mình; `ADMIN` toàn chuỗi.
- [x] **CUS-02 Bốn danh sách nổi bật (Top 20)**:
  - Mới nhất, Bán chạy nhất (đơn hoàn thành + thanh toán tại chi nhánh), Đánh giá cao nhất, Yêu thích nhất.

### Giai đoạn 5: Voucher & Khuyến mãi (Vouchers & Discounts) — HOÀN TẤT
- [x] **BR-06 & AT-05 Schema & Migration Voucher**:
  - Tạo bảng `Voucher`, `VoucherChiNhanh`, `LichSuSuDungVoucher`.
  - Thêm snapshot `MaVoucher`, `MaCodeVoucher`, `TienGiamVoucher` vào `HoaDon` và `DonHang`.
  - Migration script `V3__voucher_and_discounts.sql` tạo dữ liệu mẫu voucher `CHAOMUNG`, `MILKTEA10`, `CN01SPECIAL`.
- [x] **Quy tắc kiểm tra điều kiện**:
  - Hạn dùng (thời gian bắt đầu/kết thúc), chi nhánh áp dụng (`applyAllBranches` hoặc theo danh sách chi nhánh), giá trị đơn tối thiểu (`minOrderValue`), tổng lượt dùng (`totalUsageLimit`), số lượt cho phép mỗi khách (`usageLimitPerCustomer`).
- [x] **Xử lý tranh chấp đồng thời (AT-05 Concurrency Control)**:
  - Atomic database update `incrementUsageCount` chặn race condition khi tranh chấp lượt voucher cuối cùng.
  - Snapshot giảm giá voucher và trừ tồn số lượt khi áp dụng cho đơn hàng.
  - Tự động hoàn trả số lượt voucher và cập nhật trạng thái `DA_HOAN_TRA` khi đơn hàng bị hủy hoặc hoàn tiền.
- [x] **Giao diện & API**:
  - API khách hàng: `GET /api/v1/vouchers/active`, `POST /api/v1/vouchers/validate`.
  - API quản trị Admin: Tìm kiếm phân trang, CRUD voucher, toggle kích hoạt, xem lịch sử sử dụng voucher.
  - Giao diện giỏ hàng (`cart.html`, `customer.js`): Nhập mã voucher, chọn nhanh từ modal voucher khả dụng, tính toán giảm giá kép (cửa hàng + voucher) minh bạch.
  - Hiển thị voucher trên hóa đơn (`receipts.js`).
- [x] **Kiểm thử tự động toàn diện**:
  - `Phase5VoucherAndDiscountsTest.java` với 18 test cases bao phủ toàn bộ điều kiện voucher, AT-05 concurrency race condition, hoàn trả khi hủy đơn, lọc chi nhánh và Admin CRUD.

### Giai đoạn 6: Nghiệm thu & Bàn giao (Acceptance & Delivery) — HOÀN TẤT
- [x] Chạy bộ kiểm thử hồi quy và kiểm thử tích hợp trên database SQL Server (112/112 tests PASS).
- [x] Đối soát báo cáo chuỗi và báo cáo từng cơ sở: `Received(Chain) = sum(Received(CN_i))`, `Refunded(Chain) = sum(Refunded(CN_i))`, `NetReceived(Chain) = sum(NetReceived(CN_i))`, `CompletedOrderValue(Chain) = sum(CompletedOrderValue(CN_i))`.
- [x] Báo cáo Dashboard & Doanh thu hỗ trợ bộ lọc chi nhánh trực quan (`reports.html`, `dashboard.js`, `ReportService`, `ReportController`).
- [x] Phân quyền chuẩn hóa: MANAGER quản lý nghiệp vụ cơ sở, không trùng quyền cấu hình hệ thống với ADMIN.
- [x] Hoàn thiện hướng dẫn vận hành, demo và tài liệu bàn giao ([docs/OPERATIONS_AND_HANDOFF_GUIDE.md](docs/OPERATIONS_AND_HANDOFF_GUIDE.md)).

