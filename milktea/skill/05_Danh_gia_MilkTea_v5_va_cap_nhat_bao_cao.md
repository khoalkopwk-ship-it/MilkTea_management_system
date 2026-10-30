# Đánh giá MilkTea ZIP 5 và hướng hoàn thiện báo cáo cuối kỳ

Ngày kiểm tra: 29/10/2026. Phạm vi: source và tài liệu trong `MilkTea_management_system (5).zip`. Chỉ phục vụ tại quán qua TABLE/COUNTER; không phát triển giao nhận/Shipper/AddressBook hoặc R19 về đóng cơ sở. Không sửa source hoặc database trong lần đánh giá này.

## 1. Hệ thống đã phù hợp để viết báo cáo chưa?

**Đã đủ nền chức năng và kiến trúc để bắt đầu cập nhật, hoàn thiện báo cáo đồ án. Tuy nhiên, chưa nên chốt bản nộp với kết luận “toàn bộ API/E2E đạt 100%, hệ thống không còn lỗi”.** Một số lỗi source và giới hạn kiểm chứng vẫn còn; báo cáo hiện tại cũng mô tả sai phạm vi một chi nhánh.

Có thể làm song song hai việc: cập nhật ngay chương yêu cầu/kiến trúc/use case/dữ liệu/giao diện theo source hiện có; đóng các điểm quyền, phiên, phân trang và bằng chứng kiểm thử trước khi khóa chương kiểm thử/kết luận. Không cần đợi thêm chức năng ngoài phạm vi mới viết báo cáo.

**Ba điều kiện trước bản nộp:** phạm vi và vai trò đúng mô hình chuỗi; nội dung kỹ thuật/ảnh/sơ đồ khớp source hiện tại; số liệu kiểm thử được ghi theo đúng loại bằng chứng, không suy toàn hệ thống từ một tập ca mẫu.

## 2. Những gì đã kiểm và giới hạn

| Nội dung | Kết quả |
|---|---|
| Source chính | 185 tệp Java, 22 tệp test |
| JavaScript ngoài template | Chạy `node --check`: 12/12 đạt; không thay thế browser/inline-script test |
| Surefire kèm ZIP | 22 suite, 149 test, 0 failure/error/skipped; là báo cáo lưu sẵn |
| Annotation integration trong test source | Không thấy SpringBootTest/DataJpaTest/WebMvcTest; không lấy số unit test làm số HTTP/E2E |
| `docs/api_test_results.json` | 41 bản ghi, tất cả ghi PASSED; có 26 cặp method/path khác nhau theo chuỗi URL ghi trong log |
| Chạy Maven độc lập lần này | `bash mvnw -o test` dừng do parent Spring Boot4.1.1 chưa có trong cache offline; không kết luận dependency không tồn tại hoặc source chắc chắn compile lỗi |
| Chưa chạy độc lập | Spring context, SQL Server hiện hành, SMTP, browser, WS/concurrency, upload serving/restart và các script SQL trên DB thật |
| Word đã đọc | `BaoCao/Nhom03_BaoCaoCuoiKy.docx` và `BaoCao/Nhom03_BaoCaoCuoiKy_review.docx`: paragraph, bảng và cấu trúc nội dung |
| Giới hạn Word | Chưa render và kiểm từng trang/ảnh; chưa kết luận bố cục/ảnh/sơ đồ nhúng đã đúng hoặc đẹp. Không đoán số trang từ mục lục lưu sẵn |

41 bản ghi test không phải 41 endpoint độc lập và không chứng minh 100% route/method/alias. 26 cặp method/path trong log cũng chưa phải route-template inventory đầy đủ, vì URL có thể chứa ID cụ thể. Bộ log là bằng chứng tốt hơn bản trước nhưng cần mô tả đúng mức.

## 3. Đối chiếu 16 nhóm phát hiện bản 04

