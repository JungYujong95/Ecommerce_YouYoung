package org.example.global.security.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.domain.member.entity.Member;
import org.example.domain.member.entity.MemberStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String name;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public static CustomUserDetails from(Member member) {
        return new CustomUserDetails(
                member.getId(),
                member.getEmail(),
                member.getPassword(),
                member.getName(),
                List.of(new SimpleGrantedAuthority(member.getRole().getAuthority())),
                member.getStatus() == MemberStatus.ACTIVE
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
