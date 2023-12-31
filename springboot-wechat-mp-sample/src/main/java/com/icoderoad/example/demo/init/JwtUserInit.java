package com.icoderoad.example.demo.init;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.icoderoad.example.demo.entity.JwtUser;
import com.icoderoad.example.demo.mapper.JwtUserMapper;

@Component
public class JwtUserInit implements CommandLineRunner {

	@Autowired
    private JwtUserMapper jwtUserMapper;
	
	@Autowired
	@Qualifier("passwordEncoder")
    private PasswordEncoder passwordEncoder;

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
            user.setPassword(passwordEncoder.encode(password)); // 使用 BCrypt 对密码进行加密
            user.setNickName("管理员");
            user.setCreateTime(LocalDateTime.now());
            jwtUserMapper.insert(user);
        }
    }
}
