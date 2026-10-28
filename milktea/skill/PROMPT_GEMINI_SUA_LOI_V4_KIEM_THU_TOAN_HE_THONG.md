# PROMPT GEMINI / ANTIGRAVITY — SỬA BÁO CÁO 04 VÀ KIỂM THỬ TOÀN HỆ THỐNG

Ngày lập: 29/10/2026. Căn cứ: `04_Danh_gia_lai_MilkTea_v4.md`, bản dự án ZIP (4), các yêu cầu phục vụ tại quán đã chốt.

## Cách dùng

Đưa file này và báo cáo 04 vào workspace Antigravity đang chứa dự án. Gửi Agent yêu cầu: **“Thực hiện toàn bộ nhiệm vụ trong file này: sửa source, database, dữ liệu demo và chạy kiểm thử toàn hệ thống. Tiếp tục sửa–chạy lại đến khi đạt điều kiện nghiệm thu; không chỉ lập kế hoạch.”**

Không chép đè toàn bộ prompt vào `GEMINI.md`. Agent đọc chỉ dẫn repo và cập nhật phạm vi tại tài liệu nguồn. Nếu thiếu báo cáo đính kèm, danh sách 16 nhóm và tiêu chí trong prompt này vẫn đủ làm yêu cầu công việc; phải kiểm source thực tế trước khi sửa.

---

## NHIỆM VỤ CHÍNH

Bạn là AI Agent phụ trách sửa lỗi và nghiệm thu end-to-end MilkTea trong workspace hiện tại. Hãy xử lý đầy đủ **V4-01 đến V4-16** của báo cáo 04, đặc biệt các thay đổi database phải được kiểm chứng trên **SQL Server thực dùng riêng cho test**. Sau sửa phải **khởi động ứng dụng thật, chạy toàn bộ API thuộc ứng dụng, kiểm tra tương tác trình duyệt, WebSocket, luồng nghiệp vụ và hồi quy**.

Tôi cần kết quả triển khai và bằng chứng đã chạy, không cần một bản kế hoạch thay cho công việc. Không kết luận “hệ thống hoạt động bình thường” chỉ từ compile, unit test/mock, báo cáo Surefire cũ, SQL PRINT PASS hoặc một vài endpoint trả 200. Không cam kết tuyệt đối không còn lỗi; kết luận dựa trên phạm vi, số ca và môi trường thực đã kiểm.

## 1. Phạm vi cố định

- Chỉ phục vụ **trong quán**: TABLE qua QR gắn bàn có nhãn bàn/chi nhánh; COUNTER do nhân viên được phép tại cơ sở lập.
- Guest chưa quét QR có thể xem menu, nhưng không tự chọn bàn bằng ID để đặt, không tự tạo COUNTER. Quét QR mở ngay menu đúng bàn/cơ sở; xem menu/thêm giỏ không tự mở phiên. Đơn đầu thành công mới mở phiên, trừ thao tác staff mở tay đã được nghiệp vụ hiện hành cho phép.
- Không phát triển ONLINE/giao nhận, AddressBook, Shipper, phí ship, marketplace hay yêu cầu đóng chi nhánh cũ **R19**. Giữ endpoint ONLINE cũ từ chối rõ và legacy read-only nếu cần bảo toàn lịch sử.
- **Đóng phiên bàn khác đóng cơ sở:** quyền phiên, giỏ, đóng bàn, assignment và cách ly branch vẫn bắt buộc.
- Giữ Spring Boot MVC monolith, Java21, Thymeleaf, Bootstrap, JPA, SQL Server, kiến trúc ba tầng và phong cách thiết kế hiện có. Không đổi sang H2 để chứng minh constraint/lock SQL Server đúng; không hạ stack chỉ để né lỗi môi trường.
- Giữ các bản sửa đã đúng. Không bắt mọi đơn trả trước nếu nghiệp vụ đang cho phép thu sau. Chỉ trừ nguyên liệu khi hoàn tất pha chế, theo snapshot đã chốt; hủy/hoàn tiền không tự phục hồi nguyên liệu đã dùng.

## 2. Chuẩn bị và thực thi tự chủ

