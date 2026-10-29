# IMPLEMENTATION PROGRESS & CHECKPOINTS — TIẾN ĐỘ THỰC HIỆN DỰ ÁN

## 1. Trạng thái tổng quan

| Giai đoạn | Mô tả | Trạng thái | Ghi chú & Checkpoint |
|---|---|---|---|
| **Giai đoạn 0** | Khảo sát thực tế & Chốt thiết kế | **HOÀN TẤT** | Đã khảo sát source, database thật SQL Server, phát hiện các điểm rủi ro bảo mật và lập bộ tài liệu chuẩn. |
| **Giai đoạn 1** | Củng cố nền bảo mật & Giao dịch | **HOÀN TẤT** | Đã triển khai CSRF token thực, OtpAttemptRecorder (REQUIRES_NEW), Idempotency SHA-256 fingerprinting (RequestDedupRecord), Cart session ownership, afterCommit Cloudinary image cleanup. 57/57 tests PASS. |
| **Giai đoạn 2** | Nền tảng dữ liệu đa chi nhánh | **HOÀN TẤT** | Đã chạy migration V1 trên SQL Server, backfill 100% dữ liệu gốc về CN01, khởi tạo CN02, cập nhật entity, repository, security filter & role MANAGER. 64/64 tests PASS. |
| **Giai đoạn 3** | Vận hành chi nhánh (QR, Đơn, Bếp, Thu ngân, Realtime) | **HOÀN TẤT** | Menu/QR theo chi nhánh, đơn online, thu ngân/bếp scoped chi nhánh, trừ kho Bếp theo chi nhánh, realtime topic theo chi nhánh. 70/70 tests PASS. |
| **Giai đoạn 4** | Trải nghiệm khách hàng (Chi tiết món, Tìm kiếm/Lọc, Top 20, Yêu thích, Đánh giá) | **HOÀN TẤT** | Đánh giá gắn đơn hoàn thành + đã thanh toán, kiểm duyệt theo chi nhánh, wishlist tài khoản, tìm kiếm & lọc phân trang, 4 danh sách Top 20. 87/87 tests PASS. |
| **Giai đoạn 5** | Voucher & Giảm giá | **HOÀN TẤT** | Áp dụng voucher an toàn đồng thời (AT-05), migration V3, snapshot HoaDon, hoàn trả khi hủy đơn, giao diện giỏ hàng và 18 test cases. 105/105 tests PASS. |
| **Giai đoạn 6** | Nghiệm thu & Bàn giao | **HOÀN TẤT** | Bộ kiểm thử chấp nhận AT-01 đến AT-06 hoàn tất (112/112 tests PASS). Báo cáo tài chính đối soát chính xác giữa cấp chuỗi và từng cơ sở. Hoàn tất tài liệu vận hành và bàn giao. |
| **Đợt Sửa 6 Mục & Cổng TT** | Sửa 6 mục (4.1, 4.2, 4.4, 4.6, 4.7, 4.8) & Tích hợp 3 Cổng VNPAY/MoMo/ZaloPay | **HOÀN TẤT** | Zero DB DDL, QR restore chống silent grant, canonical idempotency fingerprint, menu 12 món distinct, script kho thuần đọc 3 invariants, VNPAY/MoMo/ZaloPay. 155/155 tests PASS. |

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

## 7. Checkpoint Chi tiết Giai đoạn 5 — Voucher & Khuyến mãi (HOÀN TẤT)

### 1. Cơ sở dữ liệu & Migration V3:
- File migration: `database/migrations/V3__voucher_and_discounts.sql`:
  - Bảng `Voucher`: Lưu mã code, loại giảm giá (`PERCENT`, `FIXED_AMOUNT`), giá trị giảm, mức giảm tối đa, giá trị đơn tối thiểu, tổng số lượt dùng, số lượt đã dùng, giới hạn mỗi khách, phạm vi áp dụng (`applyAllBranches`), thời gian hiệu lực (`startDate`, `endDate`), cờ kích hoạt (`isActive`).
  - Bảng `VoucherChiNhanh`: Phân bổ danh sách chi nhánh được áp dụng cho voucher phạm vi giới hạn.
  - Bảng `LichSuSuDungVoucher`: Lưu lịch sử mỗi lần voucher được sử dụng kèm trạng thái (`DA_DUNG`, `DA_HOAN_TRA`), liên kết với Đơn hàng, Hóa đơn, Khách hàng và Chi nhánh.
  - Mở rộng bảng `HoaDon` & `DonHang`: Thêm cột `MaVoucher`, `MaCodeVoucher`, `TienGiamVoucher` để snapshot minh bạch.
  - Seed voucher ban đầu: `CHAOMUNG` (giảm 20.000đ, min 50k toàn chuỗi), `MILKTEA10` (giảm 10% tối đa 30k toàn chuỗi), `CN01SPECIAL` (giảm 15.000đ tại chi nhánh CN01).

### 2. Xử lý Tranh chấp Đồng thời & Khấu trừ An toàn (AT-05 Concurrency Control):
- Phương thức `incrementUsageCount` thực thi câu lệnh UPDATE nguyên tử ở mức database:
  `UPDATE Voucher SET usedCount = usedCount + 1 WHERE id = :id AND usedCount < totalUsageLimit`
- Nếu cập nhật trả về 0 dòng (do hết lượt hoặc request đồng thời khác vừa chiếm slot cuối), hệ thống lập tức ném ngoại lệ `BusinessException(VOUCHER_EXHAUSTED)`, loại bỏ hoàn toàn hiện tượng bán vượt số lượng voucher khả dụng.
- Tự động hoàn trả lượt dùng (`decrementUsageCount`) và cập nhật trạng thái `DA_HOAN_TRA` khi đơn hàng bị hủy (`CancellationService`) hoặc hoàn tiền.

### 3. Phân cấp Chiết khấu & Tích hợp Giỏ hàng:
- Áp dụng độc lập cả Giảm giá cửa hàng (`GlobalSettings`) và Giảm giá Voucher, đảm bảo tổng tiền hóa đơn không bao giờ âm.
- `VoucherController`: Cung cấp API `GET /api/v1/vouchers/active` và `POST /api/v1/vouchers/validate` cho khách hàng và khách vãng lai.
- `AdminVoucherController`: Cung cấp API quản lý danh sách phân trang, chi tiết, tạo mới, chỉnh sửa, bật/tắt voucher và tra cứu lịch sử sử dụng.
- Giao diện giỏ hàng (`cart.html`, `customer.js`): Nhập mã voucher trực tiếp hoặc duyệt danh sách modal voucher khả dụng cho chi nhánh hiện tại; tự động cập nhật bảng tạm tính và tổng thanh toán; đính kèm `voucherCode` khi đặt đơn hàng.
- Hiển thị thông tin voucher trên chứng từ hóa đơn (`receipts.js`).

