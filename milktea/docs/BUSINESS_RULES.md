# BUSINESS RULES — NGUỒN CHUẨN VỀ NGHIỆP VỤ HỆ THỐNG CHUỖI MILKTEA

Tài liệu này là **Single Source of Truth (SSOT)** về các quy tắc nghiệp vụ chuỗi MilkTea, kế thừa và mở rộng từ quy tắc hệ thống một quán ban đầu. Mọi triển khai trong source code, API, Entity, Service, Controller và UI phải tuân thủ nghiêm ngặt các quy tắc dưới đây.

---

## 1. Mô hình Chuỗi (Chain & Branch Model)
1. **Một thương hiệu, nhiều chi nhánh, quản trị tập trung**:
   - Thương hiệu, logo chuỗi, danh mục (`DanhMuc`), sản phẩm (`MonUong`), size (`S, M, L`), topping và công thức định mức chuẩn (`CongThucMon`, `CongThucSoChe`) dùng chung toàn chuỗi.
   - Tài khoản khách hàng (`CUSTOMER`) dùng chung toàn chuỗi; khách có thể mua tại bất kỳ chi nhánh nào.
2. **Giá bán**:
   - Giai đoạn đầu dùng bảng giá chung toàn chuỗi được định nghĩa trên `MonUong.Gia`.
   - Mỗi chi nhánh quản lý việc có kinh doanh món (`KinhDoanh = 1/0`) và trạng thái phục vụ (`ConPhucVu = 1/0` - tạm hết hay sẵn sàng).
   - Không tự động phát sinh bảng giá riêng theo chi nhánh khi chưa có yêu cầu.
3. **Phạm vi độc lập từng chi nhánh**:
   - Bàn (`Ban`), phiên bàn (`PhienBan`), đơn hàng (`DonHang`), hóa đơn (`HoaDon`), thanh toán (`ThanhToan`), hoàn tiền (`YeuCauHoan`), chứng từ kho (`PhieuNhap`, `MeSoChe`, `KetCaKho`, `LichSuKho`) và tồn kho hai tầng (`KHO`, `BEP`) đều có phạm vi theo từng chi nhánh cụ thể (`MaCN`).
   - Tên/số bàn chỉ cần duy nhất trong từng chi nhánh (Cho phép cả Chi nhánh 1 và Chi nhánh 2 đều có "Bàn 01"). Mã QR của bàn (`MaQR`) là duy nhất toàn hệ thống.
   - Tồn kho `TonNguyenLieu` được phân biệt theo bộ ba: `(MaCN, MaNL, ViTri)`. Nghiệp vụ kho của chi nhánh A tuyệt đối không làm ảnh hưởng hay trộn lẫn số dư kho của chi nhánh B.

---

## 2. Vai trò & Ma trận Phân quyền (Roles & Authorization Matrix)

| Vai trò | Định danh Code | Phạm vi dữ liệu (Scope) | Thao tác chính được phép |
|---|---|---|---|
| Quản trị toàn chuỗi | `ADMIN` | Toàn hệ thống (Mọi chi nhánh) | Cấu hình chuỗi, quản lý chi nhánh, quản lý danh mục/món/công thức chuẩn, phân công nhân sự, báo cáo tổng hợp & so sánh chuỗi, kiểm duyệt nội dung toàn chuỗi |
| Quản lý cơ sở | `MANAGER` | Các chi nhánh được phân công | Quản lý vận hành chi nhánh được giao: cấu hình món kinh doanh/tạm hết tại cơ sở, kiểm duyệt đánh giá tại cơ sở, theo dõi báo cáo cơ sở |
| Thu ngân | `CASHIER` | Đúng chi nhánh đang làm việc | Mở/đóng phiên bàn cơ sở, tạo đơn tại quầy, xác nhận đơn bàn/online cơ sở, ghi thu tiền mặt/chuyển khoản, yêu cầu hoàn tiền, xem thông báo realtime cơ sở |
| Nhân viên bếp | `KITCHEN` | Đúng chi nhánh đang làm việc | Xem danh sách đơn cần chế biến của cơ sở, bắt đầu/hoàn thành chế biến (ghi tiêu hao kho bếp cơ sở), lập mẻ sơ chế, kiểm kê kết ca kho bếp cơ sở |
| Khách hàng | `CUSTOMER` | Tài khoản cá nhân (Toàn chuỗi) | Đặt hàng tại bàn (QR) hoặc trực tuyến, theo dõi đơn của mình, yêu thích món, đánh giá chi tiết món trong đơn đã hoàn thành & thanh toán |
| Khách vãng lai | `Guest` | Bàn/Phiên hoặc Đơn quầy được cấp | Xem menu cơ sở, thêm giỏ, đặt hàng trong phiên bàn hoặc đơn quầy được cấp token hợp lệ. **Không được đánh giá**. |