1. Xác định repo/module, branch/commit, diff chưa commit, Java/Maven, SQL Server và các dịch vụ đang dùng. Không reset/checkout ghi đè công việc có sẵn.
2. Đọc `AGENTS.md`, `GEMINI.md`, `CODEX.md` nếu có, SSOT nghiệp vụ/traceability/migration/demo và owner docs liên quan. Yêu cầu tại quán hiện tại thay thế yêu cầu ONLINE cũ.
3. Tự chuẩn bị cấu hình test riêng, Java21/toolchain, dependency, DB test và các dịch vụ test nếu quyền/môi trường cho phép. Khi Maven offline thiếu dependency, kiểm chế độ online/repository/network có sẵn; không mặc định dependency không tồn tại. Không sửa phiên bản tùy tiện trước xác minh.
4. Tạo tracker V4-01…16: hiện trạng/tái hiện → nguyên nhân → sửa → unit → DB/HTTP → E2E → evidence. Trạng thái dùng `CONFIRMED`, `ALREADY_FIXED_VERIFIED`, `FIXED_UNIT`, `FIXED_DB_HTTP`, `FIXED_E2E`, `BLOCKED_ENVIRONMENT`, không dùng một nhãn RESOLVED cho mọi mức kiểm chứng.
5. Chạy baseline thích hợp, lưu lỗi hiện hữu. Tạo ca regression bắt được lỗi trước sửa khi khả thi. Sửa theo dependency, chạy lại kiểm liên quan rồi chạy toàn bộ hồi quy cuối.
6. Tiếp tục các phần độc lập nếu một dịch vụ bị chặn. Chỉ yêu cầu thông tin thật sự không thể tự suy/đọc từ cấu hình được phép; không dừng chỉ vì một unit test xanh hoặc vừa hoàn thành kế hoạch.
7. Không deploy/push/merge hay chạy thay đổi lên DB thật ngoài yêu cầu. Không gửi email tới khách thật, thực hiện thu/hoàn tiền thật hoặc phát tán secret. Dùng mailbox/SMTP test, storage test và payment notice/VietQR dữ liệu demo.

## 3. Yêu cầu sửa đủ 16 nhóm trong báo cáo 04

### V4-01 — Quyền tạo TABLE đầu tiên

Hiện đường mở đầu có thể nhận tableId rồi tạo phiên/token mà chưa chứng minh đã quét QR. Bổ sung capability/ngữ cảnh mở đầu do server xác minh từ QR khó đoán; kiểm trước mutation/replay. Token này xác định đúng bàn/branch và có lifecycle phù hợp. TableId, tên bàn hoặc client cookie tùy ý không đủ quyền. Staff dùng quyền assignment riêng. Không thêm OTP/định vị, vẫn quét mở menu ngay.

Test: anonymous có CSRF nhưng không QR bị chặn; QR A đặt bàn B bị chặn; QR đúng đặt được; GET menu không mở phiên; hai request đơn đầu đồng thời không sinh hai OPEN session/cart. Không làm yếu test bằng cách cho `CurrentActor.guest()` tự có quyền khi chưa quét.

### V4-02 — Tách quyền phiên và chi nhánh công tác

Không điền working branch nhân viên từ token bàn khi assignment rỗng. Phân biệt `staff workingBranch/assignedBranches` với `tableContextBranch/session capability`; canAccessBranch cho nghiệp vụ staff chỉ dựa assignment active hoặc ADMIN explicit. Customer/Guest có quyền phiên không có nghĩa có quyền kho/nhân sự/COUNTER tại branch đó.

Test MANAGER/CASHIER/KITCHEN active không assignment + table token hợp lệ vẫn bị chặn mọi thao tác staff; nhân viên assignment CN01 + token bàn CN02 không được thao tác nghề nghiệp CN02; customer tại bàn dùng giỏ bình thường.

### V4-03 — Quyền xem phiên CUSTOMER

Bỏ nhánh CUSTOMER return vô điều kiện trong verifySessionAccess. Dùng policy thống nhất kiểm owner/membership/capability và dữ liệu tối thiểu. Customer ngoài phiên không được xem; đúng owner/capability được phép. Không biến tất cả người cùng phiên thành chủ mọi lịch sử đơn ngoài quyền phiên.

### V4-04 — Ngăn context cũ tự gia nhập lượt khách mới

Sửa cả server và client: localStorage `milktea.lastTableQr`, cookie/session restore, token HTTP/WS, pending cart. Giữ ngữ cảnh phiên hiện hành khác với chủ động quét lại. Sau đóng phiên, token cũ không vào lượt mới và giỏ cũ không tự trộn. QR tĩnh không chứng minh vị trí vật lý; ghi đúng giới hạn, không hứa ngăn người giữ QR cố ý quét lại tuyệt đối.

### V4-05 — OTP atomic phải kiểm affected rows nghiêm ngặt

Consume OTP persist phải affectedRows==1; 0 là thất bại, không dựa object stale còn usedAt/revokedAt null để cho qua rồi save. Bỏ fallback production read–increment–save khi atomic UPDATE trả0. Xử lý persistence context/transaction để không merge ghi đè kết quả atomic. Giữ REQUIRES_NEW cho failed attempts nhưng tránh khóa outer row rồi tự chờ transaction inner.

Test hai request mã đúng chỉ một thành công, revoke/expire/attempt limit giữa read và consume bị chặn, wrong attempts song song không mất lượt, outer rollback không mất failed attempt. Dùng DB test và nhiều transaction thực, không chỉ mock trả1.

### V4-06 — Viết lại SQL validation đúng schema và fail-fast

Đối chiếu schema thực, entity và migration. Sửa tên sai như SoLuongTon→SoLuong, DonViTinh→DonVi, LoaiBienDong→Loai, quantity ledger→LuongBienDong, PK lịch sử→MaBienDong, nguồn→NguonLoai/NguonMa. Invoice dùng TongTien/TienGiam/TienGiamVoucher/PhaiTra đúng nghĩa; paid dựa Payment/tất toán, không lọc invoice `DA_THANH_TOAN` nếu enum chỉ HIEU_LUC/DA_HUY. Voucher dùng tên cột thực SoLuotDaDung/TongSoLuot.

