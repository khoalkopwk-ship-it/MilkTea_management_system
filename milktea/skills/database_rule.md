# MilkTea — dữ liệu quan hệ và JPA

## 1. Lựa chọn và naming

Chọn một SQL Server/MySQL/PostgreSQL. Mặc định SQL Server cho project mới; giữ DB đang dùng nếu đã chốt. JPA không làm identity/index/SQL đặc thù tự tương thích ba vendor. Script theo vendor trong `database/<vendor>/` hoặc Flyway location riêng; không trộn T-SQL, PostgreSQL sequence và MySQL syntax. Không lấy trigger/procedure của mạng xã hội làm yêu cầu môn MilkTea.

Tên Java tiếng Anh; tên SQL thống nhất trong schema đã chọn, @Table/@Column map rõ nếu giữ tên tiếng Việt báo cáo. ID BIGINT, tiền DECIMAL(18,2), lượng DECIMAL(18,3), role/state VARCHAR với enum STRING, thời gian UTC. Các độ dài/precision lớn hơn được chọn nếu dữ liệu thực cần. @Version BIGINT cho giỏ/aggregate có concurrency.

Tên SQL và từng trường chuẩn của báo cáo nằm trong [database_dictionary.md](database_dictionary.md). Bảng tóm tắt dưới đây ánh xạ tên lớp Java; không tạo thêm entity đồng nghĩa khi source đã có lớp tương ứng. Sở hữu schema tại file này, giữ từ điển chi tiết cập nhật cùng migration.

## 2. Danh mục bảng đề xuất

| Domain | Entity/bảng | Quan hệ và invariant |
|---|---|---|
| account | Account, PasswordResetToken | Login unique; passwordHash; role/active/tokenVersion; reset hash và expiry |
| catalog | Category, Product | Product.categoryId; name/size/price/active/imageUrl/imagePublicId; một Product cho mỗi biến thể size |
| recipe | ProductRecipe | Product→Material, lượng mỗi ly; unique(productId,materialId) |
| recipe | PreparationRecipe, PreparationRecipeItem | Output Material SOCHE/yield/unit; input THO và định lượng chuẩn |
| table | DiningTable | number/qrCodeHash hoặc mã opaque tra cứu/active/status TRONG/CO_KHACH; activeSessionId nullable |
| table | TableSession | tableId/openedAt/closedAt/openedBy/closedBy; OPEN/CLOSED |
| table | SessionCart, SessionCartItem | sessionId unique, version; item productId/quantity; unique(cartId,productId); chỉ tồn tại cho phiên đang mở |
| table | TableSessionAccess | tokenHash/sessionId/expiresAt/revokedAt nếu dùng opaque guest credential |
| order | Order, OrderItem | accountId nullable/sessionId nullable/source/status/replacementOfOrderId; snapshot tên/size/unitPrice/quantity |
| order | OrderIngredientSnapshot | PK(orderId,materialId); totalQuantity/type/unit chốt toàn đơn như DinhMucDonHang của báo cáo |
| order | Invoice | orderId unique; subtotal/discountPercent/discountAmount/total/status; không delete khi hủy |
| order | CounterOrderAccess | tokenHash/orderId/revokedAt; không lưu raw token |
| payment | Payment, PaymentNotice | Invoice và thực thu CASH/BANK_TRANSFER, amount/paidAt/recordedBy/reference; notice không là thu thật |
| cancellation | CancellationRequest | orderId/reason/requestedAt/decidedBy/status; tối đa một request PENDING bằng khóa Service/constraint phù hợp vendor |
| cancellation | RefundRequest | paymentId unique; fullAmount/status/adminDecision/refundedAt/paidBy/reference |
| inventory | Material, Stock | Material THO/SOCHE/unit; Stock có threshold theo vị trí, unique(materialId,location), quantity>=0 |
| inventory | ImportReceipt, ImportReceiptItem | Nhập KHO; chứng từ, người lập, lượng và đơn vị |
| inventory | StockIssue, StockIssueItem | Chuyển KHO→BEP, không approval lifecycle |
| inventory | PreparationBatch, PreparationBatchItem | Công thức tham chiếu + snapshot input/output/thực thu/lý do lệch |
| inventory | StockMovement | material/location/signed delta/before/after/sourceType/sourceId/sourceLine/movementType/reason/actor |
| audit | BusinessAudit | actor/guest scope hợp lệ/action/resource/before/after/reason/time; không token |
| settings | GlobalSettings | id=1, discountPercent trong [0,100], modifiedBy/modifiedAt |
| common | RequestDedupRecord | Scope/key/fingerprint/resource/result metadata; expiry chính sách, không plaintext secret |

