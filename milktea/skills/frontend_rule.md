# MilkTea — Thymeleaf, Bootstrap và JavaScript

## 1. Giao diện chính

Thymeleaf SSR + Bootstrap + JavaScript tăng tương tác. Không tạo frontend framework thứ hai. Templates: layouts, fragments, auth, customer, cashier, kitchen/orders, kitchen/inventory, admin/accounts, admin/catalog, admin/recipes, admin/tables, admin/inventory, admin/settings, admin/refunds, admin/reports, error. Static: css và js/common, js/customer, js/order, js/catalog, js/inventory.

PageController trả ViewModel, không Entity lazy. Fragment head/nav/sidebar/modal dùng chung; mỗi nghiệp vụ có UI do chủ module xử lý, không dồn mọi UI vào một người. Đọc envelope/path từ Architecture/OpenAPI; quyền từ Domain, không định nghĩa role riêng trong JS.

## 2. Trang khách theo nhãn bàn

QR luôn mở trang chủ với số bàn rõ ở header/cart/checkout. Không render màn hình "Bàn có khách nên không truy cập" và không POST open session trong onload. GET table-context chỉ đọc. Xem sản phẩm/add cart chưa phiên giữ state local và không đổi bàn.

Nhận version dạng chuỗi theo OpenAPI, không chuyển BIGINT/ID sang Number để gửi lại. State client tối thiểu: context(tableId,tableNumber,sessionId nullable), tableAccess credential nếu có, cart/version, order list, invoice/payment/refund summaries và connection status. Tải API context khi truy cập/refresh; nếu có phiên, tải dữ liệu chung. Không dùng JWT làm mã bàn và không trộn giỏ hai bàn.

Submit đơn đầu tiên gửi QR + expectedSessionId=null + items + version nếu cart có phiên; response mới xác lập sessionId/Có khách. Khi đã có phiên, dùng token scoped phiên và cart version. Chỉ xóa các dòng đã server xác nhận gửi; API lỗi giữ lựa chọn để khách xử lý.

## 3. Giỏ, đa tab và phiên đóng

Trước phiên giỏ RAM, không cam kết qua reload. Sau phiên mọi thay đổi gửi API cart với If-Match dạng ETag có dấu ngoặc kép như `"2"`; response là giỏ chuẩn mới. Các tab cùng phiên đồng bộ event CART_CHANGED rồi tải lại; 409 tải bản mới, báo xung đột, không tự ghi đè snapshot cũ. Thao tác đặt chọn các dòng đang có trong giỏ, server giảm đúng lượng và tăng version.

TABLE_SESSION_CLOSED: clear giỏ/order/payment view và guest session token, giữ nhãn bàn, hiện "Phiên bàn đã kết thúc"; không auto-open, không auto-post giỏ cũ. Nếu mất event, HTTP lỗi closed/changed cũng clear đúng context. Không silently tự bind lệnh đang chờ sang session mới; yêu cầu khách chủ động truy cập lại QR/ngữ cảnh hiện tại.

Không copy order đã đặt về giỏ trừ nút tạo đơn thay thế có chủ đích; lập giá mới theo server và giữ liên kết đơn gốc. Order list hiển thị món/size/quantity dưới trạng thái toàn đơn, không spinner trạng thái chế biến theo item.

## 4. Quầy và tài khoản

Guest quầy chưa gán bàn: orderId/CounterOrderToken RAM; không localStorage/sessionStorage/cookie/URL khôi phục; full reload mất quyền phía khách. Sau CASHIER gán bàn, QR đúng bàn đọc được đơn theo phiên. Chọn chuyển giỏ quầy nháp sang bàn phải có trang vẫn đang giữ dữ liệu, không đoán từ tên khách/IP.

Account history chỉ các đơn server đã gắn account; không tự nhận toàn bộ đơn bàn vào account sau login. Dùng profile API của account và order history API, không tạo bộ giá/OrderService thứ hai ở customer. Refresh CSRF sau login/logout; JS không đọc raw JWT.

## 5. UI theo actor

- Khách: GET public/shop-info lấy thông tin quán/tài khoản chuyển khoản, không gọi admin/settings; menu/size/giỏ/đơn/hóa đơn/lịch sử thanh toán; thông báo đã chuyển khoản không tự đổi Đã thu; xác nhận hủy toàn đơn.
- CASHIER: đơn mới, xác nhận, yêu cầu hủy, tiền mặt/chuyển khoản ở cả TABLE/COUNTER, gán bàn, thực trả refund đã duyệt, mở Có khách và đóng phiên thủ công. Hiện trạng thái bếp nhưng không nút complete.
- KITCHEN: danh sách đơn đã xác nhận cả chưa thu; start/complete toàn đơn; tab phiếu/mẻ/hao hụt. Không chặn theo chưa thanh toán.
- ADMIN: dữ liệu nền/tài khoản/công thức/ngưỡng/giảm chung/refund/reports/audit; không approval xuất kho.

Giỏ/đơn/tiền phải có loading/empty/error; disable khi gửi, hiển thị lỗi có thể xử lý; responsive trên điện thoại QR. Bootstrap hỗ trợ layout, không tự quyết quyền/validation. Render tên/lý do bằng textContent/th:text, tránh th:utext/innerHTML dữ liệu nhập. Kiểm tra upload MIME/size phía UI nhưng server kiểm lại.

## 6. Đồng bộ và UX

Một socket lifecycle mỗi tab; reconnect có backoff giới hạn; sau reconnect fetch snapshot chuẩn của đúng context. Event chỉ báo thay đổi, không dùng arrival order làm ledger. Không reconnect bằng credential phiên đã đóng. Bảng thu ngân chỉ Có khách/Trống theo server; tuyệt đối không JS set Trống khi payment/complete callback.

Một helper HTTP thống nhất credentials, CSRF, header guest, parse ErrorResponse; không log headers/token. Tiền định dạng Intl.NumberFormat cho VND từ decimal strings an toàn; phép tính trên browser chỉ là preview. Lịch thanh toán hiển thị phương thức/thời điểm/trạng thái, không lộ dữ liệu cá nhân hoặc reference nội bộ nhạy cảm không cần.

Đối chiếu [acceptance_tests.md](acceptance_tests.md), gồm reload/tab mới, close thủ công, tiền trước/sau, hủy toàn đơn và quyền.
