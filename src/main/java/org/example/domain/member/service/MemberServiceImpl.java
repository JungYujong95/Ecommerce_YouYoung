package org.example.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.member.dto.request.SignUpRequest;
import org.example.domain.member.dto.response.MemberResponse;
import org.example.domain.member.entity.Member;
import org.example.domain.member.repository.MemberRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public MemberResponse signUp(SignUpRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = request.toEntity(passwordEncoder);
        Member savedMember = memberRepository.save(member);

        return MemberResponse.from(savedMember);
    }

    @Override
    public boolean checkEmailDuplicate(String email) {
        return memberRepository.existsByEmail(email);
    }
}
