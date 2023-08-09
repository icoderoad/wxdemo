package com.icoderoad.example.demo.password;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptPasswordEncoderTest {
	public static void main(String[] args) {
		String rawPassword = "123456";
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

		// Encode the password
		String encodedPassword = passwordEncoder.encode(rawPassword);
		System.out.println(encodedPassword);
		System.out.println(passwordEncoder.matches(rawPassword, encodedPassword));

	}

}
