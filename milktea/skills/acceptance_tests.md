# MilkTea — ca nghiệm thu nghiệp vụ và tích hợp

Đối chiếu T01–T60 với Chương 7 của báo cáo cập nhật 05/10/2026. Đây là **tiêu chí cần thực thi**, chưa xác nhận ứng dụng đã chạy đạt. Ghi ID, actor/phạm vi, dữ liệu trước/sau, bước, expected/actual, người test, ngày và commit; lưu ảnh/log không chứa token/mật khẩu.

## Danh mục ca

- T01 Khách vãng lai đặt tại bàn
- T02 Tải lại trang trong phiên bàn
- T03 Mở tab mới hoặc quét lại QR
- T04 Đơn quầy tiền mặt sau pha
- T05 Đơn quầy chuyển khoản
- T06 Tại bàn thanh toán tiền mặt
- T07 Đơn chưa thu được chế biến
- T08 Quyền hoàn thành
- T09 Trạng thái toàn đơn
- T10 Bỏ một món trước pha
- T11 Hủy chưa thu tiền
- T12 Hủy đã thu tiền trước pha
- T13 Hủy khi đang pha
- T14 Đơn thay thế
- T15 Khuyến mãi mọi khách
- T16 Giá/khuyến mãi đã chốt
- T17 Phiếu xuất của bếp
- T18 Mẻ sơ chế 5 kg/50 suất
- T19 Hoàn thành một đơn
- T20 Cảnh báo sau 45 ly
- T21 Hoàn thành gửi lại
- T22 Phiếu xuất gửi lại
- T23 Sơ chế gửi lại
- T24 Thiếu ghi nhận bổ sung
- T25 Hỏng trước và sau tiêu hao
- T26 Hoàn tiền mới duyệt
- T27 Thanh toán/hoàn gửi lặp
- T28 JWT sai/hết hạn/khóa
- T29 Quyền giữa các phiên bàn
- T30 Khóa đơn quầy khác
- T31 WebSocket sau commit
- T32 Cloudinary
- T33 Lệnh phiên cũ sau đóng
- T34 Đóng phiên nhiều đơn
- T35 Khôi phục mật khẩu
- T36 Khởi tạo demo sạch
- T37 Quét QR không mở phiên
- T38 Giỏ trước phiên
- T39 Thu ngân mở Có khách thủ công
- T40 Đơn đầu tiên lỗi
- T41 Hai lệnh mở phiên đồng thời
- T42 Hai tab cùng sửa giỏ
- T43 Đặt một phần giỏ
- T44 Thu xong không đóng bàn
- T45 Hoàn thành không đóng bàn
- T46 Đóng đủ điều kiện
- T47 Đóng bàn không đơn
- T48 Close cạnh tranh tạo đơn
- T49 Guest quầy reload
- T50 Gắn đơn quầy vào bàn
- T51 Gắn vào bàn chưa phiên
- T52 Không tự nhận đơn quầy
- T53 Thông báo tiền chưa phải thu
- T54 Hủy cạnh tranh start/thu
- T55 Thiếu CSRF
- T56 Vô hiệu JWT và socket
- T57 Nguồn báo cáo khác nhau
- T58 QR chung không lộ hồ sơ
- T59 Giữ hoàn chờ sau đóng
- T60 Phân biệt phiên và đăng nhập

## T01 Khách vãng lai đặt tại bàn

Đầu vào và thao tác Quét QR bàn Trống, thêm giỏ và gửi đơn đầu tiên.

Kết quả mong đợi Chỉ gửi đơn thành công mới mở OPEN/Có khách; đơn Chờ xác nhận, chưa thu; QR không bị chặn vì trạng thái bàn.

## T02 Tải lại trang trong phiên bàn

Đầu vào và thao tác Có phiên/giỏ/đơn/thu; tải lại trang từ QR.

Kết quả mong đợi Khôi phục đúng giỏ chung, đơn và lịch sử thu; không mở thêm phiên.

## T03 Mở tab mới hoặc quét lại QR

Đầu vào và thao tác Có phiên OPEN; mở tab mới/quét lại cùng QR.

Kết quả mong đợi Trang chủ có nhãn bàn; cùng giỏ/đơn/tiền, token riêng đúng cùng scope.

## T04 Đơn quầy tiền mặt sau pha

Đầu vào và thao tác Tạo quầy, xác nhận/pha/hoàn thành trước; thu tiền mặt sau.

Kết quả mong đợi Pha chưa thu hợp lệ; tính tiền thừa, ghi một khoản thu; trạng thái đơn không đổi.

## T05 Đơn quầy chuyển khoản

