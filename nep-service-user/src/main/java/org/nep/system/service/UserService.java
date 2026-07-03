package org.nep.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.nep.system.entity.User;

public interface UserService extends IService<User> {
    User register(User user);
    User login(String phone, String password);
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 管理员重置指定用户密码（无需原密码）。
     * <p>问题③：管理员端"账户与权限"新增重置密码能力。</p>
     *
     * @param userId      目标用户ID
     * @param newPassword 新密码明文（服务端 MD5 加密后落库）
     */
    void resetPassword(Long userId, String newPassword);
}
