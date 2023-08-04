package com.icoderoad.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import cn.hutool.core.util.RandomUtil;

@SpringBootTest
class DemoApplicationTests {

	@Test
	void contextLoads() {
		//测试提交
	}

	@Test
	void generateKey() {
		System.out.println(RandomUtil.randomString(14));
	}
}
