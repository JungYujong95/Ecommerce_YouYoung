package org.example.domain.member.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.domain.member.entity.Member;
import org.example.domain.member.entity.MemberRole;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberResponse {

    private Long id;
    private String email;
    private String name;
    private String phone;
    private MemberRole role;
    private LocalDateTime createdAt;

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .phone(member.getPhone())
                .role(member.getRole())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