| Nhóm | Kết quả từ ZIP5 | Mức kết luận phù hợp |
|---|---|---|
| V4-01 | OrderService kiểm QR capability trước tạo đơn đầu; GuestAccessService có token HMAC theo bàn/TTL | Đã sửa nguyên nhân chính trong code; cần negative/concurrency HTTP đầy đủ |
| V4-02 | JwtAuthenticationFilter chỉ gắn branch từ token bàn cho CUSTOMER, không còn gắn cho staff | Đã sửa đường cấp nhầm branch nhân viên đã chỉ ra |
| V4-03 | TableController kiểm customer đúng session hoặc openedBy, không còn return vô điều kiện | Đã sửa trong code |
| V4-04 | Client chỉ restore QR nếu còn một chuỗi table token trong bộ nhớ/sessionStorage | Cải thiện nhưng vẫn chưa xác minh token còn hiệu lực trước cấp lại quyền; còn rủi ro lượt khách — mục4.1 |
| V4-05 | consume OTP phải affectedRows==1; không save entity persist sau consume; recorder không fallback tăng non-atomic | Đã sửa điểm chính; cần concurrency DB thực |
| V4-06 | SQL validation đổi về nhiều tên đúng schema và có THROW | Cải thiện; từng phép kiểm vẫn chưa đủ chứng minh phả hệ/chuỗi/số dư — mục4.6 |
| V4-07 | Seed đã có UPDATE stock khi tiêu hao đơn | Vẫn gán tồn đầu trực tiếp; chưa khép kín nguồn nhập/chuyển/mẻ |
| V4-08 | Một số xử lý và publisher kho cải thiện | Seed và policy location/cost/fallback cần kiểm; chưa thể coi nguồn đầy đủ |
| V4-09 | Seed thêm snapshot nguyên liệu, invoice tách store/voucher đúng hơn | Cần đối soát toàn bộ fixture, trạng thái/giỏ/counter/tồn và thời gian thực |
| V4-10 | Tài liệu nói constraint và đối soát đã chạy | Chưa kiểm schema triển khai; không sửa database theo yêu cầu hiện tại, chỉ ghi giới hạn |
| V4-11 | InventoryController thêm resolveBranchScope; detail kết ca nhận actor | Sửa rõ ở API chính; cần rà các bypass/null service còn lại |
| V4-12 | TABLE kiểm quyền trước replay, so bàn đơn cũ với bàn đang thao tác | Chưa ràng buộc replay tới cùng phiên/lượt khách và đủ payload — mục4.2 |
| V4-13 | Thêm branch event import/transfer-out/waste/consumption/adjust | Một số nhánh material/recipe/threshold vẫn global; WS policy/revoke chưa chứng minh toàn bộ |
| V4-14 | Product/map thiếu bị từ chối cả cơ sở chưa có mapping | Topping còn suy loại bằng allowTopping và tên category; catalog/page chưa đồng nhất hoàn toàn |
| V4-15 | Repository bỏ cancelledStatus thừa, ranking group product/name; menu có nút page12 | Page12 là ẩn/hiện DOM client, chưa đáp ứng yêu cầu phân trang backend |
| V4-16 | Thêm JSON HTTP results và tài liệu test | Có evidence mới nhưng kết luận 100%/FIXED_E2E vẫn vượt mức log chứng minh |

Không đảo ngược các bản sửa tốt. Nền CSRF, BCrypt/JWT, OTP, scope staff, snapshot giá/recipe, nghĩa vụ hoàn tiền, stock kết ca bằng retained và upload local thật là các điểm đáng trình bày trong báo cáo.

## 4. Lỗ hổng/nhược điểm còn lại cần ưu tiên

### 4.1. P1 — Khôi phục QR dựa trên token tồn tại, chưa phải token hợp lệ

`static/js/customer/customer.js:47–88` tính hasActiveToken bằng việc có chuỗi trong window/sessionStorage, rồi restore lastTableQr và gọi public table-context. Token đã revoke/hết hạn vẫn là chuỗi không rỗng. TableContextService có thể cấp token của OPEN session hiện hành từ QR hợp lệ. Điều này chưa ngăn chắc việc browser lượt trước tự gia nhập lượt mới nếu context/token cũ còn lưu.

**Cần làm:** server xác minh token trước restore, phân biệt giữ phiên hiện tại với quét lại chủ động, clear context khi đóng/expired; test đóng phiên rồi mở lượt mới trong cùng browser mà không quét lại. Không đồng nhất QR bàn tĩnh với bằng chứng vị trí vật lý.

### 4.2. P1 — Replay TABLE còn ràng buộc bàn, chưa đủ phiên/owner/payload

