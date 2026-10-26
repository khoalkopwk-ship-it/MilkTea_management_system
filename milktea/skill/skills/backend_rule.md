# MilkTea — quy tắc backend Spring Boot/JPA

## 1. Boundary

Theo package trong [system_architecture.md](system_architecture.md). Controller nhận DTO, @Valid, current actor và gọi Service; không gọi Repository trực tiếp, tính tiền, chuyển trạng thái hay trừ kho. PageController dựng ViewModel dùng cùng Service với ApiController. Repository truy vấn/khóa/ghi, không chứa workflow nhiều module. Mapper không tự query DB.

Service dùng constructor injection. Concrete Service đủ nếu không có nhiều implementation; tránh interface/Impl và base CRUD khổng lồ tạo ra chỉ để đủ khuôn. Entity không trả trực tiếp REST/template và không dùng Lombok @Data cho quan hệ vòng.

## 2. Contract Service chính

| Service/method đề xuất | Trách nhiệm |
|---|---|
| TableContextService.readByQr | Đọc nhãn bàn và phiên hiện tại; không thay trạng thái |
| TableSessionService.openByCashier | Khóa bàn; mở hoặc dùng phiên hiện tại; Có khách |
| OrderService.createTableOrder | Khóa bàn → xác minh expectedSessionId → mở phiên nếu cần → chốt giá/định mức → đơn/hóa đơn → cập nhật giỏ cùng transaction |
| SessionCartService.replaceCart | Xác minh phiên còn mở + version; lưu giỏ nháp, không tạo đơn |
| OrderService.assignCounterOrderToTable | CASHIER gán đúng phiên; không tạo hóa đơn/thu mới |
| OrderService.confirm/start/complete | Thực hiện chuyển trạng thái hợp lệ theo actor |
| PaymentService.recordReceipt | CASHIER ghi toàn khoản thực nhận; không gọi start/complete/close |
| TableSessionService.closeByCashier | Kiểm tra đơn/khoản thu; đóng phiên + Trống + xóa giỏ; phát event sau commit |
| CancellationService.request/decide | Yêu cầu hủy và xử lý trước pha; tạo refund nếu đã thu |
| RefundService.decide/recordRefund | ADMIN quyết định; CASHIER thực trả một lần |
| InventoryService.completeOrderConsumption | Trừ BEP + ghi sổ theo snapshot, cùng transaction complete |
| StockIssueService/PreparationService/WasteService | Xuất KHO→BEP, ghi mẻ, ghi hao hụt/sự cố |
| IncidentService / InventoryService.recordAdditionalConsumption | Ghi sự cố liên kết ledger; pha bù sau complete ghi tiêu hao bổ sung, không complete toàn đơn lần hai |
| ProductImageService | Upload/thay ảnh Cloudinary; không nằm trong Config |
| ReportService | DTO tổng hợp từ giao dịch sẵn có, không Entity doanh thu sao chép |

Tránh vòng Service: OrderService phụ thuộc InventoryService và phần mở phiên nội bộ; TableSessionService.close dùng Repository/read projection kiểm tra đơn/tiền, không gọi vòng OrderService. Publisher không gọi ngược workflow.

## 3. Transaction và cạnh tranh

Đặt @Transactional ở method public của Service gọi qua Spring proxy; tránh self-invocation khiến annotation không chạy. Complete gọi InventoryService với REQUIRED; không REQUIRES_NEW làm kho commit riêng trước đơn. Xuất/mẻ/đóng phiên đều atomic theo Domain.

Theo một thứ tự khóa thống nhất: bàn/phiên → giỏ → đơn/hóa đơn → chứng từ thu/hoàn → các dòng Stock theo ID tăng. Các lệnh tạo/gán/thu/hủy thuộc bàn phải phối hợp khóa cùng phiên trước khi đổi đơn, để close và gọi thêm không lọt giữa kiểm tra và commit. Đơn không có bàn khóa đơn/hóa đơn. Không giữ transaction qua UI chờ người dùng hay gọi Cloudinary/mail lâu.