### Quy tắc kiểm tra phân quyền tại Backend:
- Backend phải xác thực cả **Vai trò** lẫn **Phạm vi Chi nhánh (Branch Scope)**.
- Thu ngân/bếp thuộc Chi nhánh A không thể đọc, ghi, xác nhận đơn hay thao tác kho của Chi nhánh B.
- Branch ID gửi từ client chỉ là dữ liệu đề xuất; backend phải đối chiếu với quyền phân công nhân viên hoặc thông tin bàn/phiên đã xác thực.

---

## 3. Quy trình Đặt hàng & Bàn / Phiên (Order & Session Flows)

### 3.1. Khách tại bàn qua mã QR (FLOW-01)
1. **Quét QR**:
   - Xác định `MaBan` và `MaCN`. Mở ngay giao diện menu của đúng chi nhánh đó với nhãn tên chi nhánh và tên bàn.
   - Quét QR hoặc xem menu, xem chi tiết, thêm món vào giỏ **KHÔNG TỰ ĐỘNG TẠO HAY MỞ PHIÊN BÀN**.
2. **Gửi đơn đầu tiên**:
   - Nếu bàn đang `TRONG` (chưa có phiên mở), hệ thống tạo `PhienBan` mới với trạng thái `OPEN`, chuyển bàn sang `CO_KHACH`, sinh `TableSessionAccess` token cho khách, và tạo đơn hàng đầu tiên (`Nguon = 'TABLE'`).
   - Xử lý đồng thời: Sử dụng khóa dòng (Pessimistic Lock trên `Ban`) để đảm bảo không bao giờ mở 2 phiên đồng thời cho cùng 1 bàn.
3. **Thêm đơn vào phiên đang mở**:
   - Khách có token phiên hợp lệ được gửi thêm đơn mới vào phiên đang mở.
   - Đơn bàn chỉ được thuộc về chi nhánh của bàn đó; không cho phép đổi chi nhánh của đơn tại bàn.
4. **Đóng phiên bàn**:
   - Chỉ `CASHIER` thuộc chi nhánh đó mới có quyền đóng phiên bàn khi: Mọi đơn đã `HOAN_THANH` hoặc `DA_HUY`, và tất cả hóa đơn hiệu lực đã thanh toán đủ (`DA_THANH_TOAN`).
   - Khi đóng phiên: Thu hồi token truy cập của phiên cũ. Lượt khách sau quét QR bàn sẽ không thấy giỏ hoặc lịch sử của lượt khách trước.

### 3.2. Khách trực tuyến (FLOW-02)
1. Xem menu dự kiến, chọn món vào giỏ.
2. Chọn chi nhánh nhận hàng: Server kiểm tra lại tính hợp lệ của toàn bộ giỏ (món có được bán tại chi nhánh không, có tạm hết không, giờ hoạt động).
3. Chốt đơn hàng trực tuyến (`Nguon = 'ONLINE'`) theo đúng một chi nhánh đã xác thực. Không trộn món của nhiều chi nhánh trong một đơn.
4. Đơn trực tuyến không bắt buộc phải có bàn hay phiên bàn.

