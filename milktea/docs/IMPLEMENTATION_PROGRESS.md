# IMPLEMENTATION PROGRESS & CHECKPOINTS — TIẾN ĐỘ THỰC HIỆN DỰ ÁN

## 1. Trạng thái tổng quan

| Giai đoạn | Mô tả | Trạng thái | Ghi chú & Checkpoint |
|---|---|---|---|
| **Giai đoạn 0** | Khảo sát thực tế & Chốt thiết kế | **HOÀN TẤT** | Đã khảo sát source, database thật SQL Server, phát hiện các điểm rủi ro bảo mật và lập bộ tài liệu chuẩn. |
| **Giai đoạn 1** | Củng cố nền bảo mật & Giao dịch | **HOÀN TẤT** | Đã triển khai CSRF token thực, OtpAttemptRecorder (REQUIRES_NEW), Idempotency SHA-256 fingerprinting (RequestDedupRecord), Cart session ownership, afterCommit Cloudinary image cleanup. 57/57 tests PASS. |
| **Giai đoạn 2** | Nền tảng dữ liệu đa chi nhánh | **HOÀN TẤT** | Đã chạy migration V1 trên SQL Server, backfill 100% dữ liệu gốc về CN01, khởi tạo CN02, cập nhật entity, repository, security filter & role MANAGER. 64/64 tests PASS. |
| **Giai đoạn 3** | Vận hành chi nhánh (QR, Đơn, Bếp, Thu ngân, Realtime) | **HOÀN TẤT** | Menu/QR theo chi nhánh, đơn online, thu ngân/bếp scoped chi nhánh, trừ kho Bếp theo chi nhánh, realtime topic theo chi nhánh. 70/70 tests PASS. |
| **Giai đoạn 4** | Trải nghiệm khách hàng (Chi tiết món, Tìm kiếm/Lọc, Top 20, Yêu thích, Đánh giá) | **SẴN SÀNG TRIỂN KHAI** | Đánh giá gắn đơn hoàn thành, kiểm duyệt, yêu thích, 4 danh sách Top 20. |
| **Giai đoạn 5** | Voucher & Giảm giá | **CHỜ BẮT ĐẦU** | Áp dụng voucher an toàn đồng thời. |
| **Giai đoạn 6** | Nghiệm thu & Bàn giao | **CHỜ BẮT ĐẦU** | Hồi quy toàn bộ ca kiểm thử, hướng dẫn vận hành. |

---

## 2. Checkpoint Chi tiết Giai đoạn 0
(Đã hoàn thành - xem chi tiết khảo sát kiến trúc, DB và rủi ro bảo mật)

---

## 3. Checkpoint Chi tiết Giai đoạn 1 — Củng cố nền bảo mật & Giao dịch (HOÀN TẤT)
(Đã hoàn thành - CSRF token thực, OtpAttemptRecorder, IdempotencyService, Cart session ownership, afterCommit Cloudinary)

---

## 4. Checkpoint Chi tiết Giai đoạn 2 — Nền tảng dữ liệu đa chi nhánh (HOÀN TẤT)
(Đã hoàn thành - xem chi tiết migration và cấu trúc đa chi nhánh)

---

## 5. Checkpoint Chi tiết Giai đoạn 3 — Vận hành chi nhánh (HOÀN TẤT)

### 1. Phục vụ Menu & QR Bàn theo Chi nhánh:
- `TableDto.TableContextResponse`: Bổ sung `branchId` và `branchName`. Khách quét QR bàn sẽ tự động nhận diện chính xác chi nhánh.
- `TableContextService`: Tự động nạp thông tin chi nhánh gắn với thực thể bàn (`table.getBranch()`).
- `CatalogService.getMenu(branchId)`: Lọc trạng thái khả dụng của từng món (`available`) theo cấu hình riêng của từng chi nhánh trong bảng `MonUongChiNhanh`. Món bị tạm ngưng tại chi nhánh này vẫn có thể phục vụ bình thường tại chi nhánh khác.
- `BranchController` & `BranchService`:
  - `GET /api/v1/branches`: Lấy danh sách chi nhánh đang hoạt động.
  - `PUT /api/v1/branches/{branchId}/products/{productId}/availability`: Quản lý (`MANAGER`) bật/tắt phục vụ món tại chi nhánh mình quản lý, chặn tuyệt đối nếu can thiệp chéo chi nhánh khác.

