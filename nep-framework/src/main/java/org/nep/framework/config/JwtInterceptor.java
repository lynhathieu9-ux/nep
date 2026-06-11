package org.nep.framework.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.nep.common.utils.JwtUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT拦截器 - 验证请求Token
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
        // 放行路径：登录、注册、API文档
        if (path.contains("/login") || path.contains("/register")
                || path.contains("/doc.html") || path.contains("/webjars")
                || path.contains("/v3/api-docs") || path.contains("/images/")
                || path.contains("/api/ai/")) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (JwtUtils.validate(token)) {
                request.setAttribute("userId", JwtUtils.getUserId(token));
                request.setAttribute("role", JwtUtils.getRole(token));
                return true;
            }
        }

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录或Token已过期\"}");
        return false;
    }
}
