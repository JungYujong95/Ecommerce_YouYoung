package org.example.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회원 권한
 * <p>
 * 회원의 역할을 정의합니다.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum MemberRole {

    /** 일반 사용자 (구매자) */
    USER("ROLE_USER", "일반 사용자"),
    /** 판매자 */
    SELLER("ROLE_SELLER", "판매자"),
    /** 관리자 */
    ADMIN("ROLE_ADMIN", "관리자");

    /** Spring Security 권한 문자열 */
    private final String authority;
    /** 권한 설명 */
    private final String description;
}
