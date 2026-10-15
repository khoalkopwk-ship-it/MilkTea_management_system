package vn.edu.ute.milktea.service.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:khoalkopwk@gmail.com}}")
    private String mailFrom;

    @Value("${app.mail.sender-name:MilkTea Cafe}")
    private String senderName;

    /**
     * Gửi email mã OTP xác thực tài khoản đăng ký mới.
     * Tách biệt khỏi database transaction để không giữ kết nối DB khi chờ SMTP.
     */
    public void sendRegistrationOtpEmail(String toEmail, String fullName, String otpCode, int expiryMinutes) {
        String recipientName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : "Quý khách";
        String subject = "[MilkTea] Mã xác thực đăng ký tài khoản";

        String htmlContent = buildOtpEmailHtml(
                recipientName,
                "Xác thực địa chỉ email đăng ký tài khoản",
                "Cảm ơn bạn đã lựa chọn MilkTea! Vui lòng sử dụng mã xác thực gồm 6 chữ số dưới đây để hoàn tất kích hoạt tài khoản của bạn:",
                otpCode,
                expiryMinutes,
                "Nếu bạn không thực hiện yêu cầu đăng ký này, xin vui lòng bỏ qua email hoặc thông báo cho chúng tôi để bảo vệ tài khoản.");

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * Gửi email mã xác thực khôi phục / đặt lại mật khẩu.
     */
    public void sendPasswordResetOtpEmail(String toEmail, String fullName, String resetCode, int expiryMinutes) {
        String recipientName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : "Quý khách";
        String subject = "[MilkTea] Mã xác thực đặt lại mật khẩu";

        String htmlContent = buildOtpEmailHtml(
                recipientName,
                "Yêu cầu đặt lại mật khẩu",
                "Hệ thống nhận được yêu cầu đặt lại mật khẩu cho tài khoản MilkTea liên kết với địa chỉ email này. Vui lòng sử dụng mã xác thực dưới đây:",
                resetCode,
                expiryMinutes,
                "Lưu ý quan trọng: Không cung cấp mã này cho bất kỳ ai, kể cả nhân viên quán. Nếu bạn không gửi yêu cầu này, hãy đổi mật khẩu ngay lập tức hoặc bỏ qua email.");

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * Thực hiện gửi mail qua SMTP và ném lỗi rõ ràng nếu thất bại.
     */
    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

            helper.setFrom(mailFrom, senderName);
            helper.setTo(toEmail.trim().toLowerCase());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Đã gửi email thành công tới: {}", maskEmail(toEmail));
        } catch (MessagingException | MailException e) {
            log.error("Lỗi gửi email SMTP tới {}: {}", maskEmail(toEmail), e.getMessage());
            throw BusinessException.badRequest(
                    ErrorCode.MAIL_SEND_FAILED,
                    "Không thể gửi email xác thực qua hệ thống SMTP. Vui lòng thử lại sau giây lát hoặc kiểm tra kết nối mạng.");
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi gửi email tới {}: {}", maskEmail(toEmail), e.getMessage());
            throw BusinessException.badRequest(
                    ErrorCode.MAIL_SEND_FAILED,
                    "Gặp lỗi khi tạo thư điện tử xác thực: " + e.getMessage());
        }
    }

    /**
     * Tạo template HTML responsive, thẩm mỹ thương hiệu MilkTea
     */
    private String buildOtpEmailHtml(String recipientName, String title, String description,
            String code, int expiryMinutes, String warning) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <style>
                        body { margin: 0; padding: 0; background-color: #f4f6f8; font-family: 'Segoe UI', Helvetica, Arial, sans-serif; color: #333333; }
                        .container { max-width: 580px; margin: 30px auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,0.06); }
                        .header { background: linear-gradient(135deg, #795548 0%%, #4e342e 100%%); padding: 32px 24px; text-align: center; color: #ffffff; }
                        .brand-title { font-size: 26px; font-weight: bold; margin: 0; letter-spacing: 1px; }
                        .brand-subtitle { font-size: 13px; opacity: 0.85; margin-top: 6px; }
                        .content { padding: 32px 28px; }
                        .greeting { font-size: 17px; font-weight: 600; margin-bottom: 12px; }
                        .intro { font-size: 14px; line-height: 1.6; color: #555555; margin-bottom: 24px; }
                        .otp-box { background-color: #faf6f0; border: 2px dashed #bcaaa4; border-radius: 10px; padding: 20px; text-align: center; margin: 24px 0; }
                        .otp-title { font-size: 12px; text-transform: uppercase; letter-spacing: 1.5px; color: #8d6e63; margin-bottom: 8px; font-weight: 600; }
                        .otp-code { font-size: 34px; font-weight: 800; letter-spacing: 8px; color: #4e342e; font-family: 'Courier New', monospace; margin: 4px 0; }
                        .otp-expiry { font-size: 13px; color: #e65100; margin-top: 8px; font-weight: 500; }
                        .warning-box { background-color: #fff8e1; border-left: 4px solid #ffb300; padding: 12px 16px; border-radius: 4px; font-size: 13px; color: #6d4c41; line-height: 1.5; margin-top: 24px; }
                        .footer { background-color: #fbfbfb; border-top: 1px solid #eeeeee; padding: 20px; text-align: center; font-size: 12px; color: #888888; }
                        .footer p { margin: 4px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="brand-title">🍵 MilkTea Management</div>
                            <div class="brand-subtitle">Hệ Thống Trà Sữa Tươi Cao Cấp</div>
                        </div>
                        <div class="content">
                            <div class="greeting">Xin chào, %s!</div>
                            <div class="intro">%s</div>
                            <div class="otp-box">
                                <div class="otp-title">MÃ XÁC THỰC CỦA BẠN</div>
                                <div class="otp-code">%s</div>
                                <div class="otp-expiry">⏳ Mã có hiệu lực trong vòng <strong>%d phút</strong></div>
                            </div>
                            <div class="warning-box">
                                <strong>Lưu ý an toàn:</strong> %s
                            </div>
                        </div>
                        <div class="footer">
                            <p><strong>Cửa hàng Trà Sữa MilkTea</strong></p>
                            <p>Địa chỉ: Số 1 Võ Văn Ngân, TP. Thủ Đức, TP. Hồ Chí Minh</p>
                            <p>Thư này được tạo tự động từ hệ thống. Xin vui lòng không trả lời thư này.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(title, recipientName, description, code, expiryMinutes, warning);
    }

    /**
     * Che địa chỉ email để bảo mật thông tin khi log (ví dụ: kh***wk@gmail.com).
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        String namePart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        if (namePart.length() <= 2) {
            return namePart.charAt(0) + "***" + domainPart;
        } else if (namePart.length() <= 4) {
            return namePart.charAt(0) + "***" + namePart.charAt(namePart.length() - 1) + domainPart;
        } else {
            return namePart.substring(0, 2) + "***" + namePart.substring(namePart.length() - 2) + domainPart;
        }
    }
}
