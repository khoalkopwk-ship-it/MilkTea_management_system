# Prompt Gemini sửa MilkTea v5 và tích hợp VNPAY MoMo ZaloPay

## Cách dùng

Đưa file này cho Agent trong Antigravity tại workspace dự án, kèm `05_Danh_gia_MilkTea_v5_va_cap_nhat_bao_cao.md` nếu có. Yêu cầu: **“Triển khai sửa sáu mục và tích hợp ba cổng VNPAY/MoMo/ZaloPay thay VietQR thủ công theo prompt, chạy lại ứng dụng và kiểm thử HTTP/browser/realtime; cập nhật báo cáo theo bằng chứng thực chạy. Giữ nguyên cấu trúc database, seed và dữ liệu cũ.”**

Đây là yêu cầu sửa source và kiểm chứng, không phải chỉ lập kế hoạch. Phát hiện được nêu từ ZIP5; workspace có thể đã mới hơn nên xác minh lại trước sửa. Không ghi đè `GEMINI.md` bằng toàn bộ prompt.

## 1. Phạm vi và ranh giới

Bạn là Agent phụ trách sửa sáu mục **4.1, 4.2, 4.4, 4.6, 4.7, 4.8** của bản đánh giá05 và tích hợp **VNPAY, MoMo, ZaloPay** theo mục8. Thanh toán điện tử tại quán không mở lại nguồn đơn ONLINE. Tiếp tục đến kết quả có thể xem xét và tái chạy; không dừng ở checklist hoặc nói đã xong vì unit tests pass.

- Hệ thống chỉ phục vụ tại quán: TABLE qua QR gắn bàn/chi nhánh và COUNTER do staff được phân công tạo. Không thêm ONLINE/giao nhận/Shipper/AddressBook hoặc R19 đóng cơ sở. Đóng phiên bàn vẫn là nghiệp vụ cần kiểm.
- Giữ Java21, Spring Boot MVC, Thymeleaf, Bootstrap, JPA, SQL Server và mô hình ba tầng. Giữ giỏ, chu trình thu ngân→bếp, thời điểm tiêu hao và quyền hợp lệ hiện có; luồng VietQR thủ công được thay cho giao dịch mới theo mục8, không xóa lịch sử.
- **Không thay database:** không DDL, migration, sửa constraint/index/trigger/table/column, reset/reseed, đổi seed hoặc sửa dữ liệu cũ bằng SQL. Không sửa entity để ngầm yêu cầu tạo bảng/cột mới. Không chạy auto-DDL/auto-migrate khi test.
- Được sửa code service/controller/repository query/DTO/security/frontend, test, tài liệu và cấu hình test riêng. Được bổ sung/chỉnh script **đối soát chỉ đọc** để trình bày đúng evidence; không biến nó thành script chữa dữ liệu.
- Được tạo giao dịch test qua API/UI nghiệp vụ hợp lệ trên môi trường test hiện có; không ghi trực tiếp dữ liệu để hợp thức hóa seed. Chỉ có DB thật thì không chạy test mutation/concurrency trên đó; ghi BLOCKED_ENVIRONMENT và tiếp tục phần độc lập.
- Kiểm `git status` và chỉ dẫn repo/SSOT trước sửa; không reset hoặc đè thay đổi có sẵn. Chỉ sửa tài liệu thuộc sáu mục và phần thanh toán mới được giao; không tự viết lại cả báo cáo Word hoặc thêm chức năng không liên quan.
- Thiếu Java21/SQLServer/browser thì tự kiểm công cụ/cấu hình hợp lệ có sẵn; nếu không thể chạy, nêu nguyên nhân/lệnh tái hiện và mức kiểm chứng. Không dùng mock/H2 hoặc báo cáo cũ để nhận đã chạy SQL Server/HTTP/E2E.

## 2. Mục 4.1 — Restore QR phải dựa token hợp lệ

### Vấn đề cần xác minh

`static/js/customer/customer.js:47–88` suy `hasActiveToken` từ việc chuỗi token tồn tại rồi restore QR cache và gọi public table-context. Token cũ có thể đã expire/revoke. Endpoint context có thể cấp token phiên đang mở từ QR hợp lệ, làm browser lượt trước tự gia nhập lượt mới mà không quét lại.

### Yêu cầu triển khai

1. Phân biệt ba trạng thái: chưa có ngữ cảnh; chủ động mở URL QR; khôi phục quyền phiên đang có. Có chuỗi trong localStorage/sessionStorage không chứng minh quyền hợp lệ.
2. Restore phải được server xác minh token, session/table/branch đúng, phiên OPEN, token chưa expire/revoke. Dùng endpoint xác minh/khôi phục thích hợp; không dùng QR cache gọi endpoint có tác dụng cấp quyền phiên mới.
3. Endpoint restore trả lại context trong đúng quyền đang tồn tại hoặc lỗi rõ; không tự issue capability của lượt khách mới khi token cũ thất bại. Không mở phiên/đơn khi GET menu.
4. Nếu token cũ invalid/expired/session closed: clear cache/token/context/giỏ tham chiếu phiên đã hết quyền theo policy, ngừng subscription phiên đó; hiển thị hướng dẫn quét lại. Không tự retry qua QR cache để lách lỗi.
5. Quét lại **chủ động** QR hợp lệ vẫn mở ngay menu đúng branch/bàn. Navigation/refresh trong phiên còn hợp lệ không làm mất giỏ hoặc bắt quét/login lại vô cớ. Đối với trước đơn đầu chưa có session, giữ đúng lifecycle capability QR mở đầu, không suy session quyền mới từ token cũ.
6. Login/logout và token phiên là hai chiều: JWT hợp lệ không thay quyền phiên; token bàn không cấp branch công tác cho staff. Token không xuất hiện trong log, URL chia sẻ ngoài thiết kế hoặc response public không cần thiết.
7. Revoke/đóng phiên phía server phải có hiệu lực cả HTTP và WS theo cơ chế hiện có; không chỉ xóa localStorage ở một browser rồi coi là đã thu hồi toàn bộ.
8. Ghi đúng giới hạn QR tĩnh: không chứng minh khách đang ở quán và không ngăn tuyệt đối người giữ QR chủ động quét lại. Không tự thêm geofence/OTP/nhân viên duyệt mỗi lần quét.

### Test bắt buộc

