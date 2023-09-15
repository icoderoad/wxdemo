package com.icoderoad.example.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.icoderoad.example.user.entity.User;

@Service
public class UserService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    // 用户验证方法，检查用户名和密码是否匹配数据库中的记录
    public boolean authenticateUser(String username, String password) {
        // 查询数据库中的用户信息
        String sql = "SELECT username, password FROM session_users WHERE username = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, new Object[]{username}, (rs, rowNum) ->
                    new User(rs.getString("username"), rs.getString("password")));
            if (user != null && passwordEncoder.matches(password, user.getPassword())) {
                return true; // 用户验证成功
            }
        } catch (Exception e) {
            // 用户不存在或发生其他异常
        }
        return false; // 用户验证失败
    }
}