# MilkTea — đặc tả 41 UC

Giữ mã UC và nội dung nghiệp vụ từ báo cáo cập nhật 05/10/2026. Đọc quy tắc hiện hành ở Domain; khi thay yêu cầu phải cập nhật đặc tả chịu ảnh hưởng. Đăng nhập là tiền điều kiện UC nhân viên, không tự include vào mọi UC.

## Danh mục

- UC01 Đăng nhập
- UC02 Đăng ký khách hàng
- UC03 Khôi phục mật khẩu
- UC04 Đăng xuất
- UC05 Xem menu
- UC06 Xem chi tiết món
- UC07 Cập nhật giỏ hàng
- UC08 Đặt đơn tại bàn
- UC09 Tạo đơn tại quầy
- UC10 Theo dõi đơn hàng
- UC11 Xác nhận đơn hàng
- UC12 Ghi nhận thanh toán
- UC13 Bắt đầu chế biến
- UC14 Hoàn thành đơn hàng
- UC15 Yêu cầu hủy đơn hàng
- UC16 Xử lý hủy đơn hàng
- UC17 Tạo đơn thay thế
- UC18 Duyệt hoàn tiền
- UC19 Ghi nhận hoàn tiền
- UC20 Xem lịch sử mua hàng
- UC21 Cập nhật hồ sơ
- UC22 Quản lý tài khoản
- UC23 Thiết lập thông tin quán
- UC24 Quản lý danh mục
- UC25 Quản lý món uống
- UC26 Quản lý công thức
- UC27 Quản lý bàn
- UC28 Quản lý phiên bàn
- UC29 Quản lý nguyên liệu
- UC30 Thiết lập cảnh báo tồn
- UC31 Lập phiếu nhập kho
- UC32 Lập phiếu xuất kho
- UC33 Ghi nhận mẻ sơ chế
- UC34 Ghi nhận hao hụt
- UC35 Xem lịch sử kho
- UC36 Xem báo cáo kinh doanh
- UC37 Thiết lập giảm giá chung
- UC38 Xem thông báo nghiệp vụ
- UC39 Báo cáo sự cố đơn hàng
- UC40 Gắn đơn quầy vào bàn
- UC41 Gửi thông báo thanh toán

## UC01 Đăng nhập

Tác nhân Người dùng khách

Tiền điều kiện Chưa đăng nhập; tài khoản đã đăng ký/cấp và đang hoạt động.

Luồng chính 

1. Nhập định danh và mật khẩu.

2. Hệ thống kiểm tra mật khẩu băm, trạng thái tài khoản và vai trò.

3. Cấp JWT trong cookie HttpOnly và đưa đến trang phù hợp vai trò được cấp.

Luồng thay thế Tại bước 1, chọn khôi phục mật khẩu để thực hiện UC03 riêng.

Luồng ngoại lệ Tại bước 2, sai thông tin hoặc tài khoản bị khóa: không cấp JWT, thông báo phù hợp.

Hậu điều kiện Phiên tài khoản được xác thực; không cấp quyền từ lựa chọn của người dùng.

## UC02 Đăng ký khách hàng

Tác nhân Người dùng khách

Tiền điều kiện Chưa đăng nhập; có email chưa đăng ký và thông tin hợp lệ.

Luồng chính 

1. Nhập họ tên, email, mật khẩu và xác nhận mật khẩu.

2. Hệ thống kiểm tra trùng và độ hợp lệ, băm mật khẩu.

3. Tạo tài khoản vai trò CUSTOMER và thông báo thành công.

Luồng thay thế Tại bước 1, người đã có tài khoản chuyển sang UC01.

Luồng ngoại lệ Tại bước 2, email trùng/mật khẩu không khớp: giữ dữ liệu an toàn và yêu cầu sửa.

Hậu điều kiện Chỉ tạo khách hàng; không tự đăng ký admin, thu ngân hoặc bếp.

## UC03 Khôi phục mật khẩu

Tác nhân Người dùng khách

Tiền điều kiện Chưa đăng nhập; tài khoản có email xác minh được và hệ thống có kênh gửi liên kết.

Luồng chính 

1. Nhập email yêu cầu khôi phục.

2. Hệ thống gửi liên kết chứa token ngẫu nhiên có hạn dùng; chỉ lưu hash token.

3. Mở liên kết, nhập và xác nhận mật khẩu mới.

4. Kiểm tra token; cập nhật mật khẩu băm, đánh dấu đã dùng và tăng phiên bản token tài khoản.

Luồng thay thế Tại bước 1, yêu cầu lại liên kết theo giới hạn gửi; không tiết lộ email có tồn tại hay không.

Luồng ngoại lệ Tại bước 4, token sai/hết hạn/đã dùng: từ chối. Lỗi gửi thư: thông báo gửi thất bại, không đổi mật khẩu.

Hậu điều kiện Mật khẩu mới có hiệu lực; token khôi phục cũ và JWT phiên bản cũ mất hiệu lực.

## UC04 Đăng xuất

Tác nhân Khách hàng có tài khoản, Thu ngân, Bếp, Quản trị hệ thống

Tiền điều kiện Đã đăng nhập bằng tài khoản đang có JWT hợp lệ.

Luồng chính 

1. Chọn đăng xuất.

2. Hệ thống tăng phiên bản token tài khoản và xóa JWT phía trình duyệt.

3. Trở về giao diện truy cập công khai.

Luồng thay thế Tại bước 1, JWT đã hết hạn: xóa thông tin đăng nhập cục bộ và về trang công khai.

Luồng ngoại lệ Lỗi kết nối: chưa xác nhận đăng xuất phía server; hiển thị để thử lại.

Hậu điều kiện Quyền tài khoản theo JWT cũ bị vô hiệu; không xóa lịch sử nghiệp vụ.

## UC05 Xem menu

Tác nhân Người dùng khách, Khách hàng

Tiền điều kiện Không yêu cầu đăng nhập; menu của quán hoạt động. Nếu truy cập QR, mã định danh bàn hợp lệ; không yêu cầu bàn Trống.