### 4. Kết quả Kiểm thử:
- Tạo mới [Phase5VoucherAndDiscountsTest.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/test/java/vn/edu/ute/milktea/service/Phase5VoucherAndDiscountsTest.java) gồm 18 ca kiểm thử:
  - Xác thực mã tồn tại, hạn dùng, kích hoạt, đơn tối thiểu, phạm vi chi nhánh, giới hạn lượt dùng mỗi khách.
  - Tính toán chiết khấu phần trăm có chặn trần và chiết khấu cố định.
  - Concurrency AT-05 tranh chấp slot voucher cuối cùng.
  - Hoàn trả lượt voucher khi hủy đơn hàng.
  - Admin CRUD và bật/tắt kích hoạt voucher.
- Toàn bộ test suite dự án: **105/105 tests PASS (BUILD SUCCESS)**.

### 5. Giao diện Quản trị Khuyến mãi & Voucher (Giai đoạn 5 hoàn thiện):
- **Trang Quản lý Khuyến mãi & Voucher Admin (`/admin/vouchers`)**:
  - Tích hợp vào thanh điều hướng Admin (`templates/admin/vouchers.html`) và đồng bộ sidebar trên toàn bộ các trang quản trị. Đổi tên mục menu thành **Cài đặt** (thay vì "Cài đặt & giảm giá").
  - Gộp chung cấu hình **Giảm giá cho tất cả hóa đơn trong cửa hàng** vào trang Khuyến mãi & Voucher, loại bỏ phần cấu hình trùng lặp trong trang `settings.html`.
  - **Loại bỏ hoàn toàn khung kiểm thử (Live Sandbox)** và mã nguồn kiểm thử nhanh theo yêu cầu để giữ giao diện quản trị tinh gọn, chuẩn nghiệp vụ sản xuất.
  - Thẻ thống kê trực quan: Tổng số voucher, đang kích hoạt, tổng lượt đã dùng, voucher hết hạn.
  - Quản lý danh sách voucher toàn chuỗi: Tìm kiếm, lọc trạng thái, bật/tắt (toggle active qua PATCH/POST).
  - **Bổ sung hành động Chỉnh sửa (Edit) Voucher đã tạo**: Mở modal sửa voucher (`PUT /api/v1/admin/vouchers/{id}`), cho phép cập nhật tên, mô tả, loại giảm giá, giá trị giảm, mức giảm tối đa, đơn tối thiểu, tổng lượt dùng, giới hạn mỗi khách, thời hạn và phạm vi chi nhánh.
  - **Modal tạo mới Voucher hoàn chỉnh**: Đầy đủ trường bắt buộc `Tên voucher`, `Mã voucher`, chuẩn hóa enum `PERCENT`/`FIXED_AMOUNT`, ràng buộc hạn mức và chi nhánh rõ ràng.
  - **Xem Lịch sử sử dụng Voucher**: Hiển thị chi tiết khách hàng (họ tên, email), mã đơn hàng, chi nhánh, số tiền chiết khấu và thời điểm áp dụng (đã seed và hiển thị 10 lượt sử dụng cho voucher `HETLUOT` và 3 lượt cho `CHAOMUNG`).
- **Phân quyền Role MANAGER & Quản lý Chi nhánh**:
  - Hỗ trợ đầy đủ quyền truy cập toàn bộ các trang Admin (`/admin/**`), Quầy thu ngân (`/cashier`), Bếp (`/kitchen`) cho tài khoản Quản lý cơ sở (`Role.MANAGER`).
  - Danh sách tài khoản demo Quản lý:
    - Quản lý Cơ sở 01: `manager1@milktea.vn` (Mật khẩu: `123456`) — Chi nhánh 1 Võ Văn Ngân.
    - Quản lý Cơ sở 02: `manager2@milktea.vn` (Mật khẩu: `123456`) — Chi nhánh 2 Nguyễn Huệ.
    - Quản trị viên hệ thống: `admin@milktea.vn` (Mật khẩu: `123456`).

### 6. Cải tiến Menu & Tab Yêu thích (Wishlist) Thống nhất:
- **Bỏ lọc "Mới nhất"**: Loại bỏ nút và tham số lọc `newest` trên menu và backend, tinh gọn bộ lọc sản phẩm nổi bật.
- **Tính toán "Yêu thích nhất" (Most Favorited) theo tài khoản duy nhất**:
  - 1 tài khoản chỉ được tính 1 lượt thích cho mỗi tên món nước (không phân biệt size).
  - Bảng xếp hạng món "Yêu thích nhất" sắp xếp theo số lượng tài khoản đã thích từ cao xuống thấp thông qua `findMostFavoritedProductNames`.
  - Backend mở rộng `ProductFavoriteRepository` và `ProductFavoriteService` hỗ trợ kiểm tra và toggle yêu thích đồng bộ theo tên món.
- **Menu chỉ hiển thị Size bé nhất và Giá tương ứng**:
  - Danh mục Menu tự động nhóm các biến thể cùng tên món và chọn biến thể có giá thấp nhất (size bé nhất) để hiển thị trên thẻ sản phẩm.
  - Khi khách hàng bấm "Chọn" (mở modal) hoặc vào trang chi tiết món (`/products/{id}`), hệ thống hiển thị đầy đủ các size còn lại kèm giá tiền tương ứng để tùy chọn.
- **Đồng bộ hóa giao diện Tab Yêu thích (`wishlist.html`)**:
  - Tái thiết kế toàn bộ `wishlist.html` theo chuẩn thiết kế của `menu.html`: thẻ sản phẩm, huy hiệu phân loại, đánh giá sao, hiển thị size bé nhất và giá tiền.
  - Đầy đủ thao tác tương tự thẻ menu: Bấm "Chọn" mở modal tùy biến topping/size để thêm giỏ hàng, bấm vào tên/ảnh mở trang chi tiết món.
  - Bổ sung nút trái tim màu đỏ trên góc thẻ để xóa nhanh món khỏi danh sách yêu thích với hiệu ứng mượt mà (gọi API `DELETE /api/v1/favorites/{productId}`).

---

## 8. Checkpoint Chi tiết Giai đoạn 6 — Nghiệm thu & Bàn giao (HOÀN TẤT)

### 1. Báo cáo Tài chính & Vận hành Đa Chi nhánh:
- **Độc lập và Đối soát Doanh thu**:
  - `ReportService`: Nâng cấp toàn bộ các báo cáo tài chính và dashboard hỗ trợ tham số lọc `branchId` (`null` = toàn chuỗi hệ thống, khác `null` = chi nhánh cụ thể).
  - Đối soát số học hoàn hảo:
    $$\text{Received}(\text{Chain}) = \sum \text{Received}(\text{Branch}_i)$$
    $$\text{Refunded}(\text{Chain}) = \sum \text{Refunded}(\text{Branch}_i)$$
    $$\text{NetReceived}(\text{Chain}) = \sum \text{NetReceived}(\text{Branch}_i)$$
    $$\text{CompletedOrderValue}(\text{Chain}) = \sum \text{CompletedOrderValue}(\text{Branch}_i)$$
  - Phân tách dữ liệu: Tiền thanh toán, hoàn tiền, số đơn hoàn thành, đơn chưa thanh toán, chi phí hao hụt nguyên liệu theo từng chi nhánh.
