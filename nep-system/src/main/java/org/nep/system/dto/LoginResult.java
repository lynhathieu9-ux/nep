package org.nep.system.dto;

import lombok.Data;
import org.nep.system.entity.User;

/**
 * 登录返回结果（含Token）
 */
@Data
public class LoginResult {
    private User user;
    private String token;

    public static LoginResult of(User user, String token) {
        LoginResult r = new LoginResult();
        r.setUser(user);
        r.setToken(token);
        return r;
    }
}