Không chỉ thay tên để script chạy. Phải bắt missing snapshot bằng NOT EXISTS/LEFT JOIN trước kiểm lượng; tránh INNER JOIN làm mất chính dòng sai. Mọi fail cần THROW/exit nonzero, chi tiết ID và expected/actual, không chỉ PRINT/WARN hoặc query lọc ra tập rỗng giả PASS.

### V4-07 — Seed stock từ nguồn, cập nhật thật theo giao dịch

Bỏ gán tồn dồi dào trực tiếp không chứng từ, before/after cố định và ledger không cập nhật Stock. Từ zero hoặc opening có nguồn tường minh, sinh giao dịch đúng thời gian và cập nhật stock/ledger/chứng từ cùng logic. Mỗi chuỗi branch/material/location phải liên tục và số dư cuối đối soát được. Không sửa bằng tăng stock hoặc thêm một phiếu tượng trưng.

### V4-08 — Nhập/chuyển/sơ chế theo đủ recipe thực

Nhập đủ mọi raw dùng, không chỉ lặp phiếu bốn loại. Chuyển tổng→cơ sở đủ hai vế, kho nguồn giảm thực. Mẻ sinh từ đủ input của recipe đúng version; không dùng trà đen thay trà lài hay một dòng đường thay cheese nếu công thức không quy định. Yield, input cost, total cost/unit cost tính từ nguồn, không hardcode 85.000/17 cho mọi mẻ.

### V4-09 — Seed đơn, tiền, voucher và phiên đúng state machine

Tạo DinhMucDonHang/snapshot đủ main+topping*quantity; completed tiêu hao đủ snapshot, không cố định trà đen150ml cho mọi món. Invoice tách giảm cửa hàng/voucher, không ghi cùng khoản vào hai trường rồi chỉ trừ một lần. Voucher kiểm min/time/branch/customer/quota và counters/history đúng. Có TABLE OPEN với GioPhien thực dùng được, đơn chưa terminal, COUNTER queue và phiên CLOSED hợp lệ; không chỉ tạo chờ pha COUNTER nên không có TABLE OPEN.

### V4-10 — Runner/migration/constraint an toàn

Tách fresh demo và upgrade, bỏ USE ép về milktea thật trong seed/validation test. Guard DB demo ở cả runner/SQL; không reset DB thật. Thêm migration kế tiếp cho constraint còn thiếu sau precheck, không cho rằng thêm index V6 đã xử lý toàn bộ invariant. Xét FK graph, default constraint, view/procedure/index phụ thuộc, transaction theo batch GO và failure recovery. Rerun phải idempotent hoặc chặn trước mutate.

### V4-11 — Scope mọi API kho/list/detail/preview

Áp policy cho CASHIER/KITCHEN/MANAGER và ADMIN rõ ràng; không chỉ ép branch Manager. Null assignment không nghĩa all. Check object branch ở settlement detail, import/movement/prep/stock summary/list/search/export nếu có. Client branch parameter không tự cấp quyền. Service cũng kiểm, không chỉ UI/controller.

### V4-12 — Replay sau quyền; ranh giới service không bypass

Kiểm quyền đối tượng/capability/assignment trước trả replay. Fingerprint chuẩn gồm các trường nghiệp vụ: action, actor/capability, branch/table/session, items/topping/quantity/note/voucher và phần giỏ còn lại nếu ảnh hưởng mutation. Cùng key khác payload phải conflict; key người/branch khác không rò dữ liệu. Sửa cả TABLE/COUNTER/nhập/sơ chế/kết ca/payment/refund theo caller map.

Bỏ overload staff chỉ nhận ID/null actor không kiểm quyền hoặc chuyển thành internal không thể gọi trái policy. Dependency bảo mật phải required; không `@Autowired(required=false)` rồi fallback kho không branch/transient branch để mock pass. Idempotency claim/unique phải an toàn đồng thời.

### V4-13 — Realtime đủ các publisher

Rà mọi mutation, gồm import, chuyển/cấp kho, waste, adjust, material/recipe, payment/cancel/refund/prep/settlement/order. Topic branch đúng scope, chuyển kho xem xét cả nơi xuất/nhận; staff không phải subscribe global để nhận đủ event. Sau commit mới phát, rollback không event thành công; reconnect tải bù. Check CONNECT/SUBSCRIBE/revoke theo assignment active, account state và phiên, không chỉ giao diện.

### V4-14 — Một sellability policy, topping tường minh, không recipe hardcode

Mapping thiếu phải chặn kể cả branch chưa có bất kỳ mapping nào; category/product/map active và available thống nhất menu/detail/search/top/cart/checkout. Không coi mọi món thuộc category allowTopping=false là topping. Chốt loại/quan hệ topping rõ và duplicate policy. Chuyển recipe fallback theo tên sang dữ liệu quản lý có version hoặc loại khỏi đường tạo đơn mới; không có recipe hợp lệ thì không publish/checkout. Recipe đổi không sửa snapshot lịch sử.

