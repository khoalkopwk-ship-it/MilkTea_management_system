# BÁO CÁO NGHIỆM THU KIỂM THỬ ĐẦU-CUỐI TOÀN HỆ THỐNG (E2E TEST REPORT)

Báo cáo nghiệm thu kiểm thử toàn diện hệ thống **MilkTea Chain Management System** (Hệ thống chuỗi trà sữa đa chi nhánh), thực hiện sau khi hoàn tất sửa lỗi từ **V4-01 đến V4-16** theo Báo cáo 04.

---

## 1. Môi trường thực thi & Cấu hình kiểm thử

- **Thời gian thực hiện**: 29/10/2026.
- **Mã nguồn ứng dụng**: Spring Boot MVC Monolith (Java 21, Thymeleaf, JPA/Hibernate 7.4.5, Microsoft JDBC Driver 13.4).
- **Hệ điều hành**: Windows 11 Enterprise (x64).
- **Cơ sở dữ liệu**: Microsoft SQL Server (localhost:1433, người dùng `sa`, CSDL thực `milktea`).
- **Phạm vi phục vụ cố định**:
  - Phục vụ **trong quán** (`IN-STORE ONLY`): Đặt tại bàn (`TABLE`) qua mã QR vật lý gắn nhãn bàn/chi nhánh; Đặt tại quầy (`COUNTER`) bởi nhân viên thu ngân có thẩm quyền.
  - Tuyệt đối loại trừ: Không phục vụ đặt hàng online từ xa (`ONLINE`), không có giao hàng/shipper, không có địa chỉ giao hàng. Endpoint `/api/v1/orders/online` phản hồi HTTP `410 GONE`.
- **Dữ liệu mẫu**: Đầy đủ 3 chi nhánh (`KHOTONG`, `CN01`, `CN02`), 77 phiếu nhập kho nhà cung cấp, 28 phiếu xuất chuyển kho, 18 mẻ sơ chế, 172 đơn hàng, 4 chương trình voucher.

---

## 2. Kết quả kiểm thử các luồng nghiệp vụ cốt lõi (FLOW01 – FLOW16)

### FLOW01: Quét mã QR tại bàn & Khởi tạo phiên ngữ cảnh (V4-01, V4-04)
- **Thực nghiệm**:
  - Gửi request `GET /api/v1/tables/context` không có tham số QR -> Máy chủ từ chối `400 QR_INVALID`.
  - Gửi request với mã QR không tồn tại `GET /api/v1/tables/context?qr=FAKE_QR_9999` -> Máy chủ từ chối `404 TABLE_NOT_FOUND`.
  - Quét mã QR hợp lệ `QR_CN01_BAN03` của Bàn 03 tại Chi nhánh 1 -> Máy chủ trả lời `200 OK`, cung cấp đầy đủ thông tin ngữ cảnh (`tableId=9`, `tableName="Bàn 03"`, `branchId=1`, `branchName="Chi nhánh 1 - Võ Văn Ngân"`) kèm theo token năng lực `tableToken` (HMAC-SHA256, thời hạn 24 giờ).
- **Đánh giá**: **ĐẠT (PASSED)**.

### FLOW02: Đặt món tại bàn lần đầu & Mở phiên bàn tự động (V4-01, V4-12)
- **Thực nghiệm**:
  - Khách vãng lai gửi request đặt món `POST /api/v1/orders/table` nhưng không đính kèm `tableToken` -> Máy chủ chặn `403 FORBIDDEN` ("Cần quét mã QR hợp lệ của bàn để đặt món").
  - Khách dùng token của Bàn 06 gửi request đặt món cho Bàn 07 -> Máy chủ chặn `403 FORBIDDEN` do sai lệch capability token.
  - Khách gửi request hợp lệ kèm đúng `tableToken`:
    - Máy chủ tự động mở phiên bàn mới `TableSession` (`TrangThai = 'OPEN'`).
    - Cập nhật trạng thái bàn sang `CO_KHACH` và gắn `MaPhienDangMo`.
    - Tạo đơn hàng `Order` ở trạng thái `CHO_XAC_NHAN`, tạo hóa đơn `HoaDon` với giá trị snapshot chuẩn xác.
    - Cấp `guestTableToken` để khách tiếp tục gọi món bổ sung trong phiên mà không cần quét lại mã QR.
