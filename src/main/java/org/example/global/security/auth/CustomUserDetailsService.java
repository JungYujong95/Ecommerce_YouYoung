package org.example.global.security.auth;

import lombok.RequiredArgsConstructor;
import org.example.domain.member.entity.Member;
import org.example.domain.member.repository.MemberRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다: " + email));

        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.INACTIVE_MEMBER);
        }

        return CustomUserDetails.from(member);
    }
}
