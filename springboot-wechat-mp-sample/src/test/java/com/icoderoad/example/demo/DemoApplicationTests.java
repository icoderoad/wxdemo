package com.icoderoad.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import cn.hutool.core.util.RandomUtil;

@SpringBootTest
class DemoApplicationTests {

	@Test
	void contextLoads() {
		//测试提交
		PasswordEncoder passwordEncoder = new	BCryptPasswordEncoder();
		String pwd="admin";
		System.out.println(passwordEncoder.encode(pwd));
	}

	@Test
	void generateKey() {
		System.out.println(RandomUtil.randomString(14));
	}
}