- **Đánh giá**: **ĐẠT (PASSED)** (Mã phản hồi HTTP `201 CREATED`).

### FLOW03: Gọi thêm món trong phiên bàn & Đồng bộ giỏ hàng (Shared Cart)
- **Thực nghiệm**:
  - Trong cùng phiên bàn đang mở, khách gửi request cập nhật giỏ hàng dùng chung `PUT /api/v1/table-sessions/{id}/cart`.
  - Giỏ hàng được lưu trữ phía máy chủ trong `SessionCart` và `SessionCartItem`.
  - Hệ thống phát sự kiện realtime STOMP tới topic `/topic/table-sessions/{id}` để tất cả các thiết bị cùng ngồi tại bàn cập nhật giao diện ngay lập tức.
  - Khi khách gửi lệnh đặt đơn tiếp theo trong cùng phiên với `expectedSessionId` trùng khớp -> Đơn hàng bổ sung được tạo thành công, gắn vào cùng phiên bàn.
- **Đánh giá**: **ĐẠT (PASSED)**.

### FLOW04: Tạo đơn tại quầy mang về (COUNTER) & Ép chi nhánh công tác (V4-02)
- **Thực nghiệm**:
  - Khách hàng cá nhân tự ý gọi API tạo đơn quầy `POST /api/v1/orders/counter` -> Bị máy chủ chặn `403 FORBIDDEN` ("Customer không thể tạo đơn COUNTER").
  - Nhân viên thu ngân Chi nhánh 1 (`cashier@milktea.vn`) gửi request tạo đơn mang về, cố tình truyền `branchId = 2` trên client payload -> Server tự động ghi đè và gán đúng `branchId = 1` theo chi nhánh công tác của nhân viên.
  - Đơn hàng quầy được khởi tạo ở trạng thái `CHO_THANH_TOAN` (hỗ trợ thanh toán trước hoặc sau pha chế).
- **Đánh giá**: **ĐẠT (PASSED)** (Mã phản hồi HTTP `201 CREATED`, `branchId` ghi nhận = 1).

### FLOW05: Thu ngân xác nhận đơn & Bếp chế biến & Trừ kho thực tế (V4-09, V4-13)
- **Thực nghiệm**:
  - Thu ngân gọi `POST /api/v1/cashier/orders/{id}/confirm` -> Đơn hàng chuyển sang `CHO_CHE_BIEN`. Phát sự kiện realtime tới `/topic/branches/1/kitchen`.
  - Nhân viên Bếp Chi nhánh 2 (`kitchen2@milktea.vn`) cố tình can thiệp bấm hoàn thành đơn của Chi nhánh 1 -> Máy chủ chặn `403 FORBIDDEN` do vi phạm phân vùng chi nhánh.
  - Nhân viên Bếp Chi nhánh 1 (`kitchen@milktea.vn`) gọi `POST /api/v1/kitchen/orders/{id}/start` -> Đơn chuyển sang `DANG_CHE_BIEN`.
  - Nhân viên Bếp Chi nhánh 1 gọi `POST /api/v1/kitchen/orders/{id}/complete`:
    - Đơn hàng chuyển trạng thái `HOAN_THANH`.
    - Hệ thống tự động kích hoạt trừ tồn kho nguyên liệu tại kho Bếp (`location = BEP`) theo đúng định mức công thức snapshot (`DinhMucMon`), bao gồm cả topping chọn thêm (Trân châu hoàng gia, Pudding, Đường, Đá).
    - Lập phiếu xuất tiêu hao trong `LichSuKho`, bảo toàn tính liên tục của sổ cái: `TonSau = TonTruoc - LuongTieuHao`.
    - Phát sự kiện realtime AFTER_COMMIT tới `/topic/branches/1/stock`.
- **Đánh giá**: **ĐẠT (PASSED)**.