### V4-15 — Catalog/report/voucher/UX theo hợp đồng chung

Dọn named param cancelledStatus không dùng và caller; chạy context/repository thực. Newest theo createdAt+tie-break; ranking không tách cùng món ngoài policy do đổi snapshotName. Bốn danh sách tối đa20, search/filter khoảng giá/category và pagination/lazy thực, metric scope rõ. Không kết luận race quota chỉ vì COUNT: kiểm transaction/atomic UPDATE và test SQL Server song song. Pricing preview=checkout, store discount/voucher/zero đúng, snapshot đủ.

Giao diện cùng Bootstrap/layout/fragments/font/màu/component đang dùng, không redesign thương hiệu hoặc thay template. Label chi nhánh/bàn ở customer và branch nhân viên ở POS/kitchen, loading/empty/validation/403/409/retry nhất quán, không mất giỏ khi lỗi; mobile/tablet/desktop dùng được. Không có form/nhãn giao nhận mới.

### V4-16 — Bằng chứng chạy thực và báo cáo trung thực

Không dùng 148 test report cũ hoặc gọi controller trực tiếp làm bằng chứng đã test HTTP. Tạo test DB, HTTP có security filter, browser và concurrency cho invariants. Persist account/assignment rồi flush/clear trước test ghi thu; assert stock/chứng từ/ledger persisted, không chỉ verify save. Mọi VERIFIED có command/log/environment/commit tương ứng. Sửa sample_data_reference, enum invoice, đường link máy cá nhân và tracker trái thực tế.

## 4. Quy trình sửa database bắt buộc

### 4.1. Khảo sát và tách môi trường

Lập bảng hiện trạng: bảng/cột/type/precision/null/default/FK/CHECK/unique/index → entity/repository/service → dữ liệu vi phạm → migration → validation → test. Kiểm schema đã áp dụng, không suy từ file SQL.

Dùng database test riêng được allowlist, ví dụ tên có hậu tố `_test`/`_demo`; connection test không tự rơi về DB thật khi env trống. Runner phải xác minh server/database đích trước ghi, SQL không tự USE sang milktea. Data tổng hợp, file/storage/mail tách riêng. Nếu hạ tầng chỉ có DB thật, không chạy test phá dữ liệu trên đó; chuẩn bị môi trường biệt lập trước.

### 4.2. Hai đường kiểm bắt buộc

**Fresh:** dựng schema sạch và migration/baseline theo convention repo, seed có nguồn, startup JPA validate và kiểm toàn vẹn. Không để V1/V4 tự sinh số dư50/1000 không chứng từ trong đường demo chuẩn.

**Upgrade:** tạo bản sao/snapshot test của schema cũ hoặc fixture upgrade tổng hợp tương đương, có dữ liệu TABLE/COUNTER và ONLINE legacy nếu cần. Precheck → backup/snapshot → migration version mới → postcheck → startup/HTTP đọc dữ liệu cũ. Không sửa migration đã áp dụng hoặc convert mọi ONLINE thành COUNTER thiếu căn cứ; không xóa giao dịch để bỏ enum. Duy trì compatibility schema/entity hợp lý.

Đối với dữ liệu cũ thiếu nguồn, lập ngoại lệ có ID/số lượng và cách xử lý; không bịa phiếu nhập/branch/cost lịch sử. Không drop/truncate trên live. Test failure giữa migration/seed chỉ trên DB test, xác nhận rollback/restore được. Nếu migration không thể rollback tự động phải chứng minh phục hồi backup trên test.

### 4.3. Ràng buộc phải thực hiện và kiểm tra

| Bất biến | Cách cưỡng chế cần xem xét |
|---|---|
| MaCN đúng, bắt buộc tại stock/movement/mẻ/kết ca/chứng từ/đơn | FK/NOT NULL và policy service; không default1 hoặc nullable để né lỗi |
| Stock unique(branch,material,location), không âm, cost hợp lệ | PK/unique/CHECK; khóa/atomic mutation đúng row |
| Bàn–phiên–đơn cùng branch/table; một OPEN/table | Transaction/lock, filtered unique phù hợp, đối soát activeSession và FK thiết kế đúng |
| Giỏ thuộc đúng phiên, không lost update | Unique/session, version/conflict; item/topping có validation/quan hệ rõ |
| Recipe FK, lượng dương, loại/đơn vị hợp lệ, không cycle | Constraint cùng dòng + validation nhiều bảng tại service; không CHECK giả chứa query không hỗ trợ |
| before+delta=after, không trùng nguồn, chuỗi liên tục | CHECK và unique theo source/line/branch/material/location/type phù hợp; query đối soát chuỗi |
| Chuyển đủ hai vế, mẻ input/output đúng | Transaction+source link và kiểm chứng từ/recipe/cost |
| Kết ca actual=retained+discarded, unique(settlement,material) | CHECK/unique; Stock cuối tại thời điểm kết ca=retained |
| Invoice payable đúng subtotal−store−voucher theo policy | Pricing chung và CHECK số học phù hợp NULL/rounding/cap |
| Payment/refund đúng amount, không thu/chi lặp/vượt paid | Transaction/lock/unique/state guard; điều kiện liên bảng không thay bằng CHECK không hợp lệ |
| Voucher min/time/branch/quota/history/counter đúng | Constraint cùng dòng và atomic service transaction; test đồng thời |
| Idempotency nullable có semantics đúng SQL Server | Key bắt buộc hoặc filtered unique trên nonnull theo policy |