- Token đúng+phiên OPEN: refresh/restore giữ đúng context và giỏ.
- Chuỗi token giả, expired hoặc revoked nhưng cache còn QR: restore bị từ chối, không được cấp token phiên mới.
- Đóng phiên A rồi mở phiên B trên cùng bàn; browser A không quét lại: không đọc/ghi giỏ, đơn hoặc WS của B.
- Chủ động quét QR lại: theo policy được vào ngữ cảnh mới, không phục hồi giỏ/đơn cũ.
- Token bàn/branch khác, JWT customer khác và token staff được kiểm đúng; không làm mất luồng gọi món Guest hợp lệ.

## 3. Mục 4.2 — Replay kiểm quyền trên đơn cũ và đủ payload

### Vấn đề cần xác minh

OrderService kiểm quyền ngữ cảnh hiện tại trước replay nhưng đối chiếu tài nguyên cũ chủ yếu bằng tableId; fingerprint thiếu note/remainingCartItems. Quyền lượt mới tại cùng bàn không tự cho phép xem replay của lượt cũ. Key bị lộ/reuse phải không mở rộng quyền.

### Yêu cầu triển khai

1. Lập caller map mọi nhánh dedup/lookup key: TABLE/COUNTER, nhập/sơ chế/kết ca, thu tiền/notice/hoàn nếu có. Không chỉ sửa một if ở TABLE rồi giữ alias/fallback khác bypass.
2. Trước trả kết quả cũ, load resource và kiểm quyền **đối với resource cũ**: owner/capability, session của đơn, branch, action và lifecycle. Có quyền tại bàn hiện tại chưa đủ. Không trả DTO cũ hoặc tiết lộ invoice/order trước kiểm này.
3. Scope dedup theo actor hoặc capability owner/session, branch và action bằng cơ chế/schema hiện có. Không gộp tất cả Guest thành cùng định danh null; không tin actorId/session/branch client tự khai. Không cần thêm bảng/cột.
4. Fingerprint chuẩn hóa toàn bộ trường ảnh hưởng nghiệp vụ: table/expected session, main/size nếu có/toppings/quantity/item note/order note/voucher và remainingCartItems hoặc thao tác giỏ tương đương. Chốt quy tắc null/blank/order danh sách/duplicate; không hash `toString()` thiếu ổn định hoặc bỏ trường chỉ vì UI chưa dùng.
5. Phân biệt payload request ban đầu với state server sinh sau đó: retry của đơn đầu có expectedSessionId=null phải xử lý ổn định sau khi server đã mở phiên, không bị conflict giả chỉ vì sinh session mới. Đồng thời replay sau kết thúc phiên phải tuân thủ quyền tài nguyên lịch sử, không cho lượt khách mới truy cập.
6. Cùng key cùng canonical payload/caller hợp lệ trả kết quả cũ, không tạo thêm order/cart side effect/payment/movement/audit/voucher. Cùng key khác payload/action/caller sai scope phải conflict/forbidden phù hợp, không rơi xuống tạo mới hoặc trả success sai.
7. Bỏ hoặc bảo vệ fallback `findByIdempotencyKey` không kiểm fingerprint/scope. Không dựa key khó đoán thay authorization.
8. Concurrency: dùng transaction/lock/unique hiện có đúng semantics; xử lý duplicate claim có kiểm soát. Không thêm constraint/migration. Nếu schema thiếu cơ chế cần thiết mà code không bảo đảm được, ghi hạn chế cụ thể; không giả định đã an toàn.
9. Chuẩn hóa header **`Idempotency-Key`** giữa controller, API client, runner và docs. Nếu quyết định hỗ trợ alias `X-Idempotency-Key` để tương thích, quy định ưu tiên/xử lý hai header khác giá trị và test; không viết docs một kiểu, code một kiểu.

### Test bắt buộc

- Same key+same payload, request đầu/retry/2 request đồng thời: một tác động hợp lệ, ID/amount/stock không nhân đôi.
- Đổi từng trường note, item note, topping, quantity, voucher, remainingCartItems: conflict nếu khác nghĩa nghiệp vụ.
- Key của account/capability/branch/session khác: không trả thông tin đơn cũ.
- Đóng phiênA, mở phiênB cùng bàn, gửi lại keyA với quyềnB: không truy cập tài nguyênA.
- Retry hợp lệ do network timeout ngay sau đơn đầu vẫn hoạt động; không nới ownership để sửa lỗi retry.
- Rà các đường payment/refund/nhập/kết ca: quyền trước replay và dữ liệu response không rò ngoài scope.

## 4. Mục 4.4 — Phân trang backend 12 sản phẩm

### Vấn đề cần xác minh

`templates/customer/menu.html` đang tải tất cả `.product-item`, dùng `slice` và ẩn/hiện DOM. CustomerPageController vẫn nạp products/allProducts. Có nút page12 không đồng nghĩa server-side paging.

### Yêu cầu triển khai

1. Nối search/filter/page/size/sort vào repository/service query thật. Query áp search, category, min/max price, branch sellability và sort trước pagination; count query dùng cùng predicate. Không tải toàn bộ rồi cắt Java/JS hoặc ẩn card.
2. Page size mặc định12, quy định 0-based/1-based thống nhất, validate page/size/max hợp lý; không để client yêu cầu size không giới hạn. Response có items, totalItems, page, pageSize và totalPages hoặc hợp đồng tương đương rõ.
3. Chốt đơn vị phân trang đúng UI: nếu một card đại diện tên món với nhiều size, total đếm cùng đơn vị đó; không count biến thể rồi hiển thị theo tên gây thiếu/trùng. Lấy size/topping theo API detail/lazy thích hợp, không nhét toàn bộ allProducts vào HTML để lách yêu cầu.
4. Một sellability predicate dùng chung: product/category active, mapping branch tồn tại và active/available theo policy. Không hiển thị món branch không bán nhưng checkout mới từ chối. Branch context của QR do server xác minh, query branch không tự cấp quyền đặt món.
5. Tìm tên, danh mục và khoảng giá phối hợp được; sort có tie-break ổn định. Không mất filter/sort khi next/back; đổi filter reset page phù hợp; ngoài trang cuối có hành vi rõ.
6. Bốn nhóm mới nhất/bán chạy/đánh giá/yêu thích giữ yêu cầu top20: rank trên tập đủ điều kiện theo policy trước giới hạn20, sau đó page12+8 nếu đủ20. Không trả top20 mỗi trang thành top40; không rank chỉ trong page đã cắt. Metric scope rõ, best-selling theo branch, completed+tất toán; zero order theo rule đã chốt.
7. Giữ nguyên giao diện nút phân trang đã có, font/màu/icon/spacing/toast/modal theo toàn hệ thống; không redesign. Thêm loading/empty/error, debounce tìm kiếm phù hợp và ngăn response cũ ghi đè filter mới; thêm giỏ từ page2 không mất state.
8. Sửa controller SSR để chỉ nạp dữ liệu trang cần thiết hoặc shell + API page; không giữ render all rồi thêm endpoint phân trang chỉ để test. Không sửa schema/index; performance cần index mới thì báo riêng.