Luồng chính 

1. Chọn danh mục hoặc nhập từ khóa tìm món.

2. Hệ thống hiển thị món đang bán, size, giá và ảnh.

3. Áp dụng bộ lọc và hiển thị kết quả.

Luồng thay thế Tại bước 1, mở từ QR thì giữ nhãn số bàn; có phiên đang mở thì hiển thị thêm dữ liệu phiên. Không có kết quả thì hiển thị danh sách rỗng.

Luồng ngoại lệ Không tải được dữ liệu: báo lỗi và cho thử lại; không dùng giá cũ để xác nhận đặt đơn.

Hậu điều kiện Menu được hiển thị; không tạo phiên và không đổi trạng thái bàn.

## UC06 Xem chi tiết món

Tác nhân Người dùng khách, Khách hàng

Tiền điều kiện Món tồn tại; không yêu cầu đăng nhập hoặc trạng thái bàn Trống.

Luồng chính 

1. Chọn món.

2. Hiển thị mô tả, ảnh, các size, giá và tình trạng bán tại quán.

3. Khách chọn size và số lượng mong muốn.

Luồng thay thế Tại bước 3, thay size: cập nhật giá theo mã món/size tương ứng.

Luồng ngoại lệ Món ngừng bán: thông báo và không cho thêm vào đơn mới.

Hậu điều kiện Hiển thị thông tin; chọn thêm giỏ hàng là UC07.

Ghi chú cập nhật C02 C03 C21: giỏ trong phiên lưu tại hệ thống theo MaPhien và version; giỏ quầy/chưa phiên giữ RAM. UC08 mở phiên chỉ khi đặt thành công.

## UC07 Cập nhật giỏ hàng

Tác nhân Khách hàng, Người dùng khách

Tiền điều kiện Khách có hoặc không có tài khoản. Có món/size hợp lệ; nếu đang có phiên bàn, có quyền đúng phiên và phiên bản giỏ hiện tại.

Luồng chính 

1. Thêm món/size và số lượng vào giỏ.

2. Thay số lượng, ghi chú hoặc bỏ món khi còn là giỏ chưa gửi.

3. Cập nhật giỏ dự kiến; khi có phiên, server lưu giỏ dùng chung và tăng version, thông báo các tab sau commit.

Luồng thay thế Tại bước 1, chưa có phiên hoặc ở quầy: giữ giỏ RAM. Khi phiên bắt đầu, giữ phần chưa gửi trong giỏ phiên. Giỏ quầy nháp chỉ chuyển vào bàn bằng thao tác chủ động khi trang còn dữ liệu; mất RAM thì không phục hồi. Bỏ hết món thì giỏ rỗng.

Luồng ngoại lệ Món ngừng bán/lượng sai: không lưu dòng lỗi. Giỏ có version cũ: trả CART_VERSION_CONFLICT, tải bản mới và yêu cầu chọn lại; phiên đóng: làm rỗng dữ liệu phiên, không tự mở lại.

Hậu điều kiện Giỏ chỉ chứa món chưa đặt; việc sửa giỏ không đổi bàn thành Có khách, không sửa đơn đã gửi.

## UC08 Đặt đơn tại bàn

Tác nhân Khách hàng, Người dùng khách

Tiền điều kiện Khách có/không có tài khoản; QR bàn hợp lệ, giỏ không rỗng. Chưa có phiên thì gửi expectedSessionId=null; đã có phiên thì có quyền phiên hiện tại và version giỏ. Không yêu cầu bàn phải Trống hoặc đã thanh toán.

Luồng chính 

1. Khách xác nhận món và ghi chú tại bàn.

2. Server khóa bàn, kiểm tra expectedSessionId, món/size và giỏ; tính giá/giảm chung và chốt định mức.

3. Nếu chưa có phiên thì mở một phiên và đổi bàn Có khách; tạo đơn Chờ xác nhận, hóa đơn hiệu lực chưa thu và chuyển phần giỏ chưa gửi sang giỏ phiên trong cùng transaction.

4. Trả ngữ cảnh phiên, đơn/hóa đơn, giỏ mới và quyền truy cập phù hợp; thông báo thu ngân sau commit.

Luồng thay thế Tại bước 3, đã có phiên thì dùng phiên hiện tại. Đặt một phần giỏ giữ các dòng chưa gửi; đơn của CUSTOMER có MaTK, không nhận các đơn khách khác vào tài khoản.

Luồng ngoại lệ Phiên đã đổi/đóng, món không bán hoặc lỗi lưu: rollback, không để bàn Có khách do một đơn lỗi. Gửi lặp key/payload hợp lệ trả cùng đơn; key khác dữ liệu gửi hoặc giỏ cũ trả 409.

Hậu điều kiện Có một đơn Chờ xác nhận và một phiên bàn OPEN; reload/tab mới phục hồi dữ liệu phiên, không yêu cầu thu trước để xác nhận.

## UC09 Tạo đơn tại quầy

Tác nhân Thu ngân, Khách hàng, Người dùng khách

Tiền điều kiện CASHIER đã đăng nhập hoặc khách có/không có tài khoản gửi giỏ quầy hợp lệ. Không bắt buộc bàn; quyền xem của khách vãng lai chưa gắn bàn chỉ giữ trong trang hiện tại.

Luồng chính 

1. Chọn món/size, lượng và ghi chú; thu ngân có thể xác định tài khoản khách hoặc chọn bàn.

2. Server tính tiền, giảm chung và chốt định mức.

3. Tạo đơn nguồn COUNTER ở Chờ xác nhận và hóa đơn hiệu lực chưa thu; trả mã đơn và khóa quầy cho khách vãng lai.

Luồng thay thế Tại bước 1, CASHIER chọn bàn: thực hiện gắn đơn vào phiên theo UC40; đơn vẫn giữ nguồn COUNTER. Phương thức thu được chọn/ghi tại UC12 hoặc thông báo tại UC41.