### 3.3. Đơn tại quầy (FLOW-03)
- Lấy chi nhánh từ ngữ cảnh thu ngân đã đăng nhập.
- Thu ngân không thể tạo đơn tại chi nhánh ngoài phạm vi phân công của mình.

---

## 4. Quản lý Đơn hàng, Tiền & Kho (Order, Payment & Inventory)
1. **Trạng thái Đơn hàng (`DonHang.TrangThai`)**:
   - `CHO_XAC_NHAN`: Đơn mới tạo.
   - `CHO_CHE_BIEN`: Thu ngân đã xác nhận đơn.
   - `DANG_CHE_BIEN`: Bếp bắt đầu pha chế.
   - `HOAN_THANH`: Bếp hoàn thành pha chế toàn bộ đơn. **Chỉ duy nhất KITCHEN được hoàn thành đơn**.
   - `DA_HUY`: Đơn đã được duyệt hủy (chỉ hủy trước khi bắt đầu pha chế).
2. **Thanh toán linh hoạt**:
   - Cho phép thanh toán tiền mặt (`CASH`) hoặc chuyển khoản (`BANK_TRANSFER`) trước, trong hoặc sau khi pha chế.
   - Không chặn chế biến chỉ vì chưa thanh toán (tùy chính sách vận hành của quán).
3. **Thời điểm trừ kho & Tính toàn vẹn**:
   - Kho nguyên liệu chỉ bị trừ khi Bếp bấm **Hoàn thành đơn** (`DANG_CHE_BIEN` -> `HOAN_THANH`).
   - Tiêu hao được tính từ snapshot định mức (`DinhMucDonHang`) đã chốt lúc tạo đơn, bảo đảm việc sửa công thức sau này không làm thay đổi lịch sử tiêu hao.
   - Kiểm tra tồn kho trước khi trừ: Nếu kho BẾP của chi nhánh không đủ số lượng, ném ngoại lệ lỗi thiếu nguyên liệu và **không chuyển trạng thái đơn sang HOAN_THANH**.
   - Trừ kho, chuyển trạng thái đơn, ghi nhật ký `LichSuKho` và `BusinessAudit` phải nằm trong cùng một database transaction.
4. **Chống lặp (Idempotency)**:
   - Các mutation quan trọng (tạo đơn, ghi thu, hoàn tiền, ghi sổ kho) phải hỗ trợ header `Idempotency-Key`.
   - Idempotency record (`BanGhiChongLap`) lưu trữ: `PhamVi` (vd: `CREATE_ORDER`, `RECORD_PAYMENT`), `KhoaYeuCau`, `DauVanPayload` (SHA-256 hash của request body + actor ID), `LoaiTaiNguyen`, `MaTaiNguyen`.
   - Nếu gửi lại cùng key và cùng payload: Trả về kết quả giao dịch đã thực hiện trước đó.
   - Nếu gửi cùng key nhưng khác payload: Báo lỗi `IDEMPOTENCY_CONFLICT` (409 Conflict).

---

## 5. Đánh giá & Bình luận Sản phẩm (CUS-06)
1. **Điều kiện bắt buộc để gửi đánh giá**:
   - Phải là tài khoản `CUSTOMER` đã đăng nhập (Guest không được đánh giá).
   - Khách hàng phải thực sự sở hữu đơn hàng chứa chi tiết món uống đó tại chi nhánh tương ứng.
   - Đơn hàng phải đồng thời ở trạng thái `HOAN_THANH` và hóa đơn đã được thanh toán (`DA_THANH_TOAN`).
   - Đánh giá gắn liền với dòng `ChiTietDonHang`. Mỗi dòng chi tiết đơn chỉ được đánh giá tối đa 1 lần (ngăn chặn duplicate bằng Unique Constraint).
2. **Nội dung & Quy chuẩn**:
   - Điểm đánh giá: từ 1 đến 5 sao (nguyên dương).
   - Nội dung bình luận: tối thiểu 50 ký tự (sau khi trim khoảng trắng đầu/cuối).
   - Hình ảnh/media đính kèm (nếu có): Kiểm tra định dạng hợp lệ (JPEG, PNG, WebP) và magic bytes.
