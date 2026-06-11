package org.nep.system.service.impl;

import jakarta.mail.internet.MimeMessage;
import org.nep.system.entity.User;
import org.nep.system.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮箱服务实现
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendWelcomeEmail(User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            // 优先使用用户填写的邮箱，否则fallback到手机号QQ邮箱
            String toEmail = (user.getEmail() != null && !user.getEmail().isEmpty())
                    ? user.getEmail() : user.getPhone() + "@qq.com";
            helper.setTo(toEmail);
            helper.setSubject("欢迎加入东软环保公众监督系统！");
            helper.setText(String.format("""
                    <h2>🌿 欢迎 %s 加入东软环保公众监督系统！</h2>
                    <p>您已成功注册为<b>公众监督员</b>。</p>
                    <p>现在您可以：</p>
                    <ul>
                        <li>选择网格区域并提交空气质量监督信息</li>
                        <li>浏览历史反馈记录</li>
                        <li>参与环保公众监督行动</li>
                    </ul>
                    <p>感谢您为环境保护贡献力量！</p>
                    <br/>
                    <p style="color:#999;">东软环保公众监督系统 NEP v1.0</p>
                    """, user.getRealName()), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("发送邮件失败: {}", e.getMessage());
        }
    }
}