### Test bắt buộc

- Tập món phù hợp hiện có >12 và >20: page1/page2/page cuối đúng count/order, không trùng/mất; kiểm page12 bằng API/network và kích thước HTML/response, không chỉ số card nhìn thấy.
- Search+category+price phối hợp, bounds sai, empty, page ngoài biên, size lớn; không500.
- CN01/CN02 món bị tạm hết/chưa map/category off, detail/checkout thống nhất.
- Top20 đủ dữ liệu trả tổng20 và tối đa12 mỗi page; rename/tie/newest/best-selling theo rule đúng.
- Browser mobile/desktop next/back/filter, lỗi request/retry, đổi filter nhanh và add-to-cart từ các page.
- Thiếu dữ liệu >12/>20 thì dùng API nghiệp vụ hợp lệ tạo test object nếu được hỗ trợ ở môi trường test; không sửa seed/SQL. Không đủ điều kiện vẫn phải báo ca blocked thay vì nhận đã chứng minh phân trang.

## 5. Mục 4.6 — Đối soát chỉ đọc và sửa cách trình bày evidence

### Ranh giới

**Không sửa schema, sample_data.sql, migration, constraint, số dư hoặc lịch sử cũ.** Seed không có nguồn thì ghi ngoại lệ, không tạo nhập giả. Chỉ sửa code phát sinh mới nếu có lỗi thuộc nhiệm vụ, query kiểm chứng chỉ đọc và tài liệu mô tả evidence.

### Yêu cầu đối soát

1. Đọc tên bảng/cột/entity/migration và schema hiện có được phép SELECT. Sửa docs/queries đang dùng `dbo.TonKho` nếu source thực là TonNguyenLieu; tên constraint chỉ ghi “đã tồn tại” khi có metadata read-only xác minh, nếu chỉ thấy file thì ghi “khai báo trong source”. Không tự đổi DB cho khớp tài liệu.
2. Tách ba bất biến độc lập:
   - **Cùng dòng:** after = before + signed delta với precision/rounding theo kiểu dữ liệu.
   - **Liên tục chuỗi:** before của dòng kế tiếp = after dòng trước cho đúng branch/material/location và thứ tự ổn định.
   - **Số dư cuối:** Stock bằng after cuối và opening có nguồn + tổng delta tại cùng mốc kiểm.
3. Đối soát chuỗi có partition branch/material/location, order bằng thời điểm nghiệp vụ và tie-break/sequence ID phù hợp. Nếu dữ liệu backdate/ID không thể hiện đúng thứ tự, báo thiếu căn cứ, không sắp xếp tùy tiện để làm khớp.
4. Kiểm missing/duplicate/source invalid, snapshot thiếu bằng LEFT JOIN/NOT EXISTS, không để INNER JOIN làm biến mất bản ghi sai. Tính mọi loại movement theo signed delta thực, không chỉ NHAP/XUAT và bỏ sơ chế/điều chỉnh/hao hụt.
5. Raw cần chứng từ nhập/chuỗi chuyển đủ lượng; SOCHE cần mẻ/input raw/cost và đường nguồn. Một `EXISTS` phiếu cùng MaNL không chứng minh toàn bộ tồn có nguồn. Chuyển kho phải đủ hai vế đúng branch/unit/quantity; mẻ phải đủ inputs/outputs đúng recipe.
6. Nếu opening dương không chứng từ hoặc lịch sử đã bị xóa, ghi `UNRECONCILED`/`INSUFFICIENT_EVIDENCE` với ID/branch/material/location; không lấy TonTruoc dòng đầu làm opening “đã xác minh” hoặc tự bổ sung balancing movement.
7. Đọc stock và ledger tại trạng thái nhất quán trên test; kiểm không có mutation chen giữa hoặc dùng cơ chế đọc nhất quán có sẵn. Không ALTER isolation/database để test. Có thay đổi đồng thời làm snapshot không nhất quán thì retry hoặc ghi giới hạn.
8. SELECT hậu kiểm giao dịch test mới: trước/sau nhập/chuyển/mẻ/complete/kết ca; expected tính từ request hợp lệ/recipe snapshot và state trước, actual từ DB/API. Tách kết quả giao dịch mới khỏi đánh giá toàn bộ lịch sử cũ.
9. Báo cáo từng check: phạm vi/thời điểm, query, expected, actual, violation count, ID và status `PASS`, `FAIL`, `INSUFFICIENT_EVIDENCE`, `BLOCKED`. Có thể dùng THROW/exit nonzero báo assertion fail mà không sửa dữ liệu; tuyệt đối không UPDATE/DELETE/INSERT chữa sai trong validator.
10. Không kết luận nguồn/tồn toàn hệ thống đúng chỉ vì0 vi phạm trong một predicate nhỏ. Nếu chỉ same-row math đạt, viết đúng “đạt kiểm tra số học từng dòng”; chain/source/closing balance chưa kiểm phải tách riêng.

### Kết quả cần bàn giao

- Query/script **chỉ đọc** hoặc test assertions tương ứng cho từng nhóm bất biến.
- Bảng ngoại lệ dữ liệu cũ có vị trí tra cứu, không chứa secret; không có thay đổi database/seed.
- Cập nhật tracker/sample-data/evidence docs về tên bảng/constraint và mức độ kiểm chứng; không đổi dữ liệu hoặc nhận đã tái tạo lịch sử.
- Kiểm query trên dữ liệu hiện có và kiểm giao dịch mới qua API test. Không cố tạo dữ liệu bẩn bằng SQL để thử validator; test âm tính bằng fixture trong bộ nhớ cho thuật toán chỉ là bổ sung, không được gọi là SQL Server validation thực.

## 6. Mục 4.7 — Hoàn thiện realtime, media và tương tác UI

### Publisher và phân quyền

