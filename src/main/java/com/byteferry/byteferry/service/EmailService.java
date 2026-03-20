package com.byteferry.byteferry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    /**
     * 发送验证码邮件
     */
    public void sendVerificationCode(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("ByteFerry - 邮箱验证码");
            helper.setText(buildHtmlContent(code), true);

            mailSender.send(message);
            log.info("验证码邮件已发送: {}", toEmail);
        } catch (MessagingException e) {
            log.error("发送邮件失败: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }
    }

    private String buildHtmlContent(String code) {
        return """
                <div style="max-width:400px;margin:0 auto;padding:32px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
                  <h2 style="color:#333;margin-bottom:24px;">ByteFerry 验证码</h2>
                  <p style="color:#666;font-size:14px;margin-bottom:16px;">您的验证码为：</p>
                  <div style="background:#f5f5f5;border-radius:8px;padding:16px;text-align:center;margin-bottom:16px;">
                    <span style="font-size:32px;font-weight:bold;letter-spacing:8px;color:#333;">%s</span>
                  </div>
                  <p style="color:#999;font-size:12px;">验证码有效期为 5 分钟，请勿将验证码告知他人。</p>
                </div>
                """.formatted(code);
    }
}
