# MilkTea — nghiệp vụ chuẩn

## Mục lục
1. Phạm vi và actor
2. Món và giỏ
3. QR, bàn và phiên
4. Đơn tại quầy và tài khoản
5. Đơn và trạng thái
6. Tiền, hủy và hoàn
7. Kho hai tầng
8. Giảm chung, báo cáo và lịch sử
9. Tính nhất quán và đặc tả UC

## Danh mục

- Mục lục
- 1. Phạm vi và actor
- 2. Món và giỏ
- 3. QR, bàn và phiên
- 3.1. QR không mở phiên
- 3.2. Mở phiên
- 3.3. Dữ liệu tạm và tải lại
- 3.4. Kết thúc thủ công
- 4. Đơn tại quầy và tài khoản
- 5. Đơn và trạng thái
- 6. Tiền, hủy và hoàn
- 7. Kho hai tầng
- 8. Giảm chung, báo cáo và lịch sử
- 9. Tính nhất quán và đặc tả UC

## 1. Phạm vi và actor

Phục vụ một quán. Không có entity/role/quyền/bộ lọc/kênh sự kiện theo chi nhánh. Giữ hai vị trí nguyên liệu KHO và BEP trong cùng quán.

| Actor | Quyền nghiệp vụ |
|---|---|
| Người chưa đăng nhập / khách vãng lai | Xem menu; đăng nhập; đăng ký CUSTOMER; quên mật khẩu; đặt/xem/yêu cầu hủy theo phiên bàn hoặc credential đơn quầy hợp lệ |
| CUSTOMER | Các thao tác khách; hồ sơ và lịch sử đơn thuộc tài khoản; sử dụng phiên bàn chung khi vào QR |
| CASHIER | Tạo đơn quầy, gán đơn vào phiên bàn, xác nhận đơn, xử lý yêu cầu hủy trước pha, ghi nhận tiền, thực trả hoàn đã duyệt, mở/kết thúc phiên thủ công |
| KITCHEN | Xem đơn đã xác nhận, bắt đầu/hoàn thành toàn đơn; lập phiếu xuất và mẻ; ghi sự cố/hao hụt |
| ADMIN | Quản trị tài khoản, món/danh mục/công thức/bàn/nguyên liệu; cấu hình cảnh báo/giảm chung; nhập kho, xem báo cáo/nhật ký; quyết định hoàn tiền |

Bốn role lưu cho tài khoản: ADMIN, CASHIER, KITCHEN, CUSTOMER. Không lưu role GUEST; khách không có tài khoản nhân viên tự động. ADMIN không được gọi thao tác bắt đầu/hoàn thành bếp chỉ vì là admin; nếu thay phân quyền phải chốt riêng.

## 2. Món và giỏ

Product là một biến thể món/size có mã riêng, giá và định mức riêng. Nhóm các biến thể theo tên món để hiển thị size; không tự tạo mô hình size thứ hai nếu schema đang dùng một Product cho mỗi size. Món/danh mục/công thức được ngừng hoạt động thay vì xóa khi đã có tham chiếu lịch sử.

Giỏ chứa món chưa gửi đặt; sửa/xóa dòng giỏ không phải hủy đơn. Khi gửi, server chốt giá, giảm chung, tên/size và định mức theo mã món hiện tại. Món đã đặt chuyển sang danh sách đơn, không còn trong giỏ. Không tin giá/tổng tiền client.

## 3. QR, bàn và phiên

### 3.1. QR không mở phiên

QR đưa đến trang chủ có nhãn số bàn, ví dụ Bàn 05. GET trang/đọc ngữ cảnh không kiểm tra trạng thái bàn để chặn truy cập; không tạo TableSession, không đổi trạng thái. Xem món và thêm giỏ cũng không mở phiên.

Một mã QR hợp lệ xác định bàn; kiểm tra mã tồn tại/hợp lệ là kiểm tra định danh, không phải yêu cầu bàn phải Trống. Không cho user nhập một số bàn rồi tự có quyền nhân viên.

### 3.2. Mở phiên

