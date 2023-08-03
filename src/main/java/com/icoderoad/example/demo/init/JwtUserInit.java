package com.icoderoad.example.demo.init;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.icoderoad.example.demo.entity.JwtUser;
import com.icoderoad.example.demo.mapper.JwtUserMapper;

@Component
public class JwtUserInit implements CommandLineRunner {

    private final JwtUserMapper jwtUserMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public JwtUserInit(JwtUserMapper jwtUserMapper, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.jwtUserMapper = jwtUserMapper;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 初始化 admin 用户
        String username = "admin";
        String password = "123456";
        QueryWrapper<JwtUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", username);
        if (jwtUserMapper.selectOne(queryWrapper) == null) {
            JwtUser user = new JwtUser();
            user.setUserName(username);
            user.setPassword(bCryptPasswordEncoder.encode(password)); // 使用 BCrypt 对密码进行加密
            user.setNickName("管理员");
            user.setCreateTime(LocalDateTime.now());
            jwtUserMapper.insert(user);
        }
    }
}