Lập event matrix từ toàn bộ caller: mutation → transaction → event name/payload → global Admin/branch/phiên topic → role subscribe → expected refresh UI. Rà đặc biệt MATERIAL_CHANGED, RECIPE_CHANGED, mọi ADJUST và nhánh threshold/cost propagation; không chỉ grep một chuỗi `topic/branches` rồi nhận đã đủ.

- Bổ sung kênh branch còn thiếu bằng đối tượng branch được server xác minh. Với dữ liệu dùng chung toàn chuỗi, xác định người nhận theo branch liên quan/policy, không phát mù một branch tùy ý hoặc mở global cho mọi staff.
- Chuyển kho có thể tác động cả source/destination; đúng recipient cho mỗi phía, không lộ số liệu cơ sở khác. Không nới ACL global stock để sửa thiếu notification.
- Phát sau commit; rollback không success event. Event có ID/version hoặc cơ chế nhận diện phù hợp để client không toast/refresh trùng khi Admin nhận global+branch.
- CONNECT/SUBSCRIBE và phiên đang kết nối phải tuân thủ assignment active, account state, ownership/token lifecycle. Khóa account/thu hồi assignment/đóng phiên phải xử lý quyền nhận tiếp theo theo cơ chế hiện có, không chỉ kiểm lúc đăng nhập đầu.
- Reconnect tải lại state từ API đúng quyền; không giả định WS luôn giao đủ event. Không thêm outbox/table mới trong nhiệm vụ này; nếu thiếu độ bền sau crash thì ghi giới hạn cụ thể.

### UI/media

- Chạy browser thật cho Guest/Customer/Cashier/Kitchen/Manager/Admin. Kiểm loading/empty/error/success/403/409/retry, click đôi, refresh/back, network failure, stale cart và realtime update.
- Giữ design tokens/font/màu/spacing/radius/icon/component Bootstrap sẵn có. Những sửa page/realtime không tạo trang một phong cách riêng hoặc làm mất label bàn/branch.
- Media: upload ảnh hợp lệ → URL GET trả đúng bytes/MIME → restart app → GET còn đúng; test owner/dung lượng/file sai và storage lỗi không trả success/URL giả. Không thay schema; storage path/config test tách dữ liệu thật.
- Test viewport360/390/768/1366, focus/keyboard/label, ảnh thiếu, tên dài, bảng dài, modal tràn. Lưu browser evidence được phép, console/page/network errors và kết quả assertion; GET HTML200 không phải browser interaction pass.

### Test realtime bắt buộc

Cashier/Kitchen CN01 và CN02, Admin, hai phiên khách: lần lượt mutation material/recipe/adjust/import/transfer/preparation/settlement/order/payment/cancel/refund trong các API có thật; xác định event nào cần có theo matrix. Mỗi ca kiểm người đúng nhận, người không quyền không nhận, payload đúng, UI cập nhật và rollback/revoke/reconnect đúng. Thiếu dịch vụ/browser thì ghi blocked, không bịa evidence.

## 7. Mục 4.8 — Sửa báo cáo kiểm thử theo mẫu số thật

### Chỉnh kết luận cũ

- Theo evidence ZIP5 đã kiểm: JSON có **41 ca HTTP**, **26 cặp method/path theo chuỗi log**; XML kèm ZIP có **149 test unit**; tài liệu ghi **2 kịch bản concurrency**. Đây là mốc lịch sử, không phải số bắt buộc sau sửa và không tự biến thành độc lập đã chạy lại.
- Phân biệt tổng case, unique concrete URL, route template và alias/method. Một endpoint có nhiều negative/happy cases; nhiều URL ID có thể thuộc cùng route template.
- Bỏ các khẳng định “41 endpoints/100% toàn API”, “hoàn hảo/xuất sắc”, “4 HTML200 chứng minh all UI/E2E” nếu không có evidence tương ứng.
- Tách evidence nhóm cung cấp, evidence Agent vừa chạy và phần chưa kiểm. Không trộn log149 cũ với code mới rồi nhận regression đã đạt.

### Inventory và test cần chạy

1. Tự lập inventory method+route template/alias từ annotations/runtime mapping được phép, đối chiếu client network và OpenAPI. Không bật mappings actuator công khai để lấy danh sách. Business API, page routes, WS và resource serving có phạm vi/denominator riêng.
2. Gắn case IDs vào từng route/caller liên quan sáu mục: happy path, role/branch/owner/session, invalid/expired/revoked token, state conflict, payload validation, CSRF khi áp dụng, replay và rollback. Kiểm positive lẫn negative;403 cho mọi người không phải fix thành công.
3. Khởi động app dùng DB test hiện có, chạy HTTP thật qua filter/cookie/token/CSRF; không gọi controller trực tiếp rồi nhận HTTP đã kiểm. Header canonical `Idempotency-Key` đúng ở client/controller/runner/docs.
4. Lưu request method/path/header names/payload và response/assertion đủ tái hiện nhưng redact authorization/cookie/OTP/secrets; không lưu token thật. Lưu time/runID/commit/environment/test-data references.
5. Concurrency dùng nhiều kết nối/transaction có điều phối thực. Hai background jobs cùng ID chỉ là một quan sát; kiểm thêm replay khác owner/session/payload, retry timeout, OTP consume đúng/sai và rollback. Không stress production.
6. Browser tự động hoặc thao tác có evidence trên app chạy thật cho các flow bị sửa, WS clients và viewport. Include console/network; thiếu browser thì `BLOCKED_BROWSER`, không PASS.
7. Báo `passed/failed/skipped/blocked/not_run/N_A`, lý doN/A, endpoint/case coverage có mẫu số. Chưa enumerate đủ route thì ghi “chưa xác định coverage toàn bộ”; không tự gán100%.
8. Nếu chỉ regression sáu mục thì ghi phạm vi sáu mục. Muốn kết luận toàn hệ thống phải chạy full suite/API inventory có sẵn và các hành trình liên quan, không mặc định các phần không chạm đều đã pass.

### Ma trận nghiệm thu tối thiểu của đợt này