Luồng ngoại lệ Giá/món không hợp lệ hoặc lỗi lưu: rollback toàn đơn/hóa đơn; gửi lặp trả đơn cũ.

Hậu điều kiện Đơn quầy được lưu; CASHIER xác nhận theo UC11 dù chưa thu. Nếu chưa gắn bàn, reload mất quyền phía khách; sau khi gắn bàn có thể xem bằng QR bàn.

## UC10 Theo dõi đơn hàng

Tác nhân Khách hàng, Thu ngân, Bếp, Quản trị hệ thống

Tiền điều kiện CUSTOMER sở hữu đơn, hoặc có quyền phiên bàn chứa đơn, hoặc khóa CounterOrderToken đúng đơn. CASHIER/KITCHEN/ADMIN đã đăng nhập và có quyền đọc nghiệp vụ tương ứng.

Luồng chính 

1. Yêu cầu xem đơn thuộc phạm vi được phép.

2. Hệ thống trả món đã chốt, trạng thái toàn đơn, hóa đơn và các thông tin thu/hoàn được phép; view phiên bao gồm các đơn bàn và quầy đã gắn.

3. Cập nhật trạng thái bằng WebSocket sau các giao dịch thành công.

Luồng thay thế Tại bước 3, mất WebSocket thì tải bản chốt bằng API. Tải lại/tab mới từ QR phục hồi đúng phiên đang mở; CUSTOMER có lịch sử riêng theo UC20.

Luồng ngoại lệ Quyền sai/phiên cũ: từ chối. Khách quầy chưa gắn bàn mất trang không được phục hồi bằng mã đơn. Phiên bàn đóng: làm rỗng hiển thị khách, không xóa giao dịch server.

Hậu điều kiện Chỉ hiển thị trạng thái đơn; không có thao tác hoàn thành cho khách/thu ngân.

Ghi chú cập nhật C05 C06: UC11–UC14 bỏ điều kiện trả tiền trước; UC12 không đổi trạng thái chế biến hoặc bàn.

## UC11 Xác nhận đơn hàng

Tác nhân Thu ngân

Tiền điều kiện Đã đăng nhập CASHIER; đơn Chờ xác nhận, không có yêu cầu hủy đang chờ. Không yêu cầu thanh toán thành công.

Luồng chính 

1. Chọn đơn và kiểm tra nội dung/nguồn.

2. Xác nhận tiếp nhận.

3. Server kiểm tra lại điều kiện, chuyển sang Chờ chế biến, ghi nhật ký và thông báo bếp.

Luồng thay thế Tại bước 1, khách muốn đổi món: xử lý hủy toàn đơn theo UC16 và tạo đơn mới.

Luồng ngoại lệ Đơn đã đổi trạng thái hoặc có yêu cầu hủy chờ: từ chối xác nhận, trả dữ liệu hiện tại. Chưa thu tiền không phải lỗi của UC này.

Hậu điều kiện Đơn Chờ chế biến được đưa vào hàng đợi bếp; trạng thái thu không đổi và chưa tiêu hao kho.

## UC12 Ghi nhận thanh toán

Tác nhân Thu ngân

Tiền điều kiện Đã đăng nhập CASHIER; hóa đơn còn hiệu lực, chưa có khoản thu toàn phần, không có yêu cầu hủy chờ. Đơn có thể trước, trong hoặc sau chế biến.

Luồng chính 

1. Kiểm tra hóa đơn và chọn tiền mặt hoặc chuyển khoản cho đơn bàn/quầy.

2. Nhận tiền mặt hoặc đối chiếu chuyển khoản thực nhận; số thu bằng số phải trả.

3. Ghi ThanhToan thành công, người/thời điểm/tham chiếu và tình trạng Đã thu trong một giao dịch; giữ nguyên trạng thái đơn và bàn.

Luồng thay thế Tại bước 2, tiền mặt: nhập tiền nhận, tính tiền thừa; chuyển khoản: nhập tham chiếu khi có. Có thể ghi thu trước, đang pha hoặc đã Hoàn thành.

Luồng ngoại lệ Chưa nhận đủ/chưa đối chiếu/hóa đơn đã hủy: không ghi thu. Gửi lặp trả bản ghi đã có; không thu lần hai và không dùng PaymentNotice làm bằng chứng đã thu.

Hậu điều kiện Có khoản thu toàn phần, không tự xác nhận/bắt đầu/hoàn thành đơn hoặc chuyển bàn Trống.

Ghi chú kỹ thuật từ Chương 4: invoice phải trả 0 do giảm 100% được CASHIER xác nhận nội bộ amount=0; không yêu cầu chuyển khoản 0 và không coi đó là tiền mặt thực nhận.

## UC13 Bắt đầu chế biến

Tác nhân Bếp

Tiền điều kiện Đã đăng nhập KITCHEN; đơn Chờ chế biến và không có yêu cầu hủy đang chờ. Đã thu hoặc chưa thu đều hợp lệ.

Luồng chính 

1. Chọn toàn đơn trong hàng đợi.

2. Xác nhận bắt đầu pha.

3. Server kiểm tra trạng thái hiện tại, chuyển Đang chế biến, ghi nhật ký và thông báo.

Luồng thay thế Tại bước 1, có cảnh báo nguyên liệu: bếp bổ sung bằng UC32/UC33 trước hoặc trong công việc.

Luồng ngoại lệ Đơn đã hủy/không đủ điều kiện: từ chối; không tạo trạng thái từng món.

Hậu điều kiện Đang chế biến toàn đơn; chưa ghi tiêu hao của đơn.

## UC14 Hoàn thành đơn hàng

Tác nhân Bếp

Tiền điều kiện Đã đăng nhập KITCHEN; đơn Đang chế biến, toàn bộ món đạt yêu cầu, có định mức chốt và dữ liệu tồn hợp lệ. Không yêu cầu đã thu tiền.

Luồng chính 

