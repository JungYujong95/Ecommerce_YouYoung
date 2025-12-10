package org.example.domain.member.service;

import org.example.domain.member.dto.request.SignUpRequest;
import org.example.domain.member.dto.response.MemberResponse;
import org.example.domain.member.entity.Member;
import org.example.domain.member.entity.MemberRole;
import org.example.domain.member.repository.MemberRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberServiceImpl 단위 테스트")
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberServiceImpl memberService;

    @Nested
    @DisplayName("signUp 메서드")
    class SignUpTest {

        @Test
        @DisplayName("정상 회원가입 성공")
        void signUp_Success() {
            // given
            SignUpRequest request = createSignUpRequest("test@example.com", "Password123!", "홍길동", "01012345678");
            Member savedMember = createMember(1L, "test@example.com", "encodedPassword", "홍길동", MemberRole.USER);

            given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
            given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");
            given(memberRepository.save(any(Member.class))).willReturn(savedMember);

            // when
            MemberResponse response = memberService.signUp(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getName()).isEqualTo("홍길동");
            assertThat(response.getRole()).isEqualTo(MemberRole.USER);

            verify(memberRepository).existsByEmail(request.getEmail());
            verify(passwordEncoder).encode(request.getPassword());
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("중복 이메일로 가입 시도 시 DUPLICATE_EMAIL 예외 발생")
        void signUp_DuplicateEmail_ThrowsException() {
            // given
            SignUpRequest request = createSignUpRequest("duplicate@example.com", "Password123!", "홍길동", "01012345678");

            given(memberRepository.existsByEmail(request.getEmail())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL);
                    });

            verify(memberRepository).existsByEmail(request.getEmail());
            verify(passwordEncoder, never()).encode(anyString());
            verify(memberRepository, never()).save(any(Member.class));
        }
    }

    @Nested
    @DisplayName("checkEmailDuplicate 메서드")
    class CheckEmailDuplicateTest {

        @Test
        @DisplayName("이미 존재하는 이메일이면 true 반환")
        void checkEmailDuplicate_Exists_ReturnsTrue() {
            // given
            String email = "exists@example.com";
            given(memberRepository.existsByEmail(email)).willReturn(true);

            // when
            boolean result = memberService.checkEmailDuplicate(email);

            // then
            assertThat(result).isTrue();
            verify(memberRepository).existsByEmail(email);
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 false 반환")
        void checkEmailDuplicate_NotExists_ReturnsFalse() {
            // given
            String email = "notexists@example.com";
            given(memberRepository.existsByEmail(email)).willReturn(false);

            // when
            boolean result = memberService.checkEmailDuplicate(email);

            // then
            assertThat(result).isFalse();
            verify(memberRepository).existsByEmail(email);
        }
    }

    // ========== Helper Methods ==========

    private SignUpRequest createSignUpRequest(String email, String password, String name, String phone) {
        return SignUpRequest.builder()
                .email(email)
                .password(password)
                .name(name)
                .phone(phone)
                .build();
    }

    private Member createMember(Long id, String email, String password, String name, MemberRole role) {
        Member member = Member.builder()
                .email(email)
                .password(password)
                .name(name)
                .role(role)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