| ID | Ca cần chạy | Điều kiện đạt |
|---|---|---|
| QR01 | Restore token đúng/giả/expired/revoked | Chỉ token đúng khôi phục đúng phiên; không silent grant phiên khác |
| QR02 | ĐóngA/mởB cùng bàn, browserA không quét lại | Không dữ liệu/event/giỏ củaB; chủ động quét lại theo policy |
| ID01 | Retry đơn đầu sau commit/timeout | Một tác động và response hợp lệ, không conflict giả |
| ID02 | Same key đổi từng note/topping/quantity/voucher/cart remaining | Conflict theo canonical payload, không response cũ sai nghĩa |
| ID03 | Key cũ khác session/owner/branch/capability | Không rò tài nguyên cũ, không tạo tác động mới trái phép |
| ID04 | Hai request cùng key/payload đồng thời | Một tác động, không double movement/payment/voucher |
| PG01 | Page12, trang tiếp/cuối, >12/>20 dữ liệu khi có | Backend trả đúng tập/count, không tải all rồi ẩn |
| PG02 | Search/category/price/sort/branch/top20 kết hợp | Filter trước page, metric/tie-break/count chính xác |
| PG03 | Browser next/back/filter nhanh/add cart | Không response cũ ghi đè, không mất giỏ/UI |
| SQL01 | Same-row/chain/Stock closing/source theo mốc cắt | Mỗi invariant có query/evidence/status riêng, không chữa dữ liệu |
| SQL02 | Opening/source thiếu hoặc chain đứt trong dữ liệu có sẵn | Ghi ngoại lệ thật, không tự gánPASS hoặc cân bằng giả |
| RT01 | Material/recipe/ADJUST và event matrix đa branch | Đủ event đúng role/branch, không leak, UI cập nhật |
| RT02 | Rollback/revoke/logout/close-session/reconnect | Không success event sai, quyền/lifecycle và tải bù đúng |
| MD01 | Upload→GET bytes→restart→GET, storage lỗi | File thật còn truy cập đúng; lỗi không URL giả |
| BR01 | Role/browser/viewport/errors | Flow hợp lệ thành công, negative bị chặn, không lỗi UI nghiêm trọng |
| EV01 | Reconcile logs với test inventory và report | Số case/endpoint/phạm vi đúng, evidence mới/cũ tách rõ |

## 8. Bổ sung chuyển VietQR thủ công sang VNPAY, MoMo và ZaloPay

### 8.1. Mục tiêu và nguồn tham khảo đã kiểm

Ngoài sáu mục trên, **tích hợp cả ba cổng VNPAY, MoMo và ZaloPay** vào luồng thanh toán TABLE/COUNTER hiện có. Đây là thanh toán điện tử cho đơn tại quán, không phải mở lại nguồn đơn ONLINE hoặc giao hàng. Giữ phương thức CASH; thay lựa chọn VietQR/chuyển khoản thủ công của giao dịch mới bằng ba cổng được xác nhận phía server.

Repo tham khảo: https://github.com/huonghuong1412/Payment-Online-Demo.git . Nhánh mặc định đã kiểm là `master`, snapshot `35eb1255c529a6566dc71bf88eb147b52a19e817`. Khi triển khai ghi commit thực dùng, đọc license/attribution nếu có, không tự giả định quyền sao chép khi repo không có license rõ.

Các file đã đọc:

- `pom.xml`: Spring Boot2.6.4, Java8, javax.servlet và Apache HttpClient đời cũ; **không copy pom/stack** sang MilkTea Java21/Spring Boot hiện hành.
- `controller/VnpayPaymentController.java`: có create URL/result, dùng giao thức/signing cũ và nhận amount từ client; result không kiểm checksum. Phải thay bằng adapter và luồng xác minh phù hợp phiên bản merchant đang đăng ký.
- `controller/MomoPayController.java`: create/query mẫu dùng các field/giao thức đời cũ; nhánh transactionStatus còn gọi CREATE_ORDER_URL. Không giữ nguyên endpoint/field/requestType chỉ vì demo từng chạy.
- `controller/ZaloPayPaymentController.java`: tạo đơn/query kiểu API cũ, hardcode dữ liệu hàng mẫu; không có callback merchant hoàn chỉnh trong controller đã kiểm. Thay item bằng snapshot đơn thật nếu giao thức cần.
- `config/*`, `utils/*`: đọc để hiểu tham số/chữ ký nhưng không sao chép credential/URL localhost/hardcode vào ứng dụng. Không sao chép `@CrossOrigin("*")`.

Repo là tài liệu tham khảo thuật toán/flow, **không phải module an toàn có thể gắn nguyên xi**. Phải đối chiếu tài liệu chính thức và API version hợp đồng merchant hiện tại. Không dùng mẫu SHA/encoding cũ nếu khác đặc tả đang tích hợp.

### 8.2. Giữ ranh giới không sửa database

Yêu cầu không DDL/migration/seed/dữ liệu cũ vẫn áp dụng cho phần cổng thanh toán. Trước code lập bảng mapping mô hình hiện có với yêu cầu mới: invoice/order/branch/owner, provider, attempt/request reference, amount/currency, trạng thái chờ/đã thu/failed/expired/refund, provider transaction ID, idempotency, audit và người/hệ thống ghi thu.

Trong ZIP5, PaymentMethod mới có CASH/BANK_TRANSFER; ThanhToan là khoản thu với một invoice unique và NguoiThu bắt buộc, không phải bảng payment attempt. ThongBaoThanhToan/BanGhiChongLap/NhatKyNghiepVu cần kiểm semantics/cột thực trước cân nhắc tái sử dụng.

- Có thể sửa enum/DTO/code nếu cột/constraint hiện có thực sự lưu được giá trị mới; phải kiểm metadata chỉ đọc và mọi nơi parse/filter/render method. Không mặc định thêm enum là đủ, không chạy auto-DDL.
- **Không nhét VNPAY/MOMO/ZALOPAY thành BANK_TRANSFER giả rồi mất provider/reference**, không ghi Payment PENDING amount0 để vượt thiếu bảng attempt, không biến audit chung thành kho dữ liệu tùy tiện không có hợp đồng.
- Có thể dùng cấu trúc lưu trữ hiện có chỉ khi chứng minh lưu được correlation bền vững, quyền, amount đã chốt, trạng thái, concurrency và restart recovery; mô tả rõ mapping và giới hạn. Không cần tự thêm bảng để làm đẹp thiết kế.
- Không chỉ dùng HashMap/sessionStorage/local file để lưu attempt/mapping tiền rồi gọi là tích hợp bền vững. Callback có thể đến sau restart hoặc không có browser/session.
- Nếu cấu trúc hiện có không đủ bảo đảm xử lý tiền an toàn, ghi `BLOCKED_SCHEMA_COMPATIBILITY`, chỉ rõ thiếu trường/ràng buộc nào, hoàn tất adapter/contract test và các phần source độc lập; không sửa database hoặc giả vờ đã tích hợp đủ. Không biến giới hạn này thành lý do bỏ sửa sáu mục cũ.
- Không xóa/chuyển đổi các khoản BANK_TRANSFER/VietQR lịch sử. Giữ khả năng đọc hóa đơn và báo cáo cũ; ba provider mới phải được nhận diện đúng trong receipt/report/refund.