1. Bếp kiểm tra toàn bộ món và chọn Hoàn thành đơn.

2. Server kiểm tra quyền/trạng thái; tổng hợp nguyên liệu theo định mức đơn.

3. Trong một transaction, trừ tồn bếp và ghi lịch sử tiêu hao đúng một lần; chuyển đơn Hoàn thành.

4. Sau commit, thông báo trạng thái và cảnh báo tồn theo ngưỡng.

Luồng thay thế Tại bước 1, món pha hỏng: thực hiện UC39/UC34 trước khi làm lại; không hoàn thành một dòng món.

Luồng ngoại lệ Tại bước 3, thiếu số liệu tồn: rollback, giữ Đang chế biến, yêu cầu ghi bổ sung hợp lệ. Gửi lại đơn đã hoàn thành: không trừ lần hai.

Hậu điều kiện Chỉ bếp hoàn thành; kho và đơn cùng commit, không trừ lại đầu vào sơ chế. Tình trạng tiền không đổi; bàn vẫn Có khách đến khi CASHIER đóng phiên.

## UC15 Yêu cầu hủy đơn hàng

Tác nhân Khách hàng, Người dùng khách

Tiền điều kiện CUSTOMER sở hữu đơn, hoặc khách có quyền phiên chứa đơn, hoặc khóa đơn quầy đúng. Đơn Chờ xác nhận/Chờ chế biến; không có yêu cầu hủy chờ khác.

Luồng chính 

1. Chọn hủy toàn đơn và nhập lý do.

2. Hệ thống ghi yêu cầu hủy đang chờ, khóa bắt đầu chế biến và thông báo thu ngân.

3. Hiển thị trạng thái xử lý yêu cầu.

Luồng thay thế Tại bước 1, chỉ muốn bỏ một/nhiều món: vẫn yêu cầu hủy toàn đơn rồi tạo mới sau khi được chấp thuận.

Luồng ngoại lệ Đơn đã bắt đầu pha: từ chối hủy thông thường. Yêu cầu lặp: trả yêu cầu hiện có.

Hậu điều kiện Có yêu cầu hủy chờ, không tự đổi thành Đã hủy. Kho không bị trừ do yêu cầu; chưa tự hoàn tiền hoặc xóa hóa đơn.

## UC16 Xử lý hủy đơn hàng

Tác nhân Thu ngân

Tiền điều kiện Đã đăng nhập CASHIER; có yêu cầu hủy hoặc lý do nghiệp vụ; đơn chưa bắt đầu chế biến.

Luồng chính 

1. Kiểm tra đơn, lý do và tiền đã thu.

2. Chấp thuận hủy toàn đơn hoặc từ chối có lý do.

3. Nếu chấp thuận, đánh dấu đơn/hóa đơn Đã hủy; nếu đã thu tạo yêu cầu hoàn toàn số tiền thực thu.

4. Ghi nhật ký, trả kết quả và thông báo.

Luồng thay thế Tại bước 2, từ chối: đóng yêu cầu, đơn giữ trạng thái và bếp có thể tiếp tục khi đủ điều kiện.

Luồng ngoại lệ Đơn đã chuyển sang chế biến: không hủy theo luồng này; thao tác lặp không tạo hoàn tiền lần hai.

Hậu điều kiện Hủy trước pha không tiêu hao kho; tiền thu cũ còn lịch sử.

Ghi chú kỹ thuật: khoản xác nhận invoice 0 không tạo RefundRequest 0. Giữ bản ghi xác nhận và hủy hóa đơn như quy tắc chung.

## UC17 Tạo đơn thay thế

Tác nhân Khách hàng, Người dùng khách, Thu ngân

Tiền điều kiện Đơn/hóa đơn cũ đã hủy; người tạo có quyền chủ đơn/quyền phiên đang mở/khóa đơn quầy hoặc CASHIER. Phiên đóng không được dùng khóa cũ để đặt cho lượt mới.

Luồng chính 

1. Lấy các món mong muốn từ đơn cũ sang giỏ mới.

2. Bỏ/đổi món trong giỏ mới rồi xác nhận.

3. Server tạo đơn/hóa đơn mã mới, liên kết mã đơn bị thay thế và tính tiền theo cấu hình hiện hành.

Luồng thay thế Tại bước 1, có thể tạo giỏ hoàn toàn mới; nguồn bàn phải còn phiên hợp lệ.

Luồng ngoại lệ Đơn cũ chưa hủy/món không bán/phiên đóng: không tạo thay thế theo dữ liệu lỗi.

Hậu điều kiện Đơn mới Chờ xác nhận, hóa đơn hiệu lực chưa thu và liên kết ThayTheMaDH; tiền cũ không tự chuyển sang đơn mới.

## UC18 Duyệt hoàn tiền

Tác nhân Quản trị hệ thống

Tiền điều kiện Đã đăng nhập ADMIN; yêu cầu hoàn toàn đơn còn chờ duyệt, gắn thanh toán thành công và hóa đơn hủy.

Luồng chính 

1. Kiểm tra đơn, lý do và tiền thực thu.

2. Chấp thuận hoàn hoặc từ chối với lý do.

3. Lưu quyết định, người duyệt và thông báo thu ngân.

Luồng thay thế Tại bước 2, từ chối: không ghi tiền đã hoàn; lưu lý do để xử lý tiếp.

Luồng ngoại lệ Số tiền vượt khoản thực thu/đã xử lý: từ chối ghi quyết định trùng.

Hậu điều kiện Chỉ quyết định hoàn; chưa ghi giao dịch thực hoàn và chưa trừ số tiền khỏi báo cáo thu.

## UC19 Ghi nhận hoàn tiền

Tác nhân Thu ngân

Tiền điều kiện Đã đăng nhập CASHIER; yêu cầu đã được ADMIN chấp thuận, chưa thực hoàn; có giao dịch thu gốc.

Luồng chính 

1. Kiểm tra người nhận, số tiền và phương thức thu gốc.

