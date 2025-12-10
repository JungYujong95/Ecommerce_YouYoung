package org.example.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.domain.auth.dto.request.LoginRequest;
import org.example.domain.auth.dto.response.TokenResponse;
import org.example.domain.auth.service.AuthService;
import org.example.global.common.ApiResponse;
import org.example.global.security.auth.CustomUserDetails;
import org.example.util.CookieUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "인증 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음")
    })
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        TokenResponse tokenResponse = authService.login(request);
        CookieUtil.addRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        return ApiResponse.success(tokenResponse);
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access Token을 발급받습니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshRequest bodyRefreshToken,
            HttpServletResponse response) {
        String refreshToken = cookieRefreshToken;
        if (refreshToken == null && bodyRefreshToken != null) {
            refreshToken = bodyRefreshToken.refreshToken();
        }

        TokenResponse tokenResponse = authService.refreshToken(refreshToken);
        CookieUtil.addRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        return ApiResponse.success(tokenResponse);
    }

    @Operation(summary = "로그아웃", description = "현재 세션을 로그아웃합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logout(
            @RequestHeader("Authorization") String bearerToken,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response) {
        String accessToken = bearerToken.substring(7);
        authService.logout(accessToken, userDetails.getEmail());
        CookieUtil.deleteRefreshTokenCookie(response);
        return ApiResponse.success();
    }

    public record RefreshRequest(String refreshToken) {
    }
}