### 2. Luồng Đơn hàng Trực tuyến & Đơn tại bàn/quầy:
- Mở rộng `OrderSource.ONLINE`: Cho phép khách hàng đặt trực tuyến (`POST /api/v1/orders/online`), gán rõ `branchId`, khởi tạo trạng thái `CHO_XAC_NHAN`, cấp `guestCounterToken` để tra cứu đơn và theo dõi tiến độ.
- Ràng buộc phân quyền thu ngân & bếp:
  - `OrderService.getCashierOrders` và `OrderService.getKitchenOrders`: Tự động lọc đơn theo chi nhánh được phân công của nhân viên (`actor.getBranchId()`).
  - Gắn đơn quầy vào bàn (`assignCounterOrderToTable`): Kiểm tra bắt buộc đơn quầy và bàn phải thuộc cùng một chi nhánh, từ chối mọi thao tác chéo chi nhánh.

### 3. Khấu trừ Kho Bếp Độc lập khi Bếp Hoàn thành Đơn:
- `InventoryService.completeOrderConsumption`:
  - Xác định chi nhánh của đơn hàng (`order.getBranch().getId()`).
  - Khóa tồn kho có chọn lọc bằng `stockRepository.findByIdWithLock(branchId, materialId, location)` thay vì khóa toàn cầu.
  - Đảm bảo 100% trừ tồn kho Bếp/Kho của chính chi nhánh thực hiện đơn, không ảnh hưởng đến tồn kho của bất kỳ chi nhánh nào khác.

### 4. Kênh Realtime WebSocket Phân tách Chi nhánh:
- Bổ sung topic theo chi nhánh: `/topic/branches/{branchId}/cashier`, `/topic/branches/{branchId}/kitchen`.
- Bắn sự kiện realtime khi mở/đóng phiên bàn (`TableSessionService`), tạo đơn, cập nhật trạng thái đơn đến đúng chi nhánh tương ứng.
- `WebSocketConfig`:
  - Kiểm tra xác thực và phân quyền `SUBSCRIBE`: Nhân viên thu ngân/bếp/quản lý chỉ được đăng ký nhận thông báo từ topic của chi nhánh mà mình được phân công (`actor.canAccessBranch(branchId)`).
  - Quản trị viên hệ thống (`ADMIN`) có thể đăng ký theo dõi toàn bộ chi nhánh.

### 5. Kết quả Kiểm thử & Toàn vẹn:
- Tạo mới [Phase3BranchOperationsTest.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/test/java/vn/edu/ute/milktea/service/Phase3BranchOperationsTest.java) (6 tests: Phân quyền Manager bật/tắt món, Quét QR trả chi nhánh, Menu theo chi nhánh, Đơn online & realtime, Chặn gán đơn chéo chi nhánh, Trừ kho Bếp độc lập).
- Toàn bộ test suite dự án: **70/70 tests PASS (BUILD SUCCESS)**, không có regression từ Giai đoạn 1 & 2.

---

## 6. Kế hoạch tiếp theo: Giai đoạn 4 (Trải nghiệm khách hàng)
1. **Tìm kiếm & Lọc món**: Phân trang backend, lọc theo danh mục, khoảng giá, sắp xếp theo tên hoặc giá.
2. **Trang chi tiết sản phẩm**: Thông tin topping, size, trạng thái chi nhánh, lịch sử xem gần đây.
3. **Sản phẩm yêu thích (Wishlist)**: Bảng `SanPhamYeuThich`, API thêm/xóa/xem danh sách yêu thích của khách hàng.
4. **Đánh giá & Bình luận có kiểm duyệt**: Bảng `DanhGia`, `DanhGiaMedia`, đánh giá 1-5 sao kèm hình ảnh/video gắn với đơn hàng đã hoàn thành và thanh toán; duyệt/ẩn đánh giá theo cấp chi nhánh (`MANAGER`) và toàn chuỗi (`ADMIN`).
5. **Top 20 danh sách nổi bật**: Mới nhất, Bán chạy nhất theo chi nhánh, Đánh giá cao nhất, Yêu thích nhất.