OrderService:118 trở đi vẫn fingerprint table/session/voucher/items, chưa gồm note/remainingCartItems. Sau kiểm quyền context hiện tại, replay chỉ kiểm order.session.table.id trùng bàn. Người có quyền lượt mới tại cùng bàn, có key/payload cũ hoặc fallback key cũ, có thể nhận lại thông tin đơn lượt cũ nếu không kiểm thêm owner/session của tài nguyên replay. Không khẳng định dễ đoán key; đây là kiểm quyền thiếu khi key lộ/reuse.

**Cần làm:** bind dedup với actor/capability/session/branch/action và payload chuẩn đầy đủ; replay kiểm quyền trên chính đơn cũ, không chỉ quyền tại bàn hiện tại. Kiểm cùng key khác note/items/voucher/caller và replay sau đóng phiên. Rà lookup sớm ở nhập/kết ca/payment/refund theo cùng nguyên tắc.

### 4.3. P1 — Topping vẫn có thể là món thường

OrderService dùng điều kiện category name có “topping” hoặc allowTopping=false để cho qua. Một đồ uống thuộc category không cho thêm topping vẫn có thể được gửi làm topping nếu mapping/active hợp lệ. `allowTopping` là khả năng nhận topping của món cha, không phải loại món con.

**Cần làm trên source hiện có:** xác định topping theo quy tắc tường minh có căn cứ, validate category/product active, duplicate IDs và quantity, chốt snapshot đúng. Không nhất thiết đổi database trong nhiệm vụ này; nếu schema không biểu diễn đủ thì ghi hạn chế riêng.

### 4.4. P1 — Menu page12 vẫn là phân trang phía trình duyệt

`templates/customer/menu.html:1599–1640`: MENU_PAGE_SIZE=12, ẩn tất cả `.product-item`, lấy filteredMenuItems.slice rồi hiện lại. CustomerPageController vẫn nạp products/allProducts. Vì vậy có nút page12 nhưng không phải server-side paging/lazy và chưa chứng minh chỉ trả món bán tại branch nhất quán với checkout.

**Cần làm:** nối search/filter/page/size tới query backend có scope, trả items/total đúng; nếu giữ UI hiện tại trong báo cáo phải ghi đúng “phân trang phía client”, không mô tả là backend. Đây là khoảng thiếu so với yêu cầu ban đầu đã chốt.

### 4.5. P1/P2 — Bảo mật capability/config và ranh giới service còn fallback

GuestAccessService có secret mặc định/fallback; token QR HMAC kiểm tableId/expiry nhưng không sử dụng branch trong payload để đối chiếu và không gắn session hiện hành. Nếu môi trường dùng secret mặc định đã biết, tính bảo vệ capability suy yếu; không khẳng định prod hiện dùng fallback vì chưa có config runtime. Phải fail-fast secret trong prod, không đưa giá trị secret vào báo cáo.

Payment/Refund/Inventory còn các nhánh optional repository, fallback Account.branch transient hoặc truy vấn stock không branch khi repository null. Service staff vẫn cần rà overload/null actor; route guard không thay kiểm quyền lớp service. Không làm test pass bằng cách bỏ guard hoặc cho nullable dependency bảo mật.

### 4.6. P1, dữ liệu ngoài phạm vi chỉnh sửa — Evidence SQL chưa chứng minh nguồn/tồn đúng toàn hệ thống

Seed vẫn DELETE rồi INSERT số dư dương ở sample_data.sql:648–701, dù đã thêm snapshot/UPDATE consumption và tách giảm giá. Chỉ vậy chưa giải thích nguồn từng raw/SOCHE hoặc cân chuyển kho/mẻ.

Validator inventory kiểm `TonSau=TonTruoc+delta` trên từng dòng là tốt nhưng khác với kiểm after dòng trước=before dòng sau và Stock=after cuối. Có thể mọi dòng đúng số học riêng mà tổng chuỗi vẫn sai. Cần đọc cả query thực và expected/actual, không nhận câu “0 vi phạm” là bằng chứng đủ mọi bất biến. Tracker còn nhắc `dbo.TonKho`/tên constraint không khớp mô tả schema trong source; cần sửa tài liệu mô tả cho đúng.

**Theo yêu cầu không sửa database:** giữ nguyên schema/seed/dữ liệu; ghi ngoại lệ và test giao dịch phát sinh mới qua API trên môi trường test hợp lệ, SELECT đối soát chỉ đọc. Báo cáo không được viết đã chứng minh phả hệ toàn bộ lịch sử nếu chưa có evidence.

