package org.example.global.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.example.global.security.auth.CustomUserDetailsService;
import org.example.util.redis.RedisService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final RedisService redisService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = jwtTokenProvider.resolveToken(request);

            if (StringUtils.hasText(token)) {
                if (redisService.isTokenBlacklisted(token)) {
                    log.debug("블랙리스트에 등록된 토큰입니다");
                    throw new BusinessException(ErrorCode.LOGOUT_USER);
                }

                if (jwtTokenProvider.validateToken(token)) {
                    Authentication authentication = getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Security Context에 '{}' 인증 정보 저장", authentication.getName());
                }
            }
        } catch (BusinessException e) {
            request.setAttribute("exception", e);
        }

        filterChain.doFilter(request, response);
    }

    private Authentication getAuthentication(String token) {
        String email = jwtTokenProvider.getEmailFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/login") ||
                path.startsWith("/api/v1/auth/refresh") ||
                path.startsWith("/api/v1/members/signup") ||
                path.startsWith("/api/v1/members/check-email") ||
                path.startsWith("/h2-console") ||
                path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs");
    }
}
