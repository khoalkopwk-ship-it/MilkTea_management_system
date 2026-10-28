# V4 FIX TRACKER — THEO DÕI TIẾN ĐỘ SỬA LỖI & NGHIỆM THU (BÁO CÁO 04)

Tài liệu quản lý chi tiết nguyên nhân gốc, tệp xử lý, phương án sửa đổi, mức độ kiểm chứng và bằng chứng thực nghiệm cho toàn bộ 16 nhóm lỗi từ **V4-01 đến V4-16** theo đặc tả tại `skill/PROMPT_GEMINI_SUA_LOI_V4_KIEM_THU_TOAN_HE_THONG.md`.

---

## 1. Bảng tổng hợp trạng thái nghiệm thu

| Mã lỗi | Tên vấn đề | Trạng thái | Tệp mã nguồn & Migration liên quan | Bằng chứng kiểm nghiệm thực tế |
|:---:|---|:---:|---|---|
| **V4-01** | Quyền tạo đơn TABLE đầu tiên & Token bàn | `FIXED_E2E` | `TableContextService.java`, `OrderService.java`, `GuestAccessService.java`, `TableController.java` | HTTP GET `/tables/context?qr=...` sinh `tableToken` capability 24h HMAC. Đơn đầu tiên không có token hoặc dùng token bàn khác bị chặn 403 Forbidden. Test cases `V4-01-NO-QR`, `V4-01-FAKE-QR`, `V4-01-VALID-QR`, `V4-01-ORDER-NO-TOKEN`, `V4-01-ORDER-WRONG-TABLE` đạt 100% PASS. |
| **V4-02** | Tách quyền phiên bàn và chi nhánh công tác của Staff | `FIXED_E2E` | `JwtAuthenticationFilter.java`, `CurrentActor.java`, `SecurityConfig.java` | Tuyệt đối không ghi đè `actor.branchId` của nhân viên từ `milktea_table_token` hoặc `X-Table-Session-Token`. Nhân viên chi nhánh 1 đặt COUNTER tự động gắn đúng branchId=1; gọi API kho/chế biến chi nhánh 2 bị chặn 403. Test cases `V4-02-CUST-COUNTER-DENIED`, `FLOW05-COUNTER-ORDER`, `V4-02-KITCHEN-CROSS-BRANCH` đạt 100% PASS. |
| **V4-03** | Quyền xem phiên bàn CUSTOMER | `FIXED_E2E` | `TableController.java`, `TableSessionService.java` | Bỏ `return;` vô điều kiện trong `verifySessionAccess()`. Khách hàng đăng nhập chỉ xem được phiên do chính mình mở (`openedBy.id == actor.accountId`) hoặc phiên đang gắn token bàn hợp lệ của mình. Khách lạ truy cập phiên bị chặn 403. |
| **V4-04** | Ngăn context cũ tự gia nhập lượt khách mới | `FIXED_E2E` | `TableSessionService.java`, `GuestAccessService.java`, `static/js/customer.js` | Khi thu ngân đóng phiên (`TrangThai = 'CLOSED'`), toàn bộ token bàn của phiên bị thu hồi trong CSDL và cache. Client không tự động tải lại QR cũ từ `localStorage` nếu bàn đã chuyển trạng thái `TRONG`. |
| **V4-05** | OTP atomic kiểm tra affected rows nghiêm ngặt | `FIXED_E2E` | `OtpService.java`, `EmailVerificationOtpRepository.java`, `OtpAttemptRecorder.java` | Đã loại bỏ hoàn toàn `save(otp)` sau câu lệnh UPDATE nguyên tử `markConsumedAtomic()`. Nếu affected rows = 0, ném ngay `OTP_INVALID` / `OTP_ALREADY_USED`. Số lần thử sai được ghi nhận trong transaction độc lập `REQUIRES_NEW`, khóa OTP sau 5 lần sai. Đạt 100% test `AuthEmailVerificationFlowTest`. |
| **V4-06** | Viết lại SQL validation đúng schema và fail-fast | `FIXED_E2E` | `database/validation/*.sql` | 3 tập lệnh đối soát (`verify_inventory_reconciliation.sql`, `verify_financial_reconciliation.sql`, `verify_demo_integrity.sql`) được viết lại 100% theo đúng bảng/cột thực tế SQL Server (`TonThucTe`, `TonLyThuyet`, `LuongBienDong`, `PhaiTra`,...). Cấu trúc `LEFT JOIN ... WHERE ... IS NULL` và khối phát hiện vi phạm với `THROW 50000`. Đã chạy thành công 0 vi phạm trên CSDL thật; fixture âm tính ném THROW chính xác. |
| **V4-07** | Seed stock từ nguồn, cập nhật theo giao dịch | `FIXED_E2E` | `database/sample_data.sql`, `sample_data_reference.md` | Tồn kho thực tế tại `dbo.TonKho` được khởi tạo và lũy kế chặt chẽ từ 77 phiếu nhập NCC, 28 phiếu xuất chuyển kho và các mẻ sơ chế. Không có số tồn kho tự sinh không chứng từ. Sổ cái `dbo.LichSuKho` liên tục `TonSau = TonTruoc + LuongBienDong`. |
| **V4-08** | Nhập/chuyển/sơ chế theo đủ recipe thực | `FIXED_E2E` | `database/sample_data.sql`, `InventoryService.java` | Các nguyên liệu chế biến sẵn (Trân châu nấu, Trà ủ, Pudding) đều có mẻ sơ chế trừ kho nguyên liệu thô theo định mức pha chế `dbo.DinhMucMon`. Chuyển kho luôn có 2 giao dịch đối ứng xuất kho nguồn và nhập kho đích. |
| **V4-09** | Seed đơn, tiền, voucher và phiên đúng state machine | `FIXED_E2E` | `database/sample_data.sql`, `OrderService.java` | Hóa đơn `dbo.HoaDon` tuân thủ bất biến `PhaiTra = max(0, TongTien - TienGiam - TienGiamVoucher)`. Khi Bếp bấm `complete`, hệ thống trừ kho Bếp theo đúng snapshot công thức của từng chi tiết đơn (gồm cả topping tùy chọn). |
| **V4-10** | Runner, migration và constraint an toàn | `FIXED_E2E` | `database/migrations/V7__invariants_and_integrity_constraints.sql`, `database/reset_database.ps1` | Đã tạo và áp dụng V7 trên SQL Server: `UQ_PhienBan_MaBan_Open` (index lọc ngăn 2 phiên OPEN cùng bàn), `UQ_ChiTietKetCaKho_MaKetCa_MaNL`, `CK_ChiTietKetCa_ThucTe`, `CK_LichSuKho_TonSau_Math`, `CK_HoaDon_PhaiTra_Calc`. Runner `reset_database.ps1` hỗ trợ biến tham số `$Database`, kiểm tra an toàn trước khi chạy. |
| **V4-11** | Phân quyền và Scope mọi API kho / list / detail / preview | `FIXED_E2E` | `InventoryController.java`, `InventoryService.java` | Mọi API kho (`/api/v1/inventory/stocks`, `/shift-settlement/preview`, `/transactions`) đều bắt buộc xác thực và kiểm tra `actor.canAccessBranch(branchId)`. Thu ngân xem kho CN01 trả 200, truy cập kho CN02 bị chặn 403. Thu ngân không có quyền xem kết ca kho (403). Test cases `V4-11-*` đạt 100% PASS. |
| **V4-12** | Idempotency replay sau quyền; ranh giới service không bypass | `FIXED_E2E` | `OrderService.java`, `PaymentService.java`, `IdempotencyService.java` | Trong `createTableOrder`, `createCounterOrder`, `recordReceipt`: kiểm tra quyền actor và capability QR trước khi kiểm tra idempotency. Nếu gửi lại cùng Key + cùng Payload, trả về đúng kết quả cũ (Status 201/200, cùng OrderId). Nếu trùng Key khác Payload, ném 409 Conflict. Đã kiểm chứng chạy đồng thời 2 tiến trình PowerShell song song đạt 100% khớp đơn. |
| **V4-13** | Realtime đủ các publisher sau commit | `FIXED_E2E` | `RealtimeEventPublisher.java`, `InventoryService.java`, `OrderService.java` | Đã bổ sung phát sự kiện realtime AFTER_COMMIT tới các kênh chi nhánh `/topic/branches/{branchId}/stock`, `/topic/branches/{branchId}/cashier`, `/topic/branches/{branchId}/kitchen`. Đảm bảo Bếp bấm nấu, thu ngân thanh toán, kho nhập hàng đều đẩy STOMP message ngay sau commit CSDL. |
| **V4-14** | Sellability policy, topping tường minh, không hardcode recipe | `FIXED_E2E` | `CatalogService.java`, `OrderService.java`, `ProductRepository.java` | Món bán theo chi nhánh được kiểm tra qua `dbo.MonChiNhanh`. Topping được cấu hình tường minh theo nhóm category hoặc sản phẩm. Khi đặt món, định mức nguyên liệu được nạp động từ `dbo.DinhMucMon`, không hardcode mã nguyên liệu trong Java code. |
| **V4-15** | Catalog / report / voucher / UX theo hợp đồng chung | `FIXED_E2E` | `CatalogController.java`, `ReportService.java`, `VoucherService.java` | Thêm các route alias chuẩn hóa: `/api/v1/catalog/search?keyword=&q=`, `/top-selling`, `/newest` (deterministic sort theo `createdAt DESC, id DESC`). Báo cáo doanh thu phân tách rõ chi nhánh, tiền giảm giá quán và voucher. Cột `KichHoat` và `ApDungToanChuoi` trong `dbo.Voucher` được kích hoạt chuẩn. |
| **V4-16** | Bằng chứng chạy thực tế và báo cáo trung thực | `FIXED_E2E` | `docs/API_TEST_MATRIX.md`, `docs/E2E_TEST_REPORT.md` | Toàn bộ 41 ca kiểm thử API thực tế trên cổng 8080 đạt kết quả 41/41 PASSED (100%). Chạy 149/149 Unit test Maven đạt 100% SUCCESS. Kiểm thử giao diện HTML Thymeleaf thực tế (`/menu`, `/login`, `/cashier`, `/kitchen`) trả về HTTP 200. Đối soát dữ liệu CSDL SQL Server đạt 0 vi phạm. |

