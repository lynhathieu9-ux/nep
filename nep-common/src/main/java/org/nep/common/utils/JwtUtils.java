package org.nep.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token 工具类 - 会话跟踪
 */
public class JwtUtils {

    private static final String SECRET = "NEP-Neusoft-Environmental-Protection-2026-SecretKey!!";
    private static final long EXPIRE = 24 * 60 * 60 * 1000L; // 24小时
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /** 生成Token */
    public static String generate(Long userId, String phone, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("phone", phone)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(KEY)
                .compact();
    }

    /** 解析Token */
    public static Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 获取用户ID */
    public static Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    /** 获取角色 */
    public static String getRole(String token) {
        return parse(token).get("role", String.class);
    }

    /** 验证Token是否有效 */
    public static boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