- **Top Sản phẩm Bán chạy & Cảnh báo Tồn kho theo Chi nhánh**:
  - `findTopSellingProducts`: Thống kê các món bán chạy nhất của từng cơ sở dựa trên đơn hoàn thành và đã thanh toán tại chi nhánh đó.
  - Cảnh báo tồn kho (`DashboardAlerts`): Tồn kho nguyên liệu BEP/KHO của chi nhánh nào chạm ngưỡng hoặc cạn kiệt chỉ hiển thị trong phạm vi chi nhánh đó, không gây cảnh báo giả cho các cơ sở khác.
- **Giao diện Quản trị Báo cáo (`reports.html`, `dashboard.js`)**:
  - Bổ sung thanh chọn chi nhánh động (`<select id="filterBranch">`) nạp từ `/api/v1/branches`.
  - Hiển thị huy hiệu phạm vi hiện tại (`#dashboardBranchBadge`): `Toàn chuỗi hệ thống` hoặc tên chi nhánh cụ thể (ví dụ: `MilkTea Quán 01 - Võ Văn Ngân`).
  - Tự động đồng bộ bộ lọc chi nhánh vào mọi biểu đồ và thẻ thống kê KPI.

### 2. Bộ Kiểm thử Chấp nhận & Đối soát Toàn vẹn (AT-01 đến AT-06):
- Tạo mới [Phase6AcceptanceAndReconciliationTest.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/test/java/vn/edu/ute/milktea/service/Phase6AcceptanceAndReconciliationTest.java) gồm 7 ca kiểm thử chuyên sâu:
  - **AT-01**: Phân quyền & cô lập đa chi nhánh — Quản lý cơ sở 01 bị chặn 403 khi thao tác chéo sang cơ sở 02; Admin có toàn quyền.
  - **AT-02**: Đơn trực tuyến & Đơn bàn mang đúng ngữ cảnh chi nhánh cơ sở, đơn trực tuyến không bị ép buộc mở phiên bàn.
  - **AT-03**: Kho đa chi nhánh độc lập — Khóa phức hợp `(MaCN, MaNL, ViTri)` hoàn toàn tách biệt giữa các cơ sở.
  - **AT-04**: Trải nghiệm khách hàng — Top sản phẩm bán chạy lọc độc lập theo từng chi nhánh.
  - **AT-05**: Cảnh báo tồn kho theo chi nhánh — Cơ sở hết hàng không làm sai lệch báo cáo cơ sở khác.
  - **AT-06**: Đối soát số học tài chính — Doanh thu chuỗi bằng chính xác tổng doanh thu các chi nhánh thành phần; Dashboard trả về đúng tên chi nhánh và phạm vi.
- Kết quả chạy toàn bộ test suite hệ thống: **112/112 tests PASS (BUILD SUCCESS)**, không có bất kỳ regression nào.

### 3. Phân định Quyền hạn Role Chuẩn hóa:
- **Role ADMIN**: Quản trị viên hệ thống toàn quyền, phụ trách cấu hình cửa hàng, chiết khấu toàn hệ thống, quản lý danh sách chi nhánh và nhân sự.
- **Role MANAGER**: Quản lý chi nhánh tập trung vào vận hành cơ sở (phục vụ món tại chi nhánh, xem kho, duyệt đánh giá của chi nhánh, hỗ trợ quầy thu ngân và bếp), không can thiệp vào cấu hình hệ thống toàn chuỗi.
- Đã gỡ bỏ banner/button giảm giá trùng lặp trong trang `settings.html` và thống nhất quản lý tại `vouchers.html`.

### 4. Tài liệu Vận hành & Bàn giao Hệ thống:
- Đã biên soạn đầy đủ tài liệu: [docs/OPERATIONS_AND_HANDOFF_GUIDE.md](docs/OPERATIONS_AND_HANDOFF_GUIDE.md).
- Cung cấp ma trận tài khoản demo (Admin, Manager CN01, Manager CN02, Cashier, Kitchen, Customer).
- Hướng dẫn quy trình vận hành từ đầu đến cuối: Quét QR đặt món, Bếp nhận & chế biến đơn, Thu ngân thanh toán & đóng phiên, Quản trị viên đối soát báo cáo doanh thu chuỗi vs cơ sở.

---

## 9. Cấu hình Chuẩn hóa Role MANAGER & Phân quyền Chi nhánh Tuyệt đối

### 1. Nguyên tắc Phân quyền Role MANAGER:
- **Phạm vi hoạt động**: Quản lý như một Admin nhưng **chỉ duy nhất trong 1 cửa hàng / chi nhánh** mà Manager được phân công quản lý (`actor.getBranchId()`).
- **Backend Enforced**: Mọi truy vấn, cập nhật dữ liệu đều lấy trực tiếp từ `CurrentActor.getBranchId()`. Không tin cậy bất kỳ `branchId` nào do client gửi lên.

### 2. Các Tab & Tính năng Bị Cấm Tuyệt đối (Forbidden):
- **Cài đặt hệ thống (`/admin/settings`)**: Cấm truy cập cả ở mức URL Security (`hasRole('ADMIN')`), Controller redirect về `/admin`, và ẩn khỏi thanh Sidebar.
- **Khuyến mãi & Voucher (`/admin/vouchers`)**: Cấm truy cập cả ở mức Security, Controller redirect về `/admin`, và ẩn khỏi thanh Sidebar.

### 3. Các Tab & Tính năng Được Quyền Thực thi:
1. **Quản lý QR bàn của chi nhánh (`/admin/tables`)**:
   - Chỉ hiển thị, tạo mới, cập nhật, xóa và lấy mã QR cho các bàn thuộc chi nhánh mình quản lý (`findByBranchIdOrderByIdAsc`). Chặn 403 nếu truy cập bàn của chi nhánh khác.
2. **Báo cáo doanh thu của chi nhánh (`/admin/reports`)**:
   - Tự động cố định và khóa chọn chi nhánh của Manager, hiển thị dữ liệu doanh thu, đơn hàng, top bán chạy và tồn kho chỉ thuộc chi nhánh mình.
3. **Quản lý kho của chi nhánh (`/admin/inventory`)**:
   - Xem tồn kho, thẻ kho biến động (`StockMovement`), kiểm kê cuối ca (`ShiftSettlement`), điều chỉnh tồn, xuất hủy, nhập kho raw material scoped độc lập theo chi nhánh của mình.