### FLOW06: Thu tiền thanh toán & Đóng phiên bàn (V4-09, V4-10)
- **Thực nghiệm**:
  - Thu ngân thực hiện ghi nhận thanh toán tiền mặt cho hóa đơn: `POST /api/v1/cashier/invoices/{id}/payments`.
  - Hệ thống kiểm tra số tiền phải trả: `PhaiTra = max(0, TongTien - TienGiam - TienGiamVoucher)`.
  - Tạo bản ghi thanh toán `ThanhToan` (`TrangThai = 'THANH_CONG'`), cập nhật hóa đơn sang `DA_THANH_TOAN`.
  - Thu ngân thực hiện đóng phiên bàn: `POST /api/v1/cashier/tables/{id}/close-session`.
  - Máy chủ kiểm tra điều kiện đóng phiên: Toàn bộ các đơn hàng trong phiên đã hoàn thành và hóa đơn đã thu đủ tiền. Phiên chuyển sang `CLOSED`, bàn chuyển về `TRONG`, thu hồi capability token của phiên.
- **Đánh giá**: **ĐẠT (PASSED)** (Khớp 100% ràng buộc `CK_HoaDon_PhaiTra_Calc`).

### FLOW07: Đặt lại mật khẩu & Kích hoạt tài khoản qua OTP (V4-05)
- **Thực nghiệm**:
  - Người dùng yêu cầu gửi OTP đặt lại mật khẩu qua email.
  - Thử nghiệm nhập sai OTP liên tiếp 5 lần: Hệ thống ghi nhận số lần thử sai qua transaction độc lập `REQUIRES_NEW`, tự động khóa và thu hồi mã OTP.
  - Khi nhập đúng OTP còn hạn: Cập nhật mật khẩu mã hóa BCrypt, tăng `tokenVersion` để vô hiệu hóa toàn bộ các phiên JWT cũ.
- **Đánh giá**: **ĐẠT (PASSED)** (Đạt 100% test `AuthEmailVerificationFlowTest`).

### FLOW08: Nhập kho nguyên liệu & Đối ứng điều chuyển kho chuỗi (V4-07, V4-08)
- **Thực nghiệm**:
  - Quản lý tạo phiếu nhập kho từ nhà cung cấp về Kho Tổng: Ghi tăng tồn kho và ghi nhật ký sổ cái kho.
  - Điều chuyển nguyên liệu từ Kho Tổng về Chi nhánh 1: Hệ thống ghi nhận chính xác 2 giao dịch đối ứng (xuất kho nguồn và nhập kho đích), đảm bảo tổng lượng điều chuyển bảo toàn, không bị thất thoát trên sổ cái.
- **Đánh giá**: **ĐẠT (PASSED)**.

### FLOW09: Sơ chế nguyên liệu tại bếp chi nhánh (V4-08)
- **Thực nghiệm**:
  - Bếp thực hiện mẻ sơ chế trân châu/trà ủ: Hệ thống trừ tồn kho nguyên liệu thô (Trà khô, Trân châu sống, Đường cát) và ghi tăng tồn kho thành phẩm sơ chế (Trân châu nấu, Trà ủ cốt) theo tỷ lệ yield quy định.
- **Đánh giá**: **ĐẠT (PASSED)**.

### FLOW10: Kiểm kê kho & Kết ca kho Bếp (V4-10, V4-11)
- **Thực nghiệm**:
  - Bếp gọi `GET /api/v1/inventory/shift-settlement/preview?location=BEP&branchId=1`: Nhận bảng xem trước chênh lệch tồn thực tế và tồn lý thuyết.
  - Thu ngân truy cập endpoint này bị chặn `403 FORBIDDEN`.
  - Bếp thực hiện chốt ca `POST /api/v1/inventory/shift-settlement`: Hệ thống xử lý phân loại lượng loại bỏ (`LuongLoaiBo`) và lượng giữ lại (`LuongGiuLai`), tuân thủ tuyệt đối ràng buộc `CK_ChiTietKetCa_ThucTe: TonThucTe = LuongLoaiBo + LuongGiuLai`.
- **Đánh giá**: **ĐẠT (PASSED)**.

### FLOW11: Áp dụng mã giảm giá Voucher & Giới hạn lượt dùng (V4-15)
- **Thực nghiệm**:
  - Khách hàng áp dụng voucher `CHAOMUNG` cho đơn hàng 100.000đ: Hệ thống kiểm tra điều kiện tối thiểu 50.000đ, hạn mức sử dụng theo khách hàng (`SoLuotMoiKhach = 20`) và áp dụng thành công mức giảm 20.000đ (`discountAmount = 20000.00`, `finalSubtotal = 80000.00`).
  - Kiểm tra voucher chi nhánh `CN01SPECIAL`: Áp dụng thành công cho đơn tại Chi nhánh 1; từ chối khi đặt tại Chi nhánh 2.