Precheck dữ liệu trước thêm ràng buộc; postcheck kiểm constraint thực tồn tại và được trust/enable theo thiết kế. Không tắt/loại FK/CHECK/unique để seed pass. Với unique ledger, không chặn hai dòng adjustment/waste hợp lệ hoặc nhiều line cùng nguyên liệu cần phân biệt/tổng hợp.

### 4.4. Script validation phải là công cụ kiểm thử

Viết lại ba file validation hiện có và bổ sung checks còn thiếu trên schema thật. Mỗi check có ID, phạm vi branch/material/time, expected/actual, violation count, ID sai; trả THROW/exit failure khi có vi phạm. Test chính validator bằng fixture sai trong transaction rollback riêng để chứng minh nó thật sự bắt lỗi.

Kiểm ít nhất: nguồn raw/SOCHE đầy đủ; stock–ledger số học/chuỗi/số dư; đủ hai vế chuyển; mẻ đúng input/output/cost; snapshot đơn đủ; consumption đủ main+topping*quantity/đơn vị; pending không tiêu hao; invoice/payment/refund/voucher; phiên/giỏ/assignment; counts/ranking và legacy. Stock hiện tại sau giao dịch mới không được so nhầm với retained của ca cũ. Không bỏ sót row thiếu do INNER JOIN hoặc lọc paid bằng enum không tồn tại.

## 5. Chuỗi dữ liệu demo và oracle độc lập

Thực hiện theo thứ tự nghiệp vụ: **branch/account/assignment/bàn/QR → nguyên liệu/đơn vị → recipe main+topping/sơ chế và mapping → nhập raw → chuyển/cấp đúng location → sơ chế → phiên/giỏ/đơn → invoice/voucher/payment → hoàn tất tiêu hao → hủy/hoàn → kết ca → đối soát**.

Món A dùng B,C: cả B và C có danh mục, định mức, kho đúng nơi và đường nguồn đủ. Raw truy tới phiếu nhập; SOCHE truy tới mẻ/input raw và nguồn nhập; chuyển branch phải có cả xuất/nhập. Bao bì trong recipe không được bỏ qua. Tồn không được sinh bằng INSERT số dư dương không opening chứng từ.

Mục tiêu fixture: hai cơ sở, kho tổng; Guest, Customer, Admin và Manager/Cashier/Kitchen từng cơ sở; hơn10 món phân biệt, dữ liệu đủ hơn20 để kiểm phân trang; topping và mọi size bật bán có recipe; TABLE OPEN+giỏ, TABLE CLOSED, bàn chưa mở; queue COUNTER/TABLE, completed paid/unpaid nếu policy cho phép; hủy chưa thu/đã thu, refund pending/approved/paid; voucher hợp lệ/sai branch/min/expired/exhausted/zero theo policy; mẻ và kết ca tăng/giảm/hao hụt. Không cần ép100 đơn nếu chưa chứng minh đúng.

Manifest deterministic: mã tra cứu ổn định, thời điểm gốc test, số lượng/giá/delta/amount đã tính trước. Expected lấy từ thiết kế transaction fixture hoặc oracle độc lập, actual từ DB/API; không SELECT actual rồi gán thành expected. Không lấy RANDOM/NEWID hiện tại làm cách tránh collision trong fixture cần rerun có kiểm chứng. Không bịa ngày nhập hoặc phiếu quá khứ cho dữ liệu thật.

Ví dụ regression: book5.000, actual4.800, discard300, retained4.500 → adjustment−200, waste−300, Stock4.500. Ví dụ chuỗi nguồn raw: nhập1.000g, chuyển400g → tổng600g, cơ sở400g; đơn hoàn tất dùng50g →350g; actual340g, bỏ40g, giữ300g → adjustment−10g, waste−40g, Stock300g. Mỗi bước có source và ledger; ca này phải khớp dữ liệu persist, không chỉ assert delta trên mock.

Tách **seed demo hợp lệ**, **fixture test**, **fixture âm tính**. Test phá/rollback không làm ô nhiễm dataset demo cuối; validation cuối phải chạy trên snapshot sạch đã biết trạng thái và mốc cắt.

## 6. Kiểm thử toàn bộ API — phải có danh mục và bằng chứng bao phủ

### 6.1. Tự phát hiện toàn bộ bề mặt hệ thống

Lập inventory từ controller annotations, security matchers, OpenAPI và **runtime Spring mappings** nếu có cách introspect được phép; không mở actuator/mappings công khai chỉ để lấy danh mục. So sánh UI network calls để bắt endpoint chưa ghi tài liệu.

