package org.example.domain.auth.service;

import org.example.domain.auth.dto.request.LoginRequest;
import org.example.domain.auth.dto.response.TokenResponse;
import org.example.domain.member.entity.Member;
import org.example.domain.member.entity.MemberRole;
import org.example.domain.member.entity.MemberStatus;
import org.example.domain.member.repository.MemberRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.example.global.security.jwt.JwtTokenProvider;
import org.example.util.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

// 🔥 @Disabled 제거
@DisplayName("AuthServiceImpl 단위 테스트")
class AuthServiceImplTest {

    private MemberRepository memberRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private RedisService redisService;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        // ✅ 이제는 인터페이스라 안전하게 mock 가능
        jwtTokenProvider = mock(JwtTokenProvider.class);
        redisService = mock(RedisService.class);
        authService = new AuthServiceImpl(memberRepository, passwordEncoder, jwtTokenProvider, redisService);
    }

    @Nested
    @DisplayName("login 메서드")
    class LoginTest {

        @Test
        @DisplayName("정상 로그인 성공")
        void login_Success() {
            // given
            LoginRequest request = createLoginRequest("test@example.com", "Password123!");
            Member member = createMember(1L, "test@example.com", "encodedPassword", MemberRole.USER, MemberStatus.ACTIVE);

            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));
            given(passwordEncoder.matches(request.getPassword(), member.getPassword())).willReturn(true);
            given(jwtTokenProvider.createAccessToken(anyLong(), anyString(), anyString())).willReturn("accessToken");
            given(jwtTokenProvider.createRefreshToken(anyLong(), anyString())).willReturn("refreshToken");
            given(jwtTokenProvider.getAccessTokenExpiration()).willReturn(3600000L);
            given(jwtTokenProvider.getRefreshTokenExpiration()).willReturn(1209600000L);

            // when
            TokenResponse response = authService.login(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
            assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
            assertThat(response.getTokenType()).isEqualTo("Bearer");

            verify(memberRepository).findByEmail(request.getEmail());
            verify(passwordEncoder).matches(request.getPassword(), member.getPassword());
            verify(jwtTokenProvider).createAccessToken(member.getId(), member.getEmail(), member.getRole().getAuthority());
            verify(redisService).saveRefreshToken(eq(member.getEmail()), anyString(), anyLong());
        }

        @Test
        @DisplayName("존재하지 않는 회원 - MEMBER_NOT_FOUND 예외")
        void login_MemberNotFound_ThrowsException() {
            // given
            LoginRequest request = createLoginRequest("notfound@example.com", "Password123!");

            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
                    });

            verify(memberRepository).findByEmail(request.getEmail());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("비활성화 회원 - INACTIVE_MEMBER 예외")
        void login_InactiveMember_ThrowsException() {
            // given
            LoginRequest request = createLoginRequest("inactive@example.com", "Password123!");
            Member member = createMember(1L, "inactive@example.com", "encodedPassword", MemberRole.USER, MemberStatus.INACTIVE);

            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INACTIVE_MEMBER);
                    });

            verify(memberRepository).findByEmail(request.getEmail());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("비밀번호 불일치 - INVALID_PASSWORD 예외")
        void login_InvalidPassword_ThrowsException() {
            // given
            LoginRequest request = createLoginRequest("test@example.com", "WrongPassword123!");
            Member member = createMember(1L, "test@example.com", "encodedPassword", MemberRole.USER, MemberStatus.ACTIVE);

            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));
            given(passwordEncoder.matches(request.getPassword(), member.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD);
                    });

            verify(memberRepository).findByEmail(request.getEmail());
            verify(passwordEncoder).matches(request.getPassword(), member.getPassword());
            verify(jwtTokenProvider, never()).createAccessToken(anyLong(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("refreshToken 메서드")
    class RefreshTokenTest {

        @Test
        @DisplayName("정상 토큰 갱신 성공")
        void refreshToken_Success() {
            // given
            String refreshToken = "validRefreshToken";
            String email = "test@example.com";
            Member member = createMember(1L, email, "encodedPassword", MemberRole.USER, MemberStatus.ACTIVE);

            given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
            given(jwtTokenProvider.getEmailFromToken(refreshToken)).willReturn(email);
            given(redisService.getRefreshToken(email)).willReturn(Optional.of(refreshToken));
            given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
            given(jwtTokenProvider.createAccessToken(anyLong(), anyString(), anyString())).willReturn("newAccessToken");
            given(jwtTokenProvider.createRefreshToken(anyLong(), anyString())).willReturn("newRefreshToken");
            given(jwtTokenProvider.getAccessTokenExpiration()).willReturn(3600000L);
            given(jwtTokenProvider.getRefreshTokenExpiration()).willReturn(1209600000L);

            // when
            TokenResponse response = authService.refreshToken(refreshToken);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
            assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");

            verify(redisService).rotateRefreshToken(eq(email), anyString(), anyLong());
        }

        @Test
        @DisplayName("null 토큰 - INVALID_REFRESH_TOKEN 예외")
        void refreshToken_NullToken_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> authService.refreshToken(null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
                    });

            verify(jwtTokenProvider, never()).validateToken(anyString());
        }

        @Test
        @DisplayName("빈 토큰 - INVALID_REFRESH_TOKEN 예외")
        void refreshToken_BlankToken_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> authService.refreshToken("   "))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
                    });

            verify(jwtTokenProvider, never()).validateToken(anyString());
        }

        @Test
        @DisplayName("유효하지 않은 토큰 - INVALID_REFRESH_TOKEN 예외")
        void refreshToken_InvalidToken_ThrowsException() {
            // given
            String invalidToken = "invalidRefreshToken";

            given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
                    });

            verify(jwtTokenProvider).validateToken(invalidToken);
            verify(jwtTokenProvider, never()).getEmailFromToken(anyString());
        }

        @Test
        @DisplayName("Redis 토큰 불일치 - INVALID_REFRESH_TOKEN 예외")
        void refreshToken_TokenMismatch_ThrowsException() {
            // given
            String refreshToken = "validRefreshToken";
            String email = "test@example.com";
            String storedToken = "differentToken";

            given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
            given(jwtTokenProvider.getEmailFromToken(refreshToken)).willReturn(email);
            given(redisService.getRefreshToken(email)).willReturn(Optional.of(storedToken));

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
                    });

            verify(redisService).getRefreshToken(email);
            verify(memberRepository, never()).findByEmail(anyString());
        }
    }

    @Nested
    @DisplayName("logout 메서드")
    class LogoutTest {

        @Test
        @DisplayName("정상 로그아웃 - 블랙리스트 추가 및 Redis 삭제")
        void logout_Success() {
            // given
            String accessToken = "validAccessToken";
            String email = "test@example.com";
            long remainingExpiration = 1800000L;

            given(jwtTokenProvider.getRemainingExpiration(accessToken)).willReturn(remainingExpiration);

            // when
            authService.logout(accessToken, email);

            // then
            verify(jwtTokenProvider).getRemainingExpiration(accessToken);
            verify(redisService).addToBlacklist(accessToken, remainingExpiration);
            verify(redisService).deleteRefreshToken(email);
        }
    }

    // ========== Helper Methods ==========

    private LoginRequest createLoginRequest(String email, String password) {
        return LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
    }

    private Member createMember(Long id, String email, String password, MemberRole role, MemberStatus status) {
        Member member = Member.builder()
                .email(email)
                .password(password)
                .name("테스트")
                .role(role)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        ReflectionTestUtils.setField(member, "status", status);
        return member;
    }
}
