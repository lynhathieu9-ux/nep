package org.nep.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /** 鉴权拦截器链：先 JWT 登录校验（写入 UserContext），再角色权限校验 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 面向公众开放、无需登录的浏览类接口
        String[] whiteList = {
                "/api/user/login", "/api/user/register",
                "/api/region/**", "/api/images/**",
                "/api/news/**", "/api/knowledge/**",
                "/api/banner/**", "/api/dict/**", "/api/ai/**"
        };
        // 1) 登录校验并写入当前用户上下文
        registry.addInterceptor(new JwtInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns(whiteList)
                .order(1);
        // 2) 角色权限校验（依赖 JwtInterceptor 写入的 UserContext）
        registry.addInterceptor(new RoleInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns(whiteList)
                .order(2);
    }

    /** 头像文件静态资源映射 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadDir);
    }
}
