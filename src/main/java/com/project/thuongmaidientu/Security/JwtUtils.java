package com.project.thuongmaidientu.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {
    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    private final String JWT_SECRET;
    private final long JWT_EXPIRATION = 86400000L; // 24 giờ

    /**
     * Secret đọc từ app.jwt.secret (biến môi trường JWT_SECRET), tối thiểu 32 ký tự.
     * Không có thì sinh khóa ngẫu nhiên cho lần chạy này (token cũ mất hiệu lực khi restart)
     * thay vì dùng khóa cứng nằm trong mã nguồn.
     */
    public JwtUtils(@Value("${app.jwt.secret:}") String configuredSecret) {
        if (configuredSecret == null || configuredSecret.length() < 32) {
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            this.JWT_SECRET = HexFormat.of().formatHex(random);
            log.warn("JWT_SECRET chưa cấu hình (hoặc < 32 ký tự): dùng khóa ngẫu nhiên tạm thời, người dùng sẽ phải đăng nhập lại sau mỗi lần khởi động.");
        } else {
            this.JWT_SECRET = configuredSecret;
        }
    }

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
            log.debug("JWT không hợp lệ: {}", e.getMessage());
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