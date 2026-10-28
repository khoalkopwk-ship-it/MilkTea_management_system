# HƯỚNG DẪN KIỂM THỬ & DEMO GIAI ĐOẠN 5 (VOUCHER & DISCOUNTS)

Tài liệu này cung cấp toàn bộ dữ liệu demo, kịch bản kiểm thử trực quan và hướng dẫn thao tác từng bước dành cho Ban Đánh Giá / Giảng Viên / Người Demo hệ thống **MilkTea Chain Management System**.

---

## 1. TỔNG QUAN GIAI ĐOẠN 5

Giai đoạn 5 triển khai toàn diện nghiệp vụ **Voucher & Khuyến Mãi Đa Chi Nhánh**:
- **Cơ chế tính toán chiết khấu**: Hỗ trợ 2 hình thức: Phần trăm (`PERCENTAGE` kèm mức trần `maxDiscount`) và Số tiền cố định (`FIXED_AMOUNT`).
- **Phân quyền phạm vi chi nhánh**: Voucher áp dụng toàn chuỗi hoặc giới hạn riêng cho từng chi nhánh cụ thể (tránh áp dụng chéo cơ sở).
- **Kiểm soát hạn mức & chống gian lận**: Ràng buộc giá trị đơn tối thiểu (`minOrderValue`), tổng số lượt dùng toàn hệ thống (`totalUsageLimit`), số lượt dùng tối đa trên mỗi tài khoản (`usageLimitPerCustomer`), và thời hạn bắt đầu/kết thúc.
- **Tính toàn vẹn & Hoàn trả**: Khi đơn hàng bị hủy/hoàn tiền, lượt dùng voucher tự động được khôi phục cho khách hàng.
- **Môi trường kiểm thử (Live Sandbox)**: Khung kiểm thử trực tiếp trên giao diện Admin giúp kiểm tra nhanh công thức và các ca kiểm thử biên mà không cần tạo đơn thực tế.

---

## 2. DANH SÁCH TÀI KHOẢN DEMO