2. Thực trả toàn bộ số tiền phải hoàn bằng phương thức phù hợp; nhập tham chiếu/lý do.

3. Server ghi số tiền, người, thời điểm thực hoàn và cập nhật yêu cầu.

Luồng thay thế Tại bước 2, chuyển khoản chưa thực hiện được: giữ trạng thái đã duyệt/chờ hoàn để xử lý lại.

Luồng ngoại lệ Gửi lặp/đã hoàn: không tạo giao dịch lần hai; lỗi lưu phải đối chiếu trước khi trả lại tiền.

Hậu điều kiện Lịch sử thu và hoàn đầy đủ; báo cáo tính tiền hoàn từ giao dịch thực hoàn.

## UC20 Xem lịch sử mua hàng

Tác nhân Khách hàng có tài khoản

Tiền điều kiện Đã đăng nhập CUSTOMER; đơn thuộc MaTK hiện tại.

Luồng chính 

1. Chọn thời gian/trạng thái cần xem.

2. Hệ thống hiển thị đơn và hóa đơn của tài khoản.

3. Chọn đơn để xem chi tiết và giao dịch liên quan.

Luồng thay thế Không có đơn: hiển thị danh sách rỗng.

Luồng ngoại lệ Truy cập đơn tài khoản khác bị từ chối. Khách vãng lai không có lịch sử tài khoản; chỉ xem dữ liệu đúng phiên mở hoặc đúng đơn quầy khi còn khóa.

Hậu điều kiện Chỉ xem lịch sử thuộc tài khoản.

## UC21 Cập nhật hồ sơ

Tác nhân Khách hàng có tài khoản

Tiền điều kiện Đã đăng nhập CUSTOMER đang hoạt động.

Luồng chính 

1. Nhập thông tin hồ sơ hoặc mật khẩu mới.

2. Hệ thống kiểm tra dữ liệu; đổi mật khẩu phải kiểm tra mật khẩu hiện tại.

3. Lưu hồ sơ; khi đổi mật khẩu, tăng phiên bản JWT và yêu cầu đăng nhập lại.

Luồng thay thế Chỉ cập nhật họ tên/số điện thoại: giữ mật khẩu hiện tại.

Luồng ngoại lệ Email/số điện thoại không hợp lệ hoặc mật khẩu cũ sai: không lưu dữ liệu lỗi.

Hậu điều kiện Không cho khách tự đổi vai trò hoặc nhận các đơn chung của bàn thành lịch sử cá nhân.

## UC22 Quản lý tài khoản

Tác nhân Quản trị hệ thống

Tiền điều kiện Đã đăng nhập ADMIN; có quyền quản lý tài khoản tại quán.

Luồng chính 

1. Nhập dữ liệu để thêm tài khoản.

2. Hệ thống kiểm tra email, vai trò ADMIN/CASHIER/KITCHEN/CUSTOMER và trạng thái hoạt động; không nhận role GUEST.

3. Lưu tài khoản và ghi người/thời điểm thao tác.

Luồng thay thế Tại bước 1, chọn sửa hoặc ngừng hoạt động tài khoản hiện có; lịch sử đã phát sinh được giữ.

Luồng ngoại lệ Tại bước 2, dữ liệu trùng/sai hoặc quan hệ bị ràng buộc: không lưu, nêu trường cần sửa.

Hậu điều kiện Tài khoản được tạo/sửa/khóa; khóa hoặc đổi quyền vô hiệu JWT cũ, không xóa lịch sử.

## UC23 Thiết lập thông tin quán

Tác nhân Quản trị hệ thống

Tiền điều kiện Đã đăng nhập ADMIN.

Luồng chính 

1. Nhập tên, địa chỉ quán và thông tin tài khoản nhận chuyển khoản.

2. Hệ thống kiểm tra độ dài, định dạng và trường bắt buộc.

3. Lưu cấu hình duy nhất của quán và người/thời điểm sửa.

Luồng thay thế Tại bước 1, chỉ sửa thông tin nhận chuyển khoản; tên/địa chỉ không thay đổi.

Luồng ngoại lệ Tại bước 2, dữ liệu sai hoặc tài khoản ngân hàng thiếu: từ chối bản sửa, giữ cấu hình đang dùng.

Hậu điều kiện Có thông tin chung của một quán; không tạo thực thể chi nhánh hoặc nhân bản cấu hình cho nhiều cửa hàng.

## UC24 Quản lý danh mục

Tác nhân Quản trị hệ thống

Tiền điều kiện Đã đăng nhập ADMIN.

Luồng chính 

1. Nhập dữ liệu để thêm danh mục.

2. Hệ thống kiểm tra mã/tên không trùng và trạng thái.

3. Lưu danh mục và ghi người/thời điểm thao tác.

Luồng thay thế Tại bước 1, chọn sửa hoặc ngừng hoạt động danh mục hiện có; lịch sử đã phát sinh được giữ.

Luồng ngoại lệ Tại bước 2, dữ liệu trùng/sai hoặc quan hệ bị ràng buộc: không lưu, nêu trường cần sửa.

Hậu điều kiện Danh mục dùng chung; món đã bán giữ quan hệ lịch sử.

## UC25 Quản lý món uống

Tác nhân Quản trị hệ thống

Tiền điều kiện Đã đăng nhập ADMIN; có danh mục hoạt động; Cloudinary được cấu hình.

Luồng chính 

1. Nhập dữ liệu để thêm món/size.

2. Hệ thống kiểm tra giá, mã size, tình trạng bán và ảnh hợp lệ; upload ảnh qua backend Cloudinary.

3. Lưu món/size và ghi người/thời điểm thao tác.

Luồng thay thế Tại bước 1, chọn sửa hoặc ngừng hoạt động món/size hiện có; lịch sử đã phát sinh được giữ.

Luồng ngoại lệ Tại bước 2, dữ liệu trùng/sai hoặc quan hệ bị ràng buộc: không lưu, nêu trường cần sửa.