Mỗi route ghi: HTTP method + path/alias + controller/service + auth/role + branch/owner/state + input/query/header/content type + response/side effects + test IDs. Tính từng alias/method thành trường hợp riêng. Include auth, catalog, table/context/session/cart, orders, notices/payment, cancellation/refund, voucher, favorite/review/media, inventory/import/preparation/settlement, manager/admin/report và endpoints phụ có thật. Include legacy ONLINE rejection, upload/download/static review resources; tách page routes, WebSocket và health/infra khỏi business API denominator nhưng vẫn có smoke phù hợp.

Nếu docs khác runtime, cập nhật docs và test runtime thực. Route không còn tồn tại cần evidence đã loại, không gạch khỏi danh mục chỉ để nâng coverage. Endpoint destructive được chạy trên DB test với fixture riêng, không bỏ qua im lặng.

### 6.2. Ma trận test mỗi endpoint

| Nhánh | Kiểm phải thực hiện khi áp dụng |
|---|---|
| Happy path | Actor hợp lệ, đủ fixture, status/body/DTO/schema đúng, DB/event side effect đúng |
| Auth | Anonymous, token thiếu/sai/expired/revoked, unverified/inactive account theo policy |
| Role | Role được phép và role không được phép, kể cả alias/wildcard security |
| Branch/object | CN01/CN02, owner/non-owner, no assignment, staff có guest token không thành staff branch |
| Validation | Null/blank/boundary length, quantity/amount âm/quá lớn, enum lạ, ID không tồn tại, page/size/sort/filter sai |
| State | State cho phép/bị cấm, repeat operation, pending cancellation, đã thanh toán/hủy/hoàn |
| CSRF/CORS | Cookie mutation thiếu/sai/đúng CSRF, origin theo profile; không coi token CSRF là quyền nghiệp vụ |
| Idempotency | Same key+payload, same key+payload khác, caller/branch khác, key trùng đồng thời |
| Read/list | Pagination/order/filter đúng, không leak data, không unbounded list ngoài rule |
| Upload/resource | Multipart đúng/sai, bytes/MIME/size/owner, GET bytes thật, persistence sau restart |
| Failure | Conflict/exception/DB rollback không để nửa dữ liệu hoặc event thành công giả |

Không phải mọi endpoint cần mọi tổ hợp; nhánh không áp dụng ghi N/A và lý do. Endpoint chưa có valid fixture phải tạo fixture, không coi401/403 là đã test happy path. Endpoint lẽ ra thành công nhưng trả500/403 vì guard nhầm là lỗi phải sửa. Với auth/public không ép role branch N/A thành case giả.

### 6.3. Cách chạy HTTP thật

Khởi động app trên port riêng với SQL Server test, cấu hình giống production ở các điểm ảnh hưởng transaction/security/OSIV/serialization. Dùng HTTP client có cookie jar/CSRF/header/token thật, không chỉ gọi controller/service trực tiếp và không mock SecurityContext thay bằng chứng full HTTP.

Tự động hóa bằng framework phù hợp repo (REST Assured, integration tests, script HTTP hoặc collection có runner), có assertions và exit code. Không đổi framework hàng loạt nếu đã có công cụ đủ tốt. Lưu method/path/status/assertion result/request ID cho từng ca, redact secret/cookie/OTP/password. Kết quả ghi `passed/failed/skipped/blocked/not_run/N_A`, không bỏ mẫu số.

Coverage endpoint = số method+route thực đã chạy qua HTTP / toàn bộ method+route business inventory. **100% endpoint được chạm chưa đồng nghĩa 100% case/quyền/luồng được bao phủ**: báo thêm branch coverage chức năng theo test matrix, failures và N/A. Nếu còn not_run/blocked, không tuyên bố “đã kiểm toàn bộ API thành công”.

## 7. Hành trình E2E bắt buộc trên ứng dụng chạy thật

| ID | Luồng và kết quả cần xác minh |
|---|---|
| FLOW01 | Guest không QR: xem menu/search/detail được, không tự gửi TABLE/COUNTER/ONLINE; không tạo phiên rác |
| FLOW02 | QR đúng→menu label branch/bàn→giỏ→voucher hợp lệ→đơn đầu→session/cart/snapshot/invoice/token đúng; sai QR/tableID bị chặn |
| FLOW03 | Gọi thêm/cùng bàn nhiều browser, stale cart409/merge theo policy, không mất món hoặc gửi lặp |
| FLOW04 | Customer login/refresh vẫn giữ capability hợp lệ, owner/non-owner rõ; không thấy phiên/lịch sử người khác |
| FLOW05 | Cashier COUNTER đúng branch→confirm→kitchen start/complete→cash/payment notice/ghi thu theo policy→receipt/report; không ép tất cả trả trước |
| FLOW06 | TABLE hoàn thành/thu đủ→đóng phiên→thu hồi token/cart/WS→lượt khách mới không thấy dữ liệu cũ |
| FLOW07 | Hủy trước pha chưa thu; hủy đã thu→obligation→duyệt→thực chi có reference→report; repeat không chi lặp, stock không tự hoàn |
| FLOW08 | Nhập tổng→chuyển cơ sở/cấp location→sơ chế→bán/hoàn tất→kết ca; đối soát quantity/cost/ledger mọi bước |
| FLOW09 | Thiếu một ingredient hoặc stock branch khác có đủ→complete rollback toàn bộ; state/stock/audit/event không nửa chừng |
| FLOW10 | Staff không assignment/cross-branch hoặc guest token không vượt quyền stock/staff/order/payment/refund |
| FLOW11 | Voucher min/time/branch/quota/zero, preview=checkout, release/retry đúng; zero order đủ rule review/ranking/đóng phiên |
| FLOW12 | Register→email OTP test→verify→login/logout; forgot/reset→token cũ vô hiệu; wrong OTP và concurrency |
| FLOW13 | Đơn đủ điều kiện→review>=50 ký tự/sao/media thật→favorite/top/review display; người chưa mua không review |
| FLOW14 | Bốn danh sách/search/filter/page/newest, metric scope/tie-break, đổi recipe/giá/tên không sửa snapshot cũ |
| FLOW15 | Admin/Manager staff/category/product/recipe/availability/voucher/report theo quyền; Manager không quyền toàn chuỗi |
| FLOW16 | App restart: dữ liệu giỏ/đơn/stock/payment và file review còn đúng theo lifecycle; health/read/write smoke sau restart |

