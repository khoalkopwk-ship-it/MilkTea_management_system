# PROMPT CHO GEMINI AGENT TRONG ANTIGRAVITY — SỬA 26 NHÓM LỖI MILKTEA

## Cách sử dụng

Đặt file này trong thư mục module Spring Boot có `pom.xml`, cùng cấp với `GEMINI.md` hiện có; hoặc đính kèm nội dung vào cuộc hội thoại Agent. Đọc nội dung từ mục “NHIỆM VỤ” trở xuống như một yêu cầu triển khai.

**Không ghi đè GEMINI.md hiện tại.** Đây là prompt công việc dành cho Agent đã đọc GEMINI.md, không phải một bộ quy tắc song song thay thế tài liệu nghiệp vụ. Khi sửa chỉ dẫn dự án, giữ một nguồn chuẩn và dùng liên kết tham chiếu.

---

## NHIỆM VỤ

Bạn là AI Agent phụ trách kiểm chứng và sửa lỗi end-to-end cho **MilkTea Chain Management System** trong workspace Antigravity hiện tại.

Hãy kiểm tra và xử lý đầy đủ **F01–F26** ở danh sách bên dưới. Tôi yêu cầu bạn thực hiện thay đổi thực tế trong source, migration, giao diện và kiểm thử cần thiết; không chỉ viết kế hoạch, mô tả giải pháp hoặc cập nhật tài liệu thành “đã hoàn thành”.

Danh sách xuất phát từ audit tĩnh bản ZIP `MilkTea_management_system (2).zip` ngày 28/10/2026. Workspace hiện tại có thể mới hơn: **phải kiểm lại từng phát hiện, không mặc định tất cả vẫn còn lỗi**. Đường dẫn và tên phương thức là điểm bắt đầu tìm kiếm; số dòng chỉ có giá trị tham khảo.

### 1. Đọc bối cảnh và xác định phiên bản trước khi sửa

1. Xác định đúng repository và module Spring Boot bằng `git status`, `git branch`, `git log`, `pom.xml`; đọc thay đổi chưa commit. Không ghi đè công việc có sẵn của người khác. Không dùng reset/checkout toàn bộ để tạo một cây làm việc sạch giả.
2. Đọc `AGENTS.md`, `GEMINI.md`, `CODEX.md` nếu có, rồi:
   - `docs/BUSINESS_RULES.md`;
   - `docs/REQUIREMENTS_TRACEABILITY.md`;
   - `docs/BRANCH_DEVELOPMENT_PLAN.md`;
   - `docs/IMPLEMENTATION_PROGRESS.md`;
   - `docs/MIGRATION_GUIDE.md`;
   - owner files trong `skill/skills/` phù hợp với phần sắp sửa.
3. Nếu chỉ dẫn một quán cũ mâu thuẫn yêu cầu chuỗi mới, lấy yêu cầu hiện tại và nguồn nghiệp vụ chuỗi làm căn cứ; ghi lại mâu thuẫn và cập nhật các tham chiếu bị lỗi thời. Không khôi phục phạm vi một quán từ bản sao tài liệu cũ.
4. Kiểm Java/Maven/dependency và cấu hình SQL Server thực tế. Bản audit thấy Java target21; không tự hạ phiên bản hoặc thay stack chỉ để chạy trên máy thiếu công cụ.
5. Mở bảng theo dõi F01–F26, mỗi mục có trạng thái:
   `CONFIRMED`, `ALREADY_FIXED`, `NOT_REPRODUCED`, `FIXED_VERIFIED`, `BLOCKED_ENVIRONMENT`, `OPEN_DECISION`.
   `NOT_REPRODUCED` không đồng nghĩa an toàn nếu chưa có điều kiện tái hiện phù hợp.

### 2. Các nguyên tắc bắt buộc giữ

- Giữ kiến trúc Spring Boot MVC monolith + Thymeleaf + Bootstrap + JPA + một SQL Server; giữ mô hình ba tầng. Không chuyển SPA/microservices hoặc viết lại toàn bộ ứng dụng.
- Giữ mục tiêu **một thương hiệu, nhiều chi nhánh, quản trị tập trung**; không tự chuyển thành sàn marketplace.
- Mọi thao tác nghiệp vụ cần đủ **vai trò + phạm vi cơ sở + quyền sở hữu đối tượng + trạng thái hợp lệ**. Kiểm tại service trước khi đọc dữ liệu nhạy cảm, sửa hoặc trả replay idempotency; không chỉ ẩn nút UI.
- Nhân viên thiếu assignment hợp lệ phải bị từ chối. Không lấy `branchId=null`, `branchId=1` hoặc chi nhánh đầu tiên làm cách né kiểm quyền.
- Bàn/QR xác định cơ sở. Quét QR, xem menu hoặc thêm giỏ không tự mở phiên. Phiên chỉ mở khi đơn đầu tiên được tạo thành công hoặc thu ngân đúng quyền mở theo nghiệp vụ có sẵn.
- Giữ trải nghiệm giỏ bàn hiện có. Bổ sung bảo vệ/quản lý giỏ tài khoản online mà không biến mọi giỏ thành cùng một giỏ hoặc làm lẫn lượt khách.
- Giữ chu trình thu ngân xác nhận → bếp bắt đầu → bếp hoàn thành. Sơ chế nguyên liệu theo mẻ khác pha chế một đơn; không tự bắt mỗi đơn tạo một mẻ sơ chế.
- Không tự thêm điều kiện phải trả tiền trước khi bếp xử lý. Với trạng thái CHO_THANH_TOAN của đơn nhân viên, đối chiếu SSOT và UI hiện tại trước khi thay đổi; ghi rõ nếu phải thống nhất hợp đồng trạng thái.
- Chỉ trừ nguyên liệu khi hoàn thành đơn; dùng snapshot đã chốt khi đặt. Trừ kho, ghi sổ, đổi trạng thái và audit phải cùng transaction. Thiếu một nguyên liệu phải rollback toàn bộ.
- Nguồn hiện tại có khác biệt mô tả vị trí tiêu hao: raw ở KHO, SOCHE ở BEP, còn tài liệu có chỗ nói mọi nguyên liệu ở BEP. Xác minh và chốt policy theo nghiệp vụ hiện hành, thống nhất tài liệu/code/test; không âm thầm chuyển tồn hay dựa vào fallback vị trí để che lỗi.
- Món A dùng B,C thì B,C phải có trong danh mục nguyên liệu và kho đúng chi nhánh/vị trí. Raw phải có chứng từ nhập; SOCHE có mẻ và đầu vào truy ngược được về nhập raw. Không tạo tồn hoặc lịch sử mua hàng giả để hợp thức hóa dữ liệu.
- Giá đang dùng chung toàn chuỗi: giữ nguyên, trừ khi có yêu cầu đã được duyệt về giá chi nhánh. KinhDoanh và ConPhucVu theo cơ sở phải được xét ở backend.
- Tách trạng thái pha chế, thanh toán và hoàn tiền. Khách bấm “đã chuyển khoản” chỉ tạo thông báo; thu ngân xác nhận mới ghi thu. Hủy đơn không xóa Payment; hoàn tiền không tự nhập lại nguyên liệu đã pha.
- Bán chạy chỉ tính số lượng món từ đơn hoàn thành và đã tất toán, theo cơ sở đang phục vụ. Review chỉ CUSTOMER sở hữu món đã mua đủ điều kiện, 1–5 sao, nội dung ít nhất50 ký tự sau trim, một review/dòng đơn.
- Không mở rộng sang Shipper, VNPAY/MOMO, chiết khấu marketplace hay các mục rubric khác ngoài phạm vi F01–F26 chỉ để tăng số tính năng. Nếu F13 cần tách phương thức nhận hàng, làm phần dữ liệu/UI tối thiểu cần thiết, không tuyên bố đã có giao hàng end-to-end.