3. **Kiểm duyệt nội dung**:
   - `MANAGER` chỉ được duyệt hoặc ẩn đánh giá thuộc chi nhánh được phân công. `ADMIN` có quyền toàn chuỗi.
   - Khi ẩn đánh giá, bắt buộc ghi rõ: Người thực hiện, thời điểm và lý do ẩn.
   - Khách sửa đánh giá của mình không được tự động gỡ trạng thái bị ẩn bởi quản lý.
   - Điểm đánh giá trung bình và số lượt đánh giá của sản phẩm chỉ tính trên các đánh giá đang ở trạng thái hiển thị (không bị ẩn).

---

## 6. Voucher & Khuyến mãi (CUS-07)
1. **Kiểm tra hợp lệ tại Backend**:
   - Kiểm tra chi nhánh áp dụng: Voucher có thể áp dụng toàn chuỗi hoặc giới hạn tại một số chi nhánh cụ thể.
   - Kiểm tra thời hạn hiệu lực (từ ngày - đến ngày).
   - Kiểm tra giá trị đơn hàng tối thiểu.
   - Kiểm tra danh mục hoặc sản phẩm được áp dụng (nếu voucher có điều kiện loại trừ).
   - Kiểm tra tổng số lượt dùng tối đa và số lượt dùng tối đa cho mỗi tài khoản khách hàng.
2. **Tính toán số tiền giảm**:
   - Giảm theo phần trăm (`PERCENT`) có mức giảm tối đa (`MaxDiscountAmount`), hoặc giảm số tiền cố định (`FIXED_AMOUNT`).
   - Tổng tiền phải trả sau khi giảm giá không bao giờ được âm (`PhaiTra = MAX(0, TongTien - TienGiam)`).
   - Tính toán tiền bằng kiểu `BigDecimal` làm tròn theo đơn vị tiền tệ VND.
3. **Tiêu thụ lượt dùng & Xử lý đồng thời**:
   - Snapshot thông tin mã voucher, tỷ lệ/giá trị giảm, số tiền giảm thực tế vào `HoaDon`.
   - Lượt dùng voucher được tiêu thụ khi đơn hàng hoặc hóa đơn được chốt giao dịch.
   - Sử dụng khóa bi quan (`Pessimistic Lock`) hoặc điều kiện cập nhật nguyên tử (`UPDATE Voucher SET SoLuotDaDung = SoLuotDaDung + 1 WHERE SoLuotDaDung < TongSoLuot`) để ngăn chặn việc hai giao dịch đồng thời tranh chấp lượt cuối cùng.
   - Khi đơn bị hủy hoặc hoàn tiền hợp lệ, hoàn trả lại lượt sử dụng voucher theo quy định.

---

## 7. Bốn Danh sách Nổi bật (CUS-02 - Top 20)
Mỗi danh sách trả về tối đa 20 sản phẩm, áp dụng bộ lọc các món đang được phép bán tại chi nhánh đang chọn:
1. **Mới nhất**: Sắp xếp theo `TaoLuc` (thời điểm tạo món) giảm dần.
2. **Bán chạy nhất**: Tổng số lượng sản phẩm (`SUM(SoLuong)`) trong các đơn hàng đồng thời đạt trạng thái `HOAN_THANH` và đã có thanh toán tại chính chi nhánh đó. Sắp xếp số lượng giảm dần.
3. **Đánh giá cao nhất**: Điểm sao trung bình (`AVG(SoSao)`) của các đánh giá đang hiển thị thuộc sản phẩm đó, sắp xếp giảm dần.
4. **Được yêu thích nhiều nhất**: Tổng số lượt yêu thích hợp lệ toàn chuỗi từ người dùng `CUSTOMER` (khử trùng theo tài khoản), lọc lấy các món có phục vụ tại chi nhánh. Sắp xếp giảm dần.