Khóa DiningTable khi tạo phiên; re-check activeSessionId sau khi có lock. Không chỉ `find open` rồi insert ngoài khóa. Dùng @Version/If-Match ở SessionCart; header If-Match phải là ETag version có dấu ngoặc kép, ví dụ `"2"`, theo OpenAPI; 409 khiến frontend tải giỏ mới và cho thao tác lại, không silent last-write-wins. Khi close chạy cùng order create: hoặc order ghi trước và close bị chặn, hoặc close ghi trước và request phiên cũ bị từ chối; không tạo đơn vào phiên đã đóng.

`expectedSessionId` trong create table order: null chỉ có nghĩa trang đang ở ngữ cảnh chưa có phiên. Nếu bàn vừa có phiên khác, trả TABLE_SESSION_CHANGED để khách đồng bộ, không âm thầm gán giỏ cũ. Không dùng trạng thái bàn làm điều kiện GET QR.

## 4. Chống ghi lặp

Tạo đơn, ghi thu/hoàn, complete, xuất, mẻ, hao hụt và close có Idempotency-Key. Lưu scope + key + fingerprint + resource/result. Lặp cùng nội dung trả kết quả đã có; đổi nội dung với key cũ trả 409. Scope có actor/QR/phiên hoặc đơn phù hợp; không cho một người đoán key rồi đọc đơn người khác. Credential trả về được bảo vệ, không lưu plaintext trong log/request-dedup thông thường.

Unique cho khoản thu thành công, refund thực trả và stock source chống ghi sổ lặp. Disable nút chỉ là UX. Complete đã thành công không tiêu hao lần hai. Close lặp đúng phiên cũ trả kết quả close cũ, không đóng lượt mới. Khi retry thất bại phiên đã đổi, giữ context để người dùng quyết định.

## 5. JPA và query

@Enumerated STRING; BigDecimal đúng precision/scale; relations LAZY; DTO/projection trong transaction read-only. Open Session in View=false; dùng fetch join/entity graph chọn lọc, không fetch cả graph với paging. @Version chỉ ở dữ liệu cạnh tranh có ý nghĩa. Không cascade REMOVE từ Account/Product/Material/DiningTable sang giao dịch.

Giá/giảm/định mức luôn snapshot khi đặt. Đọc snapshot khi complete, không đọc công thức mới và sửa đơn cũ. Tồn cập nhật có khóa hoặc conditional update kiểm tra quantity, rollback khi thiếu, không âm.

Constraint DB bảo vệ PK/FK/unique nhưng Service vẫn kiểm tra vai trò/trạng thái. Không vừa dùng trigger trừ tồn vừa Service trừ tồn. Trigger/procedure chỉ thêm nếu môn học hoặc repository thực sự yêu cầu; ghi rõ cơ chế nào sở hữu việc ghi sổ.

Ngưỡng tồn thuộc Stock(materialId,location); SettingsService quản lý thông tin quán/ngân hàng lẫn giảm chung theo OpenAPI. OrderIngredientSnapshot tổng hợp toàn đơn theo (orderId,materialId,totalQuantity), không tạo hai bộ snapshot per-item và per-order trùng nhau. Nếu source đã dùng per-item thì aggregate khi complete và giữ một nguồn định mức lịch sử.

## 6. Validation và lỗi

Request kiểm tra null/length/quantity/date; Service kiểm tra món hoạt động, phiên đúng, actor, transition, tiền thực nhận và tồn; DB kiểm tra integrity. @RestControllerAdvice và Security handlers cùng ErrorResponse theo Architecture. Không trả 200 khi lỗi. Trả conflict đủ thông tin an toàn để tải lại; không trả token/hash/SQL.

## 7. Tích hợp và kiểm chứng

Event application được publish trong transaction, listener AFTER_COMMIT gửi STOMP. Nếu gửi socket thất bại, DB vẫn đúng và client đồng bộ HTTP; không rollback khoản thu đã commit vì socket lỗi. Gửi mail reset có cơ chế retry thủ công an toàn/token hạn ngắn; không lộ token vào log.

Kiểm chứng bằng Maven wrapper thực có và các ca [acceptance_tests.md](acceptance_tests.md). Test meaningful: chuyển trạng thái, tiền độc lập, phiên/read-only QR, reload đa tab, rollback kho, ghi lặp và quyền. Repository tests dùng đúng DB đã chọn khi kiểm tra khóa/unique/SQL; H2 không thay bằng chứng vendor-specific. Không tuyên bố integration pass nếu chỉ compile hoặc chạy mock.