### 4.7. P1/P2 — Một số realtime publisher và UI chưa nghiệm thu trọn

Đã thêm nhiều kênh branch, nhưng InventoryService còn nhánh MATERIAL_CHANGED/RECIPE_CHANGED và một số ADJUST chỉ global stock/admin. Rà đầy đủ caller và event expected theo vai trò/cơ sở; không mở global cho mọi staff để vá. Cần kiểm WS revoke/reconnect, rollback không phát success, serving media sau restart, browser đa vai trò/viewport và trạng thái lỗi.

### 4.8. P2 — Test report cần hiệu chỉnh cách diễn giải

`E2E_TEST_REPORT.md` ghi 41 endpoints/100%, trong khi JSON là41 ca/26 method-path chuỗi; chưa có endpoint inventory đầy đủ làm mẫu số. Bốn trang HTML trả thành công không chứng minh tương tác DOM, JS, responsive hoặc all role journeys. Hai background jobs trả cùng ID là một quan sát tốt về dedup, không chứng minh toàn bộ race situations. Tài liệu mô tả dùng `X-Idempotency-Key` còn controller thường nhận `Idempotency-Key`: cần thống nhất header và lưu request đã redact để tái hiện, chưa đủ căn cứ kết luận code concurrency fail chỉ từ tên header trong prose.

**Cần làm:** ghi 41 ca HTTP được báo cáo đạt,149 unit theo XML kèm ZIP,2 kịch bản concurrency theo tài liệu; tách phần tự kiểm lại và phần nhóm báo cáo. Thêm positive/negative DB+HTTP+browser cho owner/branch/session, replay, OTP và rollback. Không tự chấm “xuất sắc/hoàn hảo” thay người chấm.

## 5. Kiểm tra hai file báo cáo cuối kỳ

| File | Nội dung đọc được | Đề nghị |
|---|---|---|
| Nhom03_BaoCaoCuoiKy.docx | 1.519 paragraph,105 bảng,98 inline image objects; có chương1–6 và kết luận | Chọn làm bản chính để cập nhật, vì nội dung đầy đủ hơn |
| Nhom03_BaoCaoCuoiKy_review.docx | 714 paragraph,87 bảng,43 inline image objects; vẫn có phạm vi một quán và không có đầy đủ chương5–6 theo heading đọc được | Là bản phụ/đối chiếu, không mặc định là bản mới hoặc hoàn chỉnh hơn chỉ vì có tên review |

Số image objects không tự bằng số hình đánh số và không chứng minh nội dung ảnh đúng. Paragraph Pxxx bên dưới là vị trí cấu trúc Word, không phải số trang; trong Word dùng Find cụm từ trích dẫn. Có bảng/image chứa nội dung riêng cần đối chiếu khi cập nhật trực tiếp.

### Các điểm sai chắc chắn trong bản chính

| Vị trí/cụm tìm | Nội dung hiện tại | Việc cần sửa |
|---|---|---|
| P412, đoạn mục tiêu | Hoạt động theo mô hình “một chi nhánh” | Thay bằng chuỗi nhiều cơ sở cùng thương hiệu, quản trị tập trung và scope từng cơ sở |
| P414, đoạn phạm vi | “Một cửa hàng trà sữa duy nhất, chưa tích hợp… quản lý chuỗi” | Mâu thuẫn source; thay toàn đoạn, giữ loại trừ giao nhận |
| P496–506, nhóm yêu cầu chức năng | Quản lý/Nhân viên/Bếp/Khách hàng | Tách Chain Admin và Branch Manager, phân biệt Guest/Customer; scope branch và owner |
| QĐ24, bảng quy định | “Quét QR mở/tra cứu đúng phiên” | Viết rõ scan mở menu/capability; xem menu không mở phiên; đơn đầu thành công mở phiên |
| Chương3 P770–913, danh sách bảng | Chủ yếu schema một quán; TaiKhoan P893 thiếu EmailXacThuc trong danh sách cột | Bổ sung ChiNhanh/NhanVienChiNhanh/MonUongChiNhanh/voucher/favorite/review theo entity/migration thực; thêm MaCN tại bảng có mapping |
| P1274, kiểm thử tiền mặt | HIEU_LUC được giải thích là “Đã thanh toán PAID” | HIEU_LUC là hóa đơn còn hiệu lực; paid dựa giao dịch thu/tất toán, không gán enum này nghĩa PAID |
| P1287, kiểm thử trừ kho | Cố định150ml trà/30g sữa/1ly cho một ví dụ | Chỉ giữ khi đúng recipe snapshot của đúng ID món/size/topping/quantity, kèm before/after thực; không coi mọi món cùng tiêu hao |
| P1337, kết quả khách hàng | “Đặt món trực tuyến” dễ hiểu thành remote ONLINE | Ghi “đặt món tại bàn bằng QR trong quán”, COUNTER nhân viên; không mô tả giao hàng |
| P1336, lưu trữ hình | Chỉ mô tả Cloudinary | Bổ sung local review storage khi cấu hình cho phép, phân biệt các loại ảnh và đường phục vụ thực |
| Chương4, Dashboard P928–934 | Tổng quan một cửa hàng | Bổ sung Admin toàn chuỗi/Manager cơ sở và định nghĩa thực thu–hoàn; chỉ thêm biểu đồ xếp hạng nếu đã có trong source đang bàn giao |
| Chương6 mục kiểm thử P1250 trở đi | Mô tả vài hành trình, không phản ánh scope branch/capability/replay mới | Thêm ma trận vai trò/branch/owner/state và bảng evidence, không ghi full coverage100% |

