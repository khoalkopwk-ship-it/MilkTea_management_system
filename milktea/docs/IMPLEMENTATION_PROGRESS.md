# IMPLEMENTATION PROGRESS & CHECKPOINTS — TIẾN ĐỘ THỰC HIỆN DỰ ÁN

## 1. Trạng thái tổng quan

| Giai đoạn | Mô tả | Trạng thái | Ghi chú & Checkpoint |
|---|---|---|---|
| **Giai đoạn 0** | Khảo sát thực tế & Chốt thiết kế | **HOÀN TẤT** | Đã khảo sát source, database thật SQL Server, phát hiện các điểm rủi ro bảo mật và lập bộ tài liệu chuẩn. |
| **Giai đoạn 1** | Củng cố nền bảo mật & Giao dịch | **HOÀN TẤT** | Đã triển khai CSRF token thực, OtpAttemptRecorder (REQUIRES_NEW), Idempotency SHA-256 fingerprinting (RequestDedupRecord), Cart session ownership, afterCommit Cloudinary image cleanup. 57/57 tests PASS. |
| **Giai đoạn 2** | Nền tảng dữ liệu đa chi nhánh | **HOÀN TẤT** | Đã chạy migration V1 trên SQL Server, backfill 100% dữ liệu gốc về CN01, khởi tạo CN02, cập nhật entity, repository, security filter & role MANAGER. 64/64 tests PASS. |
| **Giai đoạn 3** | Vận hành chi nhánh (QR, Đơn, Bếp, Thu ngân, Realtime) | **HOÀN TẤT** | Menu/QR theo chi nhánh, đơn online, thu ngân/bếp scoped chi nhánh, trừ kho Bếp theo chi nhánh, realtime topic theo chi nhánh. 70/70 tests PASS. |
| **Giai đoạn 4** | Trải nghiệm khách hàng (Chi tiết món, Tìm kiếm/Lọc, Top 20, Yêu thích, Đánh giá) | **HOÀN TẤT** | Đánh giá gắn đơn hoàn thành + đã thanh toán, kiểm duyệt theo chi nhánh, wishlist tài khoản, tìm kiếm & lọc phân trang, 4 danh sách Top 20. 87/87 tests PASS. |
| **Giai đoạn 5** | Voucher & Giảm giá | **SẴN SÀNG TRIỂN KHAI** | Áp dụng voucher an toàn đồng thời, hạn dùng, giới hạn lượt dùng và chi nhánh. |
| **Giai đoạn 6** | Nghiệm thu & Bàn giao | **CHỜ BẮT ĐẦU** | Hồi quy toàn bộ ca kiểm thử, đối soát chuỗi và cơ sở. |

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

## 6. Checkpoint Chi tiết Giai đoạn 4 — Trải nghiệm khách hàng (HOÀN TẤT)

### 1. Cơ sở dữ liệu & Migration:
- `V2__customer_experience_favorites_and_reviews.sql`:
  - Tạo bảng `SanPhamYeuThich` (`ProductFavorite`) với ràng buộc `UQ_SanPhamYeuThich_TaiKhoan_Mon` (`MaTK`, `MaMon`).
  - Tạo bảng `DanhGia` (`ProductReview`) với ràng buộc `UQ_DanhGia_ChiTietDonHang` (`MaChiTiet`), check constraint `CK_DanhGia_SoSao` (1-5 sao), check constraint `CK_DanhGia_NoiDung_DoDai` (LEN(LTRIM(RTRIM(NoiDung))) >= 50).
  - Tạo bảng `DanhGiaMedia` (`ReviewMedia`) lưu trữ hình ảnh/video đính kèm.

### 2. Tìm kiếm, Lọc & Phân trang Chuẩn Backend (CUS-01):
- `CatalogService.searchProducts` & `ProductRepository.searchProducts`:
  - Lọc theo từ khóa tên món (`keyword`), danh mục (`categoryId`), khoảng giá `[minPrice, maxPrice]`.
  - Sắp xếp ổn định (`price_asc`, `price_desc`, `name_asc`, mặc định `id DESC`).
  - Phân trang chuẩn backend với kích thước trang tối đa được chặn ở mức 50 phần tử.
  - Tích hợp ánh xạ trạng thái khả dụng của món theo chi nhánh (`branchId`).