### 3. Giới hạn thao tác dữ liệu và triển khai

- Không chạy `DROP DATABASE`, `TRUNCATE`, xóa sạch bảng, script reset hoặc seed ghi đè trên database thật `milktea`.
- Không thay MaCN thành nullable, bỏ FK/CHECK/unique, hoặc đặt default CN01 để làm hết lỗi insert.
- Không thay migration đã áp dụng vào môi trường dùng chung chỉ để bỏ lỗi. Thêm migration tiếp theo, có version/đối soát; baseline demo mới được chuẩn hóa riêng.
- Trước thay đổi dữ liệu có sẵn: có pre-check, bản sao/backup theo quy trình dự án, kế hoạch rollback và post-check. Kiểm thử ghi dữ liệu trong DB test/demo tách biệt, không dùng dữ liệu thật làm fixture tranh chấp.
- Dữ liệu thiếu nguồn lịch sử phải được báo cáo là ngoại lệ cần đối soát. Không đoán chi nhánh hoặc bịa chứng từ, ngày nhập, người lập, giá vốn quá khứ.
- Không in secret, OTP thật, JWT, mật khẩu hoặc chuỗi kết nối nhạy cảm vào báo cáo/log. Production thiếu secret bắt buộc thì fail fast; cấu hình demo tách riêng.
- Không tự push/merge/deploy hoặc sửa cấu hình môi trường từ xa. Công việc hiện tại là chuẩn bị bản sửa và bằng chứng có thể review trong workspace.

### 4. Quy trình thực hiện và thứ tự phụ thuộc

**Bước A — Kiểm chứng nhanh đủ26 mục:** đối chiếu code/schema/UI hiện tại, lập bảng ảnh hưởng; ghi rõ bug đã xác nhận bằng code, lỗi đã tái hiện runtime và rủi ro chưa tái hiện. Chỉ thử đủ để quyết định bản sửa; không kéo dài audit thay cho triển khai.

**Bước B — Sửa nền tảng:** F01,F02,F03,F06,F07,F08,F09,F22; kết hợp phần schema/branch bắt buộc của F04,F05,F20,F26. Có thể xử lý chéo khi phụ thuộc, không sửa tuần tự theo số một cách máy móc.

**Bước C — Sửa kho và công thức:** F04,F05,F14,F19,F20, cùng phần idempotency nhập/chuyển/sơ chế/kết ca ở F21.

**Bước D — Nối catalog và giao diện:** F10,F11,F12,F13,F18,F23,F26; sửa đủ Controller→Service→Repository/DB→DTO→JS/template.

**Bước E — Tiền/voucher/báo cáo:** F15,F16,F17,F24; kiểm retry, tranh chấp, đơn0đ và tính nhất quán số liệu.

**Bước F — Ổn định và nghiệm thu:** F21,F25, hồi quy các mục đã sửa, migration trên DB demo, hai cơ sở có cùng nguyên liệu và tên bàn, rồi báo cáo bàn giao.

Sau mỗi nhóm thay đổi, chạy kiểm tra phù hợp rồi tiếp tục phần còn lại. Không dừng chỉ để xin xác nhận những chỉnh sửa thông thường đã được yêu cầu. Chỉ đặt câu hỏi khi có lựa chọn nghiệp vụ ảnh hưởng đáng kể đến quyền/tiền/dữ liệu mà không có căn cứ quyết định; nêu lựa chọn cụ thể và tiếp tục phần độc lập.

Đặc biệt với F03 và F16, không tự thêm PIN bắt buộc/duyệt tham gia phiên hay quy trình hai bước duyệt–thực chi nếu tài liệu hiện tại chưa quy định. Trước hết chặn các đường truy cập chắc chắn trái quyền; nêu rõ thiết kế tham gia phiên và ngữ nghĩa hoàn tiền được chọn, hoặc để OPEN_DECISION nếu thực sự cần người phụ trách chốt. Không dùng trạng thái OPEN_DECISION để đánh dấu hoàn tất.

### 5. Yêu cầu kỹ thuật xuyên suốt

**Phân quyền:** gom kiểm tra lặp vào policy dùng chung, không ép tên class nếu repo có sẵn cơ chế tương đương. ADMIN có quyền toàn chuỗi theo ma trận đã duyệt; MANAGER/CASHIER/KITCHEN bị scope; CUSTOMER có account/session/capability hợp lệ. Object ID, query branchId, alias endpoint và replay cũng phải kiểm.

**Kho:** mọi API/service/repository vận hành dùng khóa `(MaCN,MaNL,ViTri)`; chứng từ và sổ gán đúng chi nhánh. Dòng sổ có nguồn, người lập, trước/delta/sau, thời gian. Đảm bảo lượng tồn khớp tổng sổ; khóa các tài nguyên theo thứ tự ổn định để giảm deadlock.

**Idempotency:** thống nhất scope actor/capability+branch+action, hash payload chuẩn hóa đầy đủ. Cùng key/cùng nội dung trả kết quả cũ sau kiểm quyền; khác nội dung trả409. Không trả dữ liệu người khác khi đoán được key. Xử lý tranh chấp unique một cách xác định; không nuốt exception rồi mặc định thành công. Không để TTL hết mà fallback key bỏ kiểm payload/quyền. Mỗi thao tác ghi sổ/thu/hoàn có chiến lược rõ, không ép mọi endpoint đọc phải dùng key.

