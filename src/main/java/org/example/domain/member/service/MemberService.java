package org.example.domain.member.service;

import org.example.domain.member.dto.request.SignUpRequest;
import org.example.domain.member.dto.response.MemberResponse;

/**
 * 회원 서비스
 * <p>
 * 회원 가입 및 이메일 중복 확인 기능을 제공합니다.
 * </p>
 */
public interface MemberService {

    /**
     * 회원 가입
     *
     * @param request 회원 가입 요청 DTO (이메일, 비밀번호, 이름, 휴대폰, 권한)
     * @return 생성된 회원 응답 DTO
     * @throws org.example.global.exception.BusinessException DUPLICATE_EMAIL - 이미 존재하는 이메일일 경우
     */
    MemberResponse signUp(SignUpRequest request);

    /**
     * 이메일 중복 확인
     *
     * @param email 확인할 이메일
     * @return 중복이면 true, 사용 가능하면 false
     */
    boolean checkEmailDuplicate(String email);
}