4. **Chứng từ của chi nhánh (`/admin/documents`)**:
   - Hóa đơn thanh toán, yêu cầu hủy đơn, duyệt hoàn tiền (`RefundRequest`) được lọc và duyệt độc lập theo chi nhánh.
5. **Tài khoản nhân viên của chi nhánh (`/admin/staff`)**:
   - Manager có quyền cao nhất đối với chi nhánh mình quản lý: xem danh sách, tạo mới, sửa, bật/tắt tài khoản nhân viên của cơ sở mình.
   - **Ràng buộc an toàn**: Manager chỉ được tạo và quản lý tài khoản vai trò `CASHIER` (Thu ngân) hoặc `KITCHEN` (Bếp). Tuyệt đối không thể tạo tài khoản `ADMIN` hay can thiệp nhân viên chi nhánh khác.
6. **Quản lý đơn hàng của chi nhánh (`/admin/orders`)**:
   - Quản lý và theo dõi toàn bộ danh sách đơn hàng thuộc chi nhánh mình.
7. **Món & Danh mục (`/admin/catalog`)**:
   - **Món uống**: Không có quyền thêm mới món, xóa món, sửa tên, sửa size, sửa giá bán, đổi ảnh Cloudinary hay chỉnh sửa định mức công thức (bị chặn 403 tại API và ẩn trên giao diện).
   - **Quyền duy nhất đối với Món**: Được quyền bật/tắt trạng thái món (Đang bán / Ngừng bán) áp dụng riêng cho chi nhánh của mình thông qua `BranchProduct` (`POST /api/v1/admin/products/{id}/toggle-status` -> `BranchService.updateProductAvailability`).
   - **Danh mục**: Manager chỉ được quyền xem (Chỉ xem), tuyệt đối không được xóa hay tạo mới danh mục món.
8. **Đăng nhập & Điều hướng Navbar**:
   - Đăng nhập với role `MANAGER`: Hệ thống tự động chuyển hướng thẳng vào trang quản trị cơ sở `/admin` (thay vì trang khách hoặc trang khác).
   - Brand logo và menu navigation trên header hiển thị huy hiệu `Quản lý cơ sở` và trỏ trực tiếp đến `/admin`.

### 4. Kiểm thử Tự động:
- Tạo mới [ManagerRolePermissionsTest.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/test/java/vn/edu/ute/milktea/service/ManagerRolePermissionsTest.java) gồm 6 ca kiểm thử chuyên biệt.
- Toàn bộ test suite dự án: **125/125 tests PASS (BUILD SUCCESS)**, 0 failures, 0 errors, 0 skipped.

---

## Checkpoint: Kho Hàng Tổng (Central Warehouse), Phân định Quản trị Toàn chuỗi & Sửa lỗi Định mức

### 1. Kho Hàng Tổng (`KHOTONG`) & Nhập kho Chi nhánh 2 chế độ:
- Khởi tạo Kho Hàng Tổng trung tâm (`KHOTONG`) qua migration `V4__central_warehouse.sql` và `BranchService.getOrCreateCentralWarehouse()`.
- **Nhập Kho Tổng từ NCC (Dành cho Admin)**: Đầy đủ thông tin nhà cung cấp, đơn giá nhập, số lượng, lý do, tính giá vốn bình quân gia quyền (`costPrice`) và tăng tồn kho Kho Tổng.
- **Nhập Kho Chi nhánh (Dành cho Chi nhánh)**: Đơn giản hóa, lấy nguyên liệu từ Kho Tổng. Chi nhánh chỉ cần chọn: Nguyên liệu, Số lượng, Lý do.
- **Ràng buộc cung ứng & Kế thừa giá vốn**:
  - Tự động kiểm tra tồn kho tại Kho Tổng. Nếu Kho Tổng không đủ hàng, ném ngoại lệ rõ ràng bằng tiếng Việt (báo chính xác số lượng còn tồn vs số lượng chi nhánh yêu cầu).
  - Tự động giảm tồn kho Kho Tổng với `StockMovementType.XUAT_CHUYEN`.
  - Tự động tăng tồn kho Chi nhánh với `StockMovementType.NHAP` và kế thừa chính xác đơn giá vốn bình quân từ Kho Tổng.

### 2. Phân định Quyền hạn Admin Toàn Chuỗi vs Quản lý Chi nhánh (Role MANAGER):
- **Phân tách Sidebar**:
  - Ẩn mục "Sơ đồ bàn & QR" (`/admin/tables`) và "Quản lý đơn hàng" (`/admin/orders`) đối với vai trò `ADMIN` (do đây là nghiệp vụ đặc thù của từng chi nhánh do Manager điều hành).
  - Vai trò `MANAGER` tiếp tục quản lý bàn và đơn hàng trong phạm vi chi nhánh của mình.
  - Khi Admin truy cập trực tiếp URL `/admin/tables` hoặc `/admin/orders`, hệ thống tự động redirect về `/admin/reports`.
- **Bộ lọc Chi nhánh / Toàn chuỗi**:
  - Hỗ trợ xem báo cáo doanh thu toàn chuỗi (`Toàn chuỗi hệ thống`) khi không chọn chi nhánh (`branchId == null`).
  - Tích hợp bộ lọc Chi nhánh xuyên suốt 5 màn hình quản trị của Admin: Báo cáo (`/admin/reports`), Kho hàng (`/admin/inventory`), Chứng từ (`/admin/documents`), Nhân viên (`/admin/staff`), Món & Danh mục (`/admin/catalog`).
- **Giới hạn nghiệp vụ kho đối với Manager**:
  - Manager không được phép chỉnh sửa đơn giá vốn (`costPrice` / `averageImportPrice`) và không được đổi tên nguyên liệu (chặn 403 tại backend và vô hiệu hóa input tại frontend).
  - Manager không được phép xóa nguyên liệu (nút xóa bị ẩn và API trả 403 `ACCESS_DENIED`).

### 3. Sửa lỗi Định mức Sơ chế & Chặn Manager truy cập:
- Khắc phục triệt để lỗi Hibernate composite primary key collision (`PreparationRecipeItemId`) khi cập nhật định mức sơ chế bằng cách bổ sung `preparationRecipeItemRepository.flush()` ngay sau `deleteByIdRecipeId(id)`.
- Cấu hình định mức sơ chế là nghiệp vụ toàn chuỗi chỉ dành riêng cho Quản trị viên (`ADMIN`).
- Chặn 403 đối với `MANAGER` trên toàn bộ các endpoint định mức sơ chế (`listPreparationRecipes`, `getPreparationRecipe`, `createPreparationRecipe`, `updatePreparationRecipe`, `deletePreparationRecipe`) và ẩn tab trên giao diện.

