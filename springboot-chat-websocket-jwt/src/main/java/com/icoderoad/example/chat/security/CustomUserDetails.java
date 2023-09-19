package com.icoderoad.example.chat.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

    private String username;
    private String password;
    private List<GrantedAuthority> authorities;

    public CustomUserDetails(String username, String password, List<String> roles) {
        this.username = username;
        this.password = password;
        this.authorities = new ArrayList<>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 帐户永不过期
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 帐户永不锁定
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 凭据永不过期
    }

    @Override
    public boolean isEnabled() {
        return true; // 帐户启用
    }
}