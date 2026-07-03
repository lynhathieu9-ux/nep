package org.nep.framework.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.nep.common.context.UserContext;
import org.nep.common.utils.JwtUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT拦截器 - 验证请求Token，并将当前用户ID/角色写入 {@link UserContext}，
 * 供下游 {@link RoleInterceptor} 与业务层做角色鉴权、数据归属校验。
 * <p>白名单仅保留真正对公众开放的浏览类接口（登录/注册/资讯/知识库/轮播/字典/地区/AI问答）。
 * AQI 提交、统计数据等涉及角色与业务安全的接口一律要求登录后按角色访问。</p>
 */
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 允许OPTIONS请求（CORS预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        // 放行路径：登录、注册、API文档、面向公众的内容浏览接口
        if (path.contains("/login") || path.contains("/register")
                || path.contains("/doc.html") || path.contains("/webjars")
                || path.contains("/v3/api-docs") || path.contains("/images/")
                || path.contains("/api/ai/")
                || path.startsWith("/api/news/")
                || path.startsWith("/api/knowledge/")
                || path.startsWith("/api/banner/")
                || path.startsWith("/api/dict/")
                || path.startsWith("/api/region/")) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (JwtUtils.validate(token)) {
                Long userId = JwtUtils.getUserId(token);
                String role = JwtUtils.getRole(token);
                // 兼容旧代码：仍写入 request 属性
                request.setAttribute("userId", userId);
                request.setAttribute("role", role);
                // 写入线程上下文，供 RoleInterceptor 与业务层读取
                UserContext.set(userId, role);
                return true;
            }
        }

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录或Token已过期\"}");
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束务必清理线程上下文，防止线程池复用导致用户信息串号
        UserContext.clear();
    }
}
