# Cẩm Nang Kiểm Thử & Kịch Bản Demo Hệ Thống MilkTea

Tài liệu này được lập nhằm phục vụ buổi báo cáo và kiểm thử nghiệm thu ứng dụng MilkTea, đối chiếu chặt chẽ theo đặc tả nghiệp vụ tại `acceptance_tests.md` và `report_traceability.md`.

---

## 1. Môi Trường & Dữ Liệu Khởi Tạo Mẫu

Khi ứng dụng được khởi chạy với profile `demo` (`--spring.profiles.active=demo`), hệ thống tự động kích hoạt `DataInitializer` nạp sẵn toàn bộ dữ liệu sạch phục vụ demo:

### 1.1 Danh sách tài khoản demo
| Vai trò | Email đăng nhập | Mật khẩu | Họ và tên | Số điện thoại |
| :--- | :--- | :--- | :--- | :--- |
| **Quản trị viên (ADMIN)** | `admin@milktea.vn` | `Password@123` | Quản Trị Viên | `0901000001` |
| **Thu ngân (CASHIER)** | `cashier@milktea.vn` | `Password@123` | Thu Ngân Quỳnh | `0901000002` |
| **Bếp trưởng (KITCHEN)** | `kitchen@milktea.vn` | `Password@123` | Bếp Trưởng Lân | `0901000003` |
| **Khách hàng (CUSTOMER)** | `customer@milktea.vn` | `Password@123` | Khách Hàng Mai | `0901000004` |
| **Khách vãng lai (GUEST)**| Không cần đăng nhập | Không | Khách Bàn | Quét mã QR tại bàn |

### 1.2 Dữ liệu bàn & mã QR
- **Số lượng bàn**: 05 bàn (`Bàn 01` đến `Bàn 05`).
- **Trạng thái ban đầu**: `TRONG` (Bàn trống, chưa có phiên hoạt động).
- **Mã định danh QR**:
  - Bàn 01: `TABLE_QR_BAN01_SECURE_TOKEN_XYZ1`
  - Bàn 02: `TABLE_QR_BAN02_SECURE_TOKEN_XYZ2`
  - Bàn 03: `TABLE_QR_BAN03_SECURE_TOKEN_XYZ3`
  - Bàn 04: `TABLE_QR_BAN04_SECURE_TOKEN_XYZ4`
  - Bàn 05: `TABLE_QR_BAN05_SECURE_TOKEN_XYZ5`

### 1.3 Menu & Danh mục sản phẩm
- **Trà Sữa Truyền Thống** (Size M: 25.000 VNĐ, Size L: 30.000 VNĐ).
- **Trà Đào Cam Sả** (Size M: 28.000 VNĐ).
- **Topping Trân Châu Đen** (5.000 VNĐ).
- **Chính sách ưu đãi quán**: Giảm giá 10% trên toàn bộ hóa đơn (`discountPercent = 10%`).

### 1.4 Nguyên liệu & Tồn kho tại Kho và Bếp
- **Kho (KHO)**: Trà Đen (50 kg), Sữa Đặc (50 kg), Đường Cát (50 kg), Ly Nhựa (500 cái), Trân Châu Thô (50 kg).
- **Bếp (BEP)**: Trà Đen (20 kg), Sữa Đặc (20 kg), Đường Cát (20 kg), Ly Nhựa (20 cái), Trân Châu Thô (20 kg), Trân Châu Nấu Chín (50 suất).

---

## 2. Kịch Bản Demo Chính (End-to-End Vòng Đời Đơn Bàn)

Thứ tự 12 bước tuần tự kiểm chứng toàn bộ luồng vận hành cốt lõi:

| Bước | Thao tác thực hiện | Diễn giải kỹ thuật & API | Kết quả mong đợi | Trạng thái |
| :---: | :--- | :--- | :--- | :---: |
| **01** | Khách quét mã QR tại bàn | Mở URL `/?tableCode=BAN-01` hoặc gọi `GET /api/v1/public/table-context?table=1` | Trang chủ hiển thị nhãn bàn "Bàn 01"; Trạng thái bàn: `TRONG`, chưa tạo phiên (`sessionId = null`). | **PASS** |
| **02** | Chọn món, thêm giỏ & gửi đơn đầu tiên | Chọn 2 ly Trà Sữa Truyền Thống (Đường 70%, Đá 100%, Topping Trân Châu Đen). Gọi `POST /api/v1/orders/table`. | Đơn tạo thành công, mã trạng thái `CHO_XAC_NHAN`. Tự động áp dụng giảm giá 10% (50.000 -> 45.000 VND). Nhận `guestTableToken`. | **PASS** |
| **03** | Khách tải lại trang / mở tab mới | Reload trang `/?tableCode=BAN-01` hoặc gọi `GET /api/v1/public/table-context?table=1`. | Hệ thống tự động phục hồi phiên: Trạng thái bàn chuyển sang `CO_KHACH`, phiên active `OPEN`. Giữ nguyên đơn và tổng tiền. | **PASS** |
| **04** | Thu ngân đăng nhập hệ thống | Mở `/login`, đăng nhập với tài khoản `cashier@milktea.vn` / `Password@123`. | Đăng nhập thành công, nhận JWT Token với vai trò `ROLE_CASHIER`. | **PASS** |
| **05** | Thu ngân xác nhận đơn hàng | Tại màn hình thu ngân, bấm xác nhận đơn hàng. Gọi `POST /api/v1/cashier/orders/{id}/confirm`. | Đơn hàng đổi trạng thái từ `CHO_XAC_NHAN` sang `CHO_CHE_BIEN`. Đẩy sang màn hình bếp. | **PASS** |
| **06** | Bếp đăng nhập hệ thống | Đăng nhập bằng tài khoản `kitchen@milktea.vn` / `Password@123`. | Nhận JWT Token với vai trò `ROLE_KITCHEN`. | **PASS** |
| **07** | Bếp bấm Bắt đầu chế biến | Bếp tiếp nhận đơn, gọi `POST /api/v1/kitchen/orders/{id}/start`. | Đơn hàng đổi trạng thái sang `DANG_CHE_BIEN`. Khách và thu ngân thấy trạng thái realtime. | **PASS** |
| **08** | Bếp bấm Hoàn thành toàn đơn | Bếp pha chế xong, gọi `POST /api/v1/kitchen/orders/{id}/complete`. | Đơn đổi trạng thái sang `HOAN_THANH`. Hệ thống tự động kích hoạt trừ kho tiêu hao nguyên liệu. | **PASS** |
| **09** | Kiểm tra tiêu hao kho nguyên liệu | Gọi `GET /api/v1/inventory/stocks`. | Tồn kho tại vị trí `BEP` bị trừ chính xác theo định lượng công thức (Trà Đen: 19.97 kg, Sữa Đặc: 19.94 kg, Đường: 19.96 kg, Ly: 18 cái). | **PASS** |
| **10** | Thu ngân ghi nhận thanh toán tiền mặt | Thu ngân nhận 45.000 VNĐ từ khách, gọi `POST /api/v1/cashier/orders/{id}/payments`. | Ghi nhận khoản thu thành công. Đơn hàng chuyển sang trạng thái `paid = true`. Hóa đơn `DA_THANH_TOAN`. | **PASS** |
| **11** | Thu ngân đóng phiên bàn | Khách ra về, thu ngân bấm đóng phiên. Gọi `POST /api/v1/cashier/tables/{id}/close-session`. | Đóng phiên bàn thành công vì toàn bộ đơn đã HOÀN THÀNH và hóa đơn đã THU ĐỦ TIỀN. | **PASS** |
| **12** | Quét lại mã QR lượt mới | Quét lại QR bàn 01 hoặc gọi `GET /api/v1/public/table-context?table=1`. | Bàn trở về trạng thái `TRONG`. `sessionId = null`. Toàn bộ dữ liệu đơn/giỏ của khách lượt trước không còn hiển thị. | **PASS** |