### 3. Chi tiết Món uống & Lựa chọn Topping/Size (CUS-03):
- `CatalogService.getProductDetail`:
  - Trả về thông tin đầy đủ của món, danh sách các kích cỡ (size) cùng loại, danh sách topping đi kèm hợp lệ (nếu danh mục cho phép topping).
  - Tự động nạp điểm đánh giá trung bình (`averageRating`) và số lượt đánh giá (`reviewCount`) từ các đánh giá không bị ẩn.
  - Xác định trạng thái món có nằm trong danh sách yêu thích của khách hàng đang đăng nhập hay không (`isFavorite`).
  - Giao diện người dùng: `templates/customer/product-detail.html` với thiết kế hiện đại, hỗ trợ tương tác yêu thích và xem đánh giá realtime.

### 4. Danh sách Yêu thích Cá nhân (CUS-05):
- `ProductFavoriteService` & `ProductFavoriteController`:
  - API `POST /api/v1/favorites/{productId}`: Toggle thêm/xóa món yêu thích, đảm bảo tính idempotent và không tạo trùng cặp `(MaTK, MaMon)`.
  - API `GET /api/v1/favorites`: Danh sách món yêu thích cá nhân có phân trang, kiểm tra quyền sở hữu theo tài khoản `CUSTOMER`.
  - Giao diện người dùng: `templates/customer/wishlist.html`.

### 5. Đánh giá, Bình luận & Kiểm duyệt Đa Cấp (CUS-06):
- `ProductReviewService` & `ProductReviewController`:
  - Điều kiện nghiêm ngặt: Chỉ tài khoản `CUSTOMER` sở hữu đơn hàng ở trạng thái `HOAN_THANH` và hóa đơn đã có thanh toán thành công mới được gửi đánh giá.
  - Ràng buộc: Mỗi dòng chi tiết đơn hàng (`OrderItem`) chỉ được đánh giá tối đa 1 lần; số sao 1-5; nội dung >= 50 ký tự sau khi loại bỏ khoảng trắng thừa.
  - Hỗ trợ kiểm tra định dạng media đính kèm (JPEG, PNG, WebP) và magic bytes nhị phân.
  - Phân quyền kiểm duyệt: Quản lý (`MANAGER`) chỉ được duyệt/ẩn các đánh giá thuộc chi nhánh mình phụ trách; Quản trị viên (`ADMIN`) có quyền kiểm duyệt toàn chuỗi.
  - Khi ẩn đánh giá, bắt buộc nhập lý do (`reason`), tự động ghi nhận người ẩn (`hiddenBy`) và thời điểm ẩn (`hiddenAt`).
  - Các đánh giá bị ẩn tự động bị loại khỏi danh sách hiển thị cho khách và không tính vào điểm sao trung bình.

### 6. Bốn Danh sách Nổi bật Top 20 (CUS-02):
- `CatalogService.getFeaturedProducts`:
  - `newest`: Món mới nhất theo thời điểm tạo.
  - `best_seller`: Tính tổng số lượng bán trong các đơn hàng đạt trạng thái `HOAN_THANH` và đã có thanh toán tại chính chi nhánh đó.
  - `top_rated`: Món có điểm sao trung bình cao nhất từ các đánh giá hiển thị.
  - `most_favorited`: Món có số lượt khách hàng lưu vào danh sách yêu thích cao nhất toàn chuỗi.
  - Tất cả các danh sách đều bị chặn tối đa 20 sản phẩm và áp dụng bộ lọc trạng thái khả dụng tại chi nhánh.

### 7. Kết quả Kiểm thử Toàn vẹn:
- Tạo mới [Phase4CustomerExperienceTest.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/test/java/vn/edu/ute/milktea/service/Phase4CustomerExperienceTest.java) gồm 17 ca kiểm thử chuyên sâu cho toàn bộ 5 nhóm nghiệp vụ.
- Toàn bộ test suite dự án: **87/87 tests PASS (BUILD SUCCESS)**, bảo toàn 100% tính tương thích và bảo mật hệ thống.

---

## 7. Kế hoạch tiếp theo: Giai đoạn 5 (Voucher & Giảm giá)
1. **Thiết kế & Bổ sung bảng**: `Voucher`, `VoucherChiNhanh`, `LichSuSuDungVoucher`.
2. **Kiểm tra tính hợp lệ**: Hạn sử dụng, chi nhánh áp dụng, giá trị đơn tối thiểu, số lượt dùng tối đa chung và theo khách hàng.
3. **Chống Race-Condition**: Sử dụng khóa bi quan (`Pessimistic Lock`) hoặc cập nhật nguyên tử để chống tranh chấp lượt dùng cuối khi hai khách hàng thanh toán đồng thời.
4. **Snapshot giảm giá**: Lưu thông tin chiết khấu vào `HoaDon` và hoàn trả lượt dùng khi đơn bị hủy hợp lệ.

