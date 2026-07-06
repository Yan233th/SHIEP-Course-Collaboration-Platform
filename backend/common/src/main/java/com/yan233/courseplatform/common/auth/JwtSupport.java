package com.yan233.courseplatform.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;

public final class JwtSupport {
    private JwtSupport() {
    }

    public static SecretKey key(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public static String createToken(String secret, Long userId, String username, List<String> roles, Duration ttl) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + ttl.toMillis());
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(key(secret))
                .compact();
    }

    @SuppressWarnings("unchecked")
    public static CurrentUser parse(String secret, String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Number userId = claims.get("userId", Number.class);
        List<String> roles = claims.get("roles", List.class);
        return new CurrentUser(userId.longValue(), claims.getSubject(), roles);
    }

    public static String stripBearer(String authorization) {
        if (authorization == null) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}