Sau mỗi mutation quan trọng, assert API response **và DB**: row/state/quantity/amount/source/audit, rồi event/UI khi có. Tách trạng thái pha chế, tất toán và thực hoàn; không dùng invoice enum không tồn tại làm “paid”.

## 8. Concurrency, rollback, WebSocket và lỗi hạ tầng

- Chạy nhiều kết nối/transaction thực với barrier/latch hoặc điều phối xác định, không chỉ gọi tuần tự rồi gắn nhãn concurrency. Mức đồng thời phục vụ kiểm correctness trên test riêng, không stress dịch vụ thật.
- Kiểm hai đơn đầu cùng bàn; hai complete cùng đơn; complete/cancel; pay/cancel; close-session/create-order; refund/retry; quota voucher lượt cuối; OTP đúng/sai đồng thời; nhập/mẻ/kết ca/idempotency trùng. Xác định kết quả cho phép: một tác động hoặc conflict/replay hợp lệ, không stock âm/chi trùng/phiên đôi/deadlock treo vô hạn.
- Kiểm rollback khi một input thiếu hoặc transaction failure có chủ đích: không Stock/ledger/payment/audit thành công nửa chừng. Lock order nhất quán, retry giới hạn có log rõ; không dùng sleep ngẫu nhiên để che race.
- WS ít nhất cashier/kitchen CN01, CN02, Admin và hai phiên khách: CONNECT/SUBSCRIBE đúng/sai, event order/payment/cancel/refund/import/transfer/prep/adjust/waste/settlement/recipe đúng nơi và đủ nơi; rollback không publish success. Revoke assignment/account/session, logout và reconnect/tải bù phải theo policy đã ghi.
- Storage/mailer test bị lỗi phải trả lỗi/khôi phục theo hợp đồng, không URL giả hoặc thông báo gửi OTP thành công giả. Dùng SMTP test thật/local và local storage thực; mock chỉ là lớp bổ sung, không được nhận đã kiểm dịch vụ live. Nếu chỉ mail sandbox được kiểm, ghi rõ chưa xác minh SMTP production.
- Backup/restore demo, migration failure và seed rerun kiểm trên copy test; sau recovery chạy lại validation và HTTP smoke. Không tạo gateway thanh toán thật ngoài scope để “đủ test”.

## 9. UX/UI phải được thao tác trong browser

Dùng browser automation có sẵn hoặc chuẩn bị runner phù hợp (ví dụ Playwright nếu repo/môi trường cho phép); mở app thực, thao tác form/nút/menu/modal, không chỉ `node --check` hoặc HTTP backend.

Giữ thiết kế chung: Bootstrap version hiện tại, layout/fragments, font/màu/CSS variables/spacing/radius/shadow/icons/button/table/modal/status/toast. Tái dùng component, không thay template/redesign. TABLE luôn có nhãn bàn/branch; POS/kitchen đúng branch nhân viên. Không có bước/form giao hàng.

Test Guest/Customer/Cashier/Kitchen/Manager/Admin với viewport360/390/768/1366, keyboard/focus/label, tên dài/ảnh thiếu/table dài, loading/empty/error/success/validation/403/409/retry, submit đôi, refresh/back/navigation/reconnect. Nút pending có disable nhưng backend vẫn dedup. Sửa console/page errors, request500 hoặc403 ngoài dự kiến, asset404, ảnh review không hiển thị, modal tràn và mất giỏ. Không chỉ chọn màn đẹp bỏ màn lỗi.

Thu evidence test runner/console/network đã redact và screenshot nếu công cụ/môi trường cho phép; nếu không có browser runner, ghi BLOCKED và không nhận UI/E2E pass. Có browser thì phải chạy, không chỉ giao hướng dẫn cho người dùng tự chạy.

## 10. Bộ tự động hóa, hồi quy và điều kiện hoàn tất