| Vai trò | Email đăng nhập | Mật khẩu mặc định | Chi nhánh / Quyền hạn |
| :--- | :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin@milktea.vn` | `password` | Toàn quyền chuỗi, quản lý & kiểm thử Voucher |
| **Thu ngân CN01 (Cashier)** | `cashier@milktea.vn` | `password` | Thu ngân POS tại Chi nhánh 1 (Võ Văn Ngân) |
| **Khách hàng 1 (Customer)** | `customer@milktea.vn` | `password` | Khách hàng thành viên tích điểm & dùng voucher |
| **Khách hàng 2 (Customer)** | `customer2@milktea.vn` | `password` | Khách hàng thành viên thứ 2 |

---

## 3. BẢNG DỮ LIỆU DEMO VOUCHER CÓ SẴN TRONG CƠ SỞ DỮ LIỆU

Hệ thống đã nạp sẵn bộ dữ liệu voucher phục vụ đầy đủ các kịch bản kiểm thử:

| Mã Voucher | Loại giảm | Giá trị giảm | Đơn tối thiểu | Phạm vi áp dụng | Giới hạn lượt | Mục đích kiểm thử |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `CHAOMUNG` | Cố định | **20.000 ₫** | 50.000 ₫ | Toàn chuỗi | 1000 lượt | Test áp dụng thành công voucher tiền mặt |
| `MILKTEA10` | Phần trăm | **10%** (Max 30k) | 60.000 ₫ | Toàn chuỗi | 500 lượt | Test giảm giá theo % có khống chế trần |
| `CN01SPECIAL` | Cố định | **15.000 ₫** | 45.000 ₫ | **Chỉ Chi nhánh 1** | 200 lượt | Test phân quyền phạm vi chi nhánh |
| `DONTOITHIEU` | Cố định | **50.000 ₫** | **200.000 ₫** | Toàn chuỗi | 500 lượt | Test vi phạm giá trị đơn tối thiểu |
| `HETLUOT` | Cố định | **30.000 ₫** | 40.000 ₫ | Toàn chuỗi | **10/10 lượt** | Test voucher đã sử dụng hết lượt |
| `HETHAN` | Cố định | **25.000 ₫** | 50.000 ₫ | Toàn chuỗi | Đã hết hạn | Test voucher quá thời hạn sử dụng |

---

## 4. CÁC KỊCH BẢN DEMO TỪNG BƯỚC (STEP-BY-STEP)

### KỊCH BẢN 1: Sử dụng Khung Kiểm Thử Nhanh (Live Sandbox) trên Admin
> **Mục tiêu**: Chứng minh thuật toán kiểm tra voucher tức thời (Realtime Validation) với phản hồi rõ ràng cho cả trường hợp hợp lệ lẫn trường hợp vi phạm nghiệp vụ.

1. Đăng nhập với tài khoản Admin: `admin@milktea.vn` / `password`.
2. Truy cập vào menu **Quản trị hệ thống** > chọn mục **[Khuyến mãi & Voucher](http://localhost:8080/admin/vouchers)**.
3. Quan sát khối màu nâu cam nổi bật: **"Khung Kiểm Thử Nhanh Nghiệp Vụ (Live Sandbox)"**.
4. **Thực hiện Test case 1.1 (Thành công - Giảm cố định)**:
   - Nhấp vào nút nhanh `CHAOMUNG (Giảm 20k)`.
   - Bấm **"Kiểm thử ngay"**.
   - **Kết quả kỳ vọng**: Hộp thông báo màu xanh ngọc hiển thị:
     - Trạng thái: `HỢP LỆ — ĐỦ ĐIỀU KIỆN ÁP DỤNG (200 OK)`
     - Tạm tính đơn hàng: `65.000 ₫`
     - Mức giảm chiết khấu: `- 20.000 ₫`
     - Thành tiền sau giảm: `45.000 ₫`
5. **Thực hiện Test case 1.2 (Thành công - Giảm phần trăm)**:
   - Nhấp vào nút nhanh `MILKTEA10 (Giảm 10%)` với đơn 80.000 ₫.
   - Bấm **"Kiểm thử ngay"**.
   - **Kết quả kỳ vọng**: Chiết khấu chính xác `- 8.000 ₫`, thành tiền `72.000 ₫`.
6. **Thực hiện Test case 1.3 (Vi phạm chi nhánh)**:
   - Nhấp nút `CN01SPECIAL (Sai Chi nhánh 2)` (áp dụng tại Chi nhánh 2 - Bình Thạnh).
   - Bấm **"Kiểm thử ngay"**.
   - **Kết quả kỳ vọng**: Hộp cảnh báo màu đỏ xuất hiện: `KHÔNG HỢP LỆ / VI PHẠM ĐIỀU KIỆN NGHIỆP VỤ`. Thông báo giải thích rõ: *"Voucher không áp dụng tại chi nhánh này"*.
7. **Thực hiện Test case 1.4 (Không đủ đơn tối thiểu)**:
   - Nhấp nút `DONTOITHIEU (Thiếu tiền đơn)` (đơn 40.000 ₫ < yêu cầu 200.000 ₫).
   - Bấm **"Kiểm thử ngay"**.
   - **Kết quả kỳ vọng**: Hệ thống từ chối và thông báo: *"Đơn hàng chưa đạt giá trị tối thiểu 200.000 ₫"*.
8. **Thực hiện Test case 1.5 (Voucher hết lượt & Hết hạn)**:
   - Thử nghiệm với nút `HETLUOT` và `HETHAN`.
   - **Kết quả kỳ vọng**: Hệ thống báo lỗi tương ứng: *"Voucher đã hết số lượt sử dụng"* hoặc *"Voucher đã hết hạn"*.

---

### KỊCH BẢN 2: Thao Tác Quản Lý Voucher (Bật/Tắt, Tạo Mới, Xem Lịch Sử)
> **Mục tiêu**: Chứng minh khả năng vận hành chuỗi của Ban Quản Trị.

1. **Bật/Tắt trạng thái hoạt động (Toggle Active)**:
   - Trên bảng danh sách voucher, tìm dòng voucher `CHAOMUNG`.
   - Bấm vào icon công tắc trạng thái ở cột **Hành động**.
   - **Kết quả kỳ vọng**: Trạng thái đổi từ `Hoạt động` sang `Tạm tắt`. Thông báo toast hiện thành công.
   - Nhập lại mã `CHAOMUNG` vào Khung kiểm thử nhanh và bấm Test -> Hệ thống báo ngay voucher đang bị tạm ngưng kích hoạt.
   - Bấm lại icon công tắc để kích hoạt lại voucher.
2. **Tạo Voucher mới**:
   - Bấm nút **"Tạo Voucher mới"** ở góc phải trên.
   - Nhập thông tin:
     - Mã Voucher: `TEST2026`
     - Mô tả: `Ưu đãi dùng thử 2026`
     - Loại giảm: `Phần trăm (%)` -> Giá trị: `15%`, Giảm tối đa: `25.000 ₫`
     - Đơn tối thiểu: `50.000 ₫`
     - Chọn áp dụng: Toàn bộ chi nhánh
   - Bấm **"Lưu Voucher"**.
   - **Kết quả kỳ vọng**: Voucher mới xuất hiện ngay trên bảng danh sách và có thể kiểm thử được ngay lập tức.
3. **Xem lịch sử sử dụng**:
   - Bấm icon đồng hồ (Lịch sử) trên dòng voucher `CHAOMUNG`.
   - **Kết quả kỳ vọng**: Modal mở ra hiển thị danh sách các đơn hàng và khách hàng đã từng áp dụng mã này (hoặc thông báo chưa có lượt dùng nếu mới tạo).

---

### KỊCH BẢN 3: Khách Hàng Áp Dụng Voucher Trong Giỏ Hàng (`/cart`)
> **Mục tiêu**: Kiểm thử trải nghiệm khách hàng khi đặt hàng và hưởng khuyến mãi.

1. Truy cập vào trang [Thực đơn (Menu)](http://localhost:8080/menu).
2. Chọn món yêu thích (ví dụ: *Trà Sữa Truyền Thống*), chọn Size L và thêm Topping, bấm **Thêm vào giỏ hàng**.
3. Đi tới trang [Giỏ hàng (Cart)](http://localhost:8080/cart).
4. Quan sát phần **Mã giảm giá & Khuyến mãi**:
   - Bấm nút **"Chọn mã có sẵn"**: Hệ thống hiển thị modal danh sách các voucher đang khả dụng kèm điều kiện đơn tối thiểu.
   - Bấm nút **Áp dụng** mã `CHAOMUNG` (hoặc tự gõ `CHAOMUNG` vào ô nhập).
5. **Kết quả kỳ vọng**:
   - Hệ thống hiển thị thông báo thành công.
   - Dòng **Giảm giá voucher**: `- 20.000 ₫` màu xanh lá cây.
   - Tổng tiền thanh toán được cập nhật tự động giảm đúng 20.000 ₫.
6. Bấm **Đặt hàng**: Đơn hàng được tạo thành công với mã voucher và số tiền giảm được ghi nhận vào hóa đơn đơn hàng.

---

### KỊCH BẢN 4: Khôi Phục Voucher Khi Hủy Đơn Hàng (Cancellation & Refund Flow)
> **Mục tiêu**: Chứng minh tính nhất quán nghiệp vụ — voucher không bị mất oan khi khách hàng hoặc cửa hàng hủy đơn trước khi pha chế.

1. Khách hàng xem đơn hàng vừa đặt tại [Danh sách đơn hàng](http://localhost:8080/orders).
2. Khi đơn hàng ở trạng thái `CHO_XAC_NHAN` hoặc `DANG_XU_LY` (chưa hoàn thành), khách hàng hoặc thu ngân thực hiện **Hủy đơn**.
3. **Kết quả kỳ vọng**:
   - Đơn hàng chuyển sang trạng thái hủy.
   - Backend gọi `cancellationService.cancelOrder(...)` -> Tự động gọi `voucherService.restoreVoucherUsage(order.getId())`.
   - Bản ghi lịch sử sử dụng voucher được cập nhật trạng thái `DA_HOAN_TRA`.
   - Số lượt dùng của voucher và hạn mức của khách hàng được hoàn trả đầy đủ, cho phép khách hàng áp dụng lại mã này ở đơn tiếp theo.

---

### KỊCH BẢN 5: Kiểm Thử Giao Diện Menu Mới & Đồng Bộ Tab Yêu Thích
> **Mục tiêu**: Xác nhận giao diện hiển thị tinh tế (chỉ hiện size bé nhất trên thẻ menu, xem chi tiết để chọn các size khác) và đồng bộ tab Yêu thích.

1. **Kiểm tra Tab Menu (`/menu`)**:
   - Quan sát các nút bộ sưu tập: Nút "Mới nhất" đã được **loại bỏ**. Các nút còn lại: `Tất cả món`, `Bán chạy nhất`, `Đánh giá cao`, `Yêu thích nhất`.
   - Quan sát thẻ các món có nhiều size (ví dụ *Trà Sữa Truyền Thống*, *Bạc Xỉu Ba Tầng*):
     - Thẻ trên menu **chỉ hiển thị 1 thẻ duy nhất** với **Size bé nhất (Size M)** và giá tiền tương ứng (ví dụ `28.000 ₫`).
   - Bấm nút **"Chọn"**: Modal tùy chỉnh (Customize Modal) bật lên hiển thị đầy đủ cả `Size M · 28.000 ₫` và `Size L · 33.000 ₫` kèm các topping để chọn!
   - Bấm vào tên món hoặc ảnh món để vào trang **Chi tiết món (`/products/{id}`)**: Hiển thị rõ các nút lựa chọn từng kích cỡ kèm giá tiền cụ thể từng size.
2. **Kiểm tra Nút Trái Tim (Lượt thích theo tài khoản duy nhất)**:
   - Khi đăng nhập tài khoản khách hàng, click vào trái tim trên thẻ món để thích.
   - Thả tim trên bất kỳ size nào cũng ghi nhận yêu thích cho món đó (1 tài khoản chỉ thích mỗi món 1 lần không tính size).
   - Click nút bộ sưu tập **"Yêu thích nhất"**: Hệ thống lọc và xếp hạng các món theo số lượng tài khoản đã thích từ cao xuống thấp.
3. **Kiểm tra Tab Yêu Thích (`/favorites`)**:
   - Truy cập vào trang [Món yêu thích](http://localhost:8080/favorites).
   - **Giao diện đồng bộ hoàn toàn**: Các thẻ món có kích thước, ảnh bo tròn, badge size, rating, giá tiền y hệt như tab Menu.
   - Thao tác trực tiếp trên tab Yêu thích:
     - Bấm nút **"Chọn"**: Mở ngay Customize Modal để chọn size, topping và thêm vào giỏ hàng ngay tại tab Yêu thích.
     - Bấm nút **Trái tim đỏ**: Gỡ món ra khỏi danh sách yêu thích với hiệu ứng mượt mà.

---

## 5. TỔNG KẾT KIỂM ĐỊNH

Tất cả các tính năng giai đoạn 5 đã được kiểm thử tự động với **105/105 test cases đạt 100%**, đáp ứng đầy đủ các yêu cầu nghiệp vụ:
- ✅ Quản lý voucher đa chi nhánh chuẩn SQL Server.
- ✅ Khung kiểm thử Live Sandbox trực quan, chính xác.
- ✅ Luồng giỏ hàng, áp dụng voucher và hoàn trả voucher khi hủy đơn chuẩn xác.
- ✅ Giao diện Menu chỉ hiện size bé nhất, xem chi tiết chọn đa size.
- ✅ Thả tim yêu thích chuẩn theo tài khoản duy nhất (không phụ thuộc size).
- ✅ Tab Yêu thích đồng bộ giao diện và thao tác như Menu.