**Database:** kiểm FK/CHECK/unique/index và mapping; SQL Server nullable UNIQUE cần xử lý theo hành vi thực, không dựa vào H2. Backfill createdAt/branch phải có nguồn hoặc ghi rõ giá trị migration, không giả đó là lịch sử thật.

**Giao diện:** giữ Bootstrap/layout hiện có, chống double submit, lỗi/rỗng/loading rõ ràng; cập nhật CSRF, route, payload và subscribe đồng bộ. Không coi “API đã có” là “người dùng đã sử dụng được”.

**Realtime:** business event sau commit, kênh nghiệp vụ có scope; mất quyền hoặc đóng phiên thì kết nối/subscription không tiếp tục rò dữ liệu. Bổ sung retry/outbox khi có nhu cầu và bằng chứng rủi ro; không mở rộng hạ tầng quá phạm vi cần thiết.

### 6. Danh sách26 phát hiện cần xử lý

Phần dưới là danh sách audit gốc. Các mô tả “chưa thấy”, “có thể”, “nguy cơ”, “chưa kiểm chứng” phải được giữ đúng mức độ chắc chắn khi bạn kiểm tra lại.


Quy ước: **P0** = chặn nghiệm thu an toàn; **P1** = sai nghiệp vụ/chức năng cần hoàn thiện; **P2** = chất lượng và vận hành. “Xác nhận tĩnh” nghĩa là nhìn thấy đường đi trong code, chưa gửi request trên server thật.

#### F01 — P0 — Đơn hàng chưa cưỡng chế quyền chi nhánh ở các mutation

**Bằng chứng:** `service/order/OrderService.java`: `confirm` dòng 429, `start` 468, `complete` 515 chỉ khóa đơn, xét trạng thái rồi xử lý; không đối chiếu chi nhánh đơn với phân công của cashier/kitchen. Controller truyền ID người thao tác chứ không truyền ngữ cảnh quyền đầy đủ. `createCounterOrder` 231–249 nhận branchId từ request và fallback `1L`; không bắt buộc actor là CASHIER hay `canAccessBranch`.

**Tái hiện cần làm:** cashier CN01 xác nhận mã đơn CN02; kitchen CN01 start/complete đơn CN02; cashier CN01 tạo counter order gửi branchId CN02. Với API alias `/api/v1/orders/counter` đang permitAll, còn có đường tạo đơn quầy từ Guest/User mà không qua kiểm tra vai trò quầy tại service.

**Ảnh hưởng:** tác động chéo đơn, kho và thông báo giữa cơ sở. Lọc danh sách theo branch ở giao diện không bảo vệ request theo ID.

**Sửa/đạt:** policy chung yêu cầu role đúng + chi nhánh được phân công + trạng thái hợp lệ; kiểm tra trước mọi read/mutation/replay. Không có branch hợp lệ thì từ chối, không mặc định CN01. API tạo quầy chỉ CASHIER; khách không tại bàn dùng ONLINE.

#### F02 — P0 — Giỏ phiên bị hở cho anonymous và CUSTOMER không sở hữu phiên

**Bằng chứng:** `controller/CartController.java`, `resolveAndVerifySessionId`: chỉ kiểm tra khi actorType là TABLE_GUEST; actor null hoặc CUSTOMER có thể cung cấp sessionId bất kỳ rồi gọi service. Hai route `/table-sessions/**` và `/customer/**` được permitAll. `SessionCartService` không nhận actor để kiểm tra lần nữa.

**Tái hiện:** anonymous GET/PUT `/api/v1/table-sessions/{phiên-người-khác}/cart`; CUSTOMER không ngồi bàn đó gửi cùng request.

**Sửa/đạt:** mọi loại actor đều phải có quyền phiên cụ thể; staff đúng chi nhánh với quyền hỗ trợ, hoặc token phiên hợp lệ, hoặc membership tài khoản–phiên rõ ràng. Anonymous không có token luôn 401/403; không ghi gì vào DB.

#### F03 — P0 — Tạo thêm đơn vào phiên chỉ dựa trên expectedSessionId

**Bằng chứng:** `OrderService.createTableOrder` 105–148 khóa bàn và so activeSessionId, nhưng không kiểm tra actor/token có quyền phiên đó. `TableController.getTables` public `/tables` trả QR và activeSessionId; `getSession` public không kiểm tra sở hữu. `TableContextService.readByQr` 44–56 cấp token mới cho bất kỳ ai có QR của bàn đang mở.

**Ảnh hưởng:** người có ID bàn/phiên hoặc QR tĩnh có thể tham gia lượt khách khác, đọc/đổi giỏ hoặc gửi thêm món. QR là locator công khai thì không thể đồng thời coi nó là bí mật bảo vệ phiên.

**Sửa/đạt:** tách QR định danh bàn với quyền tham gia phiên; cơ chế join có token phiên/PIN quay vòng hoặc thu ngân chấp thuận nếu cần bảo vệ người ngồi bàn. Không công khai QR/token/phiên đang mở trong API liệt kê; không tự cấp lại quyền chỉ từ số bàn. Gửi thêm đơn phải kiểm tra capability phiên hiện hành. Quy tắc chọn phải ghi trong SSOT và demo cả trường hợp khách cũ quay lại.

#### F04 — P0 — Sơ chế và kết ca vẫn dùng truy vấn kho của mô hình một quán

**Bằng chứng:** `InventoryService.recordPreparation` 316–377 dùng `findByIdWithLock(materialId, location)` không có branchId; tạo output bằng `new StockId(materialId, BEP)` mặc định branch=1. Entity `PreparationBatch.java` không ánh xạ MaCN dù V1 đã thêm MaCN NOT NULL vào MeSoChe. `recordShiftSettlement` 1395–1423 không gán branch vào settlement, truy vấn tồn thiếu branch. Preview 1346 đọc mọi tồn theo vị trí. `configureThreshold` và một số hàm cũ cũng cần rà soát cùng quy tắc.

**Ảnh hưởng:** khi nhiều chi nhánh có cùng nguyên liệu/vị trí, Optional truy vấn có thể nhận nhiều dòng → NonUniqueResultException; dữ liệu thiếu MaCN bị SQL Server từ chối; fallback mặc định CN01 có nguy cơ ghi sai cơ sở.

**Sửa/đạt:** mọi chứng từ và dòng sổ có chi nhánh; service nhận actor/branch đã xác thực; loại overload kho thiếu branch khỏi đường đi sản xuất. Hai cơ sở cùng có cùng material vẫn sơ chế/kết ca độc lập. Chạy test trên SQL Server với V1, không chỉ mock repository.

