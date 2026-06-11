package org.nep.system.event;

import lombok.Getter;
import org.nep.system.entity.User;
import org.springframework.context.ApplicationEvent;

/**
 * 用户注册事件 - 用于异步发送邮件
 */
@Getter
public class UserRegisterEvent extends ApplicationEvent {
    private final User user;

    public UserRegisterEvent(Object source, User user) {
        super(source);
        this.user = user;
    }
}