### 8.3. Kiến trúc tích hợp phù hợp MilkTea

1. Tạo abstraction provider/gateway trong service layer (tên theo convention repo), ba adapter VNPAY/MoMo/ZaloPay và cấu hình typed theo profile. Controller mỏng; không đặt gọi HTTP, HMAC, JSON parsing và ghi thu trực tiếp trong một controller demo.
2. Lớp nghiệp vụ payment dùng chung chịu trách nhiệm quyền invoice/branch/session/owner, payable, state, dedup, correlation, ghi nhận thành công, audit và event. Adapter chịu trách nhiệm contract/signing/transport của từng nhà cung cấp.
3. Tách các chức năng create-attempt, browser return, server notification/IPN, status/query/reconciliation và refund theo provider. Endpoint status kiểm quyền người xem; callback không dùng JWT nhân viên để thay chữ ký provider.
4. Không hardcode base URL localhost, merchant code, access/secret/hash keys trong Java hoặc frontend. Sandbox/prod tách rõ, timeout/network/retry có giới hạn và idempotency phù hợp; không giữ lock DB trong lúc gọi HTTP ra cổng.
5. Amount lấy từ invoice server đã chốt giảm giá, không từ amount client. VND dùng BigDecimal/long với quy đổi/rounding/bounds rõ, không float/int multiplication dễ overflow. Invoice0 xử lý theo chính sách tất toán0, không gửi cổng nếu provider không hỗ trợ.
6. Mã giao dịch merchant unique theo quy tắc từng provider, gắn bền vững invoice+provider+attempt. Không tái dùng một reference cũ cho lần retry khác nghĩa; không dùng random8 số không kiểm collision như demo.
7. Đảm bảo thanh toán Guest tại bàn/cashier COUNTER hoạt động đúng quyền; payment return chỉ trả thông tin tối thiểu cho owner/capability hợp lệ, không lộ đơn bằng query orderId bất kỳ.

### 8.4. Luồng người dùng và trạng thái

1. Khách hoặc cashier đúng quyền mở hóa đơn tại quán, chọn CASH hoặc provider đang bật. Giao diện không có địa chỉ/phương thức giao hàng; label branch/bàn hoặc quầy giữ nguyên.
2. Server kiểm invoice hiệu lực/chưa thu, order không bị hủy trái policy, amount/provider/config hợp lệ; tạo attempt/correlation bền vững trước redirect/gọi cổng theo khả năng schema đã xác minh.
3. Create payment trả URL/deep link/QR của provider theo API thực và khả năng được cấp; không dựng QR bank static rồi gắn logo ví để giả tích hợp. Chỉ redirect tới host/provider đã cấu hình, không dùng returnUrl client tùy ý.
4. Khi chuyển sang cổng, UI hiển thị trạng thái chờ. Người dùng đóng tab, bấm quay lại hoặc bấm “đã trả tiền” **không tự ghi thu**. Nếu callback chưa tới thì hiển thị đang xác minh và query backend/status hợp lệ.
5. Chỉ ghi nhận paid bằng notification server-to-server đã xác minh hoặc kết quả query server-to-server tin cậy phù hợp đặc tả. Browser return chủ yếu hiển thị kết quả/poll; return không là nguồn sự thật duy nhất, không tự update paid từ querystring.
6. Callback valid → kiểm merchant/provider/reference/invoice/amount/currency/success state → transaction khóa tài nguyên tương ứng → ghi nhận đúng một khoản thu/correlation/audit → event sau commit đúng branch/session. Callback trùng trả acknowledgment đúng provider nhưng không tạo thu/event/voucher lần hai.
7. Thanh toán điện tử không được giả mạo cashier: NguoiThu bắt buộc hiện có cần mapping người khởi tạo/actor kỹ thuật hợp lệ được quy định rõ và audit phân biệt automated gateway. Không lấy tùy ý cashier đầu tiên, không dùng ID1 hoặc tạo account hệ thống bằng SQL. Nếu schema/policy không biểu diễn được, báo giới hạn như8.2.
8. Không đổi flow pha chế thành luôn trả trước. Không dùng callback để complete bếp hoặc tự trừ kho; thanh toán chỉ tác động state đã được SSOT cho phép.
9. Failure/cancel/timeout chỉ kết thúc attempt theo rule; không hủy đơn hay xóa giỏ/stock tự động trái policy. Retry mới không che attempt cũ chưa chắc thất bại. Timeout mạng là trạng thái chưa biết, không phải bằng chứng chưa bị trừ tiền.
10. Nếu CASH và gateway hoặc hai provider cùng thành công, unique một payment/invoice trong DB không đủ giải quyết tiền khách đã bị trừ hai lần. Phải phát hiện late/duplicate capture, lưu/ghi audit bền vững và đưa vào xử lý refund/đối soát, không bỏ callback mà mất nghĩa vụ tài chính.
11. Callback sau hủy đơn/đóng phiên vẫn phải được xử lý như sự kiện tiền của invoice cũ, không gán sang phiên mới. Không tự mở lại phiên; nếu đã thu cần nghĩa vụ hoàn theo policy.

### 8.5. Chữ ký và callback theo từng provider

Xác minh từ tài liệu chính thức ứng với API version đã chọn; không tự thống nhất cả ba bằng một chuỗi HMAC chung.

| Cổng | Hợp đồng cần triển khai/kiểm |
|---|---|
| VNPAY | Create URL đúng version/encoding/checksum; vnp_Amount theo đơn vị provider và đối chiếu ngược chính xác; TxnRef/TmnCode/currency; IPN xác minh hash trước xử lý, success theo ResponseCode và TransactionStatus; acknowledgment đúng đặc tả; Return URL không tự update paid; query trạng thái/refund khi cần |
| MoMo | Chọn một product/API version tương thích merchant, fields request/response và canonical signature đúng tài liệu hiện hành; IPN kiểm signature, partnerCode/orderId/requestId/amount/transId/resultCode; query dùng endpoint query đúng, không CREATE_ORDER_URL; status code/ack/retry đúng provider |
| ZaloPay | Create/query/callback cùng version; app_trans_id/amount/app_id hợp lệ, timezone/format đúng; phân biệt key ký request và key xác minh callback; kiểm MAC trên data chính xác theo protocol trước parse/tác động; query chủ động khi callback thiếu; ack theo contract |

