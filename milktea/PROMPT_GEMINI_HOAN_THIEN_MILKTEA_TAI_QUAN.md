# PROMPT GEMINI / ANTIGRAVITY — HOÀN THIỆN MILKTEA PHỤC VỤ TẠI QUÁN

## Cách sử dụng

Đặt file này và báo cáo `03_Danh_gia_lai_MilkTea_v3.md` trong workspace hoặc đính kèm cho Gemini Agent. **Chạy Prompt 1 trước, Prompt 2 sau** trong cùng workspace. Có thể yêu cầu thực hiện liên tiếp cả hai. Đây là yêu cầu triển khai source, migration, seed và kiểm thử; không phải chỉ viết kế hoạch.

Không chép toàn bộ file này đè lên `GEMINI.md`. Agent phải đọc `GEMINI.md` hiện có rồi cập nhật các quy tắc phạm vi đã lỗi thời tại đúng tài liệu nguồn. Phạm vi mới dưới đây thay thế yêu cầu ONLINE/giao nhận trong các prompt và báo cáo cũ.

---

## PROMPT 1 — GỠ ONLINE/GIAO NHẬN VÀ THU GỌN VỀ TABLE/COUNTER

Bạn là AI Agent triển khai trong Antigravity IDE, đang làm việc với dự án MilkTea có sẵn. Hãy trực tiếp chỉnh source theo phạm vi sau, thực hiện đến mức build/kiểm thử được và báo rõ phần bị chặn bởi môi trường. Không dừng ở đề xuất.

### A. Phạm vi đã chốt

Hệ thống chuỗi trà sữa chỉ phục vụ **trong không gian quán**:

1. **TABLE:** khách quét QR gắn tại bàn. Nhãn QR có tên chi nhánh và tên/số bàn. Sau quét mở ngay menu đúng cơ sở, hiển thị rõ bàn/chi nhánh; khách thêm món, topping, đường/đá/ghi chú, gửi đơn theo phiên bàn.
2. **COUNTER:** khách gọi tại quầy; nhân viên đúng vai trò/chi nhánh lập đơn. Chi nhánh do server lấy từ phân công nhân viên, không từ trình duyệt khách.

Không phát triển đặt món từ xa, đơn ONLINE không gắn bàn, giao nhận, lựa chọn địa chỉ, sổ địa chỉ, phương thức PICKUP/DELIVERY, phí giao hàng, nhà vận chuyển, Shipper, COD giao tận nơi hoặc marketplace nhiều chủ shop. Không bổ sung AccountCart dùng cho đặt từ xa chỉ để đáp ứng rubric chung. Guest có thể xem menu khi chưa có QR nhưng không được tự tạo đơn COUNTER hay chọn cơ sở để đặt từ xa.

**Không triển khai mục R19 của báo cáo cũ** về đóng chi nhánh/đồng thời đóng–tạo đơn hoặc mở rộng UI vòng đời cơ sở. Đừng xóa chức năng chi nhánh đang dùng ổn chỉ vì R19 bị bỏ. Quyền chi nhánh, assignment, bàn/QR, kho và việc **đóng phiên bàn** vẫn là nghiệp vụ cốt lõi phải giữ và sửa đúng.

### B. Đọc và xác nhận thực trạng

- Kiểm module có `pom.xml`, branch, commit, thay đổi chưa commit. Không reset/checkout đè công việc đang có.
- Đọc `AGENTS.md`, `GEMINI.md`, tài liệu nguồn nghiệp vụ và các owner files liên quan. Giữ Spring Boot MVC monolith, Java 21, Thymeleaf, Bootstrap, JPA, SQL Server; không đổi SPA/microservices/stack.
- Tìm tham chiếu ONLINE/giao nhận trong Java, JS, HTML, API spec, SQL, test và tài liệu. Điểm bắt đầu từ source đã kiểm tra:

| Vị trí | Phần cần xác minh/gỡ |
|---|---|
| `controller/OrderController.java` | `/api/v1/orders/online` và alias `/api/v1/customer/orders/online` |
| `dto/OrderDto.java` | `CreateOnlineOrderRequest`; receiver/delivery fields trong response |
| `service/order/OrderService.java` | `createOnlineOrder`, ONLINE fingerprint/audit mapping, receiver/delivery mapping |
| `entity/order/OrderSource.java` | Giá trị ONLINE và việc đọc dữ liệu cũ trước khi bỏ |
| `entity/order/Order.java` | `receiverName`, `receiverPhone`, `deliveryAddress`, `deliveryType` nếu chỉ phục vụ giao nhận |
| `security/SecurityConfig.java` | Allowlist/route riêng ONLINE; các wildcard cần xét để không có alias bypass |
| `static/js/customer/customer.js` | Submit ONLINE, fallback branch, lấy receiver/address |
| `templates/customer/cart.html` | Form giao hàng, `onlineBranchSelect`, receiver inputs, delivery note |
| `database/migrations/V5__schema_alignment.sql` | Các cột DonHang `NguoiNhan`, `SoDienThoaiNhan`, `DiaChiGiao`, `HinhThucNhan` |
| Các trang order/report/receipt và API contract | Nhãn ONLINE/giao hàng, filter, thống kê, serialization |
| `docs/BUSINESS_RULES.md` và traceability/demo docs | Quy tắc ONLINE và giao nhận đã không còn áp dụng |

Đường dẫn Java trong bảng rút gọn từ `src/main/java/vn/edu/ute/milktea/`; frontend từ `src/main/resources/`. Workspace có thể mới hơn: xác minh trước khi sửa, không dựa cứng số dòng.

### C. Yêu cầu gỡ đồng bộ

1. Gỡ endpoint tạo ONLINE và alias, service/DTO chỉ phục vụ ONLINE, form giao nhận, lựa chọn địa chỉ/branch đặt từ xa, JS handler, API examples và test không còn đúng phạm vi.
2. Không chỉ ẩn form trong khi API vẫn tạo được ONLINE. Không đổi lời gọi ONLINE thành COUNTER từ browser khách để lách quyền.
3. Không xóa nhầm `customerName/customerPhone` đang được dùng cho account/profile, receipt/review, OTP/email hoặc liên hệ hợp lệ. Không xóa địa chỉ cơ sở. Chỉ gỡ trường chứng minh được là của giao nhận; kiểm mọi nơi dùng trước khi bỏ.
4. Giữ ghi chú món, đường/đá, tên khách hiển thị nếu đang dùng; chuyển nhãn “ghi chú giao hàng” thành ghi chú đơn/món khi nội dung thật sự cần tại quán.
5. Khi chưa có ngữ cảnh bàn hợp lệ: cho xem menu và hướng dẫn quét QR hoặc gọi tại quầy. Không tự chọn branch ID=1, không tự mở phiên, không tự cấp quyền vào bàn bằng ID/tên bàn.
6. Món TABLE lấy branch từ bàn/phiên do server xác minh; COUNTER lấy branch từ assignment. Không có dropdown đổi branch của bàn trong checkout. Giỏ/voucher phải theo đúng ngữ cảnh đó.
7. Giữ luồng thanh toán CASH/BANK_TRANSFER/VietQR tại quán nếu hiện có. QR bàn dùng định danh bàn khác QR chuyển khoản; khách bấm “đã chuyển khoản” chỉ là thông báo, thu ngân mới ghi thu.
8. Source chỉ tạo mới TABLE/COUNTER. Thu gọn enum phải tương thích dữ liệu cũ theo mục D, không làm ORM hỏng khi đọc lịch sử.
9. Cập nhật SSOT, OpenAPI, traceability, test, báo cáo demo và tài liệu liên quan. Không giữ yêu cầu ONLINE/R19 ở một bản sao chỉ dẫn khiến agent sau tự thêm lại. R19/F26 về vòng đời cơ sở được đánh dấu `OUT_OF_SCOPE` cho đợt này; quyền/assignment vẫn ở backlog sửa lỗi.

### D. Database: gỡ đúng phạm vi, không phá lịch sử

Trước sửa schema, kiểm database/schema thực và thống kê:

- Số đơn có `Nguon='ONLINE'`; trạng thái, invoice/payment/refund/ledger và các quan hệ phụ thuộc.
- Số dòng có dữ liệu trong bốn cột giao nhận; các default/check/index/FK/view/query/projection tham chiếu.
- Migration nào đã áp dụng, baseline nào dành riêng DB demo.

**Database đã có dữ liệu dùng chung:** thêm migration version tiếp theo, không sửa lại migration đã áp dụng. Nếu có ONLINE lịch sử, giữ giá trị/cột legacy chỉ đọc trong giai đoạn tương thích hoặc lưu trữ đầy đủ có đối soát trước khi drop. Không đổi tất cả ONLINE thành COUNTER vì hai nghiệp vụ không đồng nghĩa. Không xóa đơn/thu/hoàn hoặc bịa bàn/người thu để loại enum. Mô tả rõ lý do phần legacy chưa drop nếu còn cần đọc lịch sử; chức năng tạo mới phải bị gỡ hoàn toàn.