### 10.1. Tạo lệnh tái chạy

Theo convention repo, bổ sung runner có cấu hình env ngoài source: check prerequisites → dựng/verify DB test → migrate → seed fixture → start app/wait health có timeout → unit/context/DB/API/E2E/concurrency/reconciliation → report → cleanup process/resources do test tạo. Có port/DB run ID để không giết nhầm app đang dùng.

Runner fail-fast ở bước prerequisite/migration/seed bắt buộc; các test độc lập có thể tiếp tục để tổng hợp lỗi nhưng exit cuối phải nonzero nếu fail/not_run bắt buộc. Seed không được chạy tiếp sau migration fail. Không lưu secrets trong log/report. Một lệnh tổng hợp hoặc một chuỗi lệnh xác định phải tái tạo được, không phụ thuộc thao tác tay chưa ghi.

### 10.2. Thứ tự hồi quy sau sửa

1. Compile/build và unit hiện có, sửa lỗi test hợp đồng đúng; không xóa/bỏ qua test thất bại để xanh.
2. Context/repository mappings và migrations trên SQL Server, JPA validation, DTO serialization với cấu hình test sát runtime.
3. Fresh+upgrade/rerun/failure recovery, SQL invariant tests và validators âm tính.
4. Full HTTP route matrix, state/role/branch/owner/CSRF/validation/replay.
5. Browser FLOW01–16, WS, concurrency và restart.
6. Trên fixture cuối đúng mốc cắt, chạy reconciliation độc lập stock–ledger–recipe–invoice–payment–refund–voucher–session và so expected/actual.
7. Nếu chỉnh source/schema/fixture sau đó, chạy lại kiểm bị ảnh hưởng và full regression cuối của bản bàn giao; không trộn log từ trước thay đổi thành bằng chứng cuối.

### 10.3. Definition of Done

- V4-01…16 được kiểm và sửa hoặc có bằng chứng hiện trạng đã đúng; không còn P0 đang mở để nhận hoàn tất.
- App build/start được trên môi trường đã mô tả, fresh/upgrade DB test hợp lệ, constraints và seed/validators chạy thật.
- Toàn bộ business method+route/alias trong inventory được chạy HTTP; ca bắt buộc của matrix và FLOW01–16 đạt, hoặc ghi rõ fail/blocked và **không tuyên bố hoàn tất toàn hệ thống**.
- Stock/ledger/source/snapshot và tiền/voucher/hoàn/phiên có expected/actual khớp; validator thật sự bắt được fixture âm tính.
- Các thao tác hợp lệ vẫn thành công sau siết quyền; các thao tác trái quyền bị chặn không mutation. OTP/replay/race không tác động trùng.
- Browser/UI nhất quán, không lỗi console/network nghiêm trọng, realtime đúng branch và đủ event, restart không làm mất dữ liệu trái policy.
- Báo cáo và SSOT/API/demo docs phản ánh code/schema thực, không tự dùng nhãn VERIFIED cho phần chưa chạy. R19/ONLINE phát triển mới vẫn ngoài phạm vi.

Nếu môi trường thật sự không cho cài/chạy Java21/SQL Server/browser/SMTP/storage, hoàn tất phần độc lập, lưu exact error/prerequisite/lệnh chạy lại và trạng thái BLOCKED. Không thay bằng mock/H2 rồi nhận đã kiểm full SQL/HTTP; không bịa log/PASS hoặc hứa tiếp tục chạy nền sau khi đã dừng.

## 11. Bàn giao bắt buộc

Giữ tên/tổ chức theo repo, có thể dùng các tên sau:

- `docs/V4_FIX_TRACKER.md`: từng V4-xx, root cause, files/migration, tests và mức kiểm chứng.
- `docs/API_TEST_MATRIX.md` và kết quả machine-readable: mọi method+path/alias, role/branch/owner/state, cases và status thực; endpoint coverage có mẫu số rõ.
- `docs/E2E_TEST_REPORT.md`: FLOW01–16, WS/concurrency/restart/browser viewports, log run ID/thời gian/commit/env.
- `database/validation/*.sql`: đúng schema, có fail signal và test validator.
- Migration nâng cấp, baseline/seed test/demo, runner có guard và recovery instructions.
- `database/sample_data_reference.md` hoặc file `.md` demo nguồn chuẩn: tài khoản tổng hợp/assignment/bàn/QR label; recipes/materials; nhập/chuyển/mẻ/đơn/thu-hoàn/kết ca; expected/actual stock/tiền/ranking/count và kịch bản trình diễn.
- Commands/runner để tái chạy trên môi trường test, summary passed/failed/skipped/blocked/not_run, lỗi còn lại và limitations.

Phản hồi cuối của Agent phải nêu **đã sửa gì, đã thực sự chạy gì, kết quả định lượng nào chứng minh đúng, còn gì chưa chạy**. Không chỉ nói “đã fix toàn bộ, hệ thống ổn định” hoặc giao một checklist để tôi tự kiểm thay bạn. Thực hiện đến kết quả có thể xem xét và tái chạy được trong phạm vi môi trường cho phép.
