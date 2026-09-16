package com.project.thuongmaidientu.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {
    private final String JWT_SECRET = "9a2f8c4e1b7d3f6a8c0e2b4d6f8a1c3e5b7d9f0a2b4c6e8f1a3b5c7d9e0f2a4b"; // Chuỗi secret đủ dài (>= 256 bits)
    private final long JWT_EXPIRATION = 86400000L; // 24 giờ

    /** Tên cookie chứa JWT - dùng chung giữa JwtFilter và AuthController. */
    public static final String COOKIE_NAME = "ACCESS_TOKEN";

    /** Thời gian sống token tính theo giây, dùng để set Max-Age cho cookie. */
    public long getExpirationSeconds() {
        return JWT_EXPIRATION / 1000;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
    }

    // Bổ sung hàm generateToken nhận 2 tham số String (email/username và role)
    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION);

        return Jwts.builder()
                .subject(email)
                .claim("role", role) // Lưu role vào Claims của JWT
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey()) // Sử dụng secret key của bạn
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token không hợp lệ, hết hạn hoặc bị thay đổi
            System.err.println("JWT Validation Error: " + e.getMessage());
        }
        return false;
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject(); // Trả về Subject (thường là email hoặc username lưu trong token)
    }
}