**Database demo mới:** baseline chỉ có TABLE/COUNTER và không có cột giao nhận thừa. Không cần tạo một lần rồi thêm/xóa cột vòng vo chỉ để bắt chước môi trường cũ. Giữ migration nâng cấp và baseline sạch có quan hệ phiên bản rõ, schema cuối phục vụ ứng dụng phải khớp.

Không chạy DROP/TRUNCATE/xóa sạch trên DB thật `milktea`. DB kiểm thử/demo có tên riêng, guard môi trường và dữ liệu tổng hợp. Không bỏ FK/CHECK/unique hoặc cho MaCN nullable để script qua lỗi.

### E. UX/UI sau gỡ

Tái dùng phong cách toàn hệ thống: layout/fragments Bootstrap hiện tại, font, CSS variables, bảng màu thương hiệu, spacing, radius, shadow, icon, table/modal, toast và trạng thái. Không đưa template mới hoặc làm cart thành một phong cách khác.

Menu và cart TABLE luôn có nhãn “Chi nhánh … · Bàn …”; POS có “Chi nhánh … · Tại quầy”. Không để khoảng trống/step indicator giao hàng sau khi gỡ form. Nội dung hướng dẫn ngắn, tiếng Việt thống nhất, nút hành động rõ, mobile dùng thuận tay. Không giữ từ “đang giao/đã giao”, icon xe giao hàng hoặc phí ship trong luồng tại quán.

### F. Nghiệm thu Prompt 1

- QR hợp lệ → menu đúng bàn/chi nhánh; chưa gửi đơn thì không tạo OPEN session chỉ vì xem/thêm giỏ.
- Gửi đơn đầu → TABLE đúng cơ sở, snapshot/invoice/giỏ/token đúng transaction; không hỏi địa chỉ giao nhận.
- Cashier đúng cơ sở → COUNTER; Guest/User không được tạo COUNTER bằng API.
- Các route ONLINE cũ/alias không còn tạo đơn, trả 404/410 hoặc kết quả từ chối phù hợp; không có đường bypass qua wildcard hay DTO dùng chung.
- Không có đường chọn cơ sở tùy ý để đặt từ xa; không có form giao hàng trên UI.
- Lịch sử cũ vẫn đọc được theo migration/legacy strategy; fresh database và upgraded database đều tương thích ứng dụng.
- Dùng tìm kiếm tham chiếu để xác nhận ONLINE/delivery còn lại chỉ thuộc migration lịch sử, compatibility, ghi chú phạm vi hoặc test từ chối đã giải thích; không ép xóa mọi chuỗi chữ ONLINE một cách máy móc.
- Kiểm build Java21, Spring context, SQL Server migration và browser smoke. Chưa chạy được thì nêu `BLOCKED_ENVIRONMENT`, không nhận `VERIFIED`.

Kết quả bàn giao: source thay đổi, migration/baseline phù hợp, danh sách file đã gỡ/sửa, giải thích dữ liệu legacy, test thực chạy và phần còn chặn. Không chỉ trả một kế hoạch hoặc cập nhật tracker thành “xong”.

---

## PROMPT 2 — SỬA SỔ RỦI RO, CHUẨN HÓA DATABASE/DEMO VÀ UX/UI

Bạn là AI Agent triển khai và kiểm chứng end-to-end cho MilkTea trong Antigravity. Hãy sửa thực tế các lỗi/rủi ro còn lại từ bản đánh giá v3; ưu tiên quyền, tiền, kho, dữ liệu demo và UI nhất quán. Đọc `03_Danh_gia_lai_MilkTea_v3.md` bản đã điều chỉnh, nhưng phải xác minh source đang có trước khi kết luận lỗi còn tồn tại.

### 1. Phạm vi và nguyên tắc không được làm lệch

