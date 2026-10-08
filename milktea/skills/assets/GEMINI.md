# GEMINI.md — Hướng dẫn Antigravity & Gemini cho MilkTea

Áp dụng bộ quy tắc của website bán trà sữa **MilkTea một quán**. Dùng **ONE RULE → ONE OWNER → ONE SOURCE OF TRUTH**.

## Trình tự đọc trước khi phân tích hoặc sửa code

1. Đọc `skills/INDEX.md` và entry point `skills/SKILL.md`.
2. Đọc `skills/domain_system.md`, `skills/workflow_system.md`, `skills/system_architecture.md` để xác định nghiệp vụ, luồng vận hành và kiến trúc.
3. Đọc file sở hữu phần đang làm:
   - HTTP: `skills/milktea-openapi.yaml`.
   - Backend/transaction: `skills/backend_rule.md`.
   - JWT/CSRF/quyền guest/socket: `skills/author_rule.md`.
   - Entity/Repository/SQL: `skills/database_rule.md`, `skills/database_dictionary.md`.
   - UI: `skills/frontend_rule.md`.
   - WebSocket: `skills/realtime_rule.md`.
   - Cloudinary: `skills/media_storage_rule.md`.
   - Chạy/deploy: `skills/docker_reverse_proxy_rule.md`.
   - Phối hợp code: `skills/github_workflow.md`.
   - UC/truy vết: `skills/use_case_specifications.md`, `skills/report_traceability.md`.
   - Kiểm thử: `skills/acceptance_tests.md`.
4. Nếu có source, đọc `AGENTS.md`, `pom.xml`, config và các file thực tế liên quan; không coi tên class trong rules là class đã có.
5. Xác định actor/phạm vi/trạng thái trước-sau, transaction, chống gửi lặp và event sau commit trước khi code. Cập nhật owner rule/OpenAPI/migration/UI/test liên quan cùng thay đổi.
6. Chạy kiểm tra phù hợp; ghi bằng chứng thật và những phần chưa xác minh. Khi sửa bộ rules, dùng `python3 skills/scripts/validate_contract.py`; lệnh này không kiểm thử ứng dụng.

## Stack và quy tắc giữ nguyên

Spring Boot + Thymeleaf + Bootstrap + JPA + một SQL Server/MySQL/PostgreSQL + JWT + WebSocket/STOMP + Cloudinary. SQL Server là mặc định theo báo cáo; nếu source đã chốt DB khác trong ba DB được phép thì giữ và dùng DDL/driver đúng vendor. Đọc phiên bản Java/Spring từ `pom.xml`, không tự đặt phiên bản từ dự án mẫu.

QR luôn vào homepage có nhãn bàn, không chặn vì Có khách, không mở phiên khi xem/thêm giỏ. Phiên mở khi đơn đầu tiên thành công hoặc CASHIER mở Có khách. Giỏ phiên server phục hồi qua reload/tab mới; guest quầy chưa gắn bàn chỉ giữ RAM. CASHIER gắn cùng đơn quầy vào phiên để QR thấy, không tạo/thu lại.

Tiền mặt/chuyển khoản trước/trong/sau pha; không paid gate. CASHIER xác nhận đơn/ghi thu, chỉ KITCHEN complete toàn đơn; không trạng thái từng món. Hủy trước pha toàn đơn/hóa đơn rồi tạo mới; giữ thu/hoàn và không credit kho. Complete tiêu hao THO trực tiếp và SOCHE trong cùng transaction; bếp xuất KHO→BEP không admin duyệt, ghi mẻ bổ sung khi cảnh báo.

Chỉ CASHIER đóng phiên thủ công khi mọi đơn xong/hủy và hóa đơn hiệu lực đã thu đủ. Clear giỏ/quyền/view, giữ chứng từ và metadata lịch sử; không tự đóng từ thanh toán/complete/logout/socket timeout và không đưa lệnh phiên cũ sang lượt mới.

## Phạm vi sử dụng

Đặt file này ở gốc project, cùng cấp `skills/`. Đây là chỉ dẫn nạp cho AI; nếu môi trường không tự đọc GEMINI.md, yêu cầu AI đọc file này trước. Không tự thêm chi nhánh, SPA, kiến trúc phân tán hoặc đổi quy tắc lớn. Yêu cầu mới nhất của người dùng được ưu tiên; cập nhật bộ quy tắc chịu ảnh hưởng.