Hậu điều kiện Món/size và URL/public_id ảnh được lưu; upload lỗi giữ ảnh cũ, không ghi bản sửa dở dang.

## UC26 Quản lý công thức

Tác nhân Quản trị hệ thống

Tiền điều kiện Đã đăng nhập ADMIN; nguyên liệu có đơn vị/loại hợp lệ.

Luồng chính 

1. Nhập dữ liệu để thêm công thức món hoặc sơ chế.

2. Hệ thống kiểm tra định mức dương; đầu ra sơ chế và nguyên liệu đầu vào thô; công thức món có đúng nguyên liệu trực tiếp.

3. Lưu công thức món hoặc sơ chế và ghi người/thời điểm thao tác.

Luồng thay thế Tại bước 1, chọn sửa hoặc ngừng hoạt động công thức món hoặc sơ chế hiện có; lịch sử đã phát sinh được giữ.

Luồng ngoại lệ Tại bước 2, dữ liệu trùng/sai hoặc quan hệ bị ràng buộc: không lưu, nêu trường cần sửa.

Hậu điều kiện Công thức mới dùng cho đơn/mẻ mới; định mức của đơn/mẻ đã chốt không bị thay đổi.

## UC27 Quản lý bàn

Tác nhân Quản trị hệ thống

Tiền điều kiện Đã đăng nhập ADMIN; quản lý bàn trong một quán.

Luồng chính 

1. Nhập dữ liệu để thêm bàn và mã QR.

2. Kiểm tra số/tên bàn duy nhất, mã QR định danh hợp lệ và trạng thái hoạt động.

3. Lưu bàn và mã QR và ghi người/thời điểm thao tác.

Luồng thay thế Tại bước 1, chọn sửa hoặc ngừng hoạt động bàn và mã QR hiện có; lịch sử đã phát sinh được giữ.

Luồng ngoại lệ Tại bước 2, dữ liệu trùng/sai hoặc quan hệ bị ràng buộc: không lưu, nêu trường cần sửa.

Hậu điều kiện QR mở trang chủ có nhãn bàn; không tự mở phiên và không chứa khóa đơn hoặc quyền nhân viên.

Ghi chú cập nhật C03 C20: chỉ CASHIER kết thúc phiên thủ công; giữ lịch sử và chặn lệnh phiên cũ.

## UC28 Quản lý phiên bàn

Tác nhân Thu ngân

Tiền điều kiện Đã đăng nhập CASHIER; bàn hợp lệ. Đóng phiên cần mọi đơn Hoàn thành/Đã hủy và hóa đơn còn hiệu lực đã thu đủ.

Luồng chính 

1. Chọn Có khách để mở phiên thủ công; nếu đã có phiên thì dùng phiên hiện tại, không tạo phiên thứ hai.

2. Theo dõi giỏ, đơn bàn/quầy đã gắn, trạng thái thu và dư nợ của phiên.

3. Khi đủ điều kiện, chọn Kết thúc phiên/Trống; server khóa bàn/phiên, ghi CLOSED, chuyển Trống, xóa giỏ tạm và thu hồi quyền cùng transaction; phát sự kiện sau commit.

Luồng thay thế Tại bước 3, bàn mở thủ công chưa có đơn: đóng với dư nợ 0 và xác nhận xóa giỏ nháp. Yêu cầu hoàn còn chờ vẫn được xử lý riêng sau khi đóng, không tự ghi đã hoàn.

Luồng ngoại lệ Còn đơn đang xử lý hoặc hóa đơn hiệu lực chưa thu: từ chối đóng, giữ phiên/giỏ. Lệnh cũ gửi sau đóng hoặc sau lượt mới: từ chối, không tự mở lại.

Hậu điều kiện Chỉ CASHIER đổi bàn về Trống. Tab cũ làm rỗng dữ liệu phiên; đơn, hóa đơn, thu/hoàn và lịch sử kho không bị xóa.

## UC29 Quản lý nguyên liệu

Tác nhân Quản trị hệ thống

Tiền điều kiện Đã đăng nhập ADMIN.

Luồng chính 

1. Nhập dữ liệu để thêm nguyên liệu.

2. Hệ thống kiểm tra mã, tên, loại THO/SOCHE, đơn vị chuẩn.

3. Lưu nguyên liệu và ghi người/thời điểm thao tác.

Luồng thay thế Tại bước 1, chọn sửa hoặc ngừng hoạt động nguyên liệu hiện có; lịch sử đã phát sinh được giữ.

Luồng ngoại lệ Tại bước 2, dữ liệu trùng/sai hoặc quan hệ bị ràng buộc: không lưu, nêu trường cần sửa.

Hậu điều kiện Không sửa đơn vị/loại khi đã có biến động; ngừng hoạt động giữ lịch sử.

## UC30 Thiết lập cảnh báo tồn

Tác nhân Quản trị hệ thống

Tiền điều kiện Đã đăng nhập ADMIN; có nguyên liệu, đơn vị chuẩn và bản ghi tồn theo vị trí.

Luồng chính 

1. Chọn nguyên liệu và vị trí KHO/BEP cần cấu hình.

2. Nhập ngưỡng không âm và lưu.

3. Hệ thống đánh giá tồn hiện tại, phát thông báo nếu tồn nhỏ hơn hoặc bằng ngưỡng.

Luồng thay thế Tại bước 1, cấu hình nhiều nguyên liệu; mỗi cặp nguyên liệu/vị trí có một ngưỡng độc lập.

Luồng ngoại lệ Ngưỡng không hợp lệ/khác đơn vị chuẩn: từ chối lưu.

Hậu điều kiện Cảnh báo sau các giao dịch; không tạo giữ chỗ nguyên liệu cho đơn chờ.

## UC31 Lập phiếu nhập kho

Tác nhân Quản trị hệ thống

Tiền điều kiện Đã đăng nhập ADMIN; nguyên liệu thô hợp lệ, lượng nhập dương và đơn giá không âm.

Luồng chính 

