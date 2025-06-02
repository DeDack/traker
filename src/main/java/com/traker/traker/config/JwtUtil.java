package com.traker.traker.config;

import com.traker.traker.exception.InvalidTokenException;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Утилита для работы с JWT-токенами.
 * Использует Base64-декодированный ключ для HS256.
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final byte[] secret;

    @Value("${jwt.access_token_expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh_token_expiration}")
    private long refreshTokenExpiration;

    public JwtUtil(@Value("${jwt.secret}") String base64Secret) {
        this.secret = Base64.getDecoder().decode(base64Secret);
    }

    public TokenInfo createAccessToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        String jti = UUID.randomUUID().toString();
        String token = createToken(userDetails.getUsername(), roles, jti, accessTokenExpiration);
        return new TokenInfo(token, jti, accessTokenExpiration / 1000);
    }

    public TokenInfo createRefreshToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        String jti = UUID.randomUUID().toString();
        String token = createToken(userDetails.getUsername(), roles, jti, refreshTokenExpiration);
        return new TokenInfo(token, jti, refreshTokenExpiration / 1000);
    }

    private String createToken(String username, List<String> roles, String jti, long validityDuration) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put("roles", roles);

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityDuration);

        return Jwts.builder()
                .setClaims(claims)
                .setId(jti)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(new SecretKeySpec(secret, SignatureAlgorithm.HS256.getJcaName()), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secret)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            boolean isValid = !claims.getExpiration().before(new Date());
            if (!isValid) {
                logger.warn("Токен истёк: exp = {}, currentTime = {}", claims.getExpiration(), new Date());
            }
            return isValid;
        } catch (SignatureException e) {
            logger.error("Неверная подпись токена: {}", e.getMessage());
            throw new InvalidTokenException("Недействительная подпись JWT-токена", e);
        } catch (ExpiredJwtException e) {
            logger.error("Токен истёк: {}", e.getMessage());
            throw new InvalidTokenException("Истекший JWT-токен", e);
        } catch (MalformedJwtException e) {
            logger.error("Некорректный формат токена: {}", e.getMessage());
            throw new InvalidTokenException("Некорректный формат JWT-токена", e);
        } catch (Exception e) {
            logger.error("Ошибка валидации токена: {}", e.getMessage());
            throw new InvalidTokenException("Недействительный или истекший JWT-токен", e);
        }
    }

    public String getUsername(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secret)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secret)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return (List<String>) claims.get("roles", List.class);
    }

    public String getJti(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secret)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getId();
    }

    public long getAccessTokenValidityInSeconds() {
        return accessTokenExpiration / 1000;
    }

    public long getRefreshTokenValidityInSeconds() {
        return refreshTokenExpiration / 1000;
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        return validateToken(token) && getUsername(token).equals(userDetails.getUsername());
    }

    public static class TokenInfo {
        private final String token;
        private final String jti;
        private final long expiresIn;

        public TokenInfo(String token, String jti, long expiresIn) {
            this.token = token;
            this.jti = jti;
            this.expiresIn = expiresIn;
        }

        public String getToken() {
            return token;
        }

        public String getJti() {
            return jti;
        }

        public long getExpiresIn() {
            return expiresIn;
        }
    }
}