package com.icoderoad.example.user.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitUser implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public InitUser(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 在此处创建初始用户
        String username = "admin";
        String password = "admin";
        String encodedPassword = passwordEncoder.encode(password);

        // 检查数据库中是否已存在用户，如果不存在，则插入初始用户
        String sql = "SELECT COUNT(*) FROM session_users WHERE username = ?";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, username);

        if (count == 0) {
            jdbcTemplate.update("INSERT INTO session_users (username, password) VALUES (?, ?)", username, encodedPassword);
        }
    }
}