1. Nhập nguồn nhập, lý do và các dòng nguyên liệu vào KHO.

2. Kiểm tra rồi xác nhận phiếu.

3. Server tăng tồn Kho tổng, ghi biến động và thông báo sau commit.

Luồng thay thế Sửa phiếu nháp trước xác nhận; phiếu đã ghi sổ chỉ sửa sai bằng chứng từ điều chỉnh.

Luồng ngoại lệ Dòng trùng/số lượng sai: không ghi. Gửi lặp mã yêu cầu: trả phiếu hiện có.

Hậu điều kiện Phiếu, tồn và lịch sử nhất quán; không tăng tồn Bếp trực tiếp bằng phiếu nhập này.

## UC32 Lập phiếu xuất kho

Tác nhân Bếp

Tiền điều kiện Đã đăng nhập KITCHEN; nguyên liệu thô ở Kho tổng đủ; có lý do cấp cho bếp.

Luồng chính 

1. Nhập nguyên liệu, lượng và lý do xuất.

2. Xác nhận phiếu; server kiểm tra tồn và mã yêu cầu.

3. Giảm Kho tổng, tăng Bếp trong một giao dịch; lưu phiếu/lịch sử.

4. Thông báo quản trị sau commit.

Luồng thay thế Tại bước 1, chọn nhiều nguyên liệu thô trên cùng phiếu.

Luồng ngoại lệ Không đủ tồn/dữ liệu sai: không ghi nửa phiếu; gửi lặp không xuất thêm.

Hậu điều kiện Phiếu hiệu lực ngay; không cần ADMIN phê duyệt, không coi chuyển vị trí là tiêu hao.

## UC33 Ghi nhận mẻ sơ chế

Tác nhân Bếp

Tiền điều kiện Đã đăng nhập KITCHEN; có công thức sơ chế, nguyên liệu thô đã cấp ở Bếp và lượng thực dùng hợp lệ.

Luồng chính 

1. Chọn công thức, nhập quy mô mẻ và định mức dự kiến.

2. Thực hiện sơ chế; nhập lượng nguyên liệu thực dùng, lượng thành phẩm thực thu.

3. Xác nhận mẻ; server giảm nguyên liệu thô Bếp, tăng nguyên liệu sơ chế Bếp và ghi lịch sử.

4. Đánh giá ngưỡng và thông báo sau commit.

Luồng thay thế Tại bước 2, lượng thực thu khác dự kiến: ghi lượng thật và lý do, không mặc định đủ 50 suất.

Luồng ngoại lệ Thiếu tồn/lượng sai: giữ mẻ chưa ghi sổ, không cập nhật một phía; xác nhận lặp không cộng/trừ lần hai.

Hậu điều kiện Mẻ có đầu vào/đầu ra; đơn chỉ dùng thành phẩm, không tiêu hao lại đầu vào sơ chế.

## UC34 Ghi nhận hao hụt

Tác nhân Bếp, Quản trị hệ thống

Tiền điều kiện Đã đăng nhập KITCHEN hoặc ADMIN; có lượng thực hao chưa được ghi tiêu hao, đúng nguyên liệu/vị trí và lý do.

Luồng chính 

1. Chọn nguyên liệu/vị trí; nhập lượng hỏng thực tế, lý do và mã đơn/mẻ nếu có.

2. Kiểm tra lượng chưa được ghi tiêu hao cho cùng sự cố.

3. Ghi điều chỉnh giảm tồn, lịch sử và thông báo quản trị.

Luồng thay thế Hao hụt trong mẻ sơ chế được thể hiện qua lượng đầu vào/đầu ra của mẻ; chỉ ghi thêm phần chưa ghi, tránh trừ hai lần.

Luồng ngoại lệ Lượng vượt tồn/không có lý do/gửi trùng: không ghi thêm; không tự cộng kho khi hoàn tiền.

Hậu điều kiện Lượng thật đã sử dụng bị hỏng có dấu vết; không đổi trạng thái từng món.

## UC35 Xem lịch sử kho

Tác nhân Bếp, Quản trị hệ thống

Tiền điều kiện Đã đăng nhập KITCHEN hoặc ADMIN; có quyền xem sổ tồn của quán.

Luồng chính 

1. Chọn nguyên liệu, vị trí, loại chứng từ và khoảng thời gian.

2. Hiển thị tồn hiện tại và các dòng nhập/xuất/sơ chế/tiêu hao/hao hụt.

3. Mở chứng từ nguồn để xem người, lý do và lượng trước/sau.

Luồng thay thế Tại bước 1, không lọc nguyên liệu để xem toàn bộ lịch sử tại quán; không có phát sinh thì danh sách rỗng.

Luồng ngoại lệ Không có quyền đọc hoặc bộ lọc sai: từ chối; không sửa/xóa dòng biến động trực tiếp.

Hậu điều kiện Dữ liệu truy vết đến chứng từ; không sửa lịch sử trực tiếp.

## UC36 Xem báo cáo kinh doanh

Tác nhân Quản trị hệ thống

Tiền điều kiện Đã đăng nhập ADMIN; khoảng thời gian lọc hợp lệ.

Luồng chính 

1. Chọn thời gian theo múi giờ Việt Nam và loại chỉ tiêu.

2. Tổng hợp riêng giá trị đơn hoàn thành, thực thu, thực hoàn trong kỳ và dư nợ hiện tại.

3. Hiển thị thu ròng = thực thu − thực hoàn và các chứng từ đối chiếu; không suy ra tiền từ trạng thái chế biến.

Luồng thay thế Không có giao dịch trong kỳ: hiển thị 0. Đơn hoàn thành chưa thu vẫn thuộc giá trị hoàn thành, đồng thời thuộc dư nợ nếu hóa đơn hiệu lực.

Luồng ngoại lệ Khoảng thời gian sai: yêu cầu sửa; hoàn mới duyệt không được tính là đã trả.

