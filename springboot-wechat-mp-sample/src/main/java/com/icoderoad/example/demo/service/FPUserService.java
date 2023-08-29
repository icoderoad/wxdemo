package com.icoderoad.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icoderoad.example.demo.entity.FPUser;
import com.icoderoad.example.demo.mapper.FPUserMapper;

@Service
public class FPUserService extends ServiceImpl<FPUserMapper, FPUser> {
    private final FPUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public FPUserService(FPUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public FPUser findByUserName(String userName) {
    	  // 根据用户名查询用户
        QueryWrapper<FPUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        return userMapper.selectOne(queryWrapper);
    }
    
    public FPUser findByEmail(String email) {
    	// 根据用户名查询用户
    	QueryWrapper<FPUser> queryWrapper = new QueryWrapper<>();
    	queryWrapper.eq("email", email);
    	return userMapper.selectOne(queryWrapper);
    }

    public void updateUserPassword(Long userId, String newPassword) {
        FPUser user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userMapper.updateById(user);
        }
    }
}