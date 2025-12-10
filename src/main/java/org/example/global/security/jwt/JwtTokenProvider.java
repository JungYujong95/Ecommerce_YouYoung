package org.example.global.security.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 생성, 검증, 파싱을 담당하는 인터페이스.
 */
@Component
public interface JwtTokenProvider {

    /**
     * Access Token 생성.
     *
     * @param memberId 회원 ID
     * @param email    회원 이메일
     * @param role     회원 권한
     * @return 생성된 Access Token
     */
    String createAccessToken(Long memberId, String email, String role);

    /**
     * Refresh Token 생성.
     *
     * @param memberId 회원 ID
     * @param email    회원 이메일
     * @return 생성된 Refresh Token
     */
    String createRefreshToken(Long memberId, String email);

    /**
     * 토큰 유효성 검증.
     *
     * @param token 검증할 토큰
     * @return 유효 여부
     */
    boolean validateToken(String token);

    /**
     * 토큰에서 Claims 추출.
     *
     * @param token JWT 토큰
     * @return Claims 객체
     */
    Claims getClaims(String token);

    /**
     * 토큰에서 이메일 추출.
     *
     * @param token JWT 토큰
     * @return 이메일
     */
    String getEmailFromToken(String token);

    /**
     * 토큰에서 회원 ID 추출.
     *
     * @param token JWT 토큰
     * @return 회원 ID
     */
    Long getMemberIdFromToken(String token);

    /**
     * 토큰에서 권한 추출.
     *
     * @param token JWT 토큰
     * @return 권한
     */
    String getRoleFromToken(String token);

    /**
     * 토큰 남은 만료 시간 계산.
     *
     * @param token JWT 토큰
     * @return 남은 만료 시간 (밀리초)
     */
    long getRemainingExpiration(String token);

    /**
     * HTTP 요청에서 토큰 추출.
     *
     * @param request HTTP 요청
     * @return Bearer 토큰 (없으면 null)
     */
    String resolveToken(HttpServletRequest request);

    /**
     * Access Token 만료 시간 조회.
     *
     * @return 만료 시간 (밀리초)
     */
    long getAccessTokenExpiration();

    /**
     * Refresh Token 만료 시간 조회.
     *
     * @return 만료 시간 (밀리초)
     */
    long getRefreshTokenExpiration();
}