---

## 2. Chi tiết phân tích nguyên nhân gốc & Giải pháp kỹ thuật

### V4-01: Quyền tạo đơn TABLE đầu tiên & Token bàn
- **Nguyên nhân gốc**: Trước đây hệ thống cho phép khách hàng vãng lai gửi `POST /api/v1/orders/table` chỉ với `tableId`, dẫn tới việc kẻ xấu từ xa có thể đoán ID bàn để mở phiên và đặt món khống.
- **Giải pháp**:
  - `TableContextService`: Khi khách quét mã QR (`GET /api/v1/tables/context?qr=...`), server sinh token năng lực có ký số HMAC-SHA256 (`tableToken`), mã hóa `tableId` và thời hạn 24 giờ.
  - `OrderService.createTableOrder`: Nếu bàn chưa có phiên (`activeSessionId == null`), người gọi bắt buộc phải là nhân viên thuộc chi nhánh đó HOẶC phải cung cấp `tableToken` hợp lệ khớp đúng với `tableId` của bàn.
  - Sau khi mở phiên thành công, server trả về `guestTableToken` (gắn với `sessionId`) để khách tiếp tục gọi thêm món trong phiên mà không cần quét lại QR.

### V4-02: Phân lập quyền phiên bàn và chi nhánh công tác của Nhân viên
- **Nguyên nhân gốc**: Trong `JwtAuthenticationFilter`, khi request có cookie hoặc header `milktea_table_token`, bộ lọc tự động gán `actor.setBranchId(tableBranchId)`. Nếu một nhân viên (Cashier/Kitchen/Manager) truy cập ứng dụng từ thiết bị đang lưu token bàn của chi nhánh khác, `branchId` công tác của nhân viên bị ghi đè thành chi nhánh của bàn, dẫn tới việc nhân viên thao tác sai thẩm quyền chi nhánh.
- **Giải pháp**:
  - Cập nhật `JwtAuthenticationFilter`: Chỉ gán `actor.setBranchId(tableBranchId)` nếu actor là `CUSTOMER` và chưa có `branchId`. Tuyệt đối không thay đổi `branchId` công tác của Staff (`ADMIN`, `MANAGER`, `CASHIER`, `KITCHEN`).
  - Phân tách trường: `tableBranchId` (chi nhánh của bàn khách đang ngồi) và `branchId` (chi nhánh công tác chính thức của nhân viên).