Đầu vào và thao tác Tạo đơn tại quầy, chọn chuyển khoản.

Kết quả mong đợi Chỉ xác nhận khoản thu thành công sau khi đối chiếu tiền thực nhận; xác nhận đơn gửi bếp vẫn không yêu cầu đã thu.

## T06 Tại bàn thanh toán tiền mặt

Đầu vào và thao tác Chọn tiền mặt cho hóa đơn tại bàn; CASHIER nhận đủ.

Kết quả mong đợi Server ghi thu hợp lệ; không đổi chế biến, không tự chuyển bàn Trống.

## T07 Đơn chưa thu được chế biến

Đầu vào và thao tác CASHIER xác nhận đơn chưa thu; KITCHEN bắt đầu rồi hoàn thành với đủ tồn.

Kết quả mong đợi Được phép, có tiêu hao đúng; trạng thái tiền vẫn Chưa thu, bàn Có khách.

## T08 Quyền hoàn thành

Đầu vào và thao tác CASHIER, CUSTOMER, khách vãng lai và ADMIN gửi lệnh hoàn thành; KITCHEN thực hiện.

Kết quả mong đợi Chỉ KITCHEN hợp lệ; các actor khác bị từ chối ở server, kho không thay đổi do lệnh sai.

## T09 Trạng thái toàn đơn

Đầu vào và thao tác Đặt nhiều món, bếp bắt đầu rồi hoàn thành.

Kết quả mong đợi Chỉ một trạng thái ở DonHang; không có trạng thái/điểm hoàn thành từng món.

## T10 Bỏ một món trước pha

Đầu vào và thao tác Khách muốn bỏ một món trong hóa đơn đã tạo.

Kết quả mong đợi Hủy toàn hóa đơn/đơn; tạo hóa đơn/đơn mới; không sửa/xóa một dòng cũ.

## T11 Hủy chưa thu tiền

Đầu vào và thao tác Hủy toàn đơn chưa thanh toán.

Kết quả mong đợi Hóa đơn Đã hủy; không tạo hoàn tiền, không tiêu hao kho.

## T12 Hủy đã thu tiền trước pha

Đầu vào và thao tác Thu ngân chấp thuận hủy đơn đã trả tiền.

Kết quả mong đợi Đơn/hóa đơn Đã hủy; giao dịch thu cũ còn; tạo hoàn toàn số đã thu.

## T13 Hủy khi đang pha

Đầu vào và thao tác Khách yêu cầu hủy đơn Đang chế biến.

Kết quả mong đợi Hủy thông thường bị từ chối; không tự hoàn tiền/cộng kho.

## T14 Đơn thay thế

Đầu vào và thao tác Từ đơn hủy tạo đơn mới bớt món.

Kết quả mong đợi Mã mới, số tiền/khuyến mãi mới; thanh toán mới, không tự kế thừa đã trả tiền.

## T15 Khuyến mãi mọi khách

Đầu vào và thao tác Cùng giỏ và tỷ lệ chung, một khách vãng lai và một khách có tài khoản.

Kết quả mong đợi Mức giảm như nhau; không kiểm tra đăng nhập để áp dụng giảm.

## T16 Giá/khuyến mãi đã chốt

Đầu vào và thao tác Đổi giá và tỷ lệ chung sau khi tạo hóa đơn.

Kết quả mong đợi Hóa đơn cũ giữ giá, tỷ lệ và số tiền giảm cũ.

## T17 Phiếu xuất của bếp

Đầu vào và thao tác Bếp cấp 5 kg trân châu khô từ kho tổng sang bếp.

Kết quả mong đợi Kho tổng -5 kg, bếp +5 kg; ghi ngay, không chờ duyệt; quản trị nhận thông báo.

## T18 Mẻ sơ chế 5 kg/50 suất

Đầu vào và thao tác Xác nhận mẻ dùng 5 kg đã cấp, thu 50 suất.

Kết quả mong đợi Bếp trân châu khô -5 kg, trân châu nấu sẵn +50 suất; lưu mẻ và biến động.

## T19 Hoàn thành một đơn

Đầu vào và thao tác Định mức một ly: 1 suất trân châu, 20 g đường, 1 ly nhựa.

Kết quả mong đợi Khi hoàn thành trừ các lượng tương ứng ở bếp; không trừ thêm trân châu khô ở kho tổng.

## T20 Cảnh báo sau 45 ly

Đầu vào và thao tác Có 50 suất, ngưỡng 5; hoàn thành các đơn dùng tổng 45 suất.

