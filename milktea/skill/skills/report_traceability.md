# MilkTea — truy vết yêu cầu và triển khai

Dùng báo cáo cập nhật 05/10/2026 làm nền: 7 chương, QĐ01–QĐ26, UC01–UC41, MH01–MH22, T01–T60 và nhật ký C01–C22. Tên method/class là ánh xạ thiết kế, không chứng minh class đã tồn tại. Tìm operationId/path trong OpenAPI theo UC được gắn x-use-cases; xem đặc tả đầy đủ ở Use Case.

## Tổng thể

| Nhóm | Nghiệp vụ | Thành phần |
|---|---|---|
| F01/F08 | Truy cập, account/profile/history/reset | Auth/Account/Customer |
| F02 | QR/giỏ/phiên/đơn bàn/quầy/gắn bàn | Table/Order/Customer/Cashier |
| F03 | Confirm/start/complete, notice/receipt | Order/Payment/Kitchen/Cashier |
| F04 | Hủy toàn đơn/thay thế/refund/sự cố | Cancellation/Refund/Incident/Inventory |
| F05 | Quán, danh mục/món/ảnh/công thức | Settings/Catalog/Recipe/Cloudinary |
| F06 | Nguyên liệu/ngưỡng/nhập/xuất/mẻ/hao hụt/history | Inventory |
| F07 | Report/discount/events | Reporting/Settings/Notification |

Đối chiếu QĐ tại Domain/Workflow; đối chiếu NF01–NF08 tại Architecture/Author/Realtime/Media/Acceptance. Owner chịu nghiệp vụ xuyên tầng; Lân review schema chung, không viết toàn bộ Repository. Khoa wire config; Nam viết ProductImageService. Mai không tạo Entity giỏ quầy, nhưng giỏ phiên bàn cần Entity/Repository do Nam quản lý. Reporting có DTO/query, không bắt buộc Entity.

## UC → Service → dữ liệu → màn hình → owner → test