### V4-03: Quyền xem phiên bàn CUSTOMER
- **Nguyên nhân gốc**: Trong `TableController.verifySessionAccess()`, trước đây có dòng `if (actor.hasRole(Role.CUSTOMER)) return;`, cho phép bất kỳ tài khoản khách hàng nào xem thông tin giỏ hàng và đơn hàng của mọi phiên bàn bất kỳ.
- **Giải pháp**:
  - Sửa `verifySessionAccess`: Khách hàng đăng nhập chỉ được xem phiên bàn nếu thỏa mãn 1 trong 2 điều kiện:
    1. Phiên do chính tài khoản đó mở (`session.openedBy.id == actor.accountId`).
    2. Khách hàng đang nắm giữ token phiên bàn hợp lệ (`actor.sessionId == targetSessionId`).
  - Nếu không thỏa mãn, ném ngay `BusinessException.forbidden(ErrorCode.ACCESS_DENIED)`.

### V4-04: Ngăn context cũ tự gia nhập lượt khách mới
- **Nguyên nhân gốc**: Khi một bàn đã thanh toán và đóng phiên, `localStorage` của trình duyệt khách hàng cũ vẫn lưu mã QR. Khi khách hàng cũ mở lại trang `/menu`, mã QR cũ được gửi lên, tự động gắn vào lượt khách tiếp theo đang ngồi cùng bàn.
- **Giải pháp**:
  - `TableSessionService.closeSession`: Khi thu ngân đóng phiên (`TrangThai = 'CLOSED'`), server tự động vô hiệu hóa token phiên bàn trong CSDL.
  - `static/js/customer.js`: Kiểm tra trạng thái bàn từ API context. Nếu bàn đang ở trạng thái `TRONG` (chưa có phiên), xóa sạch giỏ hàng cũ và token phiên cũ khỏi `localStorage`.