Kết quả mong đợi Còn 5 suất, phát cảnh báo; bếp xuất và sơ chế bổ sung theo quy trình.

## T21 Hoàn thành gửi lại

Đầu vào và thao tác Gửi lại request hoàn thành đơn đã thành công.

Kết quả mong đợi Trả trạng thái hiện có, không trừ kho hoặc ghi tiêu hao lần hai.

## T22 Phiếu xuất gửi lại

Đầu vào và thao tác Gửi lại mã yêu cầu lập phiếu đã thành công.

Kết quả mong đợi Không tạo phiếu/biến động trùng.

## T23 Sơ chế gửi lại

Đầu vào và thao tác Xác nhận lại mẻ đã hoàn thành.

Kết quả mong đợi Không trừ nguyên liệu thô hoặc tăng thành phẩm lần hai.

## T24 Thiếu ghi nhận bổ sung

Đầu vào và thao tác Số liệu tồn bếp không đủ lúc hoàn thành.

Kết quả mong đợi Giữ trạng thái hiện tại; báo cần ghi nhận bổ sung; không tồn âm hoặc ghi nửa giao dịch.

## T25 Hỏng trước và sau tiêu hao

Đầu vào và thao tác Ghi hỏng trước hoàn thành, sau hoàn thành và khi pha bù.

Kết quả mong đợi Chỉ lượng chưa ghi bị trừ HAO_HUT; sau tiêu hao chỉ liên kết sự cố; pha bù chứng từ riêng, không cộng kho/trừ kép.

## T26 Hoàn tiền mới duyệt

Đầu vào và thao tác Quản trị duyệt nhưng thu ngân chưa trả tiền.

Kết quả mong đợi Báo cáo không coi là tiền đã hoàn; ghi thực hoàn mới cập nhật.

## T27 Thanh toán/hoàn gửi lặp

Đầu vào và thao tác Gửi lại cùng thao tác thu hoặc hoàn.

Kết quả mong đợi Không ghi tiền lần hai; số hoàn không vượt số còn có thể hoàn.

## T28 JWT sai/hết hạn/khóa

Đầu vào và thao tác Dùng token lỗi hoặc token tài khoản bị khóa.

Kết quả mong đợi Không truy cập chức năng nội bộ; không chỉ ẩn nút trên giao diện.

## T29 Quyền giữa các phiên bàn

Đầu vào và thao tác Token PB05 yêu cầu xem/ghi PB06 và SUBSCRIBE kênh staff.

Kết quả mong đợi Từ chối khác phạm vi hoặc role; số bàn đơn thuần không là credential.

## T30 Khóa đơn quầy khác

Đầu vào và thao tác Khách chưa gắn bàn dùng CounterOrderToken ĐH1 với ĐH2.

Kết quả mong đợi Không đọc/yêu cầu hủy/notice đơn khác; reload không phục hồi từ mã đơn.

## T31 WebSocket sau commit

Đầu vào và thao tác Xác nhận/start/complete/thu/sửa giỏ/xuất kho, thử rollback và reconnect.

Kết quả mong đợi Đúng vai trò/scope; rollback không có event thành công; HTTP resync sau reconnect.

## T32 Cloudinary

Đầu vào và thao tác Upload ảnh hợp lệ và thử file lỗi.

Kết quả mong đợi Lưu URL/public_id ảnh mới khi thành công; lỗi không mất ảnh cũ; không lộ API secret.

## T33 Lệnh phiên cũ sau đóng

Đầu vào và thao tác Đóng PB05 rồi mở lượt mới cùng bàn; tab cũ đặt bằng MaPhien cũ.

Kết quả mong đợi 409 closed/changed; không auto-open hoặc đặt vào phiên mới, không carry giỏ cũ.

## T34 Đóng phiên nhiều đơn

Đầu vào và thao tác Một bàn nhiều đơn; thử đóng phiên khi còn pha hoặc hóa đơn hiệu lực chưa thu.

Kết quả mong đợi Từ chối, không làm rỗng dữ liệu. Khi mọi đơn xong/hủy và thu đủ, CASHIER đóng phiên thủ công, giữ lịch sử.

## T35 Khôi phục mật khẩu

Đầu vào và thao tác Token sai, hết hạn hoặc đã dùng.

Kết quả mong đợi Từ chối; token đúng cập nhật mật khẩu băm và vô hiệu token theo chính sách.

## T36 Khởi tạo demo sạch

Đầu vào và thao tác Chạy DB/source theo README trên máy trình diễn.

Kết quả mong đợi Một quán, bốn role, nhiều bàn/QR, món/size/ảnh, THO/SOCHE, KHO/BEP, đủ công nghệ chạy thật.

