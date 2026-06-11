package org.nep.system.event;

import org.nep.system.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户注册事件监听器 - 异步发送欢迎邮件
 */
@Component
public class UserRegisterListener {

    private static final Logger log = LoggerFactory.getLogger(UserRegisterListener.class);
    private final EmailService emailService;

    public UserRegisterListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async("taskExecutor")
    @EventListener
    public void handleRegisterEvent(UserRegisterEvent event) {
        log.info("收到注册事件，准备发送邮件给: {}", event.getUser().getPhone());
        try {
            emailService.sendWelcomeEmail(event.getUser());
            log.info("欢迎邮件发送成功");
        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage());
        }
    }
}