Không tin payload extraData/orderInfo làm ID tài nguyên nếu chưa đối chiếu correlation server. Không log keys/signature input nhạy cảm; so sánh chữ ký an toàn và xử lý encoding/unicode/empty fields chính xác. Reject malformed/oversized/unknown reference/merchant mismatch/amount mismatch trước ghi tiền.

Callback được gọi không có cookie/CSRF người dùng: chỉ exclude CSRF chính xác route/method provider yêu cầu, bảo vệ bằng chữ ký và kiểm nghiệp vụ; không disable CSRF toàn app hoặc permitAll mọi `/payments/**`. VNPAY IPN có contract GET riêng: ngoại lệ provider được giới hạn, không áp thành mọi GET đều mutate tự do.

### 8.6. Admin cấu hình và thay phần VietQR cũ

- Rà settings hiện có để tách **thông tin ngân hàng/QR chuyển khoản thủ công** với **merchant credentials/API endpoints**. Số tài khoản ngân hàng không tự trở thành TmnCode/partnerCode/appId.
- Admin có trang/mục thanh toán đồng nhất layout hiện hành: ba provider, trạng thái bật/tắt theo cấu hình thật, sandbox/prod, merchant identifier được phép hiển thị, trạng thái đủ cấu hình, lỗi thiếu cấu hình và hướng dẫn callback/return URL. Manager/staff không xem secret/quyền cấu hình toàn chuỗi.
- Secret từ env/secret store/cơ chế có sẵn, mask trên UI, không đưa credential demo của repo vào source hay browser. Không có secret endpoint trả lại plaintext. Không thay schema để thêm settings trong đợt này.
- Nếu cơ chế hiện có không lưu được ba bộ cấu hình, dùng env/backend config làm nguồn thật và Admin hiển thị trạng thái/giá trị không nhạy cảm; control chỉ được sửa nếu có cơ chế persistence hiện hữu an toàn. Không tạo form báo “đã lưu” khi chỉ đổi JS hoặc mất sau restart. Nêu rõ giới hạn edit UI nếu bị ràng buộc no-DB.
- Thay phương thức mới trên checkout/POS/bill/payment modal/order detail; bỏ nút “Tôi đã chuyển tiền”/ghi thu BANK_TRANSFER thủ công khỏi **luồng gateway mới**. Giữ CASH, lịch sử và những dữ liệu cấu hình cũ cần để đọc chứng từ; không xóa settings ngân hàng nếu đang dùng nghiệp vụ khác đã có.
- QR do provider trả về hoặc pay URL hợp lệ là phần thanh toán, không phải QR bàn. Có thể QR có thương hiệu provider nhưng phải đúng contract, không tự dựng thông tin giả.
- Thiếu config hoặc provider disabled thì UI giải thích và không cho tạo attempt hỏng; CASH vẫn dùng được. Cập nhật receipt/report filter để phân biệt đúng CASH/VNPAY/MOMO/ZALOPAY/legacy, không gán mọi gateway thành BANK_TRANSFER mơ hồ.

### 8.7. Hủy, hoàn và đối soát cổng

Giữ approval/obligation của MilkTea; không xóa Payment khi hủy. Phân biệt hoàn thủ công với hoàn qua API cổng; không chọn CASH tùy tiện để hoàn giao dịch ví nếu policy không cho.

Nếu tài khoản sandbox được cấp API refund, triển khai thông qua adapter theo protocol, idempotency key/refund ref và kiểm status/query; chỉ mark thực hoàn khi bằng chứng hoàn tiền thành công được xác minh. Nếu chưa có quyền refund hoặc schema không biểu diễn được, giữ obligation pending và hướng dẫn thao tác/đối soát thủ công có reference hợp lệ, ghi `BLOCKED_PROVIDER_CAPABILITY` thay vì auto DA_HOAN.

Một giao dịch refund timeout phải query trước retry không kiểm soát. Tổng hoàn không vượt đã thu, duplicate callback/refund không chi hai lần. Báo cáo thực thu ròng dựa thu được xác minh trừ thực hoàn; pending/create URL/browser return không làm tăng doanh thu.

### 8.8. Kiểm thử bắt buộc cho từng cổng

Tạo contract tests cục bộ với response/callback được ký bằng **test secret riêng** để kiểm logic. Đây chỉ là contract/simulator test, không gọi là sandbox provider đã pass. Sau đó dùng credential sandbox do chủ dự án cấu hình để chạy provider thực; không dùng key công khai trong repo làm key của mình.

| ID | Ca nghiệm thu | Điều kiện đạt |
|---|---|---|
| PAY01 | Create attempt đúng invoice/owner/branch cho cả3provider | Amount từ server, correlation bền vững, không ghi paid sớm |
| PAY02 | Client sửa amount/provider/reference/returnUrl/branch | Không đổi payable/quyền hoặc redirect tùy ý |
| PAY03 | Callback hợp lệ, success/failure/pending | Đúng kết quả từng provider, chỉ success xác minh mới ghi thu |
| PAY04 | Sai/mất signature, merchant/ref/amount/currency mismatch | Không mutation paid/audit success, ack phù hợp |
| PAY05 | Return trước IPN, IPN trước return, đóng browser | Cùng kết quả cuối, không dựa browser để thu tiền |
| PAY06 | Callback trùng/đảo thứ tự, timeout rồi callback | Một khoản thu hợp lệ, không hạ paid thành failed |
| PAY07 | Duplicate create/key retry, hai provider/cash cùng race | Không thu nội bộ trùng; external double capture phát hiện và giữ nghĩa vụ xử lý |
| PAY08 | Callback trễ sau cancel/close-session/phiên mới | Gắn invoice cũ, không lộ/nghiệp vụ sang phiên mới, có refund obligation khi cần |
| PAY09 | Restart giữa create→callback và sau provider success trước local commit | Tìm được correlation, recovery/query/retry đúng, không mất/nhân đôi thu |
| PAY10 | Refund success/failure/timeout/duplicate nếu được cấp quyền | Không DA_HOAN giả, không vượt paid/chi lặp |
| PAY11 | Sandbox/prod config thiếu/sai, endpoint timeout/nonJSON | Lỗi rõ, không secrets/log hoặc URL success giả; CASH không bị hỏng |
| PAY12 | Admin settings/checkout/POS/bill/receipt/realtime/report | Style nhất quán, provider và status/amount/ref đúng branch; legacy đọc được |
| PAY13 | CSRF user routes/callback public route/signature/owner status API | Exemption đúng phạm vi, callback không bypass auth giao dịch khác |
| PAY14 | Zero payable và rule chưa trả trước | Tất toán0 đúng policy, không bắt provider nhận0 và không đổi flow bếp |

