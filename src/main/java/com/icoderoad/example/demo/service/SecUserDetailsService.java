package com.icoderoad.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.icoderoad.example.demo.entity.SecUser;
import com.icoderoad.example.demo.repos.SecUserRepository;

@Service
public class SecUserDetailsService implements UserDetailsService {

    @Autowired
    private SecUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    	 // 根据用户名从数据库中获取用户信息
        SecUser user = userRepository.findByUsername(username);
        if (user == null) {
        	 // 如果用户不存在，抛出UsernameNotFoundException异常
            throw new UsernameNotFoundException("User not found");
        }

        // 获取用户角色
        List<GrantedAuthority> authorities = new ArrayList<>();
        user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role.getName())));
        
        // 创建并返回UserDetails对象，用于Spring Security认证
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(), user.getPassword(), user.isEnabled(), true, true, true, authorities);
    }
}