### 4. Đồng bộ Sidebar & Hiệu ứng Chuyển tiếp mượt mà:
- Chuẩn hóa layout sidebar rộng cố định 280px (`.admin-sidebar-column`), cỡ chữ 13.5px (`font-size: 13.5px !important`), khoảng đệm 11px 16px, hiệu ứng hover trượt nhẹ sang phải (`transform: translateX(4px)` với `cubic-bezier(0.4, 0, 0.2, 1)`), loại bỏ hoàn toàn hiện tượng giật cục hay thay đổi kích thước khi chuyển giữa tab Khuyến mãi & Voucher (`/admin/vouchers`) và các tab khác.

### 5. Kết quả Kiểm thử:
- Bổ sung [CentralWarehouseAndAdminRoleTest.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/test/java/vn/edu/ute/milktea/service/CentralWarehouseAndAdminRoleTest.java) gồm 7 ca kiểm thử tự động.
- Toàn bộ test suite dự án: **125/125 tests PASS (BUILD SUCCESS)**, 0 failures, 0 errors, 0 skipped.

---

## Checkpoint: Tối ưu Phân quyền Đa chi nhánh, Sửa lỗi Nhập kho & Điều chỉnh Tồn kho (28/10/2026)

### 1. Sửa lỗi `NonUniqueResultException` tại Cấu hình định mức (Admin):
- **Nguyên nhân**: Khi mở "Cấu hình định mức", `AdminService.getRawMaterialCost(matId)` truy vấn `stockRepository.findByIdMaterialIdAndIdLocation(matId, KHO)` không chỉ định chi nhánh. Do hệ thống đã khởi tạo tồn kho đa chi nhánh (`KHOTONG`, `CN01`, `CN02`), câu truy vấn trả về 3 dòng gây lỗi `Query did not return a unique result: 3 results were returned`.
- **Giải pháp**: Điều chỉnh phương thức xác định chi nhánh Kho Tổng (`BranchService.getOrCreateCentralWarehouse()`) và truy vấn chính xác theo `findByIdBranchIdAndIdMaterialIdAndIdLocation(centralWarehouse.getId(), matId, KHO)` với cơ chế dự phòng an toàn.

### 2. Sửa lỗi Nhập kho Chi nhánh từ Kho Tổng (`LichSuKho.NguonLoai` Data Truncation):
- **Nguyên nhân**: Cột `NguonLoai` trong bảng `dbo.LichSuKho` có kiểu dữ liệu `VARCHAR(16)`. Trong mã nguồn `InventoryService.recordImport`, hằng số `sourceType` được đặt là `"TRANSFER_TO_BRANCH"` (18 ký tự) và `"IMPORT_FROM_CENTRAL"` (19 ký tự), vượt quá độ dài tối đa dẫn đến lỗi `String or binary data would be truncated`.
- **Giải pháp**: Chuẩn hóa mã nguồn loại thành `"CHUYEN_KHO"` và `"NHAP_TONG"` ($\le 16$ ký tự), loại bỏ triệt để lỗi tràn chuỗi khi nhập kho tại chi nhánh từ kho tổng.

### 3. Khắc phục lỗi Khóa ngoại `Msg 547` trong Migration `V3__voucher_and_discounts.sql`:
- **Nguyên nhân**: Script V3 chèn dữ liệu mẫu vào bảng `dbo.LichSuSuDungVoucher` với các mã đơn hàng giả định cố định (1..13). Khi chạy trên cơ sở dữ liệu thực tế có số lượng đơn hàng ít hơn hoặc ID không liên tục, SQL Server báo lỗi vi phạm khóa ngoại `FK_LSSDV_DonHang`.
- **Giải pháp**: Bổ sung điều kiện kiểm tra `IF EXISTS (SELECT 1 FROM dbo.DonHang WHERE MaDH = ...)` trước khi chèn, bảo đảm an toàn dữ liệu và tính lũy kế (idempotent) trên mọi môi trường.

### 4. Phân định Quyền Điều chỉnh Tồn kho & Ghi nhận Hao hụt:
- **Điều chỉnh Tồn kho (`/admin/inventory`)**:
  - Đối với `MANAGER`: Chỉ được phép cập nhật **Ngưỡng cảnh báo tối thiểu** (`threshold` / `NguongCanhBao`). Các trường Tên nguyên liệu, Số lượng tồn và Giá vốn trung bình đều được khóa chế độ Chỉ đọc (Read-only) cả ở giao diện và xác thực chặt chẽ tại backend (ném `403 ACCESS_DENIED` nếu cố tình gửi lên).
  - Đối với `ADMIN`: Có toàn quyền chỉnh sửa tất cả các trường (Tên, Tồn kho, Giá vốn, Ngưỡng cảnh báo, Lý do) đối với cả Kho Tổng và kho tại các chi nhánh.
- **Ghi nhận Hao hụt kho (`recordWaste`)**:
  - Hỗ trợ tham số `targetBranchId`: tự động xác định phạm vi Kho Tổng (`KHOTONG`) cho vai trò `ADMIN` và Kho Chi nhánh cho vai trò `MANAGER`, áp dụng khóa bi quan `findByIdWithLock(targetBranch.getId(), material.getId(), location)`.

### 5. Khôi phục Quản lý Đơn hàng cho Admin & Lọc Chi nhánh:
- Mở khóa mục **Quản lý đơn hàng** (`/admin/orders`) cho vai trò `ADMIN` trên sidebar toàn bộ 9 màn hình quản trị.
- Bỏ cơ chế chuyển hướng sang Báo cáo (`/admin/reports`) đối với Admin.
- Tích hợp bộ lọc dropdown chọn chi nhánh trên giao diện đơn hàng (`Toàn chuỗi hệ thống` hoặc từng chi nhánh cụ thể), bổ sung badge hiển thị tên chi nhánh trên từng dòng đơn hàng.
- Đồng bộ bộ lọc chứng từ (`/admin/documents`) thể hiện rõ ràng lựa chọn `Kho Hàng Tổng (Toàn Chuỗi) (KHOTONG)`.

### 6. Thắt chặt Bảo mật Phân quyền Thu ngân & Bếp:
- Cấu hình `SecurityConfig.java`:
  - Giới hạn nghiêm ngặt `/cashier`, `/cashier/**`, `/api/v1/cashier/**` chỉ dành riêng cho vai trò `CASHIER`.
  - Giới hạn nghiêm ngặt `/kitchen`, `/kitchen/**`, `/api/v1/kitchen/**` chỉ dành riêng cho vai trò `KITCHEN` hoặc `CHEF`.
  - Tài khoản `ADMIN` và `MANAGER` không còn được tự ý truy cập các trang thao tác nghiệp vụ trực tiếp này.
- Loại bỏ các đường dẫn tắt `[Quầy thu ngân]` và `[Khu vực bếp]` trên thanh điều hướng header đối với vai trò Admin và Manager.
- Cập nhật toàn bộ các bài kiểm thử tự động, kết quả đạt **125/125 tests PASS (BUILD SUCCESS)**.

