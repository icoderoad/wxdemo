package com.icoderoad.example.demo.service;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.icoderoad.example.demo.entity.JwtUser;
import com.icoderoad.example.demo.mapper.JwtUserMapper;

@Service
public class JwtUserService{

    private final JwtUserMapper jwtUserMapper;

    public JwtUserService(JwtUserMapper jwtUserMapper) {
        this.jwtUserMapper = jwtUserMapper;
    }

    public JwtUser getUserDetailsByUsername(String username) {
        // 使用QueryWrapper构造查询条件
        QueryWrapper<JwtUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", username);

        // 使用MyBatis-Plus查询用户信息
        return jwtUserMapper.selectOne(queryWrapper);
    }
}