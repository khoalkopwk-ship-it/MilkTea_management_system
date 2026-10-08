# MilkTea — Spring WebSocket/STOMP

## 1. Transport

Endpoint `/ws` (WebSocket native; SockJS chỉ thêm nếu môi trường cần). Simple broker đủ một instance đồ án. Tạo HTTP data trước, commit, mới publish bằng transactional listener AFTER_COMMIT. Không STOMP SEND để thay đổi đơn/tiền/kho/bàn; chặn client SEND command/destination không được cho phép.

CONNECT xác minh account/guest, SUBSCRIBE kiểm riêng theo [author_rule.md](author_rule.md). Trong browser account có cookie HttpOnly hoặc token CONNECT theo môi trường REST; không ép JS đọc JWT cookie. Dùng Origin allowlist và CSRF CONNECT phù hợp cấu hình Spring Security thực dùng.

## 2. Destination và người nhận

| Destination | Người nhận được phép |
|---|---|
| `/topic/table-sessions/{sessionId}` | Guest có token đúng phiên mở, CUSTOMER có quyền phiên, CASHIER |
| `/user/queue/orders` | Account owner hoặc counter principal đúng đơn được route server-side |
| `/topic/cashier` | CASHIER |
| `/topic/kitchen` | KITCHEN, chỉ thông tin đơn cần pha |
| `/topic/stock` | KITCHEN và ADMIN |
| `/topic/admin` | ADMIN |

Không có branch trong destination. Không publish full invoice/PII/token lên topic công khai hoặc topic bàn chưa kiểm quyền. Gửi TABLE_SESSION_CLOSED tới những subscriber của phiên trước khi teardown subscription; không cho subscribe phiên đó về sau. Credential/account hết hiệu lực phải ngừng dispatch, không để connection sống mãi với quyền cũ.

## 3. Event envelope

`{eventId,type,occurredAt,resourceId,sessionId,version,payload}`; id/amount là chuỗi theo Architecture; sessionId nullable cho đơn quầy chưa gán. Payload tối thiểu để client biết cần fetch API, không chứa secret hoặc toàn bộ Entity.

| Type | Điểm phát sau commit |
|---|---|
| TABLE_SESSION_OPENED | Gửi đơn đầu tiên hoặc thu ngân mở phiên |
| TABLE_SESSION_CLOSED | Thu ngân kết thúc phiên |
| CART_CHANGED | Lưu/đặt từ giỏ phiên thành công |
| ORDER_CREATED / ORDER_STATUS_CHANGED | Tạo/xác nhận/start/complete/hủy |
| ORDER_ASSIGNED_TO_TABLE | Quầy gán đơn vào phiên |
| CANCELLATION_CHANGED | Request/decision |
| PAYMENT_RECORDED / PAYMENT_NOTICE_CREATED | Thu thật hoặc notice (hai ý nghĩa riêng) |
| ORDER_INCIDENT_RECORDED / ORDER_INCIDENT_RESOLVED | Ghi hoặc kết thúc sự cố; staff đúng quyền nhận thông tin, không tự đổi đơn/tiền/kho |
| REFUND_CHANGED | Quyết định/thực trả |
| STOCK_CHANGED / STOCK_LOW | Xuất/mẻ/tiêu hao/hao hụt và cảnh báo |
| STOCK_ISSUE_RECORDED | Thông báo admin về phiếu, không approval request |

KITCHEN nhận ORDER_CREATED cho queue chỉ khi đã xác nhận (hoặc ORDER_STATUS_CHANGED có trạng thái phù hợp); không đưa đơn chưa xác nhận vào hàng đợi pha. Không lọc queue bằng đã thu.

## 4. Reconnect và độ tin cậy

Event có thể trùng/mất/đến chậm; dedupe eventId và fetch snapshot HTTP sau reconnect. Dùng version khi giỏ cạnh tranh; không ghi số tiền/tồn dựa increment event phía client. Publish thất bại không xóa giao dịch SQL; HTTP vẫn dùng được để tải lại.

Không auto-close session khi socket disconnect và không auto-open khi reconnect. Topic phiên cũ không được chuyển sang phiên mới của cùng bàn. Không cần Kafka/RabbitMQ/multi-node broker trong phạm vi này.

Test bằng hai browser/tab khác ngữ cảnh và request trái quyền; không chỉ test một log "Connected". Kiểm after-commit, rollback không phát trạng thái thành công, logout/close không tiếp tục nhận dữ liệu.
