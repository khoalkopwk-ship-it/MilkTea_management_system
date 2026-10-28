# OPERATIONS & HANDOFF GUIDE — HƯỚNG DẪN VẬN HÀNH & BÀN GIAO HỆ THỐNG MILKTEA MULTI-BRANCH

Tài liệu này cung cấp hướng dẫn vận hành toàn diện cho Hệ thống Chuỗi Trà Sữa Đa Chi Nhánh (MilkTea Chain Management System), bao gồm ma trận vai trò, hướng dẫn thao tác từng phân hệ, quy trình đối soát số liệu và kịch bản demo mẫu.

---

## 1. Danh sách Tài khoản Mẫu & Phân quyền Hệ thống

Hệ thống đã thiết lập sẵn các tài khoản demo chuẩn với mật khẩu đồng bộ: `123456` (hoặc `Admin@123456` theo cấu hình ban đầu).

| Vai trò (Role) | Email tài khoản | Chi nhánh phân công | Phạm vi quyền hạn | Đường dẫn điều hướng chính |
|---|---|---|---|---|
| **ADMIN** (Quản trị toàn chuỗi) | `admin@milktea.vn` | Toàn bộ chuỗi (CN01, CN02, ...) | Quản trị toàn hệ thống: Báo cáo chuỗi, Món & danh mục, Đơn hàng toàn chuỗi, Tài khoản nhân viên, Kho tổng, Chứng từ, Voucher toàn chuỗi & Cài đặt quán (không quản lý sơ đồ bàn và QR). | `/admin` |
| **MANAGER** (Quản lý Cơ sở 01) | `manager1@milktea.vn` | Chi nhánh 01 (Võ Văn Ngân) | Điều hành cơ sở 01: Sơ đồ bàn & QR bàn cơ sở, Quầy thu ngân cơ sở, Khu vực bếp cơ sở, Kho nguyên liệu cơ sở, Bật/tắt món tại cơ sở, Duyệt/ẩn đánh giá tại cơ sở. | `/admin` |
| **MANAGER** (Quản lý Cơ sở 02) | `manager2@milktea.vn` | Chi nhánh 02 (D2 Bình Thạnh) | Điều hành cơ sở 02: Sơ đồ bàn & QR bàn cơ sở, Quầy thu ngân cơ sở, Khu vực bếp cơ sở, Kho nguyên liệu cơ sở, Bật/tắt món tại cơ sở, Duyệt/ẩn đánh giá tại cơ sở. | `/admin` |
| **CASHIER** (Thu ngân) | `cashier@milktea.vn` | Chi nhánh 01 | Bán hàng quầy, mở/đóng phiên bàn QR, thanh toán tiền mặt & VietQR, in hóa đơn. | `/cashier` |
| **KITCHEN** (Nhân viên Bếp) | `kitchen@milktea.vn` | Chi nhánh 01 | Xem danh sách món chờ chế biến, hoàn thành món và tự động trừ kho Bếp cơ sở. | `/kitchen` |
| **CUSTOMER** (Khách hàng) | `customer@milktea.vn` | Toàn chuỗi | Xem thực đơn theo chi nhánh, tìm kiếm/lọc món, lưu danh sách yêu thích, đánh giá món sau khi thanh toán, áp dụng mã voucher. | `/` hoặc `/menu` |

---

## 2. Hướng dẫn Vận hành Dành cho Quản trị viên Toàn Chuỗi (ADMIN)

### 2.1. Báo cáo Doanh thu & Đối soát Đa Chi Nhánh (`/admin/reports`)
- **Bộ lọc thời gian**: Chọn "Từ ngày" và "Đến ngày" để xem doanh thu trong kỳ.
- **Bộ lọc phạm vi cơ sở**:
  - Chọn **"Toàn chuỗi hệ thống"**: Hiển thị tổng doanh thu thực thu, tiền hoàn, doanh thu ròng, tổng đơn hàng, chi phí hao hụt và cảnh báo tồn kho trên toàn bộ các chi nhánh.
  - Chọn **Từng chi nhánh cụ thể (CN01, CN02)**: Hệ thống tự động lọc và đối soát doanh thu riêng của chi nhánh đó.
