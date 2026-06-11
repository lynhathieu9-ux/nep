package org.nep.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.nep.common.exception.BusinessException;
import org.nep.common.utils.JwtUtils;
import org.nep.system.dto.LoginResult;
import org.nep.system.entity.User;
import org.nep.system.event.UserRegisterEvent;
import org.nep.system.mapper.UserMapper;
import org.nep.system.service.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import java.nio.charset.StandardCharsets;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final ApplicationEventPublisher eventPublisher;

    public UserServiceImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public User register(User user) {
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        w.eq(User::getPhone, user.getPhone());
        if (this.count(w) > 0) throw new BusinessException("该手机号已注册");

        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes(StandardCharsets.UTF_8)));
        user.setStatus(1);
        user.setRole("NEPS");
        this.save(user);

        // 发布注册事件（异步发送邮件）
        eventPublisher.publishEvent(new UserRegisterEvent(this, user));
        return user;
    }

    @Override
    public User login(String phone, String password) {
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        w.eq(User::getPhone, phone);
        User user = this.getOne(w);
        if (user == null) throw new BusinessException("用户不存在");
        if (user.getStatus() == 0) throw new BusinessException("账号已被禁用");

        String enc = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!enc.equals(user.getPassword())) throw new BusinessException("密码错误");

        user.setPassword(null);
        return user;
    }

    /** 登录并返回Token */
    public LoginResult loginWithToken(String phone, String password) {
        User user = login(phone, password);
        String token = JwtUtils.generate(user.getId(), user.getPhone(), user.getRole());
        return LoginResult.of(user, token);
    }
}