---

## 3. Các Ca Kiểm Thử Tình Huống Nâng Cao Đã Xác Minh Thực Tế

| Nhóm kiểm thử | Thao tác & Mô tả kịch bản | Kết quả thực tế quan sát được | Đánh giá |
| :--- | :--- | :--- | :---: |
| **Thanh toán TRƯỚC pha chế** | Khách đặt đơn tại bàn, thu ngân ghi nhận thanh toán ngay khi đơn còn ở bước `CHO_XAC_NHAN`. Sau đó bếp mới bắt đầu pha và hoàn thành. | Thu ngân ghi nhận tiền thành công (`paid = true`). Khi bếp bấm hoàn thành, trạng thái đơn thành `HOAN_THANH` và giữ nguyên trạng thái đã thanh toán. Không yêu cầu thanh toán lại. | **PASS** |
| **Đơn quầy gán vào bàn** | Thu ngân tạo đơn tại quầy (`source = COUNTER`). Sau đó khách chuyển vào bàn ngồi, thu ngân dùng chức năng gán đơn vào bàn (`/assign-table`). | Đơn quầy được chuyển thành công vào phiên của bàn mục tiêu. Mã đơn hàng, danh sách món và tổng tiền giữ nguyên vẹn 100%. | **PASS** |
| **Hủy trước pha & Hoàn tiền** | Khách đã thanh toán tiền trước. Khách đổi ý xin hủy đơn khi bếp chưa pha. Khách gửi yêu cầu hủy -> Thu ngân duyệt hủy -> Admin duyệt hoàn tiền -> Thu ngân chi hoàn tiền. | - Đơn chuyển trạng thái sang `DA_HUY`.<br>- Tự động sinh `RefundRequest` trạng thái `CHO_DUYET`.<br>- Admin duyệt hoàn và Thu ngân chi tiền thành công. Tiêu hao kho không bị trừ. | **PASS** |
| **Chống gửi lặp (Idempotency)** | Gửi liên tiếp 2 request tạo đơn cùng một `Idempotency-Key` (mô phỏng người dùng bấm nút Đặt Đơn 2 lần do mạng lag). | Hệ thống nhận diện Idempotency Key, lần 2 trả về ngay kết quả của lần 1 mà không nhân đôi số lượng đơn hay trừ trùng tài nguyên. | **PASS** |
| **Kiểm tra phân quyền (RBAC)** | Tài khoản Bếp (`ROLE_KITCHEN`) cố tình gọi API báo cáo doanh thu quản trị (`/api/v1/admin/reports/summary`). | Spring Security chặn lập tức với mã lỗi HTTP **403 Forbidden**. | **PASS** |
| **Thao tác trên phiên đã đóng** | Khách gửi request đặt đơn kèm `expectedSessionId` của một phiên đã đóng từ trước. | Hệ thống chặn thao tác và phản hồi HTTP **409 Conflict** (`TABLE_SESSION_CHANGED`), bảo vệ tính toàn vẹn phiên. | **PASS** |

---

## 4. Bảng Đối Chiếu Ma Trận Nghiệm Thu (acceptance_tests.md)

