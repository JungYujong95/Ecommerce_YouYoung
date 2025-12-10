package org.example.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회원 상태
 * <p>
 * 회원 계정의 상태를 정의합니다.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum MemberStatus {

    /** 활성 상태 - 정상 이용 가능 */
    ACTIVE("활성"),
    /** 비활성 상태 - 관리자에 의해 비활성화 */
    INACTIVE("비활성"),
    /** 휴면 상태 - 장기 미접속 */
    DORMANT("휴면"),
    /** 탈퇴 상태 - 회원 탈퇴 */
    WITHDRAWN("탈퇴");

    /** 상태 설명 */
    private final String description;
}