#### F05 — P0 — Biến động tiêu hao/sơ chế/kết ca không gán MaCN

**Bằng chứng:** `InventoryService.completeOrderConsumption` 143–148 tạo `StockMovement` không `.branch(...)`; sơ chế 354 và 395, kết ca khoảng 1451 tương tự. V1 dòng 173–179 yêu cầu LichSuKho.MaCN NOT NULL; entity lại khai báo join nullable.

**Ảnh hưởng:** hoàn thành đơn có thể rollback vì không chèn được lịch sử; trên schema lỏng hơn có thể ghi lịch sử không thuộc cơ sở nào, làm sai báo cáo.

**Sửa/đạt:** branch bắt buộc từ chứng từ gốc, entity nullable=false; trừ kho + lịch sử + trạng thái đơn trong cùng transaction. Mỗi snapshot tiêu hao phải có đúng dòng sổ theo chi nhánh. Đừng sửa bằng cách cho MaCN NULL hoặc đặt default=1.

#### F06 — P0 — Đọc/hủy/thu/hoàn/phiên có đường đi xuyên chi nhánh

**Bằng chứng:** `OrderService.getOrdersForCustomer` 825–826 trả `findAll` cho CASHIER/MANAGER; `getOrderForCustomer` 846–847 cho qua chỉ dựa vai trò. `CancellationService.canActorAccessOrder` cho staff qua không xét chi nhánh. `PaymentService.recordReceipt`, `RefundService`, `TableSessionService.openByCashier/closeByCashier` không cưỡng chế scope từ nhân viên. `InventoryController` các GET chỉ ép branch cho MANAGER, CASHIER/KITCHEN vẫn có thể truyền branch khác hoặc bỏ trống để đọc toàn chuỗi. `CancellationController` danh sách thu ngân chỉ ép branch cho MANAGER nên cashier nhìn dữ liệu không được giới hạn đúng.

**Sửa/đạt:** bảo vệ toàn bộ object ID, danh sách và thống kê; service không tin query branchId. Vai trò chưa được phân công phải bị từ chối, kể cả tài khoản đăng nhập hợp lệ. Kiểm tra Manager không có branch không được rơi về phạm vi toàn chuỗi.

#### F07 — P0 — CSRF bị tắt; endpoint CSRF không tạo bảo vệ thực

**Bằng chứng:** `security/SecurityConfig.java:43` disable CSRF. `AuthController.getCsrf` khoảng 112 tạo UUID ngẫu nhiên, không gắn vào CsrfTokenRepository và không có bộ lọc kiểm tra tương ứng. JWT được nhận từ cookie tự động gửi bởi trình duyệt.

**Sửa/đạt:** bật cơ chế CSRF thực của Spring Security cho xác thực cookie và nối header với token repository. Phân tách ngoại lệ thật sự cần cho API/webhook. Không dùng “stateless” hay CORS làm bằng chứng thay thế CSRF. Kiểm chứng POST/PUT/DELETE có cookie nhưng thiếu/sai CSRF bị từ chối, token đúng hoạt động. Tài liệu Spring Security xác nhận ứng dụng stateless dùng cookie vẫn cần xét CSRF: https://docs.spring.io/spring-security/reference/features/exploits/csrf.html .

#### F08 — P0 — Đếm OTP sai có thể rollback, recorder chưa được tích hợp

**Bằng chứng:** `OtpService.verifyOtp` lưu attempts/revoked rồi ném BusinessException trong `@Transactional`; BusinessException extends RuntimeException. `OtpAttemptRecorder` REQUIRES_NEW tồn tại nhưng không được gọi trong OtpService. Test Phase1 kiểm riêng recorder bằng mock, không chứng minh nó được dùng trong luồng xác thực.

**Ảnh hưởng suy luận theo transaction:** số lần sai không được commit nên giới hạn năm lần có thể không có hiệu lực trên DB thật. Cũng cần khóa/atomic update để không mất lượt khi nhiều request đồng thời; token reset một lần cần kiểm tra race.

**Sửa/đạt:** tích hợp recorder hoặc thiết kế transaction cho lỗi xác thực có commit an toàn; tăng attempts nguyên tử; không vô hiệu cơ chế rollback của các nghiệp vụ tiền/kho. Integration test gọi API sai 5 lần, đọc DB bằng transaction khác, lần 6 bị khóa; OTP đúng không dùng lại. Quy tắc rollback mặc định: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html .

#### F09 — P0 — WebSocket chưa cách ly chi nhánh/phiên

**Bằng chứng:** `static/js/common/websocket.js` 91–98 vẫn subscribe `/topic/cashier`, `/topic/kitchen`, `/topic/stock`. Các service còn publish cả global và branch. `WebSocketConfig.authorizeStompSubscribe` cho mọi CUSTOMER hoặc CASHIER/MANAGER subscribe bất kỳ `/topic/table-sessions/{id}` mà không đối chiếu phiên/branch. CONNECT dùng `findByAccountId` thay vì chỉ assignment HOAT_DONG; HTTP và STOMP không đồng nhất kiểm tra emailVerified/phân công. Không thấy cơ chế revalidate JWT/tokenVersion cho kết nối đã mở.

**Sửa/đạt:** bỏ kênh global chứa nghiệp vụ cơ sở; kiểm role + branch + phiên khi subscribe; kiểm assignment còn hiệu lực; thu hồi subscription khi logout/khóa tài khoản/đóng phiên. Lọc sự kiện ở client chỉ là UX, không bảo mật. Test bằng hai tab cơ sở khác nhau và STOMP client cố subscribe trái quyền.

#### F10 — P1 — Menu và tạo đơn chưa thực thi trạng thái bán tại chi nhánh

**Bằng chứng:** `CatalogService.getAvailabilityMap` chỉ dùng ConPhucVu, bỏ KinhDoanh, không có mapping thì default true. Search phân trang toàn bộ sản phẩm trước rồi chỉ gắn cờ availability, không lọc scope ở SQL. `OrderService.createOrderItemsAndSnapshots` chỉ xét Product.active, không gọi BranchProduct. Product detail cũng mặc định cho phép nếu không có mapping. TABLE/COUNTER chưa kiểm tra đầy đủ chi nhánh đóng cửa; ONLINE active check không chặn KHOTONG vì kho tổng cũng HOAT_DONG.

**Tái hiện:** P24 KinhDoanh=0 hoặc P23 ConPhucVu=0 ở CN01; gửi productId trực tiếp vẫn có thể tạo đơn. POST ONLINE branchId của KHOTONG cũng phải được thử.

