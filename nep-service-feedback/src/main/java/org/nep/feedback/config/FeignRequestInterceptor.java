package org.nep.feedback.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * OpenFeign 请求拦截器：将当前请求的鉴权信息透传到下游微服务调用。
 * <p>解决"服务间 Feign 调用不带 token 被网关/拦截器拦截、只能走降级返回假数据"的问题。
 * 透传 Authorization 以及网关注入的 X-User-Id / X-User-Role，
 * 保证跨服务调用（如反馈服务查用户信息）能通过下游的登录与角色校验。</p>
 *
 * @author NEP Team
 */
@Configuration
public class FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        // 透传 JWT，令下游服务能识别当前登录用户
        String auth = request.getHeader("Authorization");
        if (auth != null && !auth.isEmpty()) {
            template.header("Authorization", auth);
        }
        // 透传网关注入的用户上下文头
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            template.header("X-User-Id", userId);
        }
        String role = request.getHeader("X-User-Role");
        if (role != null && !role.isEmpty()) {
            template.header("X-User-Role", role);
        }
    }
}
