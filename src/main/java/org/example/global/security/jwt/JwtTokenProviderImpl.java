package org.example.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProviderImpl implements JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    protected void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String createAccessToken(Long memberId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccess().getExpiration());

        return Jwts.builder()
                .header()
                .type("JWT")
                .and()
                .subject(email)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiry)
                .claim("memberId", memberId)
                .claim("role", role)
                .claim("type", "ACCESS")
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    @Override
    public String createRefreshToken(Long memberId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefresh().getExpiration());

        return Jwts.builder()
                .header()
                .type("JWT")
                .and()
                .subject(email)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiry)
                .claim("memberId", memberId)
                .claim("type", "REFRESH")
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다: {}", e.getMessage());
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("잘못된 형식의 JWT 토큰입니다: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("잘못된 JWT 서명입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있습니다: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    @Override
    public Long getMemberIdFromToken(String token) {
        return getClaims(token).get("memberId", Long.class);
    }

    @Override
    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    @Override
    public long getRemainingExpiration(String token) {
        Date expiration = getClaims(token).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    @Override
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtProperties.getAccess().getHeader());
        if (StringUtils.hasText(bearerToken) &&
                bearerToken.startsWith(jwtProperties.getAccess().getPrefix())) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    public long getAccessTokenExpiration() {
        return jwtProperties.getAccess().getExpiration();
    }

    @Override
    public long getRefreshTokenExpiration() {
        return jwtProperties.getRefresh().getExpiration();
    }
}