## T37 Quét QR không mở phiên

Đầu vào và thao tác Bàn Trống; GET context, xem menu/chi tiết nhiều lần.

Kết quả mong đợi Trang chủ giữ nhãn; Ban/PhienBan không đổi, không có phiên mới.

## T38 Giỏ trước phiên

Đầu vào và thao tác Thêm/xóa giỏ khi chưa OPEN rồi reload.

Kết quả mong đợi Bàn vẫn Trống; giỏ RAM có thể mất; không tạo SessionCart khi chưa có phiên.

## T39 Thu ngân mở Có khách thủ công

Đầu vào và thao tác CASHIER mở bàn chưa có đơn, thêm giỏ rồi reload.

Kết quả mong đợi Một OPEN/Có khách; giỏ phiên được giữ. Mở lặp không sinh phiên thứ hai.

## T40 Đơn đầu tiên lỗi

Đầu vào và thao tác Bàn chưa phiên; đặt món ngừng bán hoặc lỗi DB.

Kết quả mong đợi Rollback toàn thao tác; không còn phiên mới/Có khách do đơn lỗi.

## T41 Hai lệnh mở phiên đồng thời

Đầu vào và thao tác Hai trang cùng gửi đơn đầu tiên expectedSessionId=null.

Kết quả mong đợi Khóa bàn cho tối đa một OPEN; request stale báo đồng bộ, không hai phiên.

## T42 Hai tab cùng sửa giỏ

Đầu vào và thao tác Hai PUT dùng cùng version/If-Match.

Kết quả mong đợi Một ghi; tab stale nhận 409 và tải bản mới, không mất giỏ tab kia.

## T43 Đặt một phần giỏ

Đầu vào và thao tác Giỏ 3 ly, chọn gửi 2 ly; gửi đúng version.

Kết quả mong đợi Đơn 2 ly, giỏ còn 1 ly; version tăng, không copy món đã gửi trở lại.

## T44 Thu xong không đóng bàn

Đầu vào và thao tác Thu đủ toàn bộ hóa đơn nhưng chưa CASHIER close.

Kết quả mong đợi Bàn vẫn Có khách, giỏ/đơn/tiền còn cho tab và QR.

## T45 Hoàn thành không đóng bàn

Đầu vào và thao tác Bếp hoàn thành, thử trạng thái bàn trước/sau.

Kết quả mong đợi Hoàn thành + tiêu hao, bàn vẫn Có khách kể cả đã thu.

## T46 Đóng đủ điều kiện

Đầu vào và thao tác Mọi đơn Hoàn thành/Đã hủy, hóa đơn hiệu lực thu đủ; CASHIER close.

Kết quả mong đợi CLOSED/TRONG; giỏ/dòng xóa, credential revoke, tab làm rỗng; chứng từ lịch sử giữ.

## T47 Đóng bàn không đơn

Đầu vào và thao tác Mở thủ công, có giỏ nháp nhưng không có đơn, dư nợ 0.

Kết quả mong đợi CASHIER xác nhận xóa giỏ và đóng; không tự tạo Order/Payment.

## T48 Close cạnh tranh tạo đơn

Đầu vào và thao tác CASHIER đóng phiên cùng lúc khách gửi thêm đơn.

Kết quả mong đợi Cùng khóa bàn/phiên: đóng phiên kiểm tra điều kiện thấy đơn mới hoặc request bị chặn; không đơn lọt sau CLOSED.

## T49 Guest quầy reload

Đầu vào và thao tác Tạo đơn COUNTER chưa bàn rồi đóng/reload trang khách.

Kết quả mong đợi Mất RAM/khóa phía khách vãng lai; CASHIER vẫn thấy đơn, không tự hủy hoặc xóa.

## T50 Gắn đơn quầy vào bàn

Đầu vào và thao tác Đơn COUNTER đã thu/đang pha; CASHIER gắn PB05 rồi quét QR.

Kết quả mong đợi Giữ mã/nguồn/hóa đơn/thu/state; QR PB05 thấy đơn, không thu lại hoặc nhân bản.

## T51 Gắn vào bàn chưa phiên

Đầu vào và thao tác CASHIER gắn đơn quầy vào bàn chưa OPEN.

Kết quả mong đợi Mở một phiên bằng quyền CASHIER, Có khách và gắn đơn cùng commit.

## T52 Không tự nhận đơn quầy

Đầu vào và thao tác Quét QR khi chưa có liên kết; đăng nhập CUSTOMER trong phiên.

Kết quả mong đợi Không đoán đơn quầy theo tên/IP; không nhận đơn khách vãng lai của cả bàn vào lịch sử cá nhân.