**Sửa/đạt:** một predicate dùng chung: cơ sở bán lẻ hoạt động + món global active + mapping KinhDoanh + ConPhucVu. Từ chối khi không có mapping nếu chọn mô hình allowlist. Menu, chi tiết, tìm kiếm, size, topping, giỏ và checkout dùng cùng policy. Giá hiện dùng chung toàn chuỗi là lựa chọn được ghi trong BUSINESS_RULES, không coi đó là lỗi tự thân.

#### F11 — P1 — Tìm kiếm/top 20 có API nhưng UI nối chưa đúng

**Bằng chứng:** `templates/customer/menu.html` 1474–1519 tìm kiếm/lọc DOM đã render toàn bộ món. Các tab 1149–1155 dùng `best-seller`, `top-rated`, `most-favorited`; request 1541 không có branchId, còn CatalogService switch nhận `best_seller`, `top_rated`, `most_favorited`, khiến giá trị không khớp rơi về best_seller. UI chỉ lọc membership, không sắp theo thứ tự ranking API. Không thấy tab newest tương ứng bốn danh sách đúng rubric, chưa có phân trang/lazy loading danh sách nổi bật. `newest` backend sort ID thay vì ngày tạo; Product chưa có cột ngày tạo món.

**Sửa/đạt:** thống nhất enum/type; truyền branch xác định từ QR/ngữ cảnh online; render theo thứ tự API; tìm kiếm và khoảng giá thực hiện tại backend; top tối đa 20 rồi phân trang/lazy load trong tập top. Thêm createdAt và tie-break id; không tự lấp món chưa bán vào “bán chạy” mà không giải thích là gợi ý bổ sung.

#### F12 — P1 — Trang Admin orders gọi route không khớp

**Bằng chứng:** `OrderController` có class `/api/v1`, method `@GetMapping({"/cashier/orders", "/api/v1/admin/orders"})` → route thực thứ hai là `/api/v1/api/v1/admin/orders`. `templates/admin/orders.html:952` gọi `/api/v1/admin/orders`.

**Sửa/đạt:** method mapping là `/admin/orders`; kiểm tra cả ADMIN/MANAGER, filter CN và response. Có HTTP test route thật; mock gọi service không phát hiện lỗi này.

#### F13 — P1 — “Đặt online” backend và giao diện chưa nối thành một luồng

**Bằng chứng:** `static/js/customer/customer.js:365–402`, không có bàn thì submit `/api/v1/orders/counter` với items/voucher, không dùng `/orders/online`, không branchId/người nhận/địa chỉ. API ONLINE đã có nhưng địa chỉ chỉ ghép chuỗi vào Order.note. DTO chưa có phương thức nhận hàng tách PICKUP/DELIVERY.

**Sửa/đạt:** checkout chọn cơ sở, nhận tại quầy hay giao; gọi endpoint ONLINE với dữ liệu được validate. Counter chỉ cho nhân viên. Tách trường người nhận/địa chỉ snapshot khỏi ghi chú 500 ký tự; kiểm điện thoại, độ dài, khu vực giao và phí server. Bổ sung giỏ khách theo tài khoản trong DB; không ép đơn online có phiên bàn.

#### F14 — P1 — Công thức có fallback hardcode và có thể thiếu nguyên liệu

**Bằng chứng:** `FixedRecipeService` chứa công thức theo tên+size; `OrderService` 608–679 fallback, `findByName(...).ifPresent(...)` bỏ qua nguyên liệu không tìm thấy; không có công thức/fallback vẫn có thể tạo đơn với snapshot rỗng hoặc một phần. Inventory chỉ chặn snapshot hoàn toàn rỗng, không biết món nào thiếu thành phần. Topping gửi ID chưa xác minh nó thực sự là sản phẩm loại TOPPING hay đủ điều kiện ở chi nhánh.

**Sửa/đạt:** công thức DB là nguồn chuẩn; món chưa có định mức đầy đủ thì không kích hoạt bán. Không bỏ qua nguyên liệu bị thiếu. Mỗi loại nguyên liệu của từng công thức phải tồn tại, đơn vị hợp lệ, có số dư đúng location; nguyên liệu thô có chứng từ nhập, thành phẩm có phả hệ mẻ sơ chế và nhập nguyên liệu đầu vào. Tách product family–variant và loại sản phẩm rõ ràng. Thiết lập chính sách định mức bao bì/topping; không dùng tên món làm khóa nghiệp vụ.

#### F15 — P1 — Paid + chờ xác nhận có thể bị hủy trực tiếp mà chưa tạo khoản hoàn

**Bằng chứng:** `CancellationService.requestCancellation` chấp thuận trực tiếp mọi CHO_XAC_NHAN, không phân biệt isPaid trong nhánh đầu. Hệ thống cho phép thu tiền ở CHO_XAC_NHAN, nên trạng thái này có thể đã thanh toán. Helper `createRefundIfPaid` tồn tại nhưng không được gọi. Thu ngân chỉ cần yêu cầu hoàn sau đó theo một luồng khác; không có đảm bảo liền mạch ở nhánh này.

**Sửa/đạt:** chính sách xét cả tình trạng tiền; hủy không xóa Payment. Bảo đảm đơn hủy có khoản thu xuất hiện trong danh sách hoàn với số tiền phải hoàn; nếu chủ ý hai bước, tạo obligation hoàn rõ ràng và cảnh báo tồn đọng. Không tính hủy là đã hoàn tiền. Test đầy đủ chờ xác nhận đã thu tiền, hủy trước bếp và công nợ hoàn.

#### F16 — P1 — Hoàn chuyển khoản có lỗi retry và lệch ngữ nghĩa duyệt/thực chi

**Bằng chứng:** `RefundService.requestBankRefund` khi existing TU_CHOI không tái sử dụng bản ghi mà insert mới; schema unique MaDH/MaTT/MaYeuCau sẽ xung đột. `CancellationController` alias `/cancellation-requests/{id}/refund-bank-request` và `/refund-cash` vẫn truyền `{id}` như orderId. `decideRefund` approved chuyển thẳng DA_HOAN, còn `recordRefundPayment` yêu cầu DA_DUYET; hai đường đi không cùng mô hình. `/api/v1/admin/**` cho MANAGER và endpoint decision không chặn riêng ADMIN.

**Sửa/đạt:** thống nhất ID của endpoint; chọn mô hình duyệt rồi thực chi hoặc thao tác xác nhận đã thực chi có bằng chứng. MANAGER không được hoàn chuyển khoản nếu SSOT giao ADMIN. Hỗ trợ yêu cầu lại sau từ chối bằng cập nhật/revision có lịch sử; chống chi hai lần; có tham chiếu thực chi.