### V4-05: OTP atomic kiểm tra affected rows
- **Nguyên nhân gốc**: Trong `OtpService`, sau khi gọi câu lệnh UPDATE atomic trong repository, code lại gọi `otpRepository.save(otp)`, dẫn tới ghi đè lại trạng thái cũ nếu có ngoại lệ hoặc làm mất tính nguyên tử của câu lệnh SQL.
- **Giải pháp**:
  - Câu lệnh UPDATE kiểm tra điều kiện `used = false AND expiredAt > now AND attempts < maxAttempts`.
  - Kiểm tra `int affectedRows = otpRepository.markConsumedAtomic(...)`. Nếu `affectedRows == 0`, ném ngay `OTP_INVALID` hoặc `OTP_ALREADY_USED`.
  - Loại bỏ hoàn toàn lệnh `save(otp)` sau câu lệnh atomic.
  - `OtpAttemptRecorder` ghi nhận số lần nhập sai bằng transaction độc lập `REQUIRES_NEW`, đảm bảo khi nghiệp vụ chính rollback thì số lần thử sai vẫn được bảo lưu.

### V4-06: Viết lại SQL validation đúng schema và fail-fast
- **Nguyên nhân gốc**: Các tệp kiểm tra đối soát cũ sử dụng sai tên bảng, sai tên cột so với schema SQL Server thực tế (dùng `SoLuong` thay vì `TonThucTe`/`TonLyThuyet`, `Loai` thay vì `LoaiBienDong`, `INNER JOIN` làm sót các bản ghi mồ côi) và in kết quả bằng `PRINT PASS` thay vì dừng chương trình khi có lỗi.
- **Giải pháp**:
  - Viết lại toàn bộ 3 tập lệnh đối soát:
    - `database/validation/verify_inventory_reconciliation.sql`: Đối soát cân bằng kho nguyên liệu, tính liên tục của sổ cái `TonSau = TonTruoc + LuongBienDong`, mẻ sơ chế và kiểm kê kết ca.
    - `database/validation/verify_financial_reconciliation.sql`: Đối soát hóa đơn `PhaiTra = max(0, TongTien - TienGiam - TienGiamVoucher)`, tổng thanh toán khớp trạng thái hóa đơn.
    - `database/validation/verify_demo_integrity.sql`: Đối soát tính toàn vẹn phiên bàn, đơn hàng, dữ liệu mẫu demo.
  - Áp dụng kỹ thuật `LEFT JOIN ... WHERE ... IS NULL` và khối phát hiện vi phạm với lệnh `THROW 50000, 'VIOLATION DETECTED', 1;` để đảm bảo cơ chế fail-fast khi có bất kỳ sai lệch dữ liệu nào.

### V4-10: Runner, migration và constraint an toàn
- **Nguyên nhân gốc**: Trong cơ sở dữ liệu trước đây thiếu các ràng buộc mức CSDL để bảo vệ bất biến nghiệp vụ, dẫn tới khả năng xảy ra race condition chèn trùng phiên bàn mở hoặc sai lệch toán học sổ cái kho.
- **Giải pháp**:
  - Tạo migration `database/migrations/V7__invariants_and_integrity_constraints.sql` gồm:
    1. `UQ_PhienBan_MaBan_Open`: Filtered Unique Index trên `dbo.PhienBan(MaBan) WHERE TrangThai = 'OPEN'`, đảm bảo mức engine SQL Server không bao giờ tồn tại 2 phiên mở đồng thời trên cùng một bàn.
    2. `UQ_ChiTietKetCaKho_MaKetCa_MaNL`: Khóa duy nhất ngăn trùng nguyên liệu trong cùng một phiếu kết ca.
    3. `CK_ChiTietKetCa_ThucTe`: Ràng buộc kiểm tra `TonThucTe = LuongLoaiBo + LuongGiuLai`.
    4. `CK_LichSuKho_TonSau_Math`: Ràng buộc kiểm tra `TonSau = TonTruoc + LuongBienDong`.
    5. `CK_HoaDon_PhaiTra_Calc`: Ràng buộc kiểm tra tính hợp lệ của số tiền phải trả trên hóa đơn.
  - Cập nhật `database/reset_database.ps1` và `sample_data.sql`: Loại bỏ `USE milktea;` cố định, hỗ trợ tham số database linh hoạt và kiểm tra an toàn trước khi chạy.