Mở phiên khi (a) khách gửi đơn đầu tiên hoặc (b) CASHIER chọn Có khách. Mỗi bàn có tối đa một phiên mở. Khi có phiên, gọi thêm sử dụng phiên đó. Mở phiên + Có khách + tạo đơn đầu tiên + hóa đơn + snapshot là một thao tác nhất quán; lỗi tạo đơn không để lại phiên do thao tác đó tạo ra.

Nếu CASHIER chỉ mở bàn chưa có đơn, phiên hợp lệ và giỏ được giữ từ thời điểm đó. Chuyển Có khách khi đã có phiên không tạo phiên thứ hai.

### 3.3. Dữ liệu tạm và tải lại

Trước phiên: giỏ nháp nằm trong RAM trang; đóng/reload có thể mất. Sau phiên: giữ giỏ trên hệ thống theo sessionId, cùng danh sách đơn, trạng thái, hóa đơn, khoản thu/hoàn và tổng còn phải trả. Giỏ sau phiên dùng chung cho những người vào đúng phiên. Reload, tab mới, quét lại QR tải dữ liệu phiên đang mở. Không chỉ dùng localStorage/RAM của một thiết bị để phục hồi phiên chung.

Các khoản tiền/đơn hiển thị được truy vấn từ bản ghi nghiệp vụ lâu dài. "Bộ nhớ tạm" là không gian phục vụ lượt khách; không phải yêu cầu xóa hóa đơn/Payment khỏi DB. Không sao chép một ledger thanh toán thứ hai vào bảng giỏ.

Phiên bàn khác phiên đăng nhập, HTTP session và socket. Logout, đóng tab, thanh toán, bếp complete và timeout không tự kết thúc phiên bàn. Khách cùng phiên được xem thông tin đơn/tiền phục vụ bàn; không được xem hồ sơ, email hay lịch sử ngoài phiên của người khác.

### 3.4. Kết thúc thủ công

Chỉ CASHIER gọi thao tác kết thúc phiên, chuyển bàn về TRONG. Điều kiện: tất cả đơn HOAN_THANH hoặc DA_HUY và hóa đơn còn hiệu lực đã thu đủ. Bàn mở thủ công chưa có đơn có thể kết thúc với dư nợ bằng 0; thông báo giỏ nháp sẽ bị xóa.

Đóng phiên + TRONG + xóa SessionCart + vô hiệu quyền guest/session phải cùng commit. Session/đơn/tiền/lịch sử kho vẫn được lưu. Refund còn chờ xử lý vẫn thuộc sổ nghiệp vụ, không tự được đánh dấu đã trả khi đóng bàn.

Gửi TABLE_SESSION_CLOSED sau commit. Tab cũ làm mới về nhãn bàn với dữ liệu phiên rỗng; không tự mở phiên, không tự gán giỏ cũ sang lượt sau. Lệnh mang sessionId cũ bị từ chối, kể cả bàn đang có một phiên mới. Khách chủ động quét/truy cập lại QR có thể vào lượt đang hoạt động; đây là quyền theo QR dùng chung cho phạm vi đồ án, không xác thực sự hiện diện vật lý của từng người.

## 4. Đơn tại quầy và tài khoản

Đơn quầy chưa gán bàn: guest giữ orderId và CounterOrderToken trong RAM trang, không tự lưu/khôi phục qua URL, cookie, browser storage hay lịch sử. Reload mất khả năng xem lại phía guest; bản ghi đơn vẫn còn ở thu ngân.

CASHIER chọn bàn khi tạo đơn hoặc gán sau: liên kết đơn vào phiên bàn hiện tại; nếu chưa có phiên, mở qua thao tác thu ngân Có khách. Không tạo lại đơn/hóa đơn, không thu lại, không đổi trạng thái pha và vẫn giữ source=COUNTER. Không chuyển đơn khỏi một phiên khác đang hoạt động trong tính năng gán đơn này. QR tự xác định bàn, không thể tự đoán đơn quầy chưa được gán; phải có liên kết rõ.

