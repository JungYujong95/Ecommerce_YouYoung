package org.example.domain.auth.service;

import org.example.domain.auth.dto.request.LoginRequest;
import org.example.domain.auth.dto.response.TokenResponse;

/**
 * 인증 서비스
 * <p>
 * 로그인, 토큰 갱신, 로그아웃 기능을 제공합니다.
 * </p>
 */
public interface AuthService {

    /**
     * 로그인
     *
     * @param request 로그인 요청 DTO (이메일, 비밀번호)
     * @return 토큰 응답 DTO (Access Token, Refresh Token)
     * @throws org.example.global.exception.BusinessException MEMBER_NOT_FOUND - 회원이 존재하지 않을 경우
     * @throws org.example.global.exception.BusinessException INVALID_PASSWORD - 비밀번호가 일치하지 않을 경우
     * @throws org.example.global.exception.BusinessException ACCOUNT_DISABLED - 비활성화된 계정일 경우
     */
    TokenResponse login(LoginRequest request);

    /**
     * 토큰 갱신 (Refresh Token Rotation)
     *
     * @param refreshToken 기존 Refresh Token
     * @return 새로운 토큰 응답 DTO (새 Access Token, 새 Refresh Token)
     * @throws org.example.global.exception.BusinessException INVALID_TOKEN - 유효하지 않은 토큰일 경우
     */
    TokenResponse refreshToken(String refreshToken);

    /**
     * 로그아웃
     * <p>
     * Access Token을 블랙리스트에 추가하고 Refresh Token을 삭제합니다.
     * </p>
     *
     * @param accessToken 블랙리스트에 추가할 Access Token
     * @param email       회원 이메일 (Refresh Token 삭제용)
     */
    void logout(String accessToken, String email);
}
