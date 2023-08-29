package com.icoderoad.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwtUserInfoController {

	@GetMapping("/api/user-info")
	public ResponseEntity<String> getUserInfo(Authentication authentication) {
		// 从 authentication 对象中获取用户名
		String username = authentication.getName();

		// 返回 JSON 数据，包含用户名信息
		String responseJson = "{\"username\": \"" + username + "\"}";
		return ResponseEntity.ok(responseJson);
	}
}
