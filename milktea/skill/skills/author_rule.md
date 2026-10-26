# MilkTea — Spring Security, JWT và quyền phiên

## 1. Tài khoản

Role và quyền hành động lấy từ [domain_system.md](domain_system.md). Enable method security nếu dùng @PreAuthorize. Lưu Role không prefix; map sang ROLE_ADMIN/ROLE_CASHIER/ROLE_KITCHEN/ROLE_CUSTOMER. Không cho form đăng ký nhận role nhân viên/admin.

AuthService kiểm tra BCrypt PasswordEncoder, active, phát JWT bằng JwtEncoder/Nimbus phù hợp phiên bản Spring; JwtDecoder kiểm chữ ký, thuật toán allowlist, issuer/audience, exp/nbf. Không cần dịch vụ đăng nhập ngoài. Nếu repository đã có thư viện JWT khác hoạt động đúng, giữ một implementation thay vì chạy hai bộ token.

Claims tối thiểu: sub=accountId, role, tokenVersion, iat, exp, iss, aud. Không có branchId/tableId/giỏ/quyền phiên động trong JWT. Mỗi request account kiểm active/tokenVersion/role hiện tại để khóa/reset/đổi role/logout làm token cũ mất quyền. Logout ở baseline này tăng tokenVersion nên kết thúc mọi JWT của tài khoản; không tự triển khai refresh rotation nếu chưa yêu cầu.

Cookie account là HttpOnly, SameSite=Lax, Path=/, Secure khi HTTPS. REST client có thể dùng Bearer; resolver thống nhất, reject account credentials mâu thuẫn. JS không đọc/lưu JWT trong localStorage. CurrentActor lấy từ SecurityContext, không lấy accountId/role từ body.

Reset mật khẩu: token random mạnh, lưu hash, hạn dùng cấu hình, dùng một lần, gửi mail có thật; trả cùng thông báo cho email tồn tại/không tồn tại. Reset thành công tăng tokenVersion. Quản trị account không xóa lịch sử, không cho khóa admin hoạt động cuối cùng nếu dẫn tới không quản trị được.

## 2. Guest có ba ngữ cảnh

| Credential/ngữ cảnh | Quyền |
|---|---|
| QR code của bàn | Đọc nhãn/trạng thái phiên hiện tại, lấy quyền truy cập đúng phiên; gửi đơn đầu tiên khi chưa có phiên |
| TableSessionToken | Đọc/sửa giỏ, đọc đơn/tiền và yêu cầu hủy trong đúng sessionId đang mở |
| CounterOrderToken | Đọc/yêu cầu hủy/thông báo thanh toán đúng một đơn quầy, chỉ giữ phía khách trong trang hiện tại |

Không cấp ADMIN/CASHIER/KITCHEN cho guest. QR dùng mã opaque ngẫu nhiên được server tra cứu; số bàn hiển thị không đủ làm bằng chứng quyền. Endpoint context được phép phục hồi quyền đúng phiên đang mở từ QR khi reload/tab mới, nhưng tuyệt đối không mở phiên/đổi bàn. QR in tĩnh có giới hạn: người có bản sao QR có thể vào ngữ cảnh bàn; không tuyên bố đã xác minh hiện diện vật lý.

TableSessionToken opaque lưu hash, scope sessionId, còn hợp lệ chỉ khi phiên mở; HTTP và STOMP cùng verifier. Khi session closed, revoke và mọi lệnh cũ bị từ chối. Bếp/quầy/admin không nhận token guest trong DTO danh sách. Không đưa guest token vào URL, log hay error. QR code có trong link QR nên không có quyền cao; không dùng QR code để trả dữ liệu tài khoản riêng tư.

Bootstrap bằng QR không lấy raw token cũ từ hash. Mỗi lần truy cập hợp lệ có phiên mở, GuestAccessService cấp một token random mới, lưu hash/expiry theo sessionId và trả raw đúng một lần trong response no-store; các tab có token riêng nhưng cùng scope/giỏ. Có hạn dùng, xóa credential hết hạn và giới hạn cấp theo QR; phiên đóng thu hồi tất cả. Đây chỉ là ghi credential kỹ thuật, không mở TableSession/đổi trạng thái bàn. TableContextService.readByQr đọc dữ liệu; phần cấp credential có transaction riêng phù hợp, không ghi trong transaction readOnly. Nếu chưa có phiên không cấp TableSessionToken; QR cấp quyền cho lệnh tạo đơn đầu tiên.

