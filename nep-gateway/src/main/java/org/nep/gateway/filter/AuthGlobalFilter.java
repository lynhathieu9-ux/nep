package org.nep.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关全局JWT鉴权过滤器
 * 在网关层统一校验JWT Token，拦截非法请求
 * 白名单路径直接放行
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class AuthGlobalFilter implements GlobalFilter {

    /** JWT密钥（与nep-common中保持一致） */
    private static final String SECRET = "NEP-Neusoft-Environmental-Protection-2026-SecretKey!!";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /** 白名单路径：不需要Token即可访问 */
    private static final List<String> WHITE_LIST = List.of(
            "/api/user/login",
            "/api/user/register",
            "/api/region/",
            "/api/news/page",
            "/api/news/latest",
            "/api/news/",
            "/api/knowledge/page",
            "/api/knowledge/hot",
            "/api/knowledge/",
            "/api/banner/list",
            "/api/dict/",
            "/api/statistics/",
            "/api/aqi/",
            "/api/export/",
            "/doc.html",
            "/webjars/",
            "/v3/api-docs",
            "/images/"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单直接放行
        for (String white : WHITE_LIST) {
            if (path.startsWith(white)) {
                return chain.filter(exchange);
            }
        }

        // 检查Token
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 将用户信息注入请求头，传递给下游微服务
            exchange = exchange.mutate()
                    .request(r -> r.header("X-User-Id", claims.getSubject())
                            .header("X-User-Role", claims.get("role", String.class)))
                    .build();
        } catch (Exception e) {
            // Token无效或过期
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }
}
