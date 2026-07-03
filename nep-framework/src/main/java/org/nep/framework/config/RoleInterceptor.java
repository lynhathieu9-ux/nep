package org.nep.framework.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.nep.common.annotation.RequiresRole;
import org.nep.common.constant.Roles;
import org.nep.common.context.UserContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 角色权限拦截器。
 * <p>在 {@link JwtInterceptor} 之后执行：读取已写入 {@link UserContext} 的当前角色，
 * 校验目标 Controller 方法/类上的 {@link RequiresRole} 声明，命中则放行，否则返回 403。
 * 未标注 {@code @RequiresRole} 的接口不受限（仅需登录）。</p>
 *
 * @author NEP Team
 */
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 非 Controller 方法（静态资源等）直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 方法级注解优先，其次类级注解
        RequiresRole requires = handlerMethod.getMethodAnnotation(RequiresRole.class);
        if (requires == null) {
            requires = handlerMethod.getBeanType().getAnnotation(RequiresRole.class);
        }
        // 未声明角色限制：仅需登录即可（登录校验已由 JwtInterceptor 完成）
        if (requires == null) {
            return true;
        }

        String currentRole = UserContext.getRole();
        for (String allowed : requires.value()) {
            if (allowed.equals(currentRole)) {
                return true;
            }
        }

        // 角色不匹配，拒绝访问
        StringBuilder allowedNames = new StringBuilder();
        for (int i = 0; i < requires.value().length; i++) {
            if (i > 0) {
                allowedNames.append("/");
            }
            allowedNames.append(Roles.displayName(requires.value()[i]));
        }
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":403,\"message\":\"无权限访问，本操作仅限[" + allowedNames + "]\"}");
        return false;
    }
}