#### F17 — P1 — Voucher còn khe hở hạn mức khách, số tiền 0 và snapshot

**Bằng chứng:** tổng lượt tăng bằng UPDATE nguyên tử là tốt; nhưng hạn mức mỗi khách được COUNT trước increment, không khóa theo khách. Guest accountId null bỏ qua hạn mức cá nhân. Order fingerprint không có voucherCode nên cùng key đổi mã chưa được phân biệt. `validateVoucher` preview chỉ tính voucher từ subtotal, trong khi applyVoucher cộng với storeDiscount nên preview có thể lệch số thực. Hóa đơn cho tổng 0, nhưng `PaymentService.recordReceipt` từ chối `<=0`; review và top seller yêu cầu tồn tại Payment nên đơn miễn phí có thể không đánh giá/không vào thống kê. Invoice mới snapshot mã và số tiền voucher, chưa đầy đủ type/value/cap voucher để giải thích khi voucher bị sửa.

**Sửa/đạt:** quy định Guest có được dùng mã giới hạn theo khách không; khóa/atomic quota cá nhân; snapshot đầy đủ; fingerprint gồm toàn bộ trường nghiệp vụ. Định nghĩa “đã tất toán” cho hóa đơn 0 bằng payment status/settlement record, không giả làm giao dịch thu tiền dương. Không làm tổng tiền âm; preview dùng cùng hàm tính cuối.

#### F18 — P1 — Quản lý cơ sở còn quyền tác động danh mục toàn chuỗi

**Bằng chứng:** `AdminController.updateCategory` chỉ chặn MANAGER đổi tên; vẫn truyền active/allowTopping sang `AdminService.updateCategory` sửa Category dùng chung. Upload avatar `/admin/accounts/{id}/avatar` không kiểm scope dù các API sửa account khác có kiểm. Manager không branchId có thể lọt logic coi branchId=null là quản trị toàn hệ thống ở nhiều service.

**Sửa/đạt:** cấu hình toàn chuỗi ADMIN-only tại backend; thay đổi cơ sở ghi bảng branch riêng. Avatar dùng cùng policy xem/sửa nhân sự. Manager thiếu assignment bị chặn, không được tăng quyền bằng giá trị null.

#### F19 — P1 — Chênh lệch kiểm kê chưa có quy tắc đối soát đầy đủ

**Bằng chứng:** `InventoryService.recordShiftSettlement` 1427–1447 ghi actualQty/retainedQty nhưng trừ kho chỉ discardedQty. Không thấy cưỡng chế quan hệ actual=discarded+retained tại service; DTO con chưa được cascade @Valid đầy đủ. Nếu tồn sổ 100, đếm thực 90, bỏ 10, giữ 80 thì code lưu tồn 90, không phải lượng giữ lại 80.

**Sửa/đạt:** thống nhất actual là trước hay sau loại bỏ. Ghi hai bút toán rõ ràng: điều chỉnh kiểm kê actual−book và loại bỏ −discarded; tồn cuối=retained. Mỗi lệch có lý do/phê duyệt theo quyền; không âm và sổ khớp tồn. Thử số âm, null, trùng dòng, thiếu lý do và lệch tổng.

#### F20 — P1 — Bộ schema/seed/migration không tái tạo được một trạng thái chuẩn bằng một lệnh an toàn

**Bằng chứng:** `database/1_schema.sql` đầu file DROP DATABASE milktea; không phải migration không phá dữ liệu. `1_schema.sql`, `2_seed.sql`, `full_demo_data.sql` không chứa MaCN; V1 thêm NOT NULL nhưng full_demo_data vẫn insert PhieuNhap, MeSoChe, DonHang, LichSuKho thiếu MaCN. V1 gán tồn CN02=50 mọi dòng; V4 gán tồn tổng=1000 mọi nguyên liệu thô mà không tạo chứng từ/lịch sử nguồn. `2_seed.sql` MERGE còn reset mật khẩu/trạng thái tài khoản có sẵn. Không có Flyway/Liquibase dependency; không thấy runner thực thi thư mục database/migrations tự động.

**Sửa/đạt:** tách baseline tạo mới, migration có version/checksum, fixture demo độc lập và dữ liệu production. Không chạy file DROP trên database hiện có. Tạo bản demo mới tên riêng; tồn bắt đầu 0 rồi phát sinh chứng từ. Mỗi lần nạp có tiền kiểm/hậu kiểm, rollback khi lỗi, không sửa lịch sử thật. Không giả lịch sử nhập để hợp thức hóa tồn đang có.

#### F21 — P2 — Tăng quy mô và vận hành cần được kiểm chứng

Nhiều danh sách tải findAll rồi stream lọc/phân trang; mapping một đơn gọi invoice/items/payment/refund/cancel/topping nhiều lần → nguy cơ N+1 và chậm khi có nhiều giao dịch. Giỏ topping lưu chuỗi ID thiếu FK. Idempotency hiện hỗ trợ một phần: import dùng UUID server thay vì key client; chống lặp order có fallback theo key bỏ qua kiểm payload khi dedup record không còn; fingerprint ghép thiếu fields; record được tạo sau mutation nên request đồng thời có thể gặp unique conflict thay vì replay. Chưa có bằng chứng tải thật, restore thành công, cảnh báo lỗi, retry/outbox cho event mất sau commit.

Cần phân trang/filter ở DB, index có MaCN, đo query; giữ invariant một transaction; key có scope actor+branch+action, lưu trạng thái xử lý/kết quả, xử lý tranh chấp và TTL. Với chuỗi nhỏ chưa cần microservices; ưu tiên monolith rõ module, backup/restore và audit trước.

#### F22 — P1/P2 — Cookie và cấu hình an toàn chưa nối đầy đủ

`AuthController.login` tạo Cookie HttpOnly nhưng không thấy setSecure/SameSite; cấu hình app.jwt.cookie-secure=true trong prod chưa tự áp vào cookie do controller không đọc thuộc tính này. JWT/OTP có secret fallback cố định; dev có mật khẩu DB mặc định. Đây là cấu hình dev, nhưng prod phải bắt buộc nhận secret riêng và không chạy âm thầm bằng fallback. Login chưa thấy giới hạn thử theo tài khoản/IP. Đồng bộ TTL, Secure, SameSite, phạm vi cookie và chính sách đăng xuất nhiều thiết bị; thử HTTPS thực.

#### F23 — P1 — Media đánh giá chưa có chuỗi upload được kiểm chứng

