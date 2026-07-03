package org.nep.common.context;

/**
 * 当前登录用户上下文（请求级 ThreadLocal）。
 * <p>由 {@code JwtInterceptor} 在请求进入时写入用户ID与角色，
 * 供 Controller / Service 在任意层直接读取，用于角色鉴权与数据归属校验，
 * 避免层层透传 userId 参数。请求结束后由拦截器 {@link #clear()} 释放，防止线程复用串号。</p>
 *
 * @author NEP Team
 */
public final class UserContext {

    private UserContext() {
    }

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    /** 写入当前用户信息（拦截器调用） */
    public static void set(Long userId, String role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }

    /** 当前登录用户ID，未登录返回 null */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /** 当前登录用户角色编码，未登录返回 null */
    public static String getRole() {
        return ROLE.get();
    }

    /** 释放上下文，务必在请求结束时调用 */
    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
    }
}