| Mã ca | Tên ca kiểm thử | Mô tả tóm tắt | Trạng thái thực tế |
| :---: | :--- | :--- | :---: |
| **T01** | Khách vãng lai đặt tại bàn | Quét QR bàn Trống, gửi đơn mở phiên `CO_KHACH` | **PASS** |
| **T02** | Tải lại trang trong phiên bàn | Reload trang giữ nguyên giỏ và phiên hiện tại | **PASS** |
| **T03** | Mở tab mới hoặc quét lại QR | Khôi phục đúng ngữ cảnh phiên bàn | **PASS** |
| **T04** | Đơn quầy tiền mặt sau pha | Tạo quầy, pha xong mới thu tiền mặt | **PASS** |
| **T05** | Đơn quầy chuyển khoản | Xác nhận thu chuyển khoản | **PASS** |
| **T06** | Tại bàn thanh toán tiền mặt | Thu ngân xác nhận thu tiền mặt tại bàn | **PASS** |
| **T07** | Đơn chưa thu được chế biến | Bếp chế biến không phụ thuộc trạng thái đã thu | **PASS** |
| **T08** | Quyền hoàn thành | Chỉ vai trò KITCHEN/ADMIN được hoàn thành | **PASS** |
| **T09** | Trạng thái toàn đơn | Quản lý trạng thái theo toàn bộ đơn, không chia món lẻ | **PASS** |
| **T10** | Bỏ một món trước pha | Thay đổi món trước khi chế biến | **NOT RUN** |
| **T11** | Hủy chưa thu tiền | Hủy đơn chưa thu, không sinh hoàn tiền | **PASS** |
| **T12** | Hủy đã thu tiền trước pha | Hủy đơn đã thu, sinh yêu cầu duyệt hoàn tiền | **PASS** |
| **T13** | Hủy khi đang pha | Chặn hủy khi bếp đã bắt đầu pha chế | **NOT RUN** |
| **T14** | Đơn thay thế | Tạo đơn thay thế cho đơn hủy | **NOT RUN** |
| **T15** | Khuyến mãi mọi khách | Áp dụng giảm giá quán tự động vào hóa đơn | **PASS** |
| **T16** | Giá/khuyến mãi đã chốt | Snapshot giá và tỷ lệ giảm giá cố định tại lúc đặt | **PASS** |
| **T17** | Phiếu xuất của bếp | Bếp xuất kho nội bộ KHO -> BEP | **NOT RUN** |
| **T18** | Mẻ sơ chế | Ghi nhận mẻ sơ chế nguyên liệu | **NOT RUN** |
| **T19** | Hoàn thành một đơn | Tiêu hao nguyên liệu theo định lượng công thức | **PASS** |
| **T20** | Cảnh báo tồn kho | Cảnh báo khi mức tồn dưới ngưỡng | **NOT RUN** |
| **T21** | Hoàn thành gửi lại | Chống hoàn thành trùng lặp cùng một đơn | **NOT RUN** |
| **T22** | Phiếu xuất gửi lại | Chống ghi lặp phiếu xuất | **NOT RUN** |
| **T23** | Sơ chế gửi lại | Chống ghi lặp mẻ sơ chế | **NOT RUN** |
| **T24** | Thiếu ghi nhận bổ sung | Ghi nhận bổ sung mẻ khi cảnh báo thiếu | **NOT RUN** |
| **T25** | Hỏng trước và sau tiêu hao | Báo cáo sự cố và hao hụt nguyên liệu | **NOT RUN** |
| **T26** | Hoàn tiền mới duyệt | Admin duyệt hoàn tiền -> Thu ngân mới được chi | **PASS** |
| **T27** | Thanh toán/hoàn gửi lặp | Chống ghi nhận thanh toán lặp bằng Idempotency | **PASS** |
| **T28** | JWT sai/hết hạn/khóa | Chặn token không hợp lệ hoặc sai chữ ký | **PASS** |
| **T29** | Quyền giữa các phiên bàn | Phiên bàn này không xem/sửa đơn của phiên bàn khác | **PASS** |
| **T30** | Khóa đơn quầy khác | Đơn quầy gắn với token riêng của quầy | **PASS** |
| **T31** | WebSocket sau commit | STOMP event phát sinh sau khi database commit | **PASS** |
| **T32** | Cloudinary | Tích hợp upload ảnh lên Cloudinary | **NOT RUN** (Cần key thật) |
| **T33** | Lệnh phiên cũ sau đóng | Chặn thao tác khi phiên đã đóng (`409`) | **PASS** |
| **T34** | Đóng phiên nhiều đơn | Đóng phiên thành công khi tất cả đơn hoàn tất | **PASS** |
| **T35** | Khôi phục mật khẩu | Reset token qua email | **NOT RUN** |
| **T36** | Khởi tạo demo sạch | Nạp dữ liệu mẫu ban đầu qua `DataInitializer` | **PASS** |
| **T37** | Quét QR không mở phiên | Xem menu/giỏ không đổi trạng thái bàn | **PASS** |
| **T38** | Giỏ trước phiên | Khách giữ giỏ hàng trước khi gửi đơn mở phiên | **PASS** |
| **T39** | Thu ngân mở Có khách thủ công | Thu ngân chủ động mở bàn Có khách | **NOT RUN** |
| **T40** | Đơn đầu tiên lỗi | Đơn lỗi không làm đổi trạng thái bàn thành Có khách | **NOT RUN** |
| **T41** | Hai lệnh mở phiên đồng thời | Khóa đồng thời xử lý đúng 1 phiên duy nhất | **NOT RUN** |
| **T42** | Hai tab cùng sửa giỏ | Xử lý ETag / Version giỏ hàng đa tab | **NOT RUN** |
| **T43** | Đặt một phần giỏ | Đặt các món đã chọn, giữ món còn lại | **NOT RUN** |
| **T44** | Thu xong không đóng bàn | Bàn vẫn giữ `CO_KHACH` sau khi thu, chờ thu ngân đóng | **PASS** |
| **T45** | Hoàn thành không đóng bàn | Bếp hoàn thành đơn không tự động đóng bàn | **PASS** |
| **T46** | Đóng đủ điều kiện | Chỉ đóng khi mọi đơn xong và hóa đơn thu đủ | **PASS** |
| **T47** | Đóng bàn không đơn | Đóng bàn trống đã lỡ mở Có khách | **NOT RUN** |
| **T48** | Close cạnh tranh tạo đơn | Ngăn chặn tạo đơn khi bàn đang thực hiện đóng | **NOT RUN** |
| **T49** | Guest quầy reload | Khách quầy giữ ngữ cảnh trên RAM trình duyệt | **NOT RUN** |
| **T50** | Gắn đơn quầy vào bàn | Chuyển đơn quầy vào phiên bàn hợp lệ | **PASS** |
| **T51** | Gắn vào bàn chưa phiên | Gắn đơn quầy mở phiên cho bàn trống | **NOT RUN** |
| **T52** | Không tự nhận đơn quầy | Phân biệt rõ đơn bàn và đơn tại quầy | **PASS** |
| **T53** | Thông báo tiền chưa phải thu | Phân biệt tiền thực thu và tiền chưa thanh toán | **PASS** |
| **T54** | Hủy cạnh tranh start/thu | Bếp đã start thì khách không được tự hủy | **NOT RUN** |
| **T55** | Thiếu CSRF | Chống CSRF cho session stateful | **NOT RUN** |
| **T56** | Vô hiệu JWT và socket | Đăng xuất làm mất hiệu lực token | **NOT RUN** |
| **T57** | Nguồn báo cáo khác nhau | Thống kê doanh thu, thực thu, công nợ | **PASS** |
| **T58** | QR chung không lộ hồ sơ | Quét mã bàn không để lộ thông tin khách khác | **PASS** |
| **T59** | Giữ hoàn chờ sau đóng | Giữ nguyên chứng từ hoàn tiền lịch sử sau đóng | **PASS** |
| **T60** | Phân biệt phiên và đăng nhập | Phân biệt rõ Guest phiên bàn và Staff login | **PASS** |

*Ghi chú*: Toàn bộ các ca đánh dấu **PASS** đều đã được thực thi và xác nhận trực tiếp trên server Spring Boot đang chạy thông qua các test script tự động hóa (`SalesFlowIntegrationTest`, `SalesFlowHttpIntegrationTest`, `test_demo_flow.py`, `test_additional_scenarios.py`).