Hậu điều kiện Không nhân bản số liệu do join dòng món; giữ khoản thu gốc của đơn hủy và chỉ trừ hoàn thực tế một lần.

## UC37 Thiết lập giảm giá chung

Tác nhân Quản trị hệ thống

Tiền điều kiện Đã đăng nhập ADMIN; tỷ lệ nhập trong 0 đến 100.

Luồng chính 

1. Nhập tỷ lệ giảm chung.

2. Hệ thống kiểm tra và lưu cấu hình cùng người/thời điểm.

3. Đơn mới của mọi khách áp dụng cấu hình này.

Luồng thay thế Nhập 0 để không giảm; không tạo chương trình hay điều kiện tài khoản.

Luồng ngoại lệ Tỷ lệ ngoài miền: không lưu; hóa đơn cũ không bị tính lại.

Hậu điều kiện Khách vãng lai và tài khoản được giảm như nhau; tỷ lệ/số tiền chốt trên hóa đơn.

## UC38 Xem thông báo nghiệp vụ

Tác nhân Thu ngân, Bếp, Quản trị hệ thống

Tiền điều kiện Đã đăng nhập CASHIER/KITCHEN/ADMIN; CONNECT/SUBSCRIBE được xác thực theo vai trò và destination.

Luồng chính 

1. Nhận sự kiện trạng thái, cảnh báo hoặc phiếu xuất trong phạm vi.

2. Hiển thị thông báo và liên kết chứng từ.

3. Mở chứng từ, gọi API kiểm tra quyền rồi hiển thị dữ liệu mới nhất.

Luồng thay thế Mất kết nối: thông báo trạng thái kết nối và tải dữ liệu qua API; kết nối lại phải xác thực lại.

Luồng ngoại lệ SUBSCRIBE ngoài quyền: từ chối; sự kiện trùng không tạo giao dịch nghiệp vụ.

Hậu điều kiện Thông báo phản ánh dữ liệu đã commit; không thay thế việc kiểm tra trạng thái trên server.

## UC39 Báo cáo sự cố đơn hàng

Tác nhân Bếp, Thu ngân, Quản trị hệ thống

Tiền điều kiện KITCHEN đã đăng nhập; đơn Đang chế biến/Hoàn thành có sự cố. CASHIER/ADMIN chỉ tham gia xử lý thông tin trong quyền, không được hoàn thành thay bếp.

Luồng chính 

1. Bếp ghi lý do, toàn đơn bị ảnh hưởng và lượng nguyên liệu hỏng thực tế.

2. Hệ thống ghi sự cố; lượng chưa tiêu hao được ghi bằng UC34, lượng đã tiêu hao giữ nguyên.

3. Nếu làm lại: giữ đơn đang pha và ghi phần hao hụt của lần hỏng; hoàn thành lần đạt qua UC14.

4. Nếu pha bù sau Hoàn thành, ghi chứng từ tiêu hao bổ sung riêng; không chạy lại UC14 để trừ định mức toàn đơn lần hai.

Luồng thay thế Tại bước 2, lượng đã ghi tiêu hao chỉ liên kết sự cố, không trừ thêm cùng lượng. Sự cố chưa xử lý xong được giữ để nhân viên phối hợp; không giả thành hủy trước pha.

Luồng ngoại lệ Thiếu lý do/lượng không xác định hoặc yêu cầu lặp: không ghi hao hụt trùng; không cho khách tự hủy đơn đang pha.

Hậu điều kiện Có lịch sử sự cố/hao hụt. Không tự hoàn tiền, cộng lại nguyên liệu hoặc mở quyền hủy thông thường cho đơn đã pha.

## UC40 Gắn đơn quầy vào bàn

Tác nhân Thu ngân

Tiền điều kiện Đã đăng nhập CASHIER; đơn nguồn COUNTER chưa thuộc phiên khác; bàn đích hoạt động, expectedSessionId đúng ngữ cảnh hiện tại.

Luồng chính

1. Chọn đơn quầy và bàn đích, xác nhận gắn bàn.

2. Server khóa bàn/đơn; dùng phiên đang mở hoặc mở Có khách theo quyền CASHIER nếu chưa có phiên.

3. Gắn cùng mã đơn vào phiên; giữ nguồn COUNTER, hóa đơn, tiền và trạng thái; lưu nhật ký.

4. Sau commit cập nhật view bàn; khách quét QR xem được đơn đã gắn.

Luồng thay thế Tại bước 2, phiên đã mở thì dùng phiên đó; gắn lại cùng bàn/phiên trả kết quả hiện có, không nhân bản đơn.

Luồng ngoại lệ Đơn thuộc phiên khác, phiên đích đã đổi/đóng hoặc lỗi lưu: từ chối/rollback, không thu lại hoặc tự chuyển bàn.

Hậu điều kiện Đơn quầy xuất hiện trong phiên đúng bàn; không tạo đơn, hóa đơn hoặc khoản thu thứ hai.

## UC41 Gửi thông báo thanh toán

Tác nhân Khách hàng, Người dùng khách

Tiền điều kiện Có quyền chủ đơn CUSTOMER, phạm vi phiên đang mở hoặc khóa đơn quầy; hóa đơn còn hiệu lực.

Luồng chính

1. Chọn tiền mặt hoặc chuyển khoản, nhập tham chiếu khi có.

2. Gửi thông báo muốn thanh toán/đã chuyển; server kiểm quyền và hóa đơn.

3. Lưu ThongBaoThanhToan và thông báo CASHIER sau commit; hiển thị Đang chờ đối chiếu.

Luồng thay thế Tại bước 1, tiền mặt thông báo nhu cầu để thu ngân nhận tiền; chuyển khoản hiển thị thông tin quán.

Luồng ngoại lệ Hóa đơn đã hủy, phạm vi sai hoặc dữ liệu lỗi: từ chối; thông báo gửi lặp không tạo khoản thu.

Hậu điều kiện Thông báo không là Payment thành công; chưa đổi Đã thu, trạng thái đơn hoặc bàn.