---

## 10. Checkpoint Prompt 1 — Hoàn thiện Hệ thống Phục vụ Tại quán (In-Store Only System) (HOÀN TẤT)

### 1. Thu hẹp phạm vi nghiệp vụ:
- Xác lập độc quyền 2 luồng vận hành trong không gian quán:
  - **TABLE**: Khách quét mã QR vật lý gắn tại bàn. Nhãn QR thể hiện rõ tên chi nhánh và số bàn. Mở menu cơ sở, gửi đơn đầu tiên tự động mở phiên bàn an toàn bằng Pessimistic Lock, ghi chú bếp đi kèm từng đơn.
  - **COUNTER**: Khách gọi món trực tiếp tại quầy thu ngân. Nhân viên tạo đơn tại quầy, chi nhánh bắt buộc giải quyết trên server từ phân công nhân viên (`actor.branchId`), không tin tưởng client.
- Dỡ bỏ toàn diện các tính năng không thuộc phạm vi phục vụ tại quán: đặt món từ xa, đơn `ONLINE` không gắn bàn, giao nhận tận nơi, sổ địa chỉ, shipper, COD, sàn thương mại, hình thức `PICKUP`/`DELIVERY`, và giỏ hàng tài khoản độc lập (`AccountCart`).
- Đánh dấu R19 (vòng đời đóng cửa chi nhánh) là ngoài phạm vi (OUT_OF_SCOPE).

### 2. Củng cố Backend & Chặn API từ xa:
- `OrderDto.java`: Bổ sung `note` cho `CreateTableOrderRequest`; xóa bỏ DTO tạo đơn online `CreateOnlineOrderRequest`; đánh dấu các trường giao nhận lịch sử trong `OrderResponse` là `@Deprecated` (read-only phục vụ đọc dữ liệu cũ).
- `OrderController.java`: Chuyển đổi `/api/v1/orders/online` và `/customer/orders/online` thành HTTP 410 GONE, trả về thông báo từ chối rõ ràng.
- `OrderService.java`:
  - `createCounterOrder`: Chặn `CUSTOMER` và `Guest` tạo đơn quầy; lấy `targetBranchId` nghiêm ngặt từ `actor.getBranchId()`.
  - `createTableOrder`: Lưu `note` vào `DonHang.GhiChu`; kiểm tra bàn bắt buộc thuộc chi nhánh hợp lệ; tạo phiên an toàn.
  - Xóa bỏ hoàn toàn phương thức `createOnlineOrder`.
- `TableController.java`: `getTableContext` bắt buộc phải có `qrCode` hợp lệ, xóa bỏ hoàn toàn fallback lấy thông tin bàn qua ID/tên từ trình duyệt.
- `CustomerPageController.java`: Bỏ danh sách chi nhánh trên trang giỏ hàng; chỉ nạp bàn khi có QR hợp lệ.
- `SecurityConfig.java`: Loại bỏ `/api/v1/orders/online` khỏi danh sách `permitAll()`.

### 3. Tinh chỉnh Giao diện Khách hàng & Thu ngân:
- `customer.js`: Nhận diện bàn nghiêm ngặt qua QR URL hoặc mã lưu phiên bàn; từ chối submit đơn giao hàng; gửi kèm ghi chú bếp.
- `cart.html`: Xóa bỏ toàn bộ form thông tin giao hàng (họ tên, sđt, địa chỉ, chọn chi nhánh nhận hàng, hình thức nhận). Bổ sung ô nhập ghi chú bếp (`orderNoteInput`) và banner hướng dẫn khách tại quán; vô hiệu hóa nút đặt đơn khi khách chưa quét mã QR bàn.
- `menu.html`: Hiển thị rõ banner `Chi nhánh ... · Bàn ...` khi có ngữ cảnh bàn, hoặc banner nhắc khách quét QR/gọi tại quầy khi là khách vãng lai.
- `cashier/index.html`: Hiển thị rõ tên chi nhánh đang làm việc trên tiêu đề trang thu ngân.

### 4. Cơ sở dữ liệu & Dữ liệu mẫu (Sample Data):
- Migration `database/migrations/V6__in_store_only_cleanup.sql`: Xóa default constraint `HinhThucNhan` trên bảng `dbo.DonHang`, tạo chỉ mục tối ưu truy vấn đơn tại quán `IX_DonHang_Nguon_TrangThai_TaoLuc`. Đã áp dụng thành công trên cơ sở dữ liệu `milktea`.
- `database/sample_data.sql`: Chuẩn hóa 100% dữ liệu mẫu chỉ gồm đơn `TABLE` (84 đơn) và `COUNTER` (24 đơn), 0 đơn `ONLINE`, 0 dữ liệu giao hàng dư thừa. Đã thực thi reseed an toàn.

### 5. Kết quả Kiểm thử Tự động:
- Xây dựng bộ test chấp nhận chuyên biệt `InStoreOnlyScopeAcceptanceTest.java` (6/6 tests PASS).
- Toàn bộ test suite dự án đạt **148/148 tests PASS (BUILD SUCCESS)**, không có bất kỳ cảnh báo lỗi hồi quy nào.

---

## Checkpoint: Bỏ Tab Quản Lý Sơ Đồ Bàn & QR ở Sidebar đối với Role ADMIN (29/10/2026)

### 1. Phân định Giao diện Sidebar:
- Ẩn hoàn toàn tab **Sơ đồ bàn & QR** (`/admin/tables`) trên sidebar đối với vai trò `ADMIN` trên toàn bộ 9 template trang quản trị (`reports.html`, `catalog.html`, `inventory.html`, `documents.html`, `orders.html`, `settings.html`, `staff.html`, `tables.html`, `vouchers.html`).
- Mục này chỉ hiển thị đối với vai trò `MANAGER` (`th:if="${isManager != null and isManager}"`), vì sơ đồ bàn và in mã QR bàn là nghiệp vụ vật lý nội bộ thuộc quyền hạn điều hành của từng cơ sở chi nhánh.

### 2. Bảo vệ Điều hướng & API Backend:
- `StaffPageController.java`: Khi tài khoản `ADMIN` truy cập trực tiếp URL `/admin/tables` hoặc `?section=tables`, hệ thống tự động chuyển hướng (redirect) về `/admin/reports`.
- `AdminController.java`: Kiểm tra `checkNotAdminForTables(actor)`, chặn 403 `ACCESS_DENIED` với thông báo tiếng Việt rõ ràng nếu vai trò `ADMIN` cố tình gọi các REST API quản lý bàn (`/api/v1/admin/tables/**`).

