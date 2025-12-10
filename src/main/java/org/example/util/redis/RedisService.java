package org.example.util.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    public void saveRefreshToken(String email, String refreshToken, long expirationMs) {
        String key = REFRESH_TOKEN_PREFIX + email;
        redisTemplate.opsForValue().set(key, refreshToken, expirationMs, TimeUnit.MILLISECONDS);
        log.debug("Refresh Token 저장: {}", email);
    }

    public Optional<String> getRefreshToken(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        String token = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(token);
    }

    public void deleteRefreshToken(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        redisTemplate.delete(key);
        log.debug("Refresh Token 삭제: {}", email);
    }

    public void addToBlacklist(String accessToken, long remainingExpirationMs) {
        String key = BLACKLIST_PREFIX + accessToken;
        redisTemplate.opsForValue().set(key, "logout", remainingExpirationMs, TimeUnit.MILLISECONDS);
        log.debug("Access Token 블랙리스트 등록");
    }

    public boolean isTokenBlacklisted(String accessToken) {
        String key = BLACKLIST_PREFIX + accessToken;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void rotateRefreshToken(String email, String newToken, long expirationMs) {
        deleteRefreshToken(email);
        saveRefreshToken(email, newToken, expirationMs);
        log.debug("Refresh Token 로테이션 완료: {}", email);
    }
}