Giỏ quầy chưa gửi chỉ chuyển vào ngữ cảnh bàn bằng thao tác chủ động khi trang vẫn giữ dữ liệu, không tự phục hồi sau khi đã mất. CUSTOMER có lịch sử đơn theo accountId được server ghi khi tạo. Gán vào bàn không tự nhận đơn guest cũ thành đơn cá nhân của người mới đăng nhập.

## 5. Đơn và trạng thái

OrderStatus: CHO_XAC_NHAN, CHO_CHE_BIEN, DANG_CHE_BIEN, HOAN_THANH, DA_HUY. Không có CHO_THANH_TOAN trong OrderStatus. OrderItem không có trạng thái chế biến hay nút hủy riêng.

| Từ | Sang | Actor/điều kiện |
|---|---|---|
| Tạo mới | CHO_XAC_NHAN | Khách hoặc CASHIER tạo hợp lệ |
| CHO_XAC_NHAN | CHO_CHE_BIEN | CASHIER xác nhận; không yêu cầu đã thu |
| CHO_CHE_BIEN | DANG_CHE_BIEN | KITCHEN bắt đầu; không có yêu cầu hủy đang chờ xử lý |
| DANG_CHE_BIEN | HOAN_THANH | Chỉ KITCHEN; tiêu hao kho hợp lệ cùng transaction |
| CHO_XAC_NHAN/CHO_CHE_BIEN | DA_HUY | CASHIER chấp thuận hủy trước pha |

Customer/guest gửi CancellationRequest khi đơn còn trước pha; CASHIER chấp thuận/từ chối có lý do. Khi có yêu cầu hủy chờ xử lý, bếp chưa được bắt đầu. Không cung cấp PATCH status tùy ý, cancel item hay CASHIER complete.

## 6. Tiền, hủy và hoàn

TABLE và COUNTER đều có CASH/BANK_TRANSFER. Thu bất kỳ lúc nào trước/trong/sau pha. Payment độc lập OrderStatus; HOAN_THANH chưa chắc đã thu, thu xong chưa chắc đã pha xong.

Phạm vi hiện tại: một hóa đơn/đơn, một khoản thu toàn phần thành công; không thanh toán từng phần/chia theo người. Phiên có nhiều đơn, tổng hợp dư nợ mỗi hóa đơn. Khách gửi PaymentNotice (phương thức/tham chiếu) chỉ là thông báo, không tự tạo khoản thu thành công. CASHIER đối chiếu tiền thực nhận và ghi Payment; không bắt buộc tích hợp ngân hàng/gateway.

InvoiceStatus HIEU_LUC/DA_HUY khác tình trạng đã thu. ReceiptState CHUA_THU/DA_THU là projection từ việc có Payment thành công, không trộn vào Invoice.status; RefundStatus CHO_DUYET/DA_DUYET/TU_CHOI/DA_HOAN là quá trình hoàn riêng. Thu gốc vẫn còn khi hoàn.

Nếu hóa đơn phải trả 0 do giảm 100%, CASHIER ghi xác nhận nghĩa vụ hoàn tất amount=0 ở sổ nội bộ, không bắt khách chuyển khoản 0 đồng. Giữ method CASH nội bộ cho bản ghi 0, không coi là tiền mặt thực nhận; không tạo yêu cầu hoàn tiền cho Payment amount=0. Không tự chuyển trạng thái đơn/bàn.

Hủy toàn đơn, không sửa đơn cũ để bỏ một món. Đơn chưa thu bị hủy: không hoàn tiền. Đơn đã thu bị hủy: giữ Payment, tạo yêu cầu hoàn toàn khoản đã thu, ADMIN quyết định và CASHIER thực trả; chỉ DA_HOAN mới tính hoàn thực tế. Không tự hoàn từ nút hủy. Đơn thay thế lưu replacementOfOrderId, lập hóa đơn/giá/thu riêng; không tự chuyển khoản thu cũ.

Hủy khi DANG_CHE_BIEN/HOAN_THANH bị từ chối theo luồng thông thường. Món đã pha hỏng là sự cố/hao hụt; không giả thành hủy trước pha và không tự cộng nguyên liệu. Lệnh hủy/thu/bắt đầu kiểm tra lại trạng thái lúc ghi để không hủy xong vẫn nhận khoản thu mới.

