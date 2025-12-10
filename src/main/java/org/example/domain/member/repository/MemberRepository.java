package org.example.domain.member.repository;

import org.example.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 회원 Repository
 * <p>
 * 회원 엔티티에 대한 데이터 접근을 담당합니다.
 * </p>
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 이메일로 회원 조회
     *
     * @param email 조회할 이메일
     * @return 회원 Optional (존재하지 않으면 empty)
     */
    Optional<Member> findByEmail(String email);

    /**
     * 이메일 중복 확인
     *
     * @param email 확인할 이메일
     * @return 이미 존재하면 true, 아니면 false
     */
    boolean existsByEmail(String email);
}