CounterOrderToken RAM only; backend giữ hash theo đơn. Nếu đơn được gán bàn, quyền TableSessionToken mở đường xem qua phiên; không tự chuyển credential quầy thành JWT. CUSTOMER chỉ xem lịch sử cá nhân đúng accountId; table view dùng quyền phiên chung, không trả email/hồ sơ những người cùng bàn.

Idempotency của guest quầy dùng key random có ít nhất 128 bit entropy giữ trong RAM của thao tác. Replay cùng key/fingerprint và scope hợp lệ trả cùng đơn; nếu phải trả credential mới thì GuestAccessService cấp thêm token scoped đơn, không tìm raw từ hash hoặc lưu plaintext trong record dedup. Không khôi phục qua browser storage sau reload và không cho dùng mã đơn đơn thuần để nhận lại token.

## 3. Filter và Service

Không `permitAll('/api/**')`. Public routes menu/auth/QR bootstrap/guest create được khai báo rõ; public guest resource vẫn cần QR/table/counter access check ở Service/filter chuyên trách. Account login không đủ để đọc arbitrary table/guest order. Route order đọc/hủy chọn một phạm vi hợp lệ: owner CUSTOMER, đúng phiên guest, đúng đơn guest hoặc staff được Domain cho phép.

SecurityConfig wiring PasswordEncoder, JWT resolver/decoder, handlers, method security và matcher. OrderAccessPolicy/TableSessionAccessPolicy xác minh phạm vi; Service kiểm tra trạng thái lần nữa ngay lúc ghi. Authorize ở backend, không dựa nút ẩn. UI không tự parse JWT để xác định quyền.

## 4. CSRF và browser

Giữ CSRF cho browser mutations cả account cookie và guest. GET /api/v1/auth/csrf trả headerName=X-CSRF-TOKEN và token cho JS; hidden input cho form Thymeleaf. Sau login/logout lấy token mới. Có thể dùng Spring CookieCsrfTokenRepository với cơ chế token phù hợp phiên bản, hoặc repository mặc định với HttpSession chỉ lưu CSRF; HttpSession kỹ thuật không phải session bàn và không thay JWT auth.

Không tắt CSRF toàn ứng dụng, không đặt credential trong query để lách WebSocket headers. Nếu dùng Bearer cho Postman vẫn gửi CSRF theo hợp đồng hiện tại; chỉ miễn trên chain REST thuần Bearer riêng khi đã thiết kế và kiểm thử rõ.

## 5. WebSocket

CONNECT xác định account từ cookie/Bearer STOMP header, hoặc guest từ table/counter token. Principal guest phải tách loại phạm vi. SUBSCRIBE kiểm tra destination + role/owner/session từng lần. Cấm SEND command nghiệp vụ từ client. Áp dụng Origin allowlist và CSRF CONNECT khi dùng Spring WebSocket Security; không copy ví dụ cấu hình bỏ protection mà không hiểu.

Logout/expired JWT/khóa/đổi role/reset/session close phải kết thúc hoặc vô hiệu socket tương ứng, không chỉ chặn lần HTTP kế tiếp. Rà còn quyền tại dispatch hoặc quản lý connection registry; không cho socket cũ tiếp tục nhận dữ liệu. Anonymous chỉ có QR context chưa phiên không được vào kênh nhân viên.

## 6. Kiểm tra

Test account invalid/expired/version mismatch; CUSTOMER vào staff bị chặn; CASHIER không complete; ADMIN không tự dùng quyền KITCHEN; phiên A không đọc B; phiên cũ không ghi lượt mới; token đơn quầy A không xem B; CSRF thiếu bị chặn; logout/close chặn SUBSCRIBE và event trên kết nối cũ. HTTP/error theo Architecture, không định nghĩa thêm envelope.