## T53 Thông báo tiền chưa phải thu

Đầu vào và thao tác Khách chọn chuyển khoản và gửi notice.

Kết quả mong đợi Notice tới CASHIER, receipt vẫn Chưa thu; không auto confirm/start/close.

## T54 Hủy cạnh tranh start/thu

Đầu vào và thao tác Gửi request hủy/chấp thuận cùng lúc KITCHEN bắt đầu hoặc ghi thu.

Kết quả mong đợi Không hủy sau đã pha; không khoản thu mới vào hóa đơn đã hủy; pending hủy khóa start.

## T55 Thiếu CSRF

Đầu vào và thao tác POST browser/guest không X-CSRF-TOKEN, CONNECT thiếu bảo vệ cần thiết.

Kết quả mong đợi Từ chối theo policy; UI lấy CSRF mới sau login/logout.

## T56 Vô hiệu JWT và socket

Đầu vào và thao tác Logout/reset/khóa/đổi role khi đang có kết nối.

Kết quả mong đợi HTTP và socket cũ mất quyền, không tiếp tục nhận event nội bộ.

## T57 Nguồn báo cáo khác nhau

Đầu vào và thao tác Đơn A trị giá 100k Hoàn thành nhưng chưa thu; đơn B trị giá 50k đã thu toàn phần, rồi hủy trước pha và hoàn thực tế trong kỳ.

Kết quả mong đợi Giá trị hoàn thành khác thực thu; thu gốc vẫn tính, hoàn thực tế trừ một lần; hiện dư nợ.

## T58 QR chung không lộ hồ sơ

Đầu vào và thao tác Guest dùng quyền phiên đọc view/đơn và thử history tài khoản.

Kết quả mong đợi View chỉ dữ liệu phục vụ phiên; không email/hồ sơ/lịch sử ngoài scope.

## T59 Giữ hoàn chờ sau đóng

Đầu vào và thao tác Đơn hủy đã thu có refund chưa trả; các hóa đơn hiệu lực đều đã thu, đơn đều kết thúc; CASHIER close.

Kết quả mong đợi Refund/audit giữ, ADMIN/CASHIER xử lý sau đóng; không tự đánh dấu DA_HOAN.

## T60 Phân biệt phiên và đăng nhập

Đầu vào và thao tác Đóng tab, ngắt socket, logout, token expiry khi bàn Có khách.

Kết quả mong đợi Không tự TRONG/CLOSED; phiên bàn vẫn còn và QR phục hồi được khi hợp lệ.

## Kiểm tra bổ sung hợp đồng kỹ thuật

| ID | Thao tác | Mong đợi |
|---|---|---|
| API01 | ADMIN sửa thông tin quán/tài khoản chuyển khoản | SettingsWrite/View có trường tương ứng UC23, không chỉ discountPercent |
| API02 | ADMIN cấu hình ngưỡng cùng nguyên liệu ở KHO và BEP | Mỗi vị trí giữ ngưỡng riêng, Material không là nơi chứa threshold chung |
| API03 | Báo sự cố sau complete rồi pha bù | Incident không tự trừ lại; chứng từ tiêu hao bổ sung chỉ ghi lượng pha bù, không chạy complete lần hai |
| API04 | Giảm 100%, CASHIER xác nhận invoice 0 rồi hủy | Không chuyển khoản 0, không tạo refund 0; order/bàn giữ quy tắc độc lập |
| API05 | CREATE đơn bàn trả thành công | Có context phiên/giỏ/version chuẩn ngay trong response; không mất giỏ còn lại |
| API06 | Hai lần thay ảnh sản phẩm đồng thời | Phiên bản bị xung đột không ghi đè ảnh mới của thao tác khác; cleanup chỉ đúng public_id do thao tác sở hữu |

Chạy `python3 scripts/validate_contract.py` để kiểm định dạng/ref/liên kết/phạm vi hợp đồng. Kiểm này không thay E2E trên DB thật, JWT, WebSocket hoặc Cloudinary. Chỉ ghi đạt sau khi có kết quả thực thi.

| API07 | Ghi sự cố → ghi pha bù → resolve; gọi resolve lặp | Projection OPEN→RESOLVED qua audit liên kết, không debit lặp, không đổi trạng thái đơn/tiền; lịch sử giữ sau đóng bàn |
| API08 | Key 65 ký tự; enum hủy qua API và DB | Từ chối key quá 64, không cắt; PENDING/APPROVED/REJECTED ánh xạ đúng CHO/CHAP_THUAN/TU_CHOI |
