package org.example.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.auth.dto.request.LoginRequest;
import org.example.domain.auth.dto.response.TokenResponse;
import org.example.domain.member.entity.Member;
import org.example.domain.member.repository.MemberRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.example.global.security.jwt.JwtTokenProvider;
import org.example.util.redis.RedisService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.INACTIVE_MEMBER);
        }

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        member.updateLastLoginAt();

        String accessToken = jwtTokenProvider.createAccessToken(
                member.getId(),
                member.getEmail(),
                member.getRole().getAuthority()
        );

        String refreshToken = jwtTokenProvider.createRefreshToken(
                member.getId(),
                member.getEmail()
        );

        redisService.saveRefreshToken(
                member.getEmail(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpiration()
        );

        log.info("로그인 성공: {}", member.getEmail());

        return TokenResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiration()
        );
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        String storedToken = redisService.getRefreshToken(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!storedToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String newAccessToken = jwtTokenProvider.createAccessToken(
                member.getId(),
                member.getEmail(),
                member.getRole().getAuthority()
        );

        String newRefreshToken = jwtTokenProvider.createRefreshToken(
                member.getId(),
                member.getEmail()
        );

        redisService.rotateRefreshToken(
                email,
                newRefreshToken,
                jwtTokenProvider.getRefreshTokenExpiration()
        );

        log.info("토큰 갱신 성공: {}", email);

        return TokenResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtTokenProvider.getAccessTokenExpiration()
        );
    }

    @Override
    public void logout(String accessToken, String email) {
        long remainingExpiration = jwtTokenProvider.getRemainingExpiration(accessToken);

        redisService.addToBlacklist(accessToken, remainingExpiration);
        redisService.deleteRefreshToken(email);

        log.info("로그아웃 성공: {}", email);
    }
}
