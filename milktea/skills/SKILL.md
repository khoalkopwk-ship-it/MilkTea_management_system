---
name: design-milktea
description: Thiết kế, triển khai, sửa và kiểm tra website bán trà sữa MilkTea một quán bằng Spring Boot, Thymeleaf, Bootstrap, JPA, SQL Server hoặc MySQL/PostgreSQL, JWT, WebSocket và Cloudinary. Dùng khi xử lý kiến trúc, API, database, phân quyền, QR/phiên bàn/giỏ đa tab, đơn tại bàn/quầy, thanh toán linh hoạt, hủy/hoàn, chế biến và kho hai tầng hoặc cập nhật UC/báo cáo của dự án MilkTea.
---

# Thiết kế hệ thống MilkTea

Đọc [INDEX.md](INDEX.md) trước. Khi chuẩn bị bộ xuất cho Gemini/Antigravity, lấy mẫu [assets/GEMINI.md](assets/GEMINI.md) làm loader ở gốc project cùng cấp thư mục `skills/`. Giữ nguyên tắc **ONE RULE → ONE OWNER → ONE SOURCE OF TRUTH**. Áp dụng yêu cầu mới nhất của người dùng; bộ này dựa trên báo cáo `Nhom03_ThietKeGiaoDien.docx` cập nhật 05/10/2026, 41 UC và 60 ca nghiệm thu.

## Thực hiện nhiệm vụ

1. Xác định đầu ra được yêu cầu: phân tích, hướng dẫn, code, review hay tài liệu. Khi có source, đọc `AGENTS.md`, `pom.xml`, cấu hình DB và những lớp thực sự liên quan.
2. Đọc [domain_system.md](domain_system.md), [workflow_system.md](workflow_system.md) và [system_architecture.md](system_architecture.md). Đọc thêm các tài liệu sở hữu phần đang làm theo bảng dưới; tránh đọc lại toàn bộ báo cáo cho mỗi thay đổi nhỏ.
3. Xác định actor, phạm vi phiên/đơn, trạng thái trước/sau, dữ liệu được ghi, transaction, hành vi khi lặp/lỗi và sự kiện sau commit trước khi viết code.
4. Giữ một Spring Boot MVC monolith. Triển khai Thymeleaf/Bootstrap, JPA và **một** DB SQL Server/MySQL/PostgreSQL; dùng JWT, WebSocket/STOMP, Cloudinary thật. Chọn SQL Server khi khởi tạo mới theo báo cáo; giữ DB/phiên bản/package đã chốt trong source khi phù hợp.
5. Hoàn thiện nghiệp vụ xuyên Controller → Service → Repository/Entity → DTO và UI. Config tạo bean/cấu hình; Service thực hiện chức năng. Không buộc báo cáo hay giỏ guest quầy phải có Entity.
6. Cập nhật OpenAPI, schema/migration, giao diện và ca kiểm thử bị ảnh hưởng cùng quy tắc sở hữu. Với quyết định mới làm đổi tiền/quyền/kho, nêu rõ ảnh hưởng; tiếp tục phần độc lập trong phạm vi đã được yêu cầu.
7. Chạy kiểm tra phù hợp source và ghi bằng chứng thực tế. Kiểm bộ hợp đồng bằng `python3 scripts/validate_contract.py`; lệnh này kiểm tài liệu, không thay kiểm thử ứng dụng.

## Đọc theo phần

| Tài liệu | Mục đích |
|---|---|
| [domain_system.md](domain_system.md) | Sở hữu quy tắc nghiệp vụ, actor, trạng thái, tiền, kho, khuyến mãi |
| [workflow_system.md](workflow_system.md) | Vận hành từ tổng thể tới chi tiết; phối hợp module và phục hồi lỗi |
| [system_architecture.md](system_architecture.md) | Stack, package, envelope/lỗi, số/thời gian, cấu hình chung |
| [milktea-openapi.yaml](milktea-openapi.yaml) | Hợp đồng HTTP, DTO, header và quyền; đọc operation liên quan |
| [backend_rule.md](backend_rule.md) | Service, transaction, khóa, idempotency, JPA |
| [database_rule.md](database_rule.md) | Entity/quan hệ/constraint/index và snapshot |
| [database_dictionary.md](database_dictionary.md) | Tên bảng/trường trong báo cáo, ánh xạ sang Java |
| [author_rule.md](author_rule.md) | JWT/CSRF, quyền tài khoản/phiên bàn/đơn quầy, quyền socket |
| [frontend_rule.md](frontend_rule.md) | Thymeleaf/Bootstrap/JS và các màn hình actor |
| [realtime_rule.md](realtime_rule.md) | STOMP, after-commit, đóng phiên, reconnect |
| [media_storage_rule.md](media_storage_rule.md) | Upload/thay ảnh Cloudinary và bù lỗi ngoài SQL |
| [docker_reverse_proxy_rule.md](docker_reverse_proxy_rule.md) | Chạy IDE/JAR, environment, Docker/Nginx khi cần |
| [github_workflow.md](github_workflow.md) | Ownership và tích hợp code của nhóm |
| [report_traceability.md](report_traceability.md) | F/NF/QĐ/UC/MH/Test và đầu mối |
| [use_case_specifications.md](use_case_specifications.md) | Tiền điều kiện và basic/alternate/exception của 41 UC |
| [acceptance_tests.md](acceptance_tests.md) | T01–T60 của báo cáo và các kiểm tra hợp đồng bổ sung |

## Giữ các ranh giới quyết định

- Quét QR luôn vào trang chủ có nhãn bàn; xem/thêm giỏ không mở phiên. Chỉ đơn đầu tiên thành công hoặc CASHIER mở Có khách mới tạo phiên.
- Giữ giỏ phiên trên server và phục hồi khi reload/tab mới; giỏ trước phiên và khóa guest quầy chưa bàn chỉ nằm trong RAM trang. Gắn đơn quầy vào bàn giữ cùng mã đơn/tiền/trạng thái.
- Tách OrderStatus, hiệu lực Invoice, khoản thu và hoàn. Cho tiền mặt/chuyển khoản trước, trong hoặc sau pha; CASHIER xác nhận/ghi thu, **chỉ KITCHEN hoàn thành toàn đơn**.
- Hủy trước pha là hủy toàn đơn/hóa đơn rồi tạo mới. Giữ khoản thu gốc và hoàn thực tế; không tự sửa món đã đặt hay cộng lại nguyên liệu.
- Ghi tiêu hao THO trực tiếp + SOCHE khi complete cùng transaction. Bếp xuất thủ công KHO→BEP, không admin duyệt; mẻ sơ chế giảm THO và tăng SOCHE một lần.
- Chỉ CASHIER đóng phiên thủ công khi mọi đơn xong/hủy và hóa đơn hiệu lực thu đủ. Xóa giỏ/quyền tạm và UI; giữ lịch sử nghiệp vụ, chặn lệnh phiên cũ.

Đọc chi tiết ở file sở hữu trước khi triển khai. Không đưa chi nhánh, trạng thái từng món, cổng thanh toán tự động, SPA hay kiến trúc phân tán vào phạm vi đã chốt. Không tự cài bộ này vào một công cụ khác khi người dùng chỉ yêu cầu đọc/thiết kế.