- **Đánh giá**: **ĐẠT (PASSED)**.

### FLOW12: Hủy đơn hàng & Hoàn trả voucher / kho (V4-09, V4-15)
- **Thực nghiệm**:
  - Khi đơn hàng ở trạng thái `CHO_XAC_NHAN` bị hủy: Voucher đã áp dụng được tự động giải phóng (giảm `SoLuotDaDung` và cập nhật lịch sử dùng sang `DA_HOAN`).
  - Đạt 100% kiểm thử trong `Phase5VoucherAndDiscountsTest`.
- **Đánh giá**: **ĐẠT (PASSED)**.

### FLOW13: Đánh giá món ăn & Yêu thích (V4-14, V4-15)
- **Thực nghiệm**:
  - Gửi đánh giá món ăn với nội dung dưới 50 ký tự (`"Ngon qua"`) -> Máy chủ từ chối `400 VALIDATION_FAILED` ("Nội dung đánh giá phải từ 50 đến 1000 ký tự").
  - Thêm món vào danh sách yêu thích `POST /api/v1/favorites/1` -> Trả về `200 OK` với thông tin cập nhật trạng thái yêu thích.
- **Đánh giá**: **ĐẠT (PASSED)**.

### FLOW14: Yêu cầu hoàn tiền & Quyết định của Quản trị viên (V4-11, V4-16)
- **Thực nghiệm**:
  - Khách hàng gửi yêu cầu hoàn tiền cho đơn hàng gặp sự cố.
  - Quản lý chi nhánh chỉ có quyền đề xuất; chỉ duy nhất Quản trị viên hệ thống (`ADMIN`) có thẩm quyền gọi `POST /api/v1/admin/refunds/{id}/decision` để phê duyệt hoặc từ chối hoàn tiền.
- **Đánh giá**: **ĐẠT (PASSED)**.

### FLOW15: Báo cáo kinh doanh phân quyền chi nhánh (V4-11, V4-15)
- **Thực nghiệm**:
  - Quản lý Chi nhánh 1 (`manager1@milktea.vn`) xem dashboard: Trả về số liệu doanh thu, số lượng đơn và giảm giá của Chi nhánh 1.
  - Quản lý Chi nhánh 1 cố tình xem báo cáo Chi nhánh 2 -> Bị chặn `403 FORBIDDEN`.
  - Admin (`admin@milktea.vn`) xem danh sách nhân sự toàn hệ thống `GET /api/v1/admin/accounts` -> Trả về `200 OK` đầy đủ nhân sự 3 chi nhánh.
- **Đánh giá**: **ĐẠT (PASSED)**.

### FLOW16: Từ chối dứt khoát luồng đặt hàng Online (In-store Only Scope)
- **Thực nghiệm**:
  - Gửi request tới `POST /api/v1/orders/online` -> Máy chủ phản hồi ngay lập tức với mã HTTP `410 GONE`:
    ```json
    {
      "success": false,
      "error": {
        "code": "VALIDATION_FAILED",
        "message": "Hệ thống chỉ phục vụ trong không gian quán (TABLE/COUNTER). Chức năng đặt hàng online từ xa đã ngừng hoạt động."
      }
    }
    ```
- **Đánh giá**: **ĐẠT (PASSED)**.

---

## 3. Kiểm thử Concurrency & Bảo vệ Idempotency (V4-10, V4-12)

### 3.1. Chạy đua song song hai tiến trình đặt đơn trùng Idempotency-Key
- **Kịch bản**: Khởi tạo 2 Background Job PowerShell độc lập, cùng gửi đồng thời một request tạo đơn quầy với cùng `X-Idempotency-Key = CONCURRENCY-TEST-1126893473` và cùng payload món.
- **Kết quả thực tế**:
  - Tiến trình 1: Mã phản hồi `201 CREATED`, `OrderId = 243`.
  - Tiến trình 2: Mã phản hồi `201 CREATED`, `OrderId = 243`.
  - Trong cơ sở dữ liệu `dbo.DonHang`: Chỉ sinh ra duy nhất **1 bản ghi đơn hàng ID 243**.
  - Kết luận: Bảng chống lặp `dbo.BanGhiChongLap` và rào chắn Idempotency hoạt động hoàn hảo dưới tải đồng thời.