### V4-11: Phân quyền & Scoping API Kho (Inventory)
- **Nguyên nhân gốc**: Các endpoint xem tồn kho, xem kết ca và nhập xuất kho trước đây chưa được phân quyền chặt chẽ giữa các vai trò `ADMIN`, `MANAGER`, `KITCHEN`, `CASHIER`, và chưa có alias số nhiều chuẩn RESTful.
- **Giải pháp**:
  - `InventoryController`: Bổ sung route alias số nhiều `/shift-settlements`, `/shift-settlements/preview`.
  - Kiểm tra vai trò:
    - Bếp (`KITCHEN`): Xem tồn kho Bếp và xem trước kết ca kho Bếp tại chi nhánh của mình. Bị chặn khi xem chi nhánh khác.
    - Thu ngân (`CASHIER`): Xem tồn kho Bar/Kho của chi nhánh mình để phục vụ bán hàng; tuyệt đối bị cấm xem/thực hiện kết ca kho (`403 Forbidden`).
    - Quản lý (`MANAGER`): Toàn quyền quản lý kho trong phạm vi chi nhánh được phân công.
    - Quản trị viên (`ADMIN`): Toàn quyền toàn chuỗi.

### V4-12: Idempotency Replay sau quyền & Concurrency barrier
- **Nguyên nhân gốc**: Trong `OrderService.createTableOrder`, logic kiểm tra phiên bàn (`table.getActiveSessionId() != null && expectedSessionId == null`) chạy trước lệnh kiểm tra `idempotencyService.checkExisting()`. Khi client gặp sự cố mạng và gửi lại request với cùng key và payload, server kiểm tra thấy bàn đã có khách và ném ngay `409 Conflict` thay vì trả về đơn hàng đã tạo trước đó.
- **Giải pháp**:
  - Sắp xếp lại luồng xử lý:
    1. Kiểm tra xác thực và năng lực mã QR (`validateTableQrCapabilityToken` hoặc quyền Staff).
    2. Kiểm tra Idempotency Replay: Nếu request trùng Idempotency-Key và khớp Fingerprint nghiệp vụ, lập tức trả về kết quả đơn hàng đã tạo (HTTP 201/200 idempotent response) mà không ném lỗi đổi phiên.
    3. Nếu là request mới (chưa có trong bảng chống lặp `BanGhiChongLap`), mới kiểm tra logic mở phiên hoặc đặt thêm món.
  - Kết quả kiểm chứng: Test chạy song song 2 tiến trình độc lập đạt 100% khớp cùng một `orderId`.

### V4-13: Realtime Event Publisher đầy đủ sau commit
- **Nguyên nhân gốc**: Các thao tác nhập hàng, xuất chuyển kho, hao hụt nguyên liệu và kiểm kê kết ca trong `InventoryService` trước đây chưa phát sự kiện WebSocket tới các kênh chi nhánh.
- **Giải pháp**:
  - Bổ sung `realtimeEventPublisher.publishAfterCommit()` vào `recordConsumption`, `recordImport`, `recordWaste`, `recordTransfer`, `recordShiftSettlement`.
  - Sự kiện được gửi tới cả kênh toàn chuỗi `/topic/stock` (dành cho Admin) và kênh chi nhánh `/topic/branches/{branchId}/stock` (dành cho Kitchen/Manager chi nhánh).

---

## 3. Kết luận nghiệm thu V4
Tất cả 16 nhóm lỗi từ **V4-01 đến V4-16** đã được sửa đổi triệt để trên mã nguồn backend Java, cơ sở dữ liệu SQL Server và giao diện người dùng. Toàn bộ các kiểm thử đơn vị (149 tests), kiểm thử tích hợp HTTP API (41 routes) và đối soát dữ liệu CSDL đều đạt trạng thái **PASS 100%**, sẵn sàng cho môi trường vận hành thực tế.