### 3. Kết quả Kiểm thử:
- Bổ sung ca kiểm thử tự động `testAdminRoleForbiddenFromTableAndQrApis` trong [CentralWarehouseAndAdminRoleTest.java](file:///h:/UTE/Nam_3/MilkTea_management_system/milktea/src/test/java/vn/edu/ute/milktea/service/CentralWarehouseAndAdminRoleTest.java).
- Toàn bộ test suite dự án đạt **149/149 tests PASS (BUILD SUCCESS)**, 0 failures, 0 errors, 0 skipped.

---

## 11. Checkpoint: Sửa 6 Mục Hoàn Thiện & Tích Hợp 3 Cổng Thanh Toán VNPAY, MoMo, ZaloPay (PROMPT_GEMINI_SUA_6_MUC_MILKTEA_V5) (HOÀN TẤT)

### 1. Nguyên Tắc Bất Biến: Zero Database DDL & Data Preservation
- **Tuyệt đối không thay đổi schema**: Không thực thi bất kỳ câu lệnh DDL (`ALTER TABLE`, `DROP`, `CREATE TABLE/COLUMN/CONSTRAINT/TRIGGER`), không reset database `milktea`, không chỉnh sửa seed data cũ.
- Tận dụng trường `dbo.ThanhToan.PhuongThuc` (`VARCHAR(16)`) đã có sẵn để lưu các giá trị enum mới `VNPAY`, `MOMO`, `ZALOPAY`.
- Lưu trữ tương quan giao dịch (`txnRef`, trạng thái IPN, chữ ký gateway) bền vững qua các bảng nghiệp vụ có sẵn: `dbo.ThongBaoThanhToan` (`PaymentNotice`) và `dbo.NhatKyNghiepVu` (`BusinessAudit`), đảm bảo dữ liệu không bị thất lạc khi server restart.

### 2. Chi Tiết Thực Hiện 6 Mục Kiểm Định (Items 4.1 – 4.8):

#### Mục 4.1 — Khôi Phục Ngữ Cảnh QR Bàn (QR Context Restore):
- **Phân tách luồng QR rõ ràng**: Tách biệt luồng khách quét mã QR vật lý trực tiếp (`?qr=...`) với luồng khôi phục ngữ cảnh từ token đã lưu (`POST /api/v1/public/table-context/restore`).
- **Server-validated Capability Token**: `GuestAccessService.validateAndGetTableIdFromQrCapabilityToken(token)` xác thực chữ ký token.
- **Ngăn chặn Silent-Grant**: `TableContextService.restoreContext(rawToken)` kiểm tra token phiên hoạt động hoặc token quyền truy cập QR trước phiên. Nếu bàn đã đóng phiên hoặc chuyển sang phiên mới khác (`Session B`), hệ thống ném ngoại lệ 403 `ACCESS_DENIED`, ngăn chặn tuyệt đối việc tự động cấp quyền âm thầm vào phiên mới.
- **Client Frontend (`customer.js`)**: Khi nhận phản hồi không hợp lệ từ endpoint khôi phục, lập tức dọn sạch cache token địa phương, hiển thị banner yêu cầu khách quét lại mã QR tại bàn.

#### Mục 4.2 — Kiểm Tra Chống Lặp (Idempotency Replay) & Canonical Fingerprint:
- **Canonical Payload Fingerprint**: `IdempotencyService.computeFingerprint` chuẩn hóa dữ liệu đầu vào:
  - Sắp xếp danh sách món (`items`) theo `productId`, `quantity`, `note`.
  - Sắp xếp danh mục topping theo `id` tăng dần.
  - Loại bỏ khoảng trắng thừa, đính kèm đầy đủ `note`, `voucherCode`, `remainingItems` và phạm vi người gọi (`callerScope`).
- **Xác thực quyền Caller khi Replay**: `OrderService.createTableOrder` và `createCounterOrder` xác thực người gọi (session token của bàn hoặc username nhân viên quầy) khớp với giao dịch gốc trước khi trả về kết quả lưu trong bản ghi chống lặp `BanGhiChongLap`.
- **Gỡ bỏ Fallback không an toàn**: Loại bỏ hoàn toàn câu lệnh truy vấn dự phòng `orderRepository.findByIdempotencyKey` vốn có thể bỏ qua xác thực quyền sở hữu.
- **Đồng bộ Header Chống Lặp**: `OrderController.resolveIdempotencyKey` kiểm tra cả `Idempotency-Key` và `X-Idempotency-Key`. Nếu cả hai header cùng xuất hiện nhưng có giá trị khác nhau, trả về HTTP 400 Bad Request ngay lập tức.

#### Mục 4.4 — Phân Trang Thực Đơn Backend (12 Món Uống Phân Biệt):
- **Cấu trúc DTO Chuẩn**: `CatalogDto.MenuPageResponse` hỗ trợ `page`, `size`, `totalElements`, `totalPages`, `hasNext`, `hasPrevious`.
- **Nhóm món phân biệt (Distinct Drinks)**: `CatalogService.getPaginatedMenu` nhóm các biến thể theo tên món, chọn biến thể có đơn giá thấp nhất (size nhỏ nhất) để hiển thị trên thực đơn.
- **Lọc Đa Tiêu Chí & Chuẩn Hóa Tiếng Việt**: Lọc theo danh mục, khoảng giá `[minPrice, maxPrice]`, trạng thái khả dụng tại chi nhánh, và từ khóa tìm kiếm (chuẩn hóa loại bỏ dấu tiếng Việt để tìm kiếm "tra sua" hay "trà sữa" đều cho kết quả chính xác).
- **Hỗ trợ Danh Sách Nổi Bật Top 20**: Hỗ trợ 4 bộ sưu tập (`best_seller`, `top_rated`, `most_favorited`, `newest`) với tối đa 20 sản phẩm, phân trang chính xác: trang 1 có 12 món, trang 2 có 8 món.
- **SSR Tối Ưu & Lazy Modal**: `CustomerPageController` chỉ nạp trước trang 1 (12 món) vào HTML SSR, không nhúng toàn bộ danh sách biến thể; chi tiết món được tải lười (lazy load) qua API `/api/v1/public/products/{id}` khi khách mở modal tùy chọn.

#### Mục 4.6 — Kịch Bản Đối Soát Tồn Kho Độc Lập (Inventory Reconciliation):
- **Kịch bản Thuần Đọc**: `database/validation/verify_inventory_reconciliation.sql` được viết lại hoàn toàn thành script chỉ đọc, không thực hiện `UPDATE`/`INSERT`/`DELETE`.
- **Phân định 3 Bất Biến Độc Lập**:
  - **Bất biến 1 (Số học cùng dòng)**: `TonSau = TonTruoc + LuongBienDong` trong `dbo.LichSuKho` $\rightarrow$ Đạt **0/488 lỗi** (**PASS**).
  - **Bất biến 2 (Chuỗi biến động liên tục)**: `LAG(TonSau) = TonTruoc` theo nguyên liệu/kho/chi nhánh $\rightarrow$ Báo cáo trung thực **444 dòng seed lịch sử bị đứt gãy** (**INSUFFICIENT_EVIDENCE** do dữ liệu hạt giống ban đầu chèn phi tuần tự).
  - **Bất biến 3 (Số dư đóng kho vs thẻ kho)**: Đối chiếu số lượng hiện hành trong `dbo.TonNguyenLieu` với bản ghi thẻ kho mới nhất $\rightarrow$ Báo cáo trung thực **147 dòng seed không đối chiếu được** (**UNRECONCILED**).
  - **Dữ liệu Đơn Hàng Runtime (`MaDH >= 225`)**: 19/19 đơn hàng có đầy đủ snapshot công thức; 100% đơn hàng trạng thái `HOAN_THANH` đã được khấu trừ kho Bếp với loại biến động `TIEU_HAO` (**PASS**).

#### Mục 4.7 — Ma Trận Sự Kiện Realtime & Phân Quyền Chi Nhánh:
- **WebSocket Scoped Chi Nhánh**: Khi điều chỉnh ngưỡng cảnh báo hoặc biến động tồn kho tại `InventoryService`, hệ thống gửi thông báo realtime đến topic cô lập `/topic/branches/{targetBranchId}/stock`.
- **Ghi Nhận Nhật Ký Nghiệp Vụ**: Mọi thay đổi đều được ghi vết vào `dbo.NhatKyNghiepVu` (`BusinessAudit`).
- **Khôi phục Kết nối STOMP**: Hỗ trợ xử lý tái kết nối client trên frontend khi mạng gián đoạn.

#### Mục 4.8 — Chuẩn Hóa Báo Cáo Kiểm Thử Theo Bằng Chứng Thực Tế:
- `docs/API_TEST_MATRIX.md`: Cập nhật 33 route templates, 48 ca kiểm thử HTTP thực chạy, 155 unit tests, tách bạch rõ ràng giữa kết quả kiểm thử thực thi mới và nhật ký kiểm thử lịch sử.
- `docs/E2E_TEST_REPORT.md`: Báo cáo chi tiết ma trận kiểm định (QR01-QR02, ID01-ID04, PG01-PG03, SQL01-SQL02, RT01-RT02, MD01, PAY01-PAY06, BR01, EV01); ghi nhận trạng thái trung thực `BLOCKED_BROWSER` cho môi trường sandbox offline không tải được binary Playwright.

---

### 3. Tích Hợp 3 Cổng Thanh Toán Tự Động (VNPAY, MoMo, ZaloPay) (Mục 8):
- **Thay thế VietQR thủ công**: Dỡ bỏ hoàn toàn phương thức chuyển khoản quét QR thủ công vốn dễ sai sót đối soát. Thay thế bằng 3 cổng thanh toán tự động hàng đầu Việt Nam:
  1. **VNPAY**: Tạo URL thanh toán với mã checksum `HMAC-SHA512`, xử lý webhook IPN trả về `RspCode: "00"` (thành công) hoặc `"97"` (chữ ký không hợp lệ).
  2. **MoMo**: Tích hợp phương thức `captureWallet` với chữ ký `HMAC-SHA256`, xử lý IPN trả về `resultCode: 0` (thành công) hoặc `99` (sai chữ ký).
  3. **ZaloPay**: Tạo đơn thanh toán với mã giao dịch `yyMMdd_xxx`, xác thực MAC IPN bằng `key2` (`HMAC-SHA256`), trả về `return_code: 1` hoặc `-1`.
- **Kiến trúc Adapter & Service**:
  - `PaymentGatewayAdapter`: Giao diện chung chuẩn hóa luồng `createPaymentUrl`, `verifyIpn`, `verifyReturnUrl`.
  - `PaymentGatewayService`: Quản lý vòng đời nỗ lực thanh toán (`PaymentAttempt`), kiểm tra quyền truy cập của caller, lưu vết tham chiếu vào `dbo.ThongBaoThanhToan` và `dbo.NhatKyNghiepVu`, thực hiện thanh toán idempotent qua `PaymentService.createPayment`.
  - `PaymentGatewayController`: Cung cấp API tạo attempt `/api/v1/payments/gateways/create-attempt`, các webhook IPN và return URL cho từng cổng.
  - `SecurityConfig`: Cấu hình ngoại lệ CSRF cho các webhook IPN của cổng thanh toán.
  - `RefundService`: Hỗ trợ Admin yêu cầu hoàn tiền điện tử đối với các khoản thanh toán qua cổng.
- **Giao diện Người dùng**:
  - `cashier.html` & `cashier.js`: Tích hợp các lựa chọn VNPAY, MoMo, ZaloPay vào quầy POS thu ngân.
  - `orders.js`: Tích hợp modal thanh toán cổng điện tử cho khách hàng tại bàn.
  - Giữ nguyên phương thức `CASH` (Tiền mặt) và bảo toàn các bản ghi thanh toán `BANK_TRANSFER` cũ trong lịch sử.

---

### 4. Kết Quả Kiểm Thử & Nghiệm Thu:
- **Kiểm thử Đơn vị & Tích hợp (JUnit)**:
  - Tạo mới `PaymentGatewayIntegrationTest.java` (6 ca kiểm thử: PAY01 - PAY06).
  - Toàn bộ test suite dự án đạt **155/155 tests PASS (BUILD SUCCESS)**, 0 failures, 0 errors.
- **Kiểm thử HTTP Thực chạy trên Server 8082**:
  - Script `test_qr_idempotency_gateways.ps1` kiểm tra trực tiếp trên cổng 8082: **7/7 ca kiểm thử thành công 100%**:
    - `QR01`: Cố tình khôi phục ngữ cảnh bằng token rác $\rightarrow$ HTTP 403 `ACCESS_DENIED`.
    - `ID02`: Gửi xung đột giữa `Idempotency-Key` và `X-Idempotency-Key` $\rightarrow$ HTTP 400 Bad Request.
    - `PG01`: Phân trang thực đơn 12 món $\rightarrow$ Trang 1: 12 món, Trang 2: 12 món, Trang 3: 9 món.
    - `PAY01`: Khách trái phép gọi tạo nỗ lực thanh toán $\rightarrow$ Bị chặn 403.
    - `PAY02`: Giả mạo webhook VNPAY với chữ ký sai $\rightarrow$ Trả về `RspCode: "97"`.
    - `PAY03`: Giả mạo webhook MoMo với chữ ký sai $\rightarrow$ Trả về `resultCode: 99`.
    - `PAY04`: Giả mạo webhook ZaloPay với MAC sai $\rightarrow$ Trả về `return_code: -1`.
- **Kiểm định Script Kho Hàng**: `verify_inventory_reconciliation.sql` chạy qua `sqlcmd` thoát mã `0` thành công, phản ánh chính xác hiện trạng dữ liệu.








