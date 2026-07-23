package com.voxel.server.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Tao va kiem tra JWT - "the dang nhap". Sau khi dang nhap dung, server ky mot the
 * chua id nguoi dung. Nhung lan goi sau, client gui lai the nay; server chi can kiem
 * chu ky (bang khoa bi mat) la biet la ai, khong phai luu phien trong CSDL.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(@Value("${voxel.jwt.secret}") String secret,
                      @Value("${voxel.jwt.expiration-millis}") long expirationMillis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    /** Ky mot the moi cho nguoi dung, han dung theo cau hinh. */
    public String issueToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key)
                .compact();
    }

    /**
     * Kiem tra the va tra ve id nguoi dung ben trong.
     *
     * @return id nguoi dung, hoac null neu the sai / het han.
     */
    public Long userIdFromToken(String token) {
        Claims claims = claims(token);
        return claims == null ? null : Long.valueOf(claims.getSubject());
    }

    /** Ten dang nhap nam trong the, hoac null neu the sai / het han. Dung de hien ten nguoi choi. */
    public String usernameFromToken(String token) {
        Claims claims = claims(token);
        return claims == null ? null : claims.get("username", String.class);
    }

    /** Kiem chu ky va tra ve phan ruot cua the; null neu the sai hoac het han. */
    private Claims claims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception invalid) {
            return null;
        }
    }
}