| UC | Service | Entity/dữ liệu | Màn hình | Đầu mối | Ca kiểm |
|---|---|---|---|---|---|
| UC01 Đăng nhập | AuthService | Account | MH01 | Khoa | T28 T56 |
| UC02 Đăng ký khách hàng | AuthService | Account | MH01 | Khoa | T28 T36 |
| UC03 Khôi phục mật khẩu | AuthService | PasswordResetToken/Account | MH01 | Khoa | T35 T56 |
| UC04 Đăng xuất | AuthService | Account.tokenVersion | MH21 | Khoa | T56 T60 |
| UC05 Xem menu | CatalogService/TableContextService | Category/Product/Table | MH02 | Nam + Mai | T01 T37 |
| UC06 Xem chi tiết món | CatalogService | Product | MH02 | Nam + Mai | T37 T40 |
| UC07 Cập nhật giỏ hàng | SessionCartService | SessionCart/Items | MH03 | Nam + Mai | T02 T03 T38 T42 T43 |
| UC08 Đặt đơn tại bàn | OrderService/TableSessionService | TableSession/Cart/Order/Invoice/Snapshot | MH04 | Quỳnh + Nam + Mai | T01 T40 T41 T43 |
| UC09 Tạo đơn tại quầy | OrderService | Order/Invoice/CounterOrderAccess | MH07 | Quỳnh + Mai | T04 T05 T49 |
| UC10 Theo dõi đơn hàng | OrderQueryService | Order/Invoice/Payment/Refund | MH04 MH06 | Quỳnh + Mai | T02 T03 T09 T29 T30 |
| UC11 Xác nhận đơn hàng | OrderService.confirm | Order/Audit | MH07 | Quỳnh | T07 T53 T54 |
| UC12 Ghi nhận thanh toán | PaymentService | Payment/Invoice | MH08 | Quỳnh | T04 T05 T06 T27 T44 |
| UC13 Bắt đầu chế biến | OrderService.start | Order/Audit | MH10 | Quỳnh | T07 T54 |
| UC14 Hoàn thành đơn hàng | OrderService/InventoryService | Order/Stock/Movement | MH10 MH11 | Quỳnh + Lân | T07 T08 T19 T21 T24 T45 |
| UC15 Yêu cầu hủy đơn hàng | CancellationService.request | CancellationRequest | MH06 MH09 | Quỳnh + Mai | T10 T11 T12 T13 T54 |
| UC16 Xử lý hủy đơn hàng | CancellationService.decide | Order/Invoice/Cancellation/Refund | MH09 | Quỳnh | T10 T11 T12 T54 |
| UC17 Tạo đơn thay thế | OrderService | Order.replacementOfOrderId | MH09 | Quỳnh + Mai | T14 |
| UC18 Duyệt hoàn tiền | RefundService.decide | RefundRequest | MH19 | Quỳnh | T26 T59 |
| UC19 Ghi nhận hoàn tiền | RefundService.recordRefund | RefundRequest/Payment | MH08 | Quỳnh | T26 T27 T59 |
| UC20 Xem lịch sử mua hàng | OrderQueryService | Order.accountId | MH21 | Quỳnh + Mai | T52 T58 |
| UC21 Cập nhật hồ sơ | AccountService | Account | MH21 | Khoa + Mai | T56 T58 |
| UC22 Quản lý tài khoản | AccountService | Account | MH17 | Khoa | T28 T56 |
| UC23 Thiết lập thông tin quán | SettingsService | GlobalSettings | MH17 | Nam | API01 |
| UC24 Quản lý danh mục | CategoryService | Category | MH15 | Nam | T36 |
| UC25 Quản lý món uống | ProductService/ProductImageService | Product | MH15 | Nam | T16 T32 API06 |
| UC26 Quản lý công thức | RecipeService | ProductRecipe/PreparationRecipe | MH16 | Nam | T16 T18 T19 |
| UC27 Quản lý bàn | TableService | DiningTable | MH18 | Nam | T01 T37 |
| UC28 Quản lý phiên bàn | TableSessionService | DiningTable/Session/Cart/Access | MH18 | Nam + Quỳnh | T33 T34 T39 T44 T46 T47 T48 T60 |
| UC29 Quản lý nguyên liệu | MaterialService | Material | MH11 | Lân | T18 T19 |
| UC30 Thiết lập cảnh báo tồn | InventoryService.configureThreshold | Stock.threshold | MH11 | Lân | T20 API02 |
| UC31 Lập phiếu nhập kho | InventoryService.recordImport | ImportReceipt/Stock/Movement | MH14 | Lân | T36 |
| UC32 Lập phiếu xuất kho | InventoryService.recordIssue | StockIssue/Stock/Movement | MH12 | Lân | T17 T22 |
| UC33 Ghi nhận mẻ sơ chế | InventoryService.recordPreparation | PreparationBatch/Stock/Movement | MH13 | Lân | T18 T23 |
| UC34 Ghi nhận hao hụt | InventoryService.recordWaste | Stock/Movement/Audit | MH13 | Lân | T25 API03 |
| UC35 Xem lịch sử kho | InventoryQueryService | Stock/Movement/Documents | MH14 | Lân | T17 T18 T19 T25 |
| UC36 Xem báo cáo kinh doanh | ReportService | Order/Payment/Refund query | MH20 | Lân | T26 T57 |
| UC37 Thiết lập giảm giá chung | SettingsService | GlobalSettings/Invoice snapshot | MH20 | Nam | T15 T16 API04 |
| UC38 Xem thông báo nghiệp vụ | NotificationService | Committed domain events | MH22 | Khoa + chủ module | T31 T55 T56 T60 |
| UC39 Báo cáo sự cố đơn hàng | IncidentService/InventoryService | Audit/StockMovement | MH10 MH13 | Quỳnh + Lân | T25 API03 |
| UC40 Gắn đơn quầy vào bàn | OrderService/TableSessionService | Order.sessionId/TableSession | MH07 MH18 | Quỳnh + Nam | T50 T51 T52 |
| UC41 Gửi thông báo thanh toán | PaymentService.createNotice | PaymentNotice | MH05 | Quỳnh + Mai | T53 |

## Dùng truy vết khi sửa

1. Xác định UC và quy tắc QĐ chịu ảnh hưởng; sửa file sở hữu, không suy luận yêu cầu từ tên màn hình.
2. Tìm operationId có x-use-cases tương ứng; sửa DTO/header/security/status cùng OpenAPI nếu đổi contract.
3. Sửa các lớp nghiệp vụ xuyên tầng và migration nếu đổi dữ liệu; đọc ma trận transaction trước đổi complete/close/cancel.
4. Sửa màn hình và event/fetch phục hồi đúng phiên; chạy test được liệt kê và bổ sung case cho quyết định mới.
5. Khi cập nhật báo cáo, giữ UC bắt đầu bằng động từ, đủ precondition/basic/alternate/exception/postcondition. Dùng Package ở sơ đồ tổng quát; không include Đăng nhập vì đó là precondition nhân viên; không kế thừa actor để vô tình cấp quyền bếp.

Phân công này là đầu mối thiết kế, không tạo lịch công việc mới. Kế hoạch 11 ngày đã chốt: 4 ngày cốt lõi + 3 ngày cải tiến/luồng phụ + 4 ngày test/demo/báo cáo; xem Chương 6 nếu cần lịch chi tiết.
