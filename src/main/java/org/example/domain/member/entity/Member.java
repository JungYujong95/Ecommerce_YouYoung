package org.example.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.global.common.BaseTimeEntity;

import java.time.LocalDateTime;

/**
 * 회원 엔티티
 * <p>
 * 회원 정보를 관리하는 엔티티입니다.
 * </p>
 */
@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    /** 회원 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이메일 (로그인 ID로 사용) */
    @Column(nullable = false, unique = true, length = 50)
    private String email;

    /** 암호화된 비밀번호 */
    @Column(nullable = false)
    private String password;

    /** 회원 이름 */
    @Column(nullable = false, length = 50)
    private String name;

    /** 휴대폰 번호 */
    @Column(length = 20)
    private String phone;

    /** 회원 권한 (USER, SELLER, ADMIN) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    /** 회원 상태 (ACTIVE, INACTIVE, DORMANT, WITHDRAWN) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    /** 마지막 로그인 일시 */
    @Column
    private LocalDateTime lastLoginAt;

    @Builder
    private Member(String email, String password, String name, String phone, MemberRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = role != null ? role : MemberRole.USER;
        this.status = MemberStatus.ACTIVE;
    }

    /**
     * 마지막 로그인 일시 갱신
     */
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * 비밀번호 변경
     *
     * @param encodedPassword 암호화된 새 비밀번호
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /**
     * 회원 정보 수정
     *
     * @param name  변경할 이름 (null이면 변경하지 않음)
     * @param phone 변경할 휴대폰 번호 (null이면 변경하지 않음)
     */
    public void updateInfo(String name, String phone) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (phone != null) {
            this.phone = phone;
        }
    }

    /**
     * 계정 비활성화
     */
    public void deactivate() {
        this.status = MemberStatus.INACTIVE;
    }

    /**
     * 회원 탈퇴 처리
     */
    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
    }

    /**
     * 활성 상태 확인
     *
     * @return 활성 상태이면 true
     */
    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }
}
