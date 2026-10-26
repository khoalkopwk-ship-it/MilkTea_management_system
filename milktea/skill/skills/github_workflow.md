# MilkTea — phối hợp source và hợp đồng

## 1. Nhánh và ownership

Một repository. Giữ mô hình nhánh nhóm đã chọn; nếu chưa có, dùng main bản demo ổn định, develop tích hợp và feature/fix ngắn hạn. Không ép thay chiến lược repo đang dùng. Tích hợp mỗi ngày, không cuối giai đoạn mới copy source.

| Nghiệp vụ xuyên các tầng | Đầu mối kiến trúc |
|---|---|
| config/security/auth/account/notification | Khoa |
| inventory/reporting, schema coordination | Lân |
| customer templates/JS/PageController và trải nghiệm khách | Mai |
| catalog/recipe/table/settings, ProductImageService | Nam |
| order/payment/cancellation/refund và UI quầy/bếp cho đơn | Quỳnh |

Đây là ranh giới kiến trúc đã có, không phải phân công ngày/tuần mới. UI kho ở bếp thuộc inventory; UI đơn bếp thuộc order. Người khác được sửa phần liên quan nhưng phối hợp owner; không dùng ownership để từ chối sửa tích hợp cần thiết.

## 2. PR và thay đổi hợp đồng

Trước sửa: đọc INDEX/owner files; sync branch; chọn phạm vi nhỏ. PR ghi vấn đề, behavior sau sửa, file hợp đồng/schema liên quan, kết quả test và bước tái hiện. Chỉ commit source, migration, config mẫu và tài liệu thực cần; không secret, .env thật, target, IDE caches, dump chứa dữ liệu cá nhân.

Thay trạng thái đơn/tiền/phiên/giỏ cập nhật Domain/OpenAPI/tests cùng change. Migration do module owner viết, Lân review thứ tự/FK/unique; không Lân viết tất cả Repository. CloudinaryConfig khác ProductImageService; account auth khác guest table access; báo cáo không tự viết trạng thái tiền.

## 3. Tích hợp và demo

Build/test liên quan trước merge; nếu CI có dùng đúng JDK/DB của project. Tối thiểu review liên module cho complete+kho, close+đơn+tiền, gán quầy+bàn, JWT+STOMP và đổi giỏ đa tab. Không force push nhánh dùng chung, không reset/xóa code người khác để giải conflict. Dùng revert khi cần và migration an toàn cho dữ liệu.

Chốt commit/tag bản demo, DB seed và README tương ứng. Chứng minh công nghệ thật bằng thao tác; report không ghi chức năng chưa chạy là đã hoàn thành. Không cập nhật lịch phân công từ bộ này nếu người dùng chỉ yêu cầu chốt thiết kế.