- **Nguyên tắc đối soát số học**:
  $$\text{Doanh thu Ròng (Toàn chuỗi)} = \sum \text{Doanh thu Ròng (Từng Chi nhánh)}$$
  $$\text{Tổng số Đơn hàng (Toàn chuỗi)} = \sum \text{Số Đơn hàng (Từng Chi nhánh)}$$

### 2.2. Quản lý Khuyến mãi & Voucher (`/admin/vouchers`)
- **Tạo mới voucher**:
  - Nhập **Tên voucher**, **Mã code** (viết hoa không dấu, ví dụ: `SUMMER2026`).
  - Chọn loại giảm: `Phần trăm (%)` (kèm hạn mức giảm tối đa) hoặc `Số tiền cố định (đ)`.
  - Thiết lập giá trị đơn hàng tối thiểu (`minOrderValue`), tổng số lượt sử dụng tối đa và số lượt cho phép mỗi khách hàng.
  - **Phạm vi chi nhánh**: Chọn *Áp dụng toàn chuỗi* hoặc chỉ định áp dụng cho danh sách chi nhánh cụ thể.
- **Bật/Tắt & Chỉnh sửa voucher**: Có thể tạm dừng hoặc sửa thông tin voucher bất cứ lúc nào.
- **Tra cứu lịch sử sử dụng**: Xem chi tiết mã đơn hàng, khách hàng, chi nhánh và số tiền giảm giá đã áp dụng.
- **Xử lý tranh chấp đồng thời (AT-05)**: Hệ thống tự động khóa nguyên tử ở tầng database khi khách hàng cùng tranh chấp lượt voucher cuối cùng, cam kết không vượt hạn mức. Khi đơn hàng bị hủy, số lượt voucher tự động được hoàn trả (`DA_HOAN_TRA`).

### 2.3. Cài đặt Hệ thống Quán (`/admin/settings`)
- Cấu hình Tên quán, Địa chỉ hiển thị trên hóa đơn và trang chủ.
- Thiết lập Tài khoản Ngân hàng nhận tiền (VietQR): Thay đổi số tài khoản, ngân hàng, tên chủ tài khoản sẽ lập tức cập nhật mã QR động tạo ra cho khách hàng và thu ngân.

---

## 3. Hướng dẫn Vận hành Dành cho Quản lý Cơ sở (MANAGER)

### 3.1. Phân quyền & Không gian Làm việc
- Sau khi đăng nhập, Quản lý được chuyển hướng trực tiếp vào **Bảng điều hành cơ sở** (`/cashier`).
- Header hiển thị huy hiệu xanh `[Quản lý cơ sở]`, kèm các lối tắt: **Quầy thu ngân** (`/cashier`), **Khu vực bếp** (`/kitchen`).
- Quản lý **không có quyền** can thiệp vào trang quản trị hệ thống (`/admin/**`) và bị backend chặn tuyệt đối nếu cố tình thao tác chéo sang chi nhánh khác.

### 3.2. Bật / Tắt Phục vụ Món tại Chi nhánh
- Khi chi nhánh tạm hết nguyên liệu pha chế cho một món cụ thể, Quản lý có thể cập nhật trạng thái `ConPhucVu = false` cho món đó tại chi nhánh mình qua API:
  `PUT /api/v1/branches/{branchId}/products/{productId}/availability`
- Món bị tạm ngưng tại chi nhánh này vẫn có thể phục vụ bình thường tại các chi nhánh khác trong chuỗi.

### 3.3. Kiểm duyệt Đánh giá Khách hàng tại Cơ sở
- Quản lý cơ sở có thể ẩn các đánh giá không phù hợp của khách hàng thuộc chi nhánh mình phụ trách:
  `PUT /api/v1/reviews/{id}/moderation` (kèm lý do `reason`).
- Khi bị ẩn, đánh giá sẽ không hiển thị trên trang chi tiết sản phẩm và không được tính vào điểm sao trung bình của món.

---

## 4. Hướng dẫn Vận hành Thu ngân & Bếp (CASHIER & KITCHEN)

### 4.1. Thu ngân Cơ sở (`/cashier`)
- **Đơn tại bàn**: Khách quét QR mở bàn, đặt món. Đơn đầu tiên gửi lên thành công sẽ kích hoạt mở phiên bàn (`TableSession`).
- **Đơn mang đi / Đơn quầy**: Thu ngân tạo đơn quầy trực tiếp tại chi nhánh mình.
- **Thanh toán**:
  - Chọn thanh toán Tiền mặt (`CASH`) hoặc Chuyển khoản VietQR (`BANK_TRANSFER`).
  - Hóa đơn ghi nhận snapshot giảm giá cửa hàng và mã Voucher (nếu có).
  - Thu ngân đóng phiên bàn sau khi hóa đơn đã được thu đủ tiền.