`ProductReviewService.validateMediaFile` có logic kiểm ảnh/video nhưng controller review chỉ nhận JSON URL; không thấy endpoint upload review gọi validator đó. URL client gửi được lưu thẳng; video 50MB ở hàm validate mâu thuẫn multipart global 5MB/file, 6MB/request nếu sau này nối upload. Cần upload có quyền, kiểm file thật, giới hạn số media, URL HTTPS thuộc storage được phép; không tuyên bố có video upload chỉ vì enum VIDEO tồn tại.

#### F24 — P1 — Dashboard và menu dùng hai định nghĩa bán chạy khác nhau

**Bằng chứng:** `OrderItemRepository.findBestSellerProductIds` yêu cầu HOAN_THANH và tồn tại Payment; nhưng `findTopSellingProducts` dùng ở `ReportService.getDashboardTopProducts` chỉ loại DA_HUY, vẫn tính đơn mới/đang nấu/chưa thu. Query dashboard gộp theo tên, query menu theo product variant. Cùng nhãn “bán chạy” có thể cho số khác nhau dù cùng cơ sở và khoảng dữ liệu.

**Sửa/đạt:** chốt metric và đơn vị nhóm. Nếu dashboard đếm nhu cầu gọi món thì đổi nhãn cho đúng; nếu là bán thực thì dùng chung điều kiện hoàn thành+tất toán và cùng khoảng thời gian. Thử H31/L02/L06 để chứng minh không tính sai. Mốc khoảng ngày nên dùng [start,end) nhất quán; một số repository `Between` lấy cả end nên cần test giao dịch đúng 00:00 ngày kế tiếp.

#### F25 — P1/P2 — Một số DTO/quan hệ lazy cần kiểm tra trong HTTP thực

`TableSessionService.getSessionResponse` không có transaction đọc bao ngoài nhưng truy cập session.table.branch; `TableController.getTables` và page controller cũng dùng các quan hệ lazy ngoài service, trong khi open-in-view=false. Đây là **nguy cơ cần tái hiện**, chưa kết luận mọi trang chắc chắn lỗi. ProductRepository đã có EntityGraph category ở nhiều query nên không gom tất cả lazy access thành một lỗi chung.

Tạo HTTP integration test với persistence context thật để bắt LazyInitializationException; map DTO trong service transaction hoặc query DTO/fetch join rõ ràng. SMTP hiện được gọi đồng bộ bên trong transaction register/reset, dù comment MailService mô tả tách transaction: cần đo thời gian giữ connection và xử lý lỗi gửi mail, cân nhắc after-commit/outbox với trạng thái gửi lại rõ ràng.

#### F26 — P1 — Quản trị vòng đời chi nhánh còn thiếu

**Bằng chứng:** `BranchController` và `BranchService` hiện có liệt kê/đọc cơ sở, bật tắt availability món và tự tạo KHOTONG; chưa thấy CRUD quản trị mở cơ sở mới, cập nhật địa chỉ/giờ mở cửa, ngừng cơ sở hay UI phân công/điều chuyển nhân viên đầy đủ. `AdminService.createStaffAccount` tạo assignment khi có branch hợp lệ, nhưng cập nhật account chưa tạo một quy trình điều chuyển có lịch sử rõ ràng. Khi mã branch không tồn tại, create staff còn có thể tạo account mà không có assignment thay vì trả lỗi.

**Sửa/đạt:** thêm quản trị chi nhánh cho ADMIN và quản lý assignment có ngày hiệu lực/trạng thái, từ chối branch sai; việc đóng cơ sở phải xử lý đơn/phiên đang mở và không xóa lịch sử. Ngăn tạo đơn khi cơ sở đóng và không cho biến kho tổng thành cửa hàng chỉ vì nó cũng là một row ChiNhanh. Bộ fixture cung cấp hai cơ sở để kiểm cách ly, nhưng mở cơ sở thứ ba là ca nghiệm thu riêng cần có giao diện/API quản trị.


## 7. Kiểm thử nghiệm thu bắt buộc

Tạo hoặc cập nhật regression test có ý nghĩa cho các lỗi được xác nhận. Không xóa test hợp lệ, hạ invariant, tắt Security/CSRF hoặc đổi expected sai chỉ để test xanh.

### 7.1. HTTP và quyền

- Anonymous/User không có quyền phiên không đọc/ghi giỏ phiên bất kỳ.
- Khách không đặt thêm vào phiên người khác chỉ bằng tableId/expectedSessionId.
- CASHIER CN01 không tạo quầy, xác nhận, thu, hủy, hoàn hoặc đóng phiên của CN02.
- KITCHEN CN01 không đọc kho, start/complete, sơ chế, xem mẻ hoặc kết ca của CN02.
- MANAGER CN01 không sửa cấu hình toàn chuỗi hoặc avatar nhân viên ngoài scope; không branch không được hưởng quyền toàn chuỗi.
- ONLINE không tạo được tại KHOTONG hoặc cơ sở đóng.
- Thử route thật `/api/v1/admin/orders`, cả ADMIN/MANAGER và các filter.
- Có cookie nhưng thiếu/sai CSRF phải bị từ chối; token đúng hoạt động. Cookie prod có Secure/HttpOnly/SameSite phù hợp.
- HTTP và STOMP dùng cùng assignment/active/emailVerified; subscription trái phiên/cơ sở bị chặn; logout/khóa/đóng phiên thu hồi quyền đang mở.

### 7.2. OTP và transaction

- Sai OTP5 lần được commit; đọc DB từ transaction khác; lần6 bị khóa.
- OTP hết hạn/đã dùng/mã cũ sau resend bị từ chối.
- Request OTP/reset đồng thời không vượt lượt hoặc dùng một token nhiều lần.
- Không vô hiệu rollback chung của tiền/kho để giải quyết lỗi OTP.

### 7.3. Kho, định mức và dữ liệu

- Hai cơ sở cùng có một nguyên liệu/cùng location: không lỗi query nhiều dòng và không tiêu hao nhầm cơ sở.
- Mọi MeSoChe/KetCaKho/LichSuKho mới có MaCN đúng và chứng từ nguồn hợp lệ.
- Sửa công thức sau khi đặt không đổi snapshot của đơn cũ; món thiếu định mức không được bán với snapshot rỗng/một phần.
- Thiếu một thành phần: complete rollback toàn bộ, đơn không hoàn thành, tồn khác không bị trừ.
- Hai complete/nhập/chuyển/sơ chế/kết ca retry không ghi sổ hai lần.
- Book100, actual90, discarded10, retained80: nếu actual là đếm trước loại bỏ thì tồn cuối80; có−10 điều chỉnh và−10 loại bỏ, không lưu90 hoặc tự bỏ chênh lệch.
- Không còn tồn “tự sinh” của fixture; tổng biến động từ số dư đầu có chứng từ khớp Stock; mỗi dòng sau=trước+delta; dữ liệu cũ thiếu nguồn được báo cáo riêng.
- Migration chạy từ database bản cũ và trên baseline mới ở môi trường test, bảo toàn số lượng lịch sử theo kế hoạch. Chạy lại migration/seed theo chiến lược version không tạo trùng.

