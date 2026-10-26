# MilkTea — hướng dẫn AI đọc và thực hiện thiết kế

Đọc file này trước khi phân tích, viết code hoặc review website bán trà sữa MilkTea (phạm vi hiện tại: một quán). Đây là bộ quy tắc Markdown và OpenAPI dành cho AI/developer, giữ hình thức thư mục `skills/` của bộ được cung cấp; không tự cài skill cá nhân hay thực thi lệnh khi chỉ được yêu cầu đọc tài liệu.

## 0. Nguồn báo cáo và cách nạp

Bám báo cáo `Nhom03_ThietKeGiaoDien.docx`, bản cập nhật 05/10/2026 (95 trang, 41 UC, 22 màn hình, 60 ca T01–T60). Báo cáo dùng SQL Server trong các DB được phép. Bản quy tắc này bổ sung hợp đồng kỹ thuật cần để thực thi các nghiệp vụ đó; không chứng nhận source đã chạy.

`GEMINI.md` ở gốc project là chỉ dẫn nạp cho Gemini/Antigravity. `skills/SKILL.md` là entry point chuẩn cho agent hỗ trợ SKILL.md. Nếu dùng Codex/Claude hoặc công cụ khác, đặt cả thư mục skill vào vị trí mà công cụ thực tế hỗ trợ và giữ các đường dẫn tương đối; không coi các file Markdown rời là đã tự được nạp. Đọc INDEX → Domain/Workflow/Architecture → file liên quan theo nhiệm vụ.

## 1. Nguồn quyết định

Áp dụng yêu cầu mới nhất của người dùng trước tài liệu cũ. Bộ này phản ánh quyết định ngày 05/10/2026: một quán; QR chỉ gắn nhãn bàn; gửi đơn hoặc thu ngân mở phiên; thanh toán linh hoạt; đóng phiên thủ công. Khi sửa yêu cầu, cập nhật file sở hữu quy tắc rồi sửa các hợp đồng, code và ca kiểm thử liên quan. Không khôi phục nghiệp vụ cũ từ báo cáo/phân công chưa cập nhật.

Một quy tắc có một file sở hữu. File còn lại tham chiếu và triển khai, không tự định nghĩa khác.

| File có thật trong bộ này | Nội dung sở hữu | Đọc khi |
|---|---|---|
| [domain_system.md](domain_system.md) | Actor, chức năng, trạng thái, phiên bàn, đơn/tiền/kho, quyền nghiệp vụ | Mọi thay đổi nghiệp vụ |
| [workflow_system.md](workflow_system.md) | Luồng tổng thể/chi tiết, giao dịch và trao đổi giữa module | Hiểu vận hành hoặc sửa workflow |
| [report_traceability.md](report_traceability.md) | Truy vết 41 UC, màn hình, Service và test | Đối chiếu thiết kế với báo cáo |
| [use_case_specifications.md](use_case_specifications.md) | Đặc tả UC theo báo cáo hiện tại | Viết/sửa yêu cầu và tài liệu UC |
| [database_dictionary.md](database_dictionary.md) | Tên SQL và trường trong báo cáo | Ánh xạ schema/entity |
| [system_architecture.md](system_architecture.md) | Stack, package, HTTP envelope, lỗi, pagination, thời gian, ranh giới hệ thống | Thiết kế và tích hợp |
| [milktea-openapi.yaml](milktea-openapi.yaml) | Method/path, request/response, header và security theo API | Viết Controller hoặc gọi API |
| [backend_rule.md](backend_rule.md) | Service/JPA/DTO/transaction/idempotency và phối hợp các tầng | Viết backend |
| [author_rule.md](author_rule.md) | Spring Security, JWT, CSRF, quyền tài khoản/phiên bàn/đơn quầy | Auth, quyền và bảo vệ API |
| [database_rule.md](database_rule.md) | Mô hình bảng, FK/unique/index, snapshot, migration và lựa chọn DB | Entity, Repository, SQL |
| [frontend_rule.md](frontend_rule.md) | Thymeleaf/Bootstrap/JS, giỏ, khôi phục phiên, UI từng actor | Giao diện |
| [realtime_rule.md](realtime_rule.md) | STOMP, destination, event, after-commit, reconnect | WebSocket |
| [media_storage_rule.md](media_storage_rule.md) | Cloudinary, upload/thay ảnh, lỗi ngoài transaction SQL | Ảnh sản phẩm |
| [docker_reverse_proxy_rule.md](docker_reverse_proxy_rule.md) | Chạy IDE/JAR, Docker/Nginx nếu triển khai, biến môi trường | Chạy và deploy |
| [github_workflow.md](github_workflow.md) | Nhánh, PR, tích hợp và review liên module | Phối hợp code |
| [scripts/validate_contract.py](scripts/validate_contract.py) | Kiểm link/ref/schema cơ bản và invariant hợp đồng | Khi sửa bộ rules/OpenAPI |
| [acceptance_tests.md](acceptance_tests.md) | Các ca kiểm chứng quy tắc và mẫu ghi kết quả | Kiểm thử và demo |