### 4.2. Bếp Cơ sở (`/kitchen`)
- Màn hình bếp tự động nhận thông báo realtime qua WebSocket:
  `/topic/branches/{branchId}/kitchen`
- Bếp bấm nhận món -> Bắt đầu chế biến -> Bấm hoàn thành.
- **Khấu trừ kho tự động**: Khi Bếp hoàn thành đơn, hệ thống tự động kiểm tra định lượng và trừ tồn kho nguyên liệu Bếp/Kho của **chính chi nhánh đó**. Tuyệt đối không ảnh hưởng đến tồn kho của bất kỳ chi nhánh nào khác.

---

## 5. Hướng dẫn Khách hàng (CUSTOMER)

1. **Khách tại quán qua QR**: Quét mã QR dán trên bàn sẽ tự động nhận diện chính xác mã bàn và cơ sở. Xem thực đơn không mở phiên; khi gửi đơn đầu tiên thành công mới mở phiên an toàn.
2. **Khách trực tuyến**: Truy cập `/menu`, chọn chi nhánh nhận hàng (CN01 hoặc CN02).
3. **Tìm kiếm & Lọc món**: Lọc theo danh mục, khoảng giá, sắp xếp theo giá hoặc tên món (phân trang chuẩn 100% tại backend).
4. **Danh sách nổi bật Top 20**: Xem top món bán chạy (chỉ tính đơn hoàn thành + đã thanh toán), top đánh giá cao, top yêu thích.
5. **Yêu thích (Wishlist)**: Bấm biểu tượng trái tim để lưu món vào danh sách yêu thích cá nhân (`/wishlist`).
6. **Đánh giá & Bình luận**: Khách hàng chỉ có thể đánh giá những món nước thuộc các đơn hàng đã nhận (`HOAN_THANH`) và đã thanh toán tiền. Đánh giá từ 1 đến 5 sao, nội dung tối thiểu 50 ký tự, có thể đính kèm ảnh/video.
7. **Áp dụng Voucher**: Nhập mã voucher hoặc chọn từ danh sách voucher khả dụng trong giỏ hàng (`/cart`).

---

## 6. Kịch bản Demo Nhanh Nghiệm thu (Quick Acceptance Scenario)

```
Bước 1: Đăng nhập Admin (admin@milktea.vn / 123456)
        -> Vào /admin/reports: Thấy bộ lọc Chi nhánh ("Toàn chuỗi hệ thống", "CN01", "CN02").
        -> Chọn CN01: Biểu đồ và thực thu chuyển về số liệu cơ sở 01.
        -> Chọn Toàn chuỗi: Số liệu thực thu = CN01 + CN02 (đối soát hoàn hảo).

Bước 2: Quản lý Voucher (/admin/vouchers)
        -> Tạo voucher "CHAOMUNG2026" giảm 20.000đ cho đơn từ 50.000đ.
        -> Xem danh sách lịch sử áp dụng voucher.

Bước 3: Đăng nhập Khách hàng (customer@milktea.vn / 123456)
        -> Vào /menu, chọn món, vào /cart, nhập mã "CHAOMUNG" hoặc chọn voucher từ popup.
        -> Thấy tổng tiền được trừ 20.000đ rõ ràng.
        -> Đặt đơn trực tuyến.

Bước 4: Đăng nhập Quản lý Cơ sở (manager1@milktea.vn / 123456)
        -> Tự động vào /cashier với huy hiệu [Quản lý cơ sở].
        -> Thử gõ đường dẫn /admin -> Bị chặn 403 Forbidden theo đúng ma trận phân quyền.
        -> Kiểm tra và xác nhận đơn hàng tại cơ sở 01.

Bước 5: Đăng nhập Bếp (kitchen@milktea.vn / 123456)
        -> Vào /kitchen, thấy đơn cần chế biến.
        -> Bấm "Hoàn thành" -> Kho Bếp CN01 tự động trừ nguyên liệu theo định lượng công thức.
```