### 7.4. Tiền, hủy, hoàn, voucher và thống kê

- Đơn CHO_XAC_NHAN đã thu rồi hủy vẫn có Payment và nghĩa vụ hoàn được theo dõi.
- Chọn đúng cancellationId/orderId; hoàn lại sau TU_CHOI không lỗi unique; không chi hai lần.
- Phân biệt duyệt và đã thực chi; DA_HOAN phải có bằng chứng/người/thời điểm/tham chiếu theo quy trình đã chốt.
- Một voucher còn1 lượt, hai khách tranh: một thành công. Một khách limit1 gửi hai request đồng thời: không vượt quota.
- Guest và quota cá nhân theo policy rõ ràng; preview và checkout cùng số tiền.
- Đổi voucherCode/địa chỉ/topping trong cùng key bị409; không trả replay trái quyền.
- Đơn miễn phí0đ tất toán hợp lệ, không tạo khoản thu dương giả; review/top seller dùng định nghĩa tất toán thống nhất.
- Bán chạy menu và dashboard cùng metric/scope/đơn vị nhóm đã công bố; không tính đơn mới/chưa hoàn tất/chưa tất toán/hủy.
- Biên thời gian `[start,end)` không đếm giao dịch đầu ngày kế tiếp hai lần; thực thu và thực hoàn theo đúng mốc tiền.

### 7.5. Giao diện, media và vận hành

- Tìm/lọc/khoảng giá và phân trang dùng backend; bốn tab đúng enum, branch và thứ tự API, tối đa20 món/tập.
- Quầy chỉ nhân viên; online gọi ONLINE, có cơ sở, thông tin nhận hàng hợp lệ; giỏ tài khoản online được lưu DB mà không làm mất giỏ bàn.
- Món/topping/size không bán tại cơ sở bị chặn kể cả gửi request trực tiếp.
- Upload review đi qua endpoint thật, kiểm quyền/file/magic bytes/dung lượng/số media/URL; cấu hình multipart nhất quán.
- Kiểm HTTP/Thymeleaf với open-in-view=false để tái hiện lazy error; không bật open-in-view=true chỉ để che lỗi.
- Đo query/pagination cho danh sách lớn; chỉ tối ưu theo vấn đề thực đo, không đổi kiến trúc.

Các test transaction, FK/CHECK/unique, SQL Server syntax và tranh chấp phải được xác minh trên SQL Server test khi có điều kiện. Mock unit test dùng để kiểm logic, không được gọi là integration/E2E. Nếu dùng H2 để phản hồi nhanh, ghi rõ giới hạn tương thích.

Nếu có `02_Du_lieu_mau_kiem_thu_demo_MilkTea.md`, dùng fixture và truy vấn đối soát trong đó; không giả định file này có trong repo nếu chưa được cung cấp. Nếu thiếu, tự tạo fixture test tối thiểu cùng nguyên tắc: hai cơ sở, cùng tên bàn, cùng nguyên liệu, nhập→chuyển→sơ chế→đơn→thu/hủy/hoàn, số dư đối soát được.

## 8. Cách báo cáo tiến độ và bàn giao

Tạo tài liệu theo dõi, ưu tiên `docs/AUDIT_FIX_TRACKER.md` và `docs/AUDIT_FIX_HANDOFF.md` nếu repo chưa có owner tương đương. Không tạo nhiều bản sao khác nhau của cùng quy tắc.

Bảng theo dõi phải đủ26 dòng F01–F26, gồm:

| Mã | Mức độ | Hiện trạng xác minh | Nguyên nhân | File/phương thức sửa | Migration | Test/bằng chứng | Trạng thái | Còn tồn đọng |
|---|---|---|---|---|---|---|---|---|

Báo cáo cuối cần có:

1. Tóm tắt thay đổi thực tế và hành vi trước/sau.
2. F nào đã sửa, đã có sẵn bản sửa, chưa tái hiện, bị môi trường chặn hoặc chờ quyết định. Không gộp tất cả thành DONE.
3. Danh sách file sửa và lý do; không liệt kê secret hoặc dữ liệu cá nhân.
4. Migration mới, tiền kiểm/hậu kiểm, điều kiện áp dụng, backup/rollback; lệnh chạy trên test/demo và cách kiểm đúng DB trước khi chạy.
5. Lệnh kiểm thử đã chạy, kết quả thực, số test pass/fail/skipped, tên môi trường/database và giới hạn. Không lấy báo cáo125 test trong ZIP làm kết quả mới.
6. Cách chạy lại demo hai cơ sở, truy vấn tiền/kho và ca trái quyền.
7. Quyết định nghiệp vụ đã chốt; mục nào thực sự cần người phụ trách xác nhận, nêu phương án cụ thể và tác động.
8. Cập nhật SSOT/traceability/progress theo thực tế; không khai các chức năng chưa có như Shipper/cổng thanh toán là hoàn thành.

Nếu thiếu Java21/Maven/dependency/SQL Server/SMTP/Cloudinary: nêu chính xác phần bị chặn, không kết luận code đã chạy. Tiếp tục các thay đổi và kiểm tra độc lập làm được, cung cấp cách tái kiểm trên môi trường đủ điều kiện.

## 9. Điều kiện hoàn thành

Chỉ kết thúc với tuyên bố hoàn tất khi đủ F01–F26 đã có trạng thái và bằng chứng, không còn P0 mở, các thay đổi liên quan tiền/kho/quyền đã được kiểm tra phù hợp và những giới hạn còn lại được nêu rõ. Nếu môi trường hoặc quyết định bắt buộc còn chặn, báo cáo là chưa hoàn tất phần đó; không che bằng việc cập nhật Markdown.

**Bắt đầu ngay: đọc chỉ dẫn dự án, xác định phiên bản và thay đổi hiện tại, kiểm chứng F01–F26 rồi triển khai lần lượt theo phụ thuộc. Đừng dừng ở kế hoạch; thực hiện bản sửa và bàn giao bằng chứng.**