Bản review cũng sai phạm vi tại P56/P58. Sửa bản chính rồi mới đồng bộ bản review nếu còn dùng; không tồn tại hai bản cuối kỳ trái nhau.

## 6. Nội dung cần bổ sung theo chương

### Mở đầu và chương1 yêu cầu

Đổi mục tiêu sang hệ thống chuỗi cùng thương hiệu: Admin quản trị chung; Manager được phân công cơ sở; Cashier/Kitchen chỉ nghiệp vụ cơ sở; Guest/Customer sử dụng bàn/phiên. Không tự thay Vendor marketplace bằng Manager rồi nhận đã đáp ứng mọi rubric.

Bổ sung yêu cầu: QR label chi nhánh/bàn; TABLE branch từ bàn, COUNTER từ assignment; không assignment bị chặn; menu/availability/recipe hợp lệ; giỏ phiên có ownership/capability; đăng ký/forgot OTP; bán chạy chỉ completed+tất toán và branch scope; đánh giá chỉ người mua có account. Nêu trạng thái triển khai thực của phân trang client12 và hạn chế backend paging.

### Chương2 use case

Thêm tác nhân Branch Manager và quan hệ quyền rõ với Admin, Cashier, Kitchen, Guest/Customer. Các use case chi tiết cần có precondition, main/alternate/error flows, postcondition, branch/owner/state, transaction/retry và test ID:

- QR context/capability→giỏ→đơn đầu; gọi thêm; token sai/hết hạn/khác bàn.
- COUNTER lấy assignment; không cho khách tự gọi endpoint quầy.
- Customer xem đúng phiên/lịch sử; staff thiếu assignment/cross-branch bị chặn.
- Thu tiền/notice/hoàn: thông báo chuyển khoản khác ghi thu, duyệt hoàn khác thực chi.
- Kho tổng→cơ sở→sơ chế→tiêu hao/kết ca; không mở rộng vòng đời đóng chi nhánh R19.

### Chương3 dữ liệu và ERD

**Đây là sửa báo cáo mô tả database, không yêu cầu thay database.** Đọc entity+schema/migration hiện có, lập data dictionary đúng tên/type/PK/FK/NULL/unique thực; phân biệt constraint được khai báo trong file với constraint đã xác minh trên DB triển khai.

Bổ sung bảng/quan hệ chuỗi: ChiNhanh, NhanVienChiNhanh, MonUongChiNhanh; MaCN của đơn/stock/movement/chứng từ/mẻ/kết ca; review/favorite/voucher/usage/branch mapping theo source. Phân biệt giỏ phiên với giỏ cá nhân từ xa; không tuyên bố account cart nếu code không có.

Phân biệt QR bảnghi bàn, QR capability HMAC ngắn hạn và session token/hash; không vẽ chúng thành cùng một khóa. Snapshot tên/size/giá/định mức tách recipe hiện hành; BanGhiChongLap ràng buộc scope/key/fingerprint. Cột ONLINE/receiver legacy nếu còn thì ghi read-only compatibility, không xem là chức năng giao hàng.