### 3.2. Kiểm thử bất biến cơ sở dữ liệu `UQ_PhienBan_MaBan_Open`
- **Kịch bản**: Cố tình thực hiện câu lệnh SQL chèn một phiên mở thứ hai cho Bàn 12 (bàn hiện đang có phiên mở ID 162):
  ```sql
  INSERT INTO dbo.PhienBan (MaBan, TrangThai, MoLuc, Version) VALUES (12, 'OPEN', SYSUTCDATETIME(), 0);
  ```
- **Kết quả thực tế**:
  - SQL Server ném lỗi vi phạm khóa duy nhất:
    `Cannot insert duplicate key row in object 'dbo.PhienBan' with unique index 'UQ_PhienBan_MaBan_Open'. The duplicate key value is (12).`
  - Câu lệnh bị hủy bỏ ngay lập tức (Rollback). Không thể tồn tại 2 phiên mở đồng thời trên cùng một bàn.

---

## 4. Kết quả kiểm tra đối soát CSDL SQL Server (V4-06)

Đã chạy trực tiếp 3 tập lệnh kiểm tra đối soát trên database `milktea`:

1. **`database/validation/verify_inventory_reconciliation.sql`**:
   - Kiểm tra cân bằng tồn kho nguyên liệu chi nhánh.
   - Kiểm tra tính liên tục của sổ cái lịch sử kho (`TonSau = TonTruoc + LuongBienDong`).
   - Kiểm tra tính toàn vẹn của phiếu nhập, phiếu xuất chuyển và mẻ sơ chế.
   - **Kết quả**: **0 vi phạm detected (THÀNH CÔNG)**.

2. **`database/validation/verify_financial_reconciliation.sql`**:
   - Kiểm tra công thức số tiền phải trả: `PhaiTra = max(0, TongTien - TienGiam - TienGiamVoucher)`.
   - Kiểm tra tổng tiền các lần thanh toán khớp với hóa đơn đã thu.
   - Kiểm tra không có hóa đơn mồ côi đơn hàng.
   - **Kết quả**: **0 vi phạm detected (THÀNH CÔNG)**.

3. **`database/validation/verify_demo_integrity.sql`**:
   - Kiểm tra tính toàn vẹn của các bàn, tài khoản mẫu demo, quyền chi nhánh của nhân viên.
   - Kiểm tra không có bàn nào vi phạm trạng thái phiên.
   - **Kết quả**: **0 vi phạm detected (THÀNH CÔNG)**.

---

## 5. Tổng kết nghiệm thu toàn diện

| Hạng mục kiểm tra | Số lượng ca kiểm tra | Số lượng ĐẠT | Tỷ lệ ĐẠT | Kết luận |
|---|:---:|:---:|:---:|:---:|
| **Kiểm thử đơn vị Maven (`mvn test`)** | 149 tests | 149 | 100% | **XUẤT SẮC** |
| **Bề mặt API HTTP thực tế (Port 8080)** | 41 endpoints | 41 | 100% | **XUẤT SẮC** |
| **Giao diện HTML Thymeleaf thực tế** | 4 trang chính | 4 | 100% | **XUẤT SẮC** |
| **Kiểm thử bất biến & Concurrency** | 2 kịch bản | 2 | 100% | **XUẤT SẮC** |
| **Đối soát dữ liệu SQL Server** | 3 tập lệnh fail-fast | 3 | 100% | **XUẤT SẮC** |

**Kết luận chung**: Hệ thống MilkTea Multi-Branch đã hoàn tất toàn bộ các yêu cầu sửa lỗi và nghiệm thu từ V4-01 đến V4-16. Ứng dụng hoạt động ổn định, bảo mật chặt chẽ theo chi nhánh, bảo toàn dữ liệu tài chính - kho hàng và đáp ứng đầy đủ tiêu chuẩn nghiệm thu thực tế.
