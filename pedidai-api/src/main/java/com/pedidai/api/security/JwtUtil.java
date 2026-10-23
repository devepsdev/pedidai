package com.pedidai.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

/**
 * Tokens de sessió. Duren {@code app.jwt.expiration-days} (30 per defecte) i la web els renova
 * cada dia mentre s'utilitza. Porten la versió de sessió de l'usuari (claim «tv»): quan canvia la
 * contrasenya, la versió puja i els tokens anteriors deixen de servir.
 */
@Component
public class JwtUtil {

    static final String TOKEN_VERSION_CLAIM = "tv";

    private final SecretKey key;
    private final long jwtExpiration;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-days:30}") long expirationDays) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.jwtExpiration = Duration.ofDays(expirationDays).toMillis();
    }

    public String generateToken(String username, String role, int tokenVersion) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim(TOKEN_VERSION_CLAIM, tokenVersion)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key)
                .compact();
    }

    private Claims claims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUsernameFromToken(String token) {
        return claims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return claims(token).get("role", String.class);
    }

    /** Versió de sessió del token; {@code null} en els tokens d'una hora emesos abans d'aquest canvi. */
    public Integer getTokenVersion(String token) {
        return claims(token).get(TOKEN_VERSION_CLAIM, Integer.class);
    }

    public boolean isTokenExpired(String token) {
        return getExpirationDateFromToken(token).before(new Date());
    }

    public Date getExpirationDateFromToken(String token) {
        return claims(token).getExpiration();
    }

    public boolean validateToken(String token, String username) {
        try {
            String tokenUsername = getUsernameFromToken(token);
            return (tokenUsername.equals(username) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