Vẽ lại ERD/logic diagram tương ứng thay đổi, không chỉ thêm văn bản trong khi hình vẫn một quán. Không tự thêm bảng/constraint chưa có để “làm đẹp thiết kế”.

### Chương4 giao diện

Chụp lại UI từ bản chạy cuối: customer có tên cơ sở/bàn, menu page12 và trạng thái thiếu context; cashier/kitchen label branch; Manager scope riêng; Admin filter toàn chuỗi/kho tổng; giỏ, bill/notice, refund, review/media. Loại ảnh form địa chỉ/online nếu còn.

Giữ phong cách Bootstrap toàn hệ thống, mô tả component/header/nav/badge/modal/toast và responsive. Danh sách yêu thích phải phản ánh đúng vai trò thực; nếu staff đã bỏ menu này không dùng ảnh cũ. Tương tự nút nhập/báo hao hụt cơ sở của Admin phải theo template hiện tại, không theo ý định ở cuộc trao đổi khác chưa vào ZIP.

### Chương5 xử lý

Vẽ lại sequence/activity cho QR capability và mở phiên, scope assignment, actor+token, CSRF/JWT/OTP atomic consume, order/invoice/snapshot, complete tiêu hao transaction, hủy/obligation/duyệt/thực hoàn, branch topic sau commit, idempotency/replay. Rà hình nhúng cũ: đây là yêu cầu cập nhật, không khẳng định đã xem và xác nhận từng hình đúng.

Sơ chế theo mẻ khác pha chế đơn. Món hoàn tất mới tiêu hao; không áp paid gate toàn bộ nếu code cho phép thu sau. Hủy/hoàn tiền không tự trả lại raw đã dùng. Tồn raw KHO và SOCHE BEP hoặc policy khác phải theo source/SSOT hiện tại, không viết “trừ kho Bếp tất cả” nếu code còn phân location.

### Chương6 cài đặt và kiểm thử

Giữ Java21/Spring Boot4.1.1 theo pom.xml, ghi dependency/profile/env thực, SQL Server và hướng dẫn cấu hình đủ nhưng không lộ secret. Phân biệt app chạy local, SMTP/storage test và production chưa kiểm. Không chép tài khoản/OTP/token thật vào báo cáo.

Ghi evidence thành các lớp: unit XML149; HTTP log41 ca/26 method-path chuỗi; HTML smoke4 trang theo tài liệu; concurrency2 kịch bản theo tài liệu; SQL3 script theo nhóm báo cáo. Chỉ ghi kết quả của nhóm là “theo log kèm phiên bản” nếu chưa tái chạy tại mốc báo cáo. Bổ sung test ID/request/payload đã redact/expected/actual/DB side effects/environment/time/commit, các fail/blocked/not_run và negative cases. Không gộp tất cả thành149 E2E hoặc41 endpoint coverage100%.

Ưu tiên test còn thiếu: token phiên cũ, replay khác lượt khách, Customer ownership, staff no-assignment+QR, topping giả, paging server/client, missing ingredient rollback, full WS branch events, media restart, OTP concurrency, voucher last slot dưới SQL isolation thật.

### Kết luận, hạn chế và hướng phát triển

Kết quả đạt được là chức năng và bản sửa có bằng chứng, không “bảo mật tuyệt đối/hoàn hảo”. Hạn chế còn lại phải ghi khả năng tái hiện và ảnh hưởng: backend pagination, replay/restore token, test coverage, dữ liệu seed/phả hệ chưa xác minh. Hướng phát triển ưu tiên correctness/UX/observability; không tự đưa giao hàng/R19 trở lại backlog bắt buộc.

## 7. Các đoạn có thể dùng để thay vào báo cáo

### Mục tiêu và phạm vi thay P412/P414

“Đề tài xây dựng hệ thống quản lý chuỗi cửa hàng trà sữa cùng thương hiệu trên nền tảng web, hỗ trợ quản trị tập trung và tổ chức nghiệp vụ theo từng chi nhánh. Hệ thống phục vụ khách gọi món tại bàn bằng mã QR hoặc gọi trực tiếp tại quầy. Mỗi bàn thuộc một chi nhánh; đơn tại bàn lấy chi nhánh từ bàn và phiên phục vụ, trong khi đơn tại quầy lấy chi nhánh từ phân công hợp lệ của nhân viên. Các vai trò chính gồm khách hàng, thu ngân, bếp, quản lý chi nhánh và quản trị toàn chuỗi.”