## 7. Kho hai tầng

MaterialType THO (đường, ly, nguyên liệu đầu vào) hoặc SOCHE (trân châu đã nấu). Stock theo materialId + location KHO/BEP + đơn vị chuẩn. ADMIN đặt ngưỡng từng nguyên liệu theo vị trí KHO/BEP; cảnh báo là thông tin để bếp bổ sung, không giữ chỗ nguyên liệu cho các đơn chờ.

StockIssue do KITCHEN lập: phiếu/dòng/người/thời điểm/lý do, chuyển KHO → BEP, ghi hai phía, thông báo ADMIN sau commit. Không có bước chờ ADMIN duyệt xuất.

PreparationRecipe là công thức chuẩn; PreparationBatch là mẻ thực tế. Ví dụ 5 kg thô tạo 50 suất SOCHE: ghi mẻ giảm đầu vào THO tại BEP và tăng 50 suất SOCHE tại BEP. Ghi sản lượng thực thu, có lý do nếu lệch chuẩn.

Order complete tiêu hao BEP theo snapshot: trân châu SOCHE tính suất, đường/ly THO tính trực tiếp. Không trừ lại 5 kg thô đã dùng khi nấu mẻ. Khi tồn gần ngưỡng, bếp chủ động xuất thêm thô và ghi mẻ, hoặc xuất nguyên liệu trực tiếp tùy loại.

Không giữ chỗ kho khi xác nhận. Lúc ghi sổ vẫn phải kiểm tra tồn hiện có, không cho âm, chống ghi lặp. Nếu thiếu lúc complete: rollback complete và tiêu hao; bếp bổ sung rồi thử lại. Phiếu không tự tạo nguyên liệu từ hư không.

Hỏng khi chưa ghi tiêu hao: ghi lượng đã thực dùng thành HAO_HUT. Hỏng sau khi đã ghi tiêu hao: lưu sự cố liên kết lượng đã ghi, không trừ lại cùng lượng; nếu pha bù, ghi tiêu hao bổ sung có chứng từ riêng. Không tự khôi phục nguyên liệu.

## 8. Giảm chung, báo cáo và lịch sử

Một GlobalSettings có discountPercent 0..100 cho mọi khách, không có chương trình/code voucher/member discount. Snapshot hóa đơn giữ giá và giảm tại lúc đặt, không đổi khi cấu hình thay đổi.

Báo cáo tách: giá trị đơn hoàn thành theo completedAt; thực thu theo Payment.paidAt; thực hoàn theo Refund.refundedAt; thu ròng=thực thu-thực hoàn trong cùng kỳ; dư nợ hiện tại theo hóa đơn còn hiệu lực chưa thu. Không loại khoản thu gốc khỏi thực thu chỉ vì đơn đã hủy rồi trừ hoàn lần nữa. Không đếm DA_DUYET là đã trả hoàn.

Lưu lịch sử quyết định trạng thái, thu/hoàn, xuất/mẻ/tiêu hao/hao hụt. Không ghi mật khẩu/JWT/guest token vào lịch sử. Không xóa giao dịch khi bàn trở về Trống.

## 9. Tính nhất quán và đặc tả UC

Các phép ghi nhiều bảng cùng thành công hoặc cùng rollback: mở phiên/tạo đơn; xác nhận hủy/tạo refund; complete/tiêu hao; chuyển kho hai phía; ghi mẻ hai tầng; đóng phiên/xóa giỏ. Phát sự kiện sau commit.

Giữ yêu cầu giảng viên: UC bắt đầu bằng động từ; tiếng Việt thống nhất; basic/alternate/exception path; tiền điều kiện ghi actor/vai trò/trạng thái cụ thể; bước chỉ mô tả công việc của UC; phân biệt include và tiền điều kiện; kiểm soát kế thừa; sơ đồ tổng quát dùng Package. Đăng nhập là UC của người chưa đăng nhập và là tiền điều kiện các UC nhân viên, không include login vào mọi UC. Không tự sửa các UC đã đúng nếu thay đổi không tác động.