Test riêng VNPAY unit scaling/signature/encoding; MoMo query đúng endpoint/canonical signature; ZaloPay callback MAC và app transaction date. Chạy browsers mobile/desktop và callback bằng HTTP server-to-server thật vào app test.

Thiết lập HTTPS callback sandbox có thể truy cập bằng hạ tầng/tunnel được phép, callback URL allowlisted đúng môi trường; localhost không tự nhận được webhook từ provider. Không expose DB/admin/actuator hoặc secrets để tạo tunnel. Merchant onboarding/credentials/mạng thiếu phải ghi cụ thể `BLOCKED_PROVIDER_CREDENTIALS`/`BLOCKED_CALLBACK_REACHABILITY`, vẫn hoàn tất phần độc lập và contract tests; không giả lập rồi nhận sandbox pass.

### 8.9. Bàn giao phần tích hợp

- Bảng repo mẫu→file MilkTea→điều đã tái sử dụng/viết lại, commit và version API chính thức, dependency tương thích; attribution/license rõ.
- API/event/state/correlation mapping, chính sách amount/rounding, dedup, restart recovery, late payment và refund; khả năng/giới hạn schema hiện có.
- Cấu hình `.env.example`/profile chỉ placeholder, hướng dẫn lấy sandbox merchant IDs/keys, khai báo return/IPN và cách chạy; không chứa secret thật và không triển khai prod/thu tiền thật.
- Test results chia theo provider và lớp unit/contract/HTTP/browser/sandbox, passed/failed/blocked với evidence mới. Callback/refund chưa chạy được phải nêu đúng, không nhận “cả3đã hoàn chỉnh”.
- Tài liệu thanh toán và báo cáo cập nhật từ VietQR thủ công sang gateway xác nhận tự động; không gọi create URL thành công là thanh toán thành công. Sáu mục cũ vẫn có tracker và evidence riêng.

### Nguồn chính thức để Agent xác minh khi triển khai

- VNPAY: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
- MoMo notification: https://developers.momo.vn/v3/docs/payment/api/result-handling/notification/
- MoMo one-time payment: https://developers.momo.vn/v3/docs/payment/api/credit/onetime/
- ZaloPay callback: https://docs.zalopay.vn/docs/specs/callback-api/
- ZaloPay query: https://docs.zalopay.vn/docs/specs/order-query/

Nguồn có thể thay đổi; Agent phải đối chiếu version/product thực được merchant cấp, không trộn fields các thế hệ khác nhau hoặc lấy repo demo làm đặc tả cuối.


## 9. Trình tự sửa và kiểm hồi quy

1. Chụp hiện trạng version/diff, đọc SSOT và xác minh từng phát hiện. Trường hợp đã sửa ghi evidence, không sửa lại để đủ checklist.
2. Đóng QR restore/replay trước, giữ luồng hợp lệ; bổ sung regression tests có ca thất bại trước sửa khi khả thi.
3. Sửa query paging và client, bỏ tải all từ SSR/JS cho luồng đã chuyển; kiểm catalog/giỏ/checkout không regression.
4. Hoàn thiện publisher/ACL/reconnect và media/UI; test nhiều role/branch/browser.
5. Chạy các query chỉ đọc và test side effects; ghi rõ ngoại lệ lịch sử ngoài phạm vi, không chỉnh DB.
6. Hoàn thành adapter, callback/query/config/UI của ba cổng theo mục8 và test từng provider; sau đó chạy build/context/unit/API/browser/concurrency theo khả năng môi trường, full regression hiện có; nếu source đổi tiếp thì kiểm lại phần ảnh hưởng và regression cuối đúng version bàn giao.
7. Cập nhật evidence reports sau chạy, không trước. Không sửa expected chỉ để test xanh; không xóa/skip case fail không lý do. Không chỉ syntax-check rồi dừng.

## 10. Bàn giao và điều kiện hoàn tất

- Source/test/frontend, phần tích hợp ba cổng và tài liệu liên quan đã sửa; diff không có thay schema/migration/seed/dữ liệu cũ. Query đối soát nếu tạo/sửa phải chỉ đọc dữ liệu.
- Tracker theo sáu mục: root cause, file/method, sửa gì, test IDs, evidence level và phần còn blocked/ngoài phạm vi. Không nhận đã chữa phả hệ dữ liệu seed.
- API/event/test matrices, kết quả máy đọc được và log run hiện tại; commands có thể tái chạy với kết nối test hiện có, không chứa bước reset/migrate/seed hoặc secret.
- `E2E_TEST_REPORT.md`, `API_TEST_MATRIX.md`, JSON results và tracker đang dùng được cập nhật nhất quán; số liệu mới dựa run thật, số liệu149/41/26/2 cũ ghi rõ là lịch sử nếu giữ.
- Báo cáo readonly SQL: tên bảng/constraint đúng mức xác minh, same-row/chain/closing/source có expected/actual và phạm vi; FAIL/INSUFFICIENT_EVIDENCE không bị che bằng tổng0 vi phạm.
- Browser/realtime/media evidence và thiết kế UI thống nhất; positive/negative tests QR/replay/paging/role/branch/session đạt trong môi trường đã mô tả.

**Chỉ kết luận hoàn tất phạm vi source và tích hợp thanh toán khi các ca bắt buộc tương ứng đã chạy và đạt.** Credential/schema/callback blocker phải nêu rõ, không giả lập thành sandbox pass. Còn fail hoặc blocked thì ghi chính xác, tiếp tục phần độc lập và bàn giao chỗ chặn; không hứa đã chạy nền hoặc gắn VERIFIED cho việc chưa thực hiện. Dữ liệu lịch sử ngoài phạm vi không làm lý do dừng mọi sửa code, nhưng phải giới hạn kết luận về toàn hệ thống.

Phản hồi cuối: tóm tắt sáu mục đã sửa, ba cổng và mức tích hợp thực, files, tests thực chạy và passed/failed/blocked, query/evidence đối soát, giới hạn còn lại, lệnh tái hiện. Không chỉ nói “đã sửa xong toàn bộ, hệ thống hoàn hảo”.
