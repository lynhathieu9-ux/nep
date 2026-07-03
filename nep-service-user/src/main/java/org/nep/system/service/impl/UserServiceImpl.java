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

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = this.getById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        String oldEnc = DigestUtils.md5DigestAsHex(oldPassword.getBytes(StandardCharsets.UTF_8));
        if (!oldEnc.equals(user.getPassword())) throw new BusinessException("原密码错误");

        String newEnc = DigestUtils.md5DigestAsHex(newPassword.getBytes(StandardCharsets.UTF_8));
        user.setPassword(newEnc);
        this.updateById(user);
    }

    /**
     * 问题③：管理员重置指定用户密码（无需校验原密码）。
     * 新密码同样 MD5 加密后落库，与注册/改密加密方式保持一致。
     */
    @Override
    public void resetPassword(Long userId, String newPassword) {
        User user = this.getById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码至少6位");
        }
        String newEnc = DigestUtils.md5DigestAsHex(newPassword.getBytes(StandardCharsets.UTF_8));
        user.setPassword(newEnc);
        this.updateById(user);
    }
}
