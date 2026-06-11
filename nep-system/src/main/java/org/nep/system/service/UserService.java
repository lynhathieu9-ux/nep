package org.nep.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.nep.system.entity.User;

public interface UserService extends IService<User> {
    User register(User user);
    User login(String phone, String password);
}