- Chỉ phục vụ tại quán qua TABLE quét QR bàn có nhãn chi nhánh/bàn và COUNTER do nhân viên đúng quyền lập. Không phục hồi ONLINE/giao nhận, AddressBook, shipper/phí ship hoặc marketplace. Nếu Prompt 1 chưa chạy, hoàn tất phần gỡ đó trước khi sửa tiếp.
- Không triển khai R19 của báo cáo cũ về đóng chi nhánh. Không loại nhầm assignment, kiểm quyền branch, tạo/đóng phiên bàn và các FK branch khỏi dự án.
- Giữ Spring Boot MVC + Thymeleaf + Bootstrap + JPA + SQL Server, Java21, kiến trúc ba tầng; giữ giá chung toàn chuỗi nếu source/SSOT hiện hành quy định như vậy.
- Đọc thay đổi chưa commit, `AGENTS.md`, `GEMINI.md`, SSOT và owner docs trước sửa. Yêu cầu tại quán trong prompt này thay thế hướng ONLINE ở tài liệu cũ; cập nhật đúng nguồn chuẩn, không tạo hai bộ quy tắc đối nghịch.
- Quyền = role + assignment active + object branch + ownership/capability + state. Kiểm tại service trước đọc nhạy cảm/mutation/**replay**, không chỉ controller/nút UI.
- Actor/branch thiếu thì fail-closed; không lấy branch=1, kho tổng hoặc cơ sở đầu tiên làm fallback. Không lấy `@Transient Account.branch` làm nguồn phân công thật.
- QR hợp lệ mở ngay menu, không tự thêm bước OTP/nhân viên duyệt mỗi lần quét. Định danh QR phải khó đoán; token phiên gắn phiên, không cấp quyền chỉ từ ID/tên bàn. QR tĩnh không chứng minh vị trí vật lý: không thêm định vị/geofence rồi tuyên bố đã xác minh khách ở quán.
- Xem menu/thêm giỏ chưa mở phiên. Đơn đầu thành công mới mở phiên theo nghiệp vụ đang có; nếu staff có chức năng mở tay thì giữ đúng quyền. Một bàn chỉ có một phiên OPEN.
- Giỏ tạm trước phiên và giỏ DB theo phiên không được lẫn bàn/cơ sở/lượt khách. Người đăng nhập vẫn sử dụng được capability bàn hợp lệ; không bỏ token chỉ vì đã có JWT.
- Không bắt trả tiền trước pha chế nếu nghiệp vụ đã cho phép thu sau. Giữ cashier xác nhận → kitchen bắt đầu → hoàn tất. Chỉ tiêu hao khi hoàn tất đơn, theo snapshot đã chốt; thiếu một nguyên liệu thì rollback toàn bộ kho/sổ/trạng thái/audit.
- Sơ chế theo mẻ khác pha chế đơn; hủy/hoàn tiền không tự trả lại nguyên liệu đã pha. Khách báo chuyển khoản không phải đã thu.
- Chốt chính sách location từ source và SSOT: nếu chuẩn là mọi tiêu hao ở BEP thì phải có chứng từ cấp KHO→BEP; nếu raw KHO/SOCHE BEP là policy được giữ thì code/seed/test phải cùng policy. Không dùng fallback location/branch để che thiếu dữ liệu, không âm thầm chuyển tồn không chứng từ.

### 1.1. Hợp đồng thao tác theo vai trò — phải hoàn thiện toàn luồng

Đối chiếu bảng này với role hiện có, giữ quyền đã được chấp thuận trong SSOT; nếu controller/security/service đang khác nhau, sửa cho thống nhất và ghi ma trận cuối. Không cấp quyền mới chỉ để nút UI hoạt động. Các quyền hệ thống phải kiểm explicit ADMIN.

| Actor | Chức năng trong phạm vi | Giới hạn bắt buộc |
|---|---|---|
| Guest chưa quét QR | Trang chủ/menu, tìm kiếm/lọc, chi tiết món, thông tin quán | Không tạo đơn COUNTER/ONLINE; không đọc phiên/giỏ/đơn người khác |
| Guest có QR/capability hợp lệ | Menu đúng bàn/cơ sở, giỏ tại bàn, gọi món/đặt thêm, xem đơn được phép trong phiên, thông báo thanh toán | Branch/table/session do server xác minh; không dùng ID/tên bàn để tự cấp capability; không tự ghi thu/đóng phiên |
| CUSTOMER | Các quyền Guest phù hợp ngữ cảnh, đăng ký/OTP/login/logout/reset, profile, yêu thích/đã xem, lịch sử mua và review đủ điều kiện | Đăng nhập không làm mất token bàn hợp lệ; không mặc nhiên là owner mọi đơn trong phiên; không xem giỏ/đơn phiên khác |
| CASHIER | Lập COUNTER, xử lý TABLE theo cơ sở, xác nhận đơn/thanh toán, xử lý yêu cầu hủy, thực chi hoàn đã được duyệt đúng policy, gắn đơn quầy vào bàn nếu có sẵn, đóng phiên | Chỉ branch được phép; không duyệt hoàn thay Admin nếu policy không cho; không sửa snapshot tiền/công thức đã chốt |
| KITCHEN | Queue đúng cơ sở, bắt đầu/hoàn tất đơn, sơ chế/kết ca và kho trong quyền đã quy định | Không thao tác đơn cơ sở khác, không ghi thu/duyệt hoàn; không đổi branch bằng request |
| MANAGER | Nhân viên/phạm vi cơ sở, sản phẩm được phân bổ, tồn/chứng từ/báo cáo của cơ sở theo policy | Không quản lý Admin/Manager khác vượt quyền; không sửa danh mục/công thức/giá chung/khuyến mãi toàn chuỗi nếu dành cho Admin; thiếu assignment không thành toàn chuỗi |
| ADMIN | Dữ liệu dùng chung, phân quyền/phân công, danh mục/món/công thức/khuyến mãi, duyệt hoàn và báo cáo toàn chuỗi theo chức năng hiện có | Bypass branch phải có chủ ý và audit; không bypass validation tiền/kho/trạng thái |

Không phát triển Vendor đăng ký shop hoặc Shipper. Thêm Cashier/Kitchen là ánh xạ nghiệp vụ chuỗi cùng thương hiệu; ghi rõ trong ma trận rubric, không tuyên bố tự động đủ mọi tiêu chí marketplace.

### 1.2. Hợp đồng QR → phiên → giỏ → đơn

1. QR chứa định danh/capability khó đoán của bàn; label in/hiển thị có chi nhánh và bàn. Server resolve quan hệ bàn→chi nhánh, không tin các query branch/table client tự sửa. Không lộ bí mật QR của cơ sở khác trong danh sách staff ngoài scope.
2. Quét mở menu ngay, GET không tạo phiên/chứng từ/đơn. Menu chỉ món/category active, mapping bán hợp lệ tại cơ sở, giá/trạng thái đúng. Không có QR hợp lệ vẫn có thể xem menu nhưng nút gửi đơn hướng dẫn quét hoặc gọi tại quầy.
3. Giỏ trước phiên có thể tạm lưu phía client. Đơn đầu phải khóa/đồng bộ bàn phù hợp, tạo hoặc sử dụng đúng phiên, tạo giỏ DB và capability phiên trong cùng hợp đồng giao dịch. Nếu đơn lỗi thì không để phiên/giỏ/chứng từ rác. Hai khách đặt đồng thời không sinh hai OPEN session.
4. Gọi thêm tạo đơn mới trong phiên đúng quyền; không sửa lịch sử đơn đã xác nhận/đã pha. Lọc line đã gửi và phần còn lại trong giỏ đúng lượng, không xóa món của người khác do stale update. Nếu giỏ chia sẻ cả bàn thì quy tắc đó phải được nêu rõ và version/merge được kiểm chứng.
5. Giỏ DB có owner/session, version hoặc cơ chế chống lost update, item/topping hợp lệ; trả 409 khi dữ liệu cũ thay vì ghi đè âm thầm. Sau refresh/reconnect, tải giỏ từ nguồn DB đúng phiên. Không lấy cart local của bàn trước gắn sang bàn mới.
6. Tài khoản và quyền phiên là hai chiều độc lập: người đăng nhập có thể có capability bàn nhưng chỉ thấy lịch sử cá nhân mình ngoài phạm vi capability. Không gán mọi đơn Guest cho tài khoản đăng nhập sau đó một cách mặc định.
7. Đóng phiên bàn chỉ khi các đơn đã kết thúc và nghĩa vụ thanh toán/hủy/hoàn được xử lý theo rule rõ ràng. Token hết quyền, giỏ không được tái sử dụng cho lượt khách sau; giữ lịch sử đơn/tiền/audit. Nêu rõ refund pending có chặn đóng phiên hay được tách nghĩa vụ; giữ nghiệp vụ hiện hành đã hợp lý và test nó, không để mỗi controller một cách hiểu.
8. QR tĩnh có thể được người giữ QR quét lại; không tuyên bố đã chống truy cập từ xa tuyệt đối. Bỏ bypass ID/tên bàn, không tự restore QR cache để âm thầm gia nhập phiên mới. Không thêm định vị/OTP cho khách ngoài phạm vi.

### 1.3. Hợp đồng trạng thái đơn–tiền–hủy/hoàn

Trích các enum và transition thực tế, lập bảng trước/sau trong tài liệu và test. Không đổi tên trạng thái hoặc viết state mới chỉ để khớp bảng minh họa. Với trạng thái `CHO_THANH_TOAN` có sẵn, xác định rõ nó chặn thao tác nào theo nguồn tạo đơn; không bắt tất cả đơn trả trước nếu policy hiện cho thu sau.

| Thao tác | Điều kiện trước | Kết quả và tác động phải kiểm |
|---|---|---|
| Tạo TABLE/COUNTER | Actor/context hợp lệ, món/recipe/voucher/giá được server kiểm | Đơn+line+topping+invoice+recipe snapshot đầy đủ; chưa tiêu hao; branch đúng; dedup đúng |
| Xác nhận | Đúng vai trò/branch, trạng thái cho phép, không có yêu cầu hủy đang chặn | Chuyển queue pha chế một lần, audit/event sau commit; không tiêu hao |
| Bắt đầu | Kitchen đúng branch, đơn đã được phép pha, không bị hủy/pending chặn | Chuyển đang pha, ghi thời điểm/audit; không trừ kho sớm nếu policy là hoàn tất mới trừ |
| Hoàn tất | Đúng branch/state, snapshot đủ, stock đúng vị trí đủ cho toàn bộ định mức | Trừ từng nguyên liệu đúng snapshot, movement/cost/state/audit cùng transaction; thiếu bất kỳ thành phần rollback toàn bộ |
| Khách thông báo trả tiền | Có quyền trên hóa đơn/đơn, method hợp lệ | Chỉ notice/pending; chưa có Payment thực, không tăng doanh thu thực thu |
| Thu ngân ghi thu | Đúng branch, invoice hiệu lực, chưa thu, amount server | Một Payment đúng payable; state theo rule, audit/event đúng; retry không thu hai lần |
| Hủy chưa tiêu hao | Đúng owner/role/state và rule hủy trực tiếp hoặc duyệt | Giữ lịch sử, invoice/voucher theo rule, không hoàn stock chưa hề xuất; paid thì có obligation hoàn |
| Yêu cầu hủy đang chờ | Giao dịch khóa/đồng bộ với thao tác bếp | Không vừa duyệt hủy vừa hoàn tất tiêu hao trái policy; từ chối duyệt/bắt đầu trái state rõ ràng |
| Hủy sau tiêu hao nếu nghiệp vụ cho phép | Đúng quyền và có lý do | Không tự hoàn raw/SOCHE đã dùng; hoàn tiền tách riêng, hao hụt ghi đúng khi có |
| Duyệt hoàn | Admin hoặc role được SSOT cấp, obligation hợp lệ | Phân biệt duyệt với thực chi; không mặc nhiên DA_HOAN khi chưa có bằng chứng trả tiền |
| Thực chi hoàn | Đã duyệt hoặc cash policy hợp lệ, đúng branch, tiền/phương thức/reference hợp lệ | Một lần thực chi, người/thời điểm/reference/audit đầy đủ; không vượt paid; không xóa Payment |

Chốt và kiểm đồng thời confirm/cancel, complete/cancel, pay/cancel, refund/retry, close-session/order-create. Dùng lock order nhất quán, transaction đủ nhỏ và retry có kiểm soát; không dựa vào UI để loại race. HTTP hợp lệ vẫn thành công sau khi siết quyền, không chỉ thêm test 403.

### 1.4. Chức năng khách hàng và catalog phải nối hoàn chỉnh

- Tìm tên, lọc danh mục/khoảng giá có validation; API và UI dùng cùng branch context. Không lọc trên vài card đã render rồi coi là tìm toàn bộ dữ liệu.
- Bốn danh sách mới nhất, bán chạy, đánh giá cao, yêu thích nhiều: tối đa 20 theo yêu cầu, có phân trang/lazy loading cho danh sách phù hợp; thống nhất quy tắc biến thể size, tie-break, dữ liệu rỗng và phạm vi tổng hợp rating/favorite. Đừng tự áp branch vào dữ liệu toàn chuỗi nếu nghiệp vụ chọn toàn chuỗi: hiển thị chỉ các món được bán tại cơ sở và ghi rõ metric scope.
- Bán chạy tính món hoàn tất và tất toán theo cùng predicate report/menu. Không tính canceled/unpaid chỉ vì có line. Hóa đơn 0 dùng quy tắc tất toán đã chốt.
- Profile/auth/OTP/password hash giữ nguyên chức năng hợp lệ; không xóa phone/email vì bỏ giao nhận. Không thêm AddressBook. Recently viewed nếu thuộc yêu cầu đang có phải lưu/giới hạn đúng owner hoặc browser, không làm lộ dữ liệu người khác.
- Review chỉ đúng customer sở hữu dòng mua đã đủ điều kiện, sao 1–5, text >=50 ký tự sau trim; một review/dòng theo rule. Ẩn/hiện kiểm scope và audit phù hợp, output escape an toàn. Media URL không đủ chứng minh file tồn tại hoặc quyền sở hữu.
- Không dùng `allowTopping=false` của category để suy ra mọi món trong category đó là topping. Chốt định danh topping rõ; snapshot và nhân quantity main+topping đúng. Đường/đá/ghi chú không được âm thầm làm thay đổi định mức nếu chưa có rule định lượng tương ứng.
- Tạo đơn mới không dùng fallback recipe theo tên hardcode. Chuyển công thức cần giữ sang dữ liệu có version/hiệu lực hoặc loại đường fallback; món chưa có công thức không bật bán/không checkout. Đổi công thức không sửa snapshot đơn cũ.

### 1.5. Rà soát bảo mật bổ sung cho các luồng còn giữ

Kiểm đầu vào trên server: blank/null, độ dài, quantity/topping count, danh sách quá lớn, số âm/overflow, enum lạ, sort/page/size, thời gian, số tiền/precision, reference trống. Server chốt giá/cost/branch/actor/status; DTO không cho mass assignment role/state/paidAmount. Query ID phải kiểm object-level quyền ở cả detail/list/export/search/replay.

Rà các điểm render JS/Thymeleaf/receipt/modal/review/voucher/error message để escape nội dung người dùng, tránh XSS; không ghép query SQL từ input. Upload kiểm bytes/MIME/size/path/owner, không chỉ extension. Không xuất JWT/token/OTP/password hash/secret ra API/log. Không bỏ CSRF vì dùng JWT cookie; cấu hình login/OTP origin/rate limit theo môi trường và ghi phạm vi đã kiểm.

Chỉ kiểm trên source và môi trường test được phép; không thử tải/phá dữ liệu trên dịch vụ thật. Không tuyên bố “an toàn tuyệt đối” từ unit test.


### 2. Thứ tự thực hiện và bằng chứng

1. Xác nhận scope, trạng thái source và môi trường Java21/Maven/SQL Server; tạo tracker từng Rxx với `CONFIRMED`, `ALREADY_FIXED`, `FIXED_UNIT`, `FIXED_DB_HTTP`, `FIXED_E2E`, `BLOCKED_ENVIRONMENT`, `OUT_OF_SCOPE`.
2. Sửa quyền/thu tiền/kết ca trước; bổ sung test thực bắt được lỗi trước sửa khi có thể.
3. Chuẩn hóa schema/migration và seed theo chuỗi nguồn gốc, rồi chạy assertion DB.
4. Sửa tích hợp realtime/voucher/media/search và UX/UI trên cùng hợp đồng nghiệp vụ.
5. Chạy build/context/DB/HTTP/browser phù hợp, cập nhật tài liệu và bàn giao evidence. Không dùng report test cũ trong `target` làm kết quả mới.

Không đổi stack/hạ Java để né môi trường, không hardcode object `.branch(...)` trong mock rồi coi là kiểm thử phân công từ DB. Không sửa test để hợp thức hóa tồn sai hay chỉ assert “đã gọi save”.

### 3. Các rủi ro phải rà soát và sửa

Các tên lớp dưới đây là điểm bắt đầu, không giới hạn phạm vi tìm kiếm. Dùng báo cáo để mở đúng file.

| Mã | Lỗi/rủi ro còn lại | Yêu cầu sửa và nghiệm thu |
|---|---|---|
| R01 | PaymentService đọc `Account.branch @Transient`, chặn thu ngân hợp lệ | Dùng actor/assignment active thực. Persist account + assignment, flush/clear, cùng branch thu được, khác branch bị chặn. Replay vẫn kiểm quyền |
| R02 | Preparation/settlement nhận request branch khác actor vì chỉ điền khi null | Non-admin không sửa branch ngoài assignment; service cưỡng chế, null fail-closed; test cả body có branch giả và không branch |
| R03 | Public context đổi ID/tên bàn thành QR và cấp token phiên | Bỏ bypass ID/name; QR không đoán được, token đúng phiên; menu quét mở trực tiếp, không tự mở phiên; token lượt cũ không truy cập lượt mới |
| R04 | Kết ca ghi ledger book→actual→retained nhưng Stock vẫn book−discarded | Stock cuối = retained; example 5000/4800/300/4500 phải lưu 4500, không 4700. Test tăng/giảm/không bỏ/retry và đối soát ledger |
| R05 | CUSTOMER có nhánh đọc phiên vô điều kiện; JWT làm mất token phiên | Một SessionAccessPolicy cho HTTP/cart/đơn/WS; owner/membership/capability đúng; customer ngoài phiên bị chặn, customer cùng bàn có token dùng được |
| R06 | Execute refund và assign counter-to-table thiếu branch actor | Actor bắt buộc tại service; scope trước state/replay; refund reference/chứng cứ theo method, một thực chi; order/table cùng branch chưa đủ quyền người gọi |
| R07 | Kho query fallback mất branch; list/detail/preview quá rộng | Bỏ overload/fallback thiếu MaCN khỏi luồng nhiều cơ sở; mọi list/detail/query theo scope; thiếu stock mục tiêu không được dùng stock khác; cost đúng branch/nguồn/thời điểm |
| R08 | Manager không assignment có null được hiểu thành quyền Admin | Kiểm role explicit, null không nghĩa admin/toàn chuỗi; không tạo ADMIN, sửa người ngoài scope hoặc tự gán cơ sở đầu tiên; assignment rõ |
| R09 | Staff JS thiếu branchId; publisher vẫn topic chung bị khóa | Client lấy branch từ server, thống nhất mọi publisher/subscriber theo branch; test cashier/kitchen hai cơ sở, rollback không phát, reconnect tải bù |
| R10 | Idempotency replay trước quyền, fingerprint thiếu, race tạo tác động trùng | Quyền trước replay; scope actor/capability+branch+action; payload chuẩn đầy đủ cho TABLE/COUNTER/kho/tiền; key khác payload conflict; unique/claim atomic |
| R11 | Không có mapping vẫn bán; category/detail/menu khác policy; đồ uống làm topping | Một SellabilityPolicy dùng chung. Mapping bắt buộc active/available; category/product active; topping đúng loại/quan hệ, không duplicate sai; server quyết giá/công thức |
| R12 | ONLINE/giao nhận tồn tại ngoài phạm vi | Hoàn thành Prompt 1; hai nguồn tạo mới TABLE/COUNTER, giỏ/voucher theo context tại quán; bảo toàn lịch sử khi thu gọn schema |
| R13 | Seed tạo tồn trực tiếp, thiếu nguồn/công thức/snapshot, invoice/voucher sai | Viết lại seed theo mục 4–6 bên dưới, không chỉ sửa con số hoặc thêm nguyên liệu tượng trưng |
| R14 | Reset/seed phá DB hoặc xóa dở vì FK/batch | Tách migration nâng cấp và rebuild demo có guard; transaction/error handling phù hợp; rerun an toàn hoặc từ chối DB không sạch trước khi mutate |
| R15 | Voucher quota theo khách race, preview khác checkout, zero chưa thống nhất | Pricing dùng chung, quota atomic, snapshot đầy đủ; branch từ bàn/staff; hóa đơn 0 có tất toán hợp lệ, review/ranking/đóng phiên theo cùng rule |
| R16 | OTP attempts/consume read-modify-write có race | Atomic attempts và one-time consume; giữ failed attempt khi outer rollback; tránh khóa ngoài rồi REQUIRES_NEW tự chờ row |
| R17 | CSRF/config/HTTP-WS policy và revoke chưa đồng nhất | Kiểm CSRF thật, secret bắt buộc prod, origins theo profile, assignment/verified/active đồng nhất; quyền bị thu hồi không tiếp tục nhận event đặc quyền theo policy |
| R18 | Review upload không storage vẫn trả URL không file | Có storage thật hoặc fail rõ; upload→GET bytes→restart→GET còn tồn tại; owner/dòng đơn, MIME/dung lượng/quota, dọn orphan; video chỉ giữ nếu đã cam kết và có luồng thật |
| R20 | Newest theo ID, search/top/page chưa nối đầy đủ, query còn param thừa | Newest createdAt+tie-break; search/filter/page thực; dọn named param/caller và chạy context/repository test; ranking theo product đúng định nghĩa, branch đúng |
| R21 | Test chủ yếu mock, list all/N+1, vận hành chưa chứng minh | Thêm SQLServer/HTTP/context tests cho tiền-kho-quyền, pagination/query đo được, snapshot cost; log/audit/backup phù hợp. Chưa chạy không coi verified |

R19 không thuộc nhiệm vụ. Không đánh số lại R20/R21 để tránh nhầm với báo cáo; khi tổng hợp ghi rõ 20 nhóm đang xử lý, có một nhóm là thu gọn phạm vi.

### 4. Database — bất biến bắt buộc và ràng buộc còn phải sửa

Kiểm cả entity, schema thật, migration, repository query, service và seed. Một annotation không chứng minh DB hiện tại có constraint; một FK tồn tại không chứng minh nghiệp vụ đúng.

#### 4.1. Branch, bàn, phiên, quyền

- Order phải có branch; TABLE branch = session.table.branch. Counter branch = cơ sở nhân viên đang được phép làm việc. Chi nhánh không do client quyết.
- Tên bàn có thể trùng giữa hai cơ sở: uniqueness theo `(MaCN,TenBan)`, không tìm global bằng tên để cấp token. QR unique/khó đoán, label đúng tên bàn/chi nhánh.
- Một OPEN session/table, dùng transaction/lock và filtered unique phù hợp SQL Server nếu schema hỗ trợ. `Ban.MaPhienDangMo` phải trỏ đúng phiên OPEN của chính bàn; tạo/đóng phiên cập nhật nhất quán.
- Phiên mở qua nghiệp vụ phải có giỏ tương ứng và capability/token đúng lifecycle; quyền phiên bị thu hồi khi đóng. Session token/hash không để lộ trong API danh sách công khai/log.
- Staff thiếu assignment active bị chặn. Admin bypass phải dựa role xác thực rõ, không dựa branch null. Đừng tạo `Account.branch @Transient` thay thế bảng phân công.

#### 4.2. Công thức, nguyên liệu, đơn vị và tồn

- Công thức món/topping có FK tới đúng nguyên liệu; định lượng >0; mỗi nguyên liệu xuất hiện đúng rule, không trùng gây tiêu hao hai lần. Mọi món bật bán trong demo đều có công thức hợp lệ, không trông chờ tên hardcode.
- PK/unique tồn theo `(MaCN,MaNL,ViTri)`; FK đầy đủ; quantity không âm; unit cost hợp lệ. Không đặt default CN01 khi thiếu MaCN.
- Chuẩn hóa đơn vị lưu trữ và quy đổi; kg↔g, l↔ml phải có rule và rounding. Không nhân số tồn tùy đơn vị để “trông đủ hàng”. Không cho dùng nguyên liệu khác đơn vị mà không đổi.
- Món A dùng B,C thì B,C phải có danh mục, định mức và stock đúng location/branch. B,C raw phải truy đến chứng từ nhập hợp lệ; B,C SOCHE phải truy đến mẻ, input và nguồn raw nhập. Có số dư không đủ.
- Công thức sơ chế output/input đúng loại, lượng >0, không chu trình tự sinh/cycle; yield/cost giải thích được. Không cần có mẻ riêng cho từng đơn.
- Không lấy stock chi nhánh khác/location khác để thay stock thiếu; thiếu phải báo/rollback hoặc tạo zero có kiểm soát theo chính sách, không tạo dương vô nguồn.

#### 4.3. Chứng từ, ledger, cost và kết ca

- Nhập raw tăng đúng kho và có phiếu+chi tiết+ledger tương ứng; chứng từ đã ghi sổ không mutate ngược làm mất lịch sử.
- Điều chuyển đủ hai vế cùng nguyên liệu/đơn vị, nguồn/đích đúng, lượng xuất = lượng nhận theo policy demo không hao hụt. Cấp KHO→BEP cũng phải có nguồn nếu policy tiêu hao BEP.
- Sơ chế giảm input, tăng output, lưu branch, mẻ, yield và cost; stock/ledger/chứng từ cùng transaction. Không chỉ tạo row MeSoChe rồi gán tồn thành phẩm bằng tay.
- Ledger: `TonSau = TonTruoc + LuongBienDong`; before/after không âm theo policy; cùng branch/material/location chuỗi liên tục. Khi nhiều movement cùng thời gian dùng thứ tự ID/sequence ổn định.
- Unique nguồn ghi sổ phù hợp branch/material/location/type/source/line để không lặp. Nếu một nguồn có nhiều dòng cùng nguyên liệu, tổng hợp hoặc phân biệt sourceLine đúng; đừng đặt unique khiến adjustment và waste hợp lệ đụng nhau.
- Stock cuối = opening có nguồn + tổng delta = after của movement cuối. Nếu có opening balance phải là chứng từ/nguồn tường minh, không số dư giấu trong migration.
- Kết ca unique `(MaKetCa,MaNL)`; actual/retained/discarded không âm; `actual = retained + discarded`. Adjustment = actual−book, waste = −discarded; Stock cuối = retained. Reason bắt buộc cho chênh lệch theo rule. Không default trường đếm thiếu thành 0 rồi xóa sạch tồn.
- Snapshot giá vốn/nguồn dùng để giải thích tiêu hao và lợi nhuận theo thời điểm; không lấy giá nhập tương lai làm thay đổi lịch sử. Không dùng trung bình toàn chuỗi nếu rule đang là từng cơ sở.

#### 4.4. Snapshot đơn, tiền, voucher và hoàn

- Dòng đơn quantity >0; snapshot tên/size/giá/định mức/topping đủ. Đơn hoàn tất có tiêu hao đúng tổng snapshot, không recipe hiện tại sau khi đã sửa công thức.
- Tạo đơn không trừ kho; hoàn thành trừ một lần. Thiếu một input rollback cả đơn và ledger. Hủy trước tiêu hao không xuất kho; hủy sau pha không tự hoàn raw.
- Xác định tường minh trường giảm cửa hàng và giảm voucher. Theo policy cộng hai khoản: `PhaiTra = max(0,TongTien - TienGiam - TienGiamVoucher)`. Nếu rule cap khác thì dùng một pricing function có test; không lưu cùng 20k vào cả hai trường rồi chỉ trừ một lần.
- Payment một invoice theo rule hiện hành, số tiền đúng payable; refund thực chi không vượt đã thu; một nghĩa vụ không được chi hai lần. Hủy giữ payment và tạo obligation hợp lệ, không xóa giao dịch cũ.
- Voucher thỏa thời hạn/branch/minimum/quota total/quota customer trước apply; counters không âm, không vượt; `%` trong khoảng hợp lệ, fixed/min/cap không âm. History/status/snapshot khớp invoice; release khi hủy theo rule, retry không tăng/giảm lặp.
- Hóa đơn 0 có phương thức tất toán rõ, review/ranking/đóng phiên dùng cùng định nghĩa. Không bịa giao dịch thu tiền dương.
- Kiểm unique nullable idempotency phù hợp SQL Server: bắt buộc key hoặc filtered unique `WHERE key IS NOT NULL` theo policy. Không thả unique để tránh exception.

#### 4.5. Migration và dữ liệu cũ

- Kiểm V1 tạo stock CN02=50 và V4 cấp kho tổng=1000 không nguồn. Với demo baseline mới: bỏ số dư vô nguồn, khởi tạo zero rồi ghi sổ nguồn hợp lệ. Với DB đã tồn tại: reconciliation/ngoại lệ rõ, không bịa lịch sử nhập hàng quá khứ để hợp thức hóa.
- V5 có receiver/delivery ngoài phạm vi: xử lý theo Prompt 1; giữ thay đổi còn cần như createdAt/index/FK, không xóa toàn V5 vì nó chứa cả việc khác.
- Trước thêm CHECK/unique/FK mới, chạy precheck tìm vi phạm; backfill dựa dữ liệu có căn cứ, không đoán MaCN. Thêm constraint trên dữ liệu sạch rồi post-check và test rollback.
- Không sửa migration đã áp dụng trên môi trường dùng chung; thêm version tiếp theo và tài liệu nâng cấp. Không tạo file tên V6 trùng nếu V6 đã tồn tại.
- Tách đường dựng demo sạch và nâng cấp dữ liệu hiện hữu; script demo phải có allowlist DB demo, fail-fast và transaction phù hợp. Nếu rerun không hỗ trợ, từ chối trước khi xóa/ghi; không lỗi giữa chừng sau khi đã xóa hóa đơn/tồn.

#### 4.6. Ma trận sửa schema phải lập từ database thật

Trước khi sửa, lập bảng cho từng invariant: bảng/cột hiện tại → entity mapping → constraint/index hiện có → dữ liệu vi phạm → cách sửa service → migration → query hậu kiểm → test. Phân biệt CHECK cùng dòng với điều kiện nhiều bảng cần transaction/service hoặc thiết kế FK tổng hợp; không viết CHECK chứa query liên bảng không được hỗ trợ rồi coi là đã xong.

Các điểm phải kiểm rõ: NOT NULL MaCN ở stock/movement/mẻ/kết ca/chứng từ; khóa/unique phiên OPEN; FK và trạng thái activeSession đúng bàn; unique dòng kết ca; phương trình lượng kết ca; phương trình invoice; unique nguồn ledger; key nullable; FK usage voucher/invoice; index branch/status/time và query preview/list không mất branch. Soát CHECK có NULL lọt điều kiện, precision/scale giữa Java và SQL, default constraint cần gỡ trước drop cột, view/procedure/index phụ thuộc và migration transaction theo batch GO.

Nếu dữ liệu cũ vi phạm, xuất báo cáo ngoại lệ có ID, số lượng và cách xử lý. Chỉ backfill điều suy ra chắc chắn từ chứng từ/quan hệ; không đoán branch, không phát sinh phiếu nhập giả quá khứ, không lấy giá hiện tại gán cost lịch sử. Tách dữ liệu test cần dựng mới khỏi dữ liệu cần bảo toàn. Test constraint âm tính bằng transaction rollback riêng, không để fixture sai nằm trong demo chuẩn.

Seed phải sử dụng chính logic nghiệp vụ khi thực tế khả thi; nếu INSERT trực tiếp thì tạo đủ mọi side effect tương đương (snapshot, cart, token record khi cần, usage/counter, cost, ledger, audit). Không gọi private/internal methods thiếu policy để tạo fixture khác hành vi ứng dụng. Các bảng xóa/rerun phải xét đầy đủ FK graph, kể cả access token, payment notice, giỏ và lịch sử audit. Chạy lại seed không được reset tồn nhưng giữ ledger cũ.


### 5. Chuỗi dữ liệu demo bắt buộc tuân thủ

**Đây là thứ tự nghiệp vụ, không chỉ thứ tự INSERT để thỏa FK.** Hãy viết seed thực thi trên schema hiện tại và file giải thích kết quả mong đợi, không chỉ tạo một tài liệu mô tả.

1. **Danh mục nền:** hai chi nhánh CN01/CN02, một KHOTONG tách vai trò; bảng/quầy, QR/label, account và assignment đúng vai trò. Hai cơ sở có cùng tên “Bàn 01” để kiểm scope; QR khác nhau.
2. **Nguyên liệu/đơn vị:** khai báo toàn bộ raw/SOCHE và quy đổi chuẩn; chưa có tồn dương khi chưa có nguồn. Mọi thành phần phụ và bao bì trong công thức đều cần khai báo, không bỏ qua ly/ống hút nếu hệ thống tính tiêu hao.
3. **Danh mục/món/topping/công thức:** product active, category active, BranchProduct tường minh; công thức đủ cho mọi size bật bán, topping đúng loại. Có một món bị tạm hết/một món chưa phân bổ để kiểm không bán.
4. **Nhập từ nhà cung cấp vào kho tổng:** phiếu nhập ghi sổ, chi tiết đủ mọi raw dùng trong demo, quantity/unit price/người lập/time/key đúng; tăng stock và ledger từ 0. Nếu có nước/đá/utility thì mô hình và rule nguồn/cost phải tường minh, không âm thầm bỏ khỏi công thức.
5. **Cấp cho cơ sở:** chứng từ chuyển KHOTONG→KHO CN01/CN02, đủ vế xuất–nhập, nguồn đủ, không trộn branch. Nếu tiêu hao tại BEP, cấp KHO→BEP có chứng từ theo policy đã chốt.
6. **Sơ chế:** có input raw thực từ nguồn trên; mẻ tại đúng cơ sở, trừ input, cộng output, lưu yield/cost và ledger cả hai. Bán thành phẩm tại CN02 không được tự xuất hiện từ mẻ chỉ của CN01 nếu không có điều chuyển chứng minh.
7. **QR/phiên/giỏ:** giữ một bàn chưa mở, một phiên đang hoạt động có giỏ, một phiên đã đóng; mở phiên theo luồng nghiệp vụ và tạo mọi record phụ cần thiết. Quét/xem không tạo phiên rỗng ngoài rule.
8. **Đơn TABLE/COUNTER:** lấy branch từ bàn/staff, không có ONLINE; snapshot món/size/giá/recipe/topping đúng. Tạo các trạng thái xác nhận/pha chế/hoàn tất/hủy theo state machine, không insert tổ hợp bất khả thi.
9. **Invoice/voucher/payment:** dùng mã đủ điều kiện, tính đúng store discount/voucher/payable; có chưa thu/đã thu/zero theo policy. Ngày dùng voucher nằm trong hiệu lực, subtotal đạt min, usage/counter khớp.
10. **Hoàn thành và tiêu hao:** đơn hoàn tất trừ đủ từng snapshot; đơn chưa hoàn tất không có consumption; mọi movement bám nguồn/order/line, cập nhật stock thật chứ không chỉ INSERT lịch sử.
11. **Hủy/hoàn:** có hủy chưa thu, hủy đã thu chờ duyệt/đã duyệt/đã chi theo rule; giữ lịch sử payment, không hoàn nguyên liệu đã dùng; tiền/cashflow/audit đúng.
12. **Kiểm kê/kết ca:** tạo chênh lệch giải thích được, adjustment và waste riêng, stock cuối bằng retained. Không dùng kết ca để che tồn seed sai không có nguồn.
13. **Yêu thích/review/media:** customer đúng dòng đã mua, đủ trạng thái/tất toán, text >=50 ký tự sau trim, sao hợp lệ; file demo tồn tại thật nếu hiển thị media. Không tạo review của người chưa mua.
14. **Assertion và tài liệu demo:** xuất tồn cuối từng branch/material/location, tổng nhập/chuyển/sơ chế/tiêu hao/hao hụt, doanh thu/thu/hoàn, ranking và counts. Số mong đợi phải tính từ transaction fixture, không dựa số nhập tay trong PRINT.

#### Độ phủ dữ liệu mục tiêu

- Hai chi nhánh có nhân viên và đơn thật; tài khoản Admin, Manager/Cashier/Kitchen cho từng cơ sở, ít nhất vài Customer và Guest không đăng nhập.
- Tối thiểu hơn 10 món phân biệt; nên có >=24 món bán được để kiểm top20/phân trang và vài topping. Không khai “70 món” khi chỉ có 14 biến thể; không coi mỗi size là món độc lập nếu báo cáo/rubric tính theo tên món.
- Đủ nguyên liệu, phiếu nhập, chuyển và mẻ tương ứng. Không ấn định số nguyên liệu nhiều cho đẹp nếu không dùng; không làm thiếu công thức cho size còn lại.
- Có đơn TABLE/COUNTER ở cả hai cơ sở và đủ trạng thái. Có voucher branch, hết hạn, hết lượt, chưa đạt min và mã hợp lệ; ca bị từ chối phải được kiểm bằng request chứ không chèn dữ liệu sai làm ví dụ “hợp lệ”.
- Dữ liệu âm tính dành riêng test rollback hoặc DB fixture có chủ đích, tách seed demo hợp lệ. Seed demo chuẩn không chứa invoice sai/voucher áp trái điều kiện.

#### Ví dụ số học nhỏ để tự kiểm nguồn tồn

Ví dụ minh họa cho một nguyên liệu C, không thay công thức món thật: nhập tổng 1.000 g, chuyển 400 g tới CN01 → tổng còn 600 g, CN01 có 400 g. Đơn hoàn tất dùng 50 g → CN01 còn 350 g; đơn đang pha chưa tiêu hao thì vẫn 350 g. Kết ca book 350 g, actual 340 g, bỏ 40 g, giữ 300 g → adjustment −10 g, waste −40 g, Stock cuối 300 g. Mỗi bước phải có nguồn/ledger và hai vế chuyển. Không được lưu 310 g bằng book−discarded.

Với B là bán thành phẩm, nguồn phải đi từ nhập raw→chuyển→mẻ tạo B→tiêu hao B; chỉ tạo stock B rồi thêm một mẻ tượng trưng không đủ. Món A dùng B và C thì cả hai đường nguồn đều phải truy được.

### 5.1. Manifest và kịch bản demo có kết quả định lượng

Bàn giao một file `.md` hoàn chỉnh về **dữ liệu mẫu kiểm thử và demo toàn hệ thống** trong tài liệu nguồn chuẩn của repo. File phải khớp seed đã chạy, không chỉ dẫn sang một bản thiết kế cũ chưa triển khai. Nội dung bắt buộc:

- Bảng tài khoản tổng hợp/role/assignment, branch/table/QR label và mục đích sử dụng; tài khoản Guest không cần được bịa thành Account. Thông tin đăng nhập chỉ dùng demo, không chép credential thật.
- Manifest món/size/topping/giá/category/mapping bán từng branch, công thức định lượng và version; nguyên liệu/đơn vị/nguồn nhập/điều chuyển/mẻ liên quan. Với mỗi món minh họa, truy được đủ B và C tới chứng từ nguồn, không chỉ một thành phần đại diện.
- Bảng chứng từ và các ID/key ổn định hoặc mã tra cứu: nhập, chuyển, sơ chế, đơn, invoice/payment, hủy/refund, settlement. Không dựa số identity cố định có thể khác sau rerun.
- Ma trận trạng thái thực: đơn chờ xác nhận, chờ pha, đang pha, hoàn tất chưa thu nếu policy cho phép, hoàn tất đã thu, hủy chưa thu, hủy đã thu có hoàn đang chờ/đã duyệt/đã chi; phiên chưa mở/đang mở/đã đóng. Mỗi trạng thái có ít nhất ca đại diện cần demo, không tạo mọi tổ hợp giả định nếu state machine không cho phép.
- Bảng số dư đầu, nhập, chuyển vào/ra, input/output sơ chế, consumption, adjustment/waste và số dư cuối theo branch/material/location. Tách tổng raw và SOCHE; không cộng các đơn vị khác nhau thành một tổng vô nghĩa.
- Bảng subtotal/storeDiscount/voucher/payable/paid/refunded/outstanding theo đơn; tổng thực thu, thực hoàn, doanh thu và giá vốn theo định nghĩa đã ghi rõ. Số liệu expected lấy từ fixture manifest, actual lấy SELECT độc lập; không lấy actual gán lại thành expected để tạo PASS.
- Dữ liệu mong đợi của bốn danh sách/ranking, tie-break và trang đầu/trang sau; tập đủ hơn 20 khi kiểm phân trang. Ghi rõ đếm theo product hay tên món/size.
- Kịch bản theo từng vai trò: điều kiện trước → thao tác UI/API → trạng thái/stock/tiền/event trước và sau → SQL hậu kiểm. Có ít nhất một hành trình TABLE và COUNTER hoàn chỉnh ở hai cơ sở, refund, sơ chế, kết ca và review.
- Danh sách negative tests: sai branch/token, stale cart, thiếu recipe/ingredient, thiếu stock, voucher sai min/quota, duplicate key, repeat refund. Dữ liệu negative chạy trong test/rollback; cuối demo vẫn 0 vi phạm.

Lưu thời điểm gốc của fixture và thứ tự phát sinh thật: nhập trước chuyển, chuyển trước sơ chế, sơ chế trước tiêu hao, paid trước thực hoàn; không gán tất cả cùng timestamp rồi suy thứ tự từ thời gian. QR/token phiên dùng để thử trong runtime không đưa raw token thật vào tài liệu hoặc repository.

### 5.2. Lộ trình triển khai và phân công truy vết được

Lập task theo gói Guest/Customer UI, Cashier–Payment–Refund, Kitchen–Inventory, Manager/Admin–Security và Database/Integration QA. Mỗi task có người phụ trách nếu danh sách thành viên có sẵn, dependency, Rxx/Fxx liên quan, file thay đổi, acceptance và evidence. Nếu chưa có tên thì dùng vai trò phụ trách, không bịa tên/commit.

Đối chiếu F01–F26 của audit đầu với Rxx hiện tại để tránh bỏ các điểm đã sửa một phần: route Admin orders, lazy DTO, mapping MaCN, OTP rollback, paid-cancel obligation, cookie và ranking phải có regression check. F13 chuyển thành thu gọn ONLINE; F26/R19 về vòng đời đóng cơ sở là ngoài phạm vi, phần assignment vẫn bắt buộc. Không tái mở chức năng bị loại từ các prompt cũ.

Nếu repo có quy trình branch/PR thì theo quy trình hiện hành, chuẩn bị diff/commit đề xuất cho từng gói. Không giả lịch sử đóng góp, tự push/merge hoặc thay tác giả. Báo cáo rubric ghi rõ phần áp dụng tại quán và phần không áp dụng; không tự chấm điểm thay thầy.

### 6. Script kiểm chứng database cần bàn giao

Tạo script SQL thực thi và automated integration tests trên SQL Server test riêng. Tên gợi ý, có thể theo convention repo:

- `database/validation/verify_demo_integrity.sql`;
- `database/validation/verify_inventory_reconciliation.sql`;
- `database/validation/verify_financial_reconciliation.sql`;
- `database/sample_data_reference.md` hoặc tài liệu demo nguồn chuẩn đang dùng.

Script không chỉ PRINT “PASS”; phải xuất tên kiểm, expected/actual/violation count, chi tiết ID sai và `THROW`/exit failure nếu có vi phạm. Cố định transaction/time base hoặc dùng dữ liệu hiệu lực sao cho chạy lại vẫn kiểm được; không giấu lỗi vì thời gian hiện tại khác ngày seed.

| Mã kiểm | Điều phải phát hiện | Kết quả chuẩn |
|---|---|---|
| DB01 | TABLE/COUNTER branch sai, source tạo mới ngoài phạm vi; legacy tách rõ | 0 vi phạm |
| DB02 | Bàn–activeSession–session branch/table/status không khớp; nhiều OPEN/table | 0 |
| DB03 | Phiên cần giỏ mà thiếu GioPhien; token/record phụ mồ côi | 0 |
| DB04 | Nhân viên được cấp quyền nhưng thiếu assignment; dữ liệu ngoài branch | 0 theo policy vai trò |
| DB05 | Món/topping bật bán thiếu category/map/recipe/định lượng/ingredient hợp lệ | 0 |
| DB06 | Tồn raw dương nhưng không có nhập/chuỗi chuyển nguồn hoặc opening hợp lệ | 0 |
| DB07 | Tồn SOCHE không có mẻ/nguồn chuyển; input mẻ không truy được raw | 0 |
| DB08 | Chuyển thiếu vế, sai lượng/đơn vị/branch; source không đủ | 0 |
| DB09 | Mẻ có sản lượng nhưng không trừ input/cộng output đúng ledger/cost | 0 |
| DB10 | Movement sai before+delta=after, âm trái policy, source thiếu/trùng | 0 |
| DB11 | Chuỗi ledger đứt hoặc Stock khác after cuối/opening+sum(delta) | 0 |
| DB12 | Đơn hoàn tất thiếu recipe snapshot hoặc thiếu/sai lượng consumption | 0 |
| DB13 | Đơn chưa tiêu hao đã có consumption, hoặc retry tiêu hao hai lần | 0 |
| DB14 | Kết ca actual != retained+discarded; stock cuối không bằng retained tại thời điểm kết ca | 0 |
| DB15 | Invoice sai subtotal/discount/voucher/payable hoặc snapshot không khớp | 0 |
| DB16 | Payment sai amount, lặp; thực hoàn vượt paid/chi lặp/status thiếu chứng cứ | 0 |
| DB17 | Voucher trái min/time/branch/quota; usage/counter không khớp rule release | 0 |
| DB18 | Review không sở hữu dòng mua/không đủ điều kiện, text/sao/media sai | 0 |
| DB19 | Counts thực khác fixture manifest; ranking/doanh thu/thu-hoàn khác expected | Khớp dữ liệu đã tính |
| DB20 | Script rebuild/rerun/migration có thể chạy nhầm DB hoặc để dữ liệu nửa chừng | Bị guard trước ghi hoặc rollback an toàn |

DB06/DB07 cần truy nguồn theo chứng từ/line, không chỉ `EXISTS` một phiếu nhập bất kỳ cho cùng MaNL rồi coi mọi số tồn đều hợp lệ. DB12 tính đủ main+topping+quantity và quy đổi; DB14 so tại mốc kết ca, không lấy Stock hiện tại sau các giao dịch mới rồi so sai. DB17 dựa trạng thái usage và chính sách release, không ép counter đã dùng bằng count mọi history bất kể trạng thái.

### 7. Đồng nhất UX/UI toàn hệ thống — yêu cầu bắt buộc

1. Rà layout/fragments/header/footer, CSS variables, typography, màu, spacing, radius, shadow, Bootstrap version, icons, button/modal/table/badge/toast của các màn hình hiện có. Ghi design conventions ngắn và tái dùng component; không thiết kế lại thương hiệu hoặc thay template.
2. Trang khách, cart, chi tiết, lịch sử đơn, POS, kitchen, admin/manager và kho phải cùng phong cách. Có thể khác mật độ theo vai trò, nhưng không khác font/bảng màu/nút/trạng thái vô cớ. Không dùng emoji thay icon hệ thống, không rải inline CSS hoặc `!important` để vá cục bộ.
3. TABLE hiển thị nhãn chi nhánh/bàn nhất quán ở menu, giỏ và xác nhận đơn; COUNTER hiển thị đúng cơ sở nhân viên. QR in/hiển thị có label rõ, dễ đọc, không đưa session token hiện hành vào danh sách public.
4. Chưa có QR/ngữ cảnh không đủ thì trạng thái hướng dẫn đúng, không hiện form giao nhận, branch chọn tùy ý hoặc tự tạo đơn quầy. Không làm khách phải đăng nhập chỉ để xem menu/đặt Guest tại bàn nếu hệ thống đã hỗ trợ Guest.
5. Loading/empty/error/success/validation/403/409 và retry phải đồng nhất. Lỗi giá/availability/voucher/giỏ version cần hướng dẫn cập nhật mà không mất món. Disable nút khi đang submit và vẫn dùng idempotency backend; không chỉ dựa disable.
6. Tiền VNĐ, số lượng và đơn vị, thời gian, status badge, tên vai trò, “sơ chế”/“pha chế”/“ghi thu”/“thực hoàn”/“đóng phiên” thống nhất. Hoàn tất pha chế không hiển thị “đã giao”.
7. POS/kitchen ưu tiên thao tác nhanh; kho/tiền hiển thị branch, chứng từ, trước/sau, tổng tiền và xác nhận rõ. Không bắt nhập dữ liệu server đã biết hoặc lộ thông tin kỹ thuật trong flow khách.
8. Kiểm viewport 360/390/768/1366px, bàn phím/focus/label/contrast, bảng dài, tên món dài, ảnh thiếu, danh sách rỗng và lỗi API. Tránh modal/tràn ngang chặn thao tác trên mobile.
9. Kiểm realtime nhiều cửa sổ đúng role/branch; UI không phải F5 mới thấy event hợp lệ. Khi mất kết nối hiển thị tình trạng và tải lại dữ liệu cần thiết theo convention, không gây thông báo thành công giả.
10. Bàn giao bằng ảnh trước/sau hoặc browser evidence trên các màn hình sửa, dữ liệu thực từ fixture; ghi test viewport và vấn đề còn lại. Không nhận “UI đồng nhất” chỉ vì đã thêm Bootstrap CDN.

### 8. Các kiểm thử nghiệm thu bắt buộc trước đánh dấu xong

- Payment positive với cashier persist thật cùng branch sau flush/clear; negative khác branch; không mock trường transient để pass.
- Preparation/settlement giả branch, thiếu assignment, stock mục tiêu thiếu nhưng nơi khác có; không có mutation trái scope.
- Kết ca ví dụ 5000/4800/300/4500 và ví dụ 350/340/40/300; assert Stock và toàn bộ ledger cuối, không chỉ delta.
- QR đúng mở menu có label; ID/tên bàn không đổi thành token; hai khách gửi đơn đầu song song chỉ có một OPEN session; không mở phiên chỉ vì GET menu.
- JWT+table token hợp lệ, Guest token, Customer ngoài phiên, đóng phiên rồi dùng token cũ; HTTP/cart/orders/WS cùng policy.
- Cashier CN01 không execute refund/assign-table CN02; cashier CN01 thao tác CN01 thành công; refund retry không thực chi lặp.
- Manager không assignment không có đặc quyền Admin, không tạo Admin, không đọc/sửa toàn chuỗi.
- Idempotency cùng key/payload, khác payload, khác caller/capability/branch, hai request đồng thời; không double stock/payment/refund/voucher.
- Món thiếu mapping/category tắt, topping giả/duplicate, công thức thiếu và thiếu một nguyên liệu phải bị từ chối hoặc rollback toàn bộ, không chỉ cảnh báo UI.
- Voucher lượt cuối song song, preview=checkout theo TABLE/COUNTER, zero total, usage release khi hủy theo rule.
- OTP wrong attempts và đúng consume song song; CSRF thiếu/sai/đúng trên mutation; policy login/cookie/origin được kiểm và ghi rõ giới hạn.
- Realtime cashier/kitchen CN01/CN02+Admin, payment/cancel/refund/import/prep/settlement; đủ đúng branch, không lọt branch, rollback không event.
- Upload ảnh có GET bytes thật và tồn tại sau restart, thiếu storage báo lỗi; ownership, MIME/size/quota đúng.
- Search/filter/top20/page/newest đúng branch và dữ liệu >20, không lặp/mất; query repository và DTO chạy qua Spring context/HTTP thật.
- Fresh baseline+seed+DB01–DB20 đạt; upgrade copy DB có dữ liệu cũ đạt; rerun seed an toàn hoặc chặn trước mutation; không dùng DB thật làm test.
- UI các vai trò đồng nhất tại các viewport, không còn flow ONLINE/giao nhận; endpoints ONLINE/alias cũ không tạo đơn; test không liên quan R19 về đóng cơ sở.

Test quan trọng cần cả ca được phép và ca bị chặn. Khởi động Spring context và tích hợp SQL Server cho tiền/kho/quyền; mock-only không đủ chứng minh mapping, transaction, lock, FK và security filter. Chạy checks thích hợp sau thay đổi; không thêm test chỉ lặp lại implementation mà không kiểm bất biến.

### 9. Kết quả bàn giao bắt buộc

- Source/migration/baseline/seed/validation SQL đã sửa, phù hợp TABLE/COUNTER và phong cách UI hiện có.
- Tracker theo từng Rxx với bằng chứng code, test command, kết quả và môi trường; R19 `OUT_OF_SCOPE`, không “đã sửa” giả. Lỗi đã không còn trong workspace mới thì ghi bằng chứng `ALREADY_FIXED`, không sửa lại để đủ danh sách.
- Danh sách constraint/index/FK thay đổi, precheck/postcheck và chiến lược dữ liệu legacy; không chỉ ghi “database đã chuẩn”.
- Tài liệu demo: tài khoản/role/branch tổng hợp, bàn/QR, thứ tự thao tác, dữ liệu đầu/cuối, tồn/tiền/ranking expected, các request bị chặn; không chứa secret thật.
- Tài liệu UX/UI component/convention và browser evidence của màn hình đã sửa.
- Build/test logs từ phiên chạy hiện tại; phân biệt unit/context/DB/HTTP/browser. Thiếu Java21/SQLServer/SMTP/storage thì nêu chính xác hạng mục bị chặn và cách chạy lại, không dùng test report cũ để tuyên bố hoàn tất.
- Tóm tắt thay đổi, rủi ro còn lại và lệnh demo/test có thể lặp lại. Không tự deploy/push/reset DB thật nếu nhiệm vụ chỉ yêu cầu hoàn thiện source.

Hãy thực hiện tuần tự đến khi hoàn tất phần đã được giao. Ưu tiên tính đúng nghiệp vụ, ràng buộc và dữ liệu chứng minh được; không tăng số chức năng hoặc số test để thay cho việc đóng lỗi.


### 10. Definition of Done và mẫu phản hồi cuối của Agent

Chỉ coi hoàn tất khi: hai luồng TABLE/COUNTER chạy được, không có đường tạo ONLINE; các P0 về quyền/tiền/kho có cả positive và negative test; schema/seed/DB01–DB20 khớp trên SQL Server; dữ liệu demo có expected độc lập; UI cùng phong cách và thử browser; SSOT/API/test không mâu thuẫn. R19 không được kéo lại vào điều kiện này.

Nếu bị chặn môi trường, tiếp tục hoàn tất các phần độc lập có thể làm, lưu lệnh tái hiện, test/SQL và chỗ còn thiếu; không gắn VERIFIED cho phần chưa chạy. Nếu đã pass thì không lặp test vô hạn hoặc mở rộng tính năng ngoài scope.

Phản hồi cuối phải có: (1) kết quả từng Rxx/Fxx và bằng chứng, (2) các luồng đã chạy thật, (3) migration/constraint và dữ liệu ngoại lệ, (4) bảng expected/actual tồn–tiền–ranking, (5) test command + kết quả hiện tại, (6) UI/browser/viewport đã thử, (7) file dữ liệu mẫu `.md` và cách demo, (8) rủi ro còn mở/blocked. Không chấp nhận câu “đã sửa tất cả” chỉ vì build pass hoặc tracker đã đánh dấu xong.