“Phạm vi triển khai gồm tài khoản và xác thực email, thực đơn, giỏ theo phiên bàn, đơn hàng, thanh toán tại quán, hủy và hoàn tiền, công thức, nhập và sử dụng nguyên liệu, sơ chế, kiểm kê và báo cáo. Hệ thống không triển khai đặt món giao tận nơi, sổ địa chỉ giao nhận hoặc nghiệp vụ vận chuyển. Các dữ liệu và thao tác cần được kiểm soát theo vai trò, chi nhánh, quyền truy cập đối tượng và trạng thái nghiệp vụ.”

### Quy tắc QR và phiên thay QĐ24

“Quét mã QR hợp lệ giúp khách truy cập menu đúng bàn và chi nhánh, đồng thời nhận ngữ cảnh truy cập cần thiết. Việc xem menu không tự mở phiên phục vụ. Khi đơn đầu tiên được tạo thành công, hệ thống mở hoặc sử dụng phiên phù hợp theo quy tắc xử lý đồng thời. Quyền gọi thêm và xem dữ liệu phiên được kiểm tra qua capability/token hoặc quyền sở hữu phù hợp. Token của phiên đã kết thúc không được sử dụng để truy cập lượt phục vụ sau.”

Đoạn trên là hợp đồng nghiệp vụ đích; phần token restore/replay chưa được đóng đầy đủ phải nêu ở hạn chế/test thay vì nhận đã đạt tuyệt đối.

### Thanh toán thay cách hiểu HIEU_LUC=PAID

“Trạng thái hiệu lực của hóa đơn được quản lý độc lập với tình trạng đã thu tiền. Hóa đơn HIEU_LUC là hóa đơn còn hiệu lực, không tự đồng nghĩa đã thanh toán. Khoản thu được ghi nhận theo giao dịch thanh toán hợp lệ; thao tác khách thông báo đã chuyển khoản chỉ tạo thông tin chờ kiểm tra. Khoản hoàn được phân biệt giữa đề nghị, phê duyệt và thực chi để bảo toàn lịch sử dòng tiền.”

### Trình bày kết quả test trung thực

“Bản mã nguồn bàn giao kèm báo cáo149 kiểm thử đơn vị thành công và41 bản ghi kiểm thử HTTP thành công. Các bản ghi HTTP bao phủ26 cặp phương thức–đường dẫn khác nhau theo URL ghi nhận. Đây là tập bằng chứng kiểm thử hiện có, chưa được xem là tỷ lệ bao phủ toàn bộ API hoặc mọi tương tác giao diện. Nhóm cần bổ sung danh mục endpoint và các ca theo quyền, chi nhánh, trạng thái, quyền sở hữu và xử lý đồng thời để đánh giá mức độ bao phủ đầy đủ hơn.”

## 8. Trình tự chốt báo cáo

1. Chọn bản chính, lưu phiên bản và bỏ nhầm lẫn với file review.
2. Sửa phạm vi một quán→chuỗi tại quán, actor matrix và tên thuật ngữ trước.
3. Cập nhật data dictionary/ERD và các sequence/activity theo source; không thay database.
4. Chốt một source snapshot, chạy demo/test và chụp lại ảnh đồng nhất version.
5. Viết lại chương6 từ evidence có thể tái chạy; phân loại code/recorded-test/independent-test/blocked.
6. Sửa kết luận/hạn chế, không nhận100% khi thiếu mẫu số.
7. Cập nhật mục lục, danh sách hình/bảng/code, cross-reference; sửa heading cấp6 đang dùng cho nhiều mục con nếu gây nhảy cấp; rà caption như Bảng4.11 đang dùng caption_pictures.
8. Render Word/PDF rồi kiểm từng trang/ảnh/bảng, số thứ tự và ngắt trang trước nộp. Lần rà này chưa thực hiện khâu render, nên đây là bước cần làm chứ không phải kết quả đã xác nhận.

**Kết luận:** nên bắt đầu hoàn thiện báo cáo ngay, nhưng chưa nộp nguyên bản hiện tại. Giá trị lớn nhất của đợt cập nhật là mô tả đúng hệ thống chuỗi và chứng minh luồng thật, không kéo dài số trang hoặc thêm chức năng ngoài phạm vi.