Đây là schema thiết kế đề xuất; khi ánh xạ báo cáo/code có sẵn, giữ tên thực tế và áp dụng invariant, không tự tạo thêm bảng trùng. Reporting dùng query/DTO, không bắt buộc entity. Không có Branch/BranchProduct/branchId, ItemStatus, PromotionProgram, GuestCart của quầy.

## 3. Một phiên mở và giỏ dùng chung

DiningTable.activeSessionId trỏ phiên mở của chính bàn; null khi Trống. TableSession giữ tableId lịch sử. Service giữ lock bàn và kiểm cùng bàn/trạng thái trước khi cập nhật pointer. DB FK không tự bảo đảm pointer thuộc đúng tableId nếu dùng FK đơn; phải kiểm invariant hoặc composite FK nếu schema triển khai.

Không rely partial unique index OPEN mà không có phương án cho MySQL. Dùng row lock + active pointer để tạo phiên portable. Có thể bổ sung filtered/partial index ở SQL Server/PostgreSQL khi triển khai riêng nhưng không thay khóa Service.

Giỏ theo sessionId + version, không theo HttpSession/browserId. Tiền/đơn không lưu lại trong SessionCart; query join/projection khi dựng TableSessionView. Close xóa cart/items/access tạm hoặc revoke access; giữ TableSession, Order, Invoice, Payment, Refund và StockMovement.

## 4. Snapshot, tiền và kho

OrderItem snapshot giá/tên/size; Invoice snapshot giảm chung; OrderIngredientSnapshot snapshot định mức; PreparationBatch snapshot công thức thực dùng. Sửa Product/Recipe/Settings không cập nhật chứng từ cũ.

Một khoản thu toàn phần thành công mỗi Invoice ở phạm vi hiện tại. Unique invoiceId của Payment nếu Payment chỉ lưu khoản thu thành công; giao dịch lỗi/notice nằm riêng. RefundRequest unique paymentId; ghi thực trả một lần. Cancellation pending uniqueness xử lý dưới lock Order hoặc constraint vendor phù hợp.

StockMovement unique(sourceType,sourceId,sourceLine,movementType,location). Một issue có XUAT_DI KHO và NHAN_BEP BEP; một mẻ có SOCHE_RA đầu vào và SOCHE_VAO đầu ra; hoàn thành có TIEU_HAO. HAO_HUT có lý do và source, không ghi lặp lượng đã TIEU_HAO. Cascade không được xóa ledger.

## 5. Index và script

Index: Order(sessionId,createdAt), Order(accountId,createdAt), Order(status,createdAt); Payment(paidAt), RefundRequest(status,refundedAt); StockMovement(materialId,location,createdAt); TableSession(tableId,openedAt); request key unique scoped. Không thêm index dư cho mọi cột.

Version hóa DDL/seed bằng script đánh số hoặc một Flyway/Liquibase đã chọn. Demo dùng validate/none, không dựa ddl-auto=update làm schema cuối. Dữ liệu mẫu một quán, nhiều bàn, bốn role, món đủ size, nguyên liệu THO/SOCHE, KHO/BEP, giảm chung và 5kg→50 suất. Không commit mật khẩu/secret thật.

Nếu chuyển từ schema cũ: bỏ yêu cầu branch; chuyển order CHO_THANH_TOAN thành CHO_XAC_NHAN còn giữ trạng thái tiền; migration không làm mất lịch sử. Kiểm tra lại PK/unique tồn khi bỏ branchId. Không DROP dữ liệu thật để đổi thiết kế; dùng DB demo mới nếu được phép và đúng mục tiêu.

## 6. Lưu sự cố và chứng từ pha bù

Dùng NhatKyNghiepVu/BusinessAudit với MaNKCha và DuLieu JSON theo [database_dictionary.md](database_dictionary.md). Incident và chứng từ pha bù là projection/sự kiện audit có ID BIGINT riêng; ledger trỏ chứng từ pha bù, không trỏ trực tiếp cùng ID sự cố cho mọi lần pha. Audit + stock + movement phải cùng transaction. Converter CancellationStatus và giới hạn key 64 theo từ điển; đây là lựa chọn kỹ thuật đã chốt cho bộ skill.
