package org.nep.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限注解。标注在 Controller 类或方法上，声明允许访问的角色集合。
 * <p>由 {@code RoleInterceptor} 在请求进入时校验：当前登录用户的角色
 * （来自网关注入并由 JwtInterceptor 解析写入 {@code UserContext}）
 * 必须命中 {@link #value()} 之一，否则返回 403。</p>
 *
 * <pre>
 *   &#64;RequiresRole(Roles.ADMIN)                 // 仅管理员可指派
 *   &#64;RequiresRole({Roles.ADMIN, Roles.DECISION}) // 管理员或决策者可看统计
 * </pre>
 *
 * @author NEP Team
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {

    /** 允许访问的角色编码（见 {@link org.nep.common.constant.Roles}），满足其一即可放行 */
    String[] value();
}