`author_rule.md` giữ tên file gốc nhưng nội dung là authentication/authorization, không phải quy tắc tác giả bài viết. Hợp đồng API của bộ này là `milktea-openapi.yaml`. Không tham chiếu các tên file khác không tồn tại trong ZIP.

## 2. Quy trình cho AI

1. Xác định người dùng yêu cầu thiết kế, hướng dẫn, sửa code hay tạo artifact. Không tuyên bố đã viết/chạy hệ thống khi chỉ sửa rules.
2. Đọc Domain, Architecture; sau đó chỉ đọc các owner file cần cho nhiệm vụ. Khi làm HTTP đọc OpenAPI; khi làm DB đọc Database.
3. Nếu có repository, đọc `AGENTS.md`, `pom.xml`, cấu hình và file liên quan; dùng `rg`. Tên class trong bộ là hợp đồng thiết kế đề xuất, không phải bằng chứng class đã có.
4. Giữ code hiện có nếu phù hợp. Không đổi phiên bản Spring/Java, đổi DB hoặc package nền chỉ vì sở thích; xác minh tương thích theo tài liệu chính thức của phiên bản đang dùng.
5. Hoàn thiện nghiệp vụ xuyên suốt Controller → Service → Repository/Entity → DTO/UI. Không để mock thay dữ liệu thật trong bản nghiệm thu.
6. Với thay đổi nhiều trạng thái, xác định transaction, chống ghi lặp, quyền và sự kiện sau commit trước khi code.
7. Chạy kiểm tra phù hợp, ghi kết quả thực tế; nêu phần chưa xác minh. Dùng các ca trong Acceptance để review.
8. Nếu chỉ thiếu lựa chọn nội bộ, chọn cách đơn giản phù hợp bộ này. Nếu thiếu quyết định làm đổi quyền/tiền/kho/hủy, nêu đúng điểm cần chốt; tiếp tục phần độc lập. Không tự thêm quy tắc lớn.

## 3. Phạm vi

Dùng Spring Boot + Spring MVC + Thymeleaf + Bootstrap + JavaScript + Spring Data JPA + một SQL Server/MySQL/PostgreSQL + Spring Security/JWT + Spring WebSocket/STOMP + Cloudinary. Cloudairy trong đề bài được hiểu là Cloudinary.

Không chuyển sang SPA, kiến trúc phân tán hoặc dịch vụ dữ liệu khác. Không mang chức năng mạng xã hội, quota video, chatbot, chi nhánh, trạng thái từng món hoặc yêu cầu thanh toán trước vào bản này. Docker/Nginx là cách triển khai khi cần, không phải điều kiện để code cốt lõi chạy từ IDE.

## 4. Điểm kiểm tra nhanh

Trước mỗi thay đổi hỏi: quy tắc thuộc file nào; phiên nào được phép thao tác; actor nào được phép; ghi sổ có lặp không; tiền và trạng thái đơn có bị trộn không; reload/đóng phiên có đúng không?

Không biến giỏ tạm thành doanh thu; không xóa giao dịch lịch sử khi xóa dữ liệu phiên; không tự chuyển bàn về Trống từ payment/order/job/trigger. Các ví dụ triển khai trong bộ không cho phép bỏ qua yêu cầu mới của người dùng.
