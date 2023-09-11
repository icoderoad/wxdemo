package com.icoderoad.example.useronline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
public class UserOnlineApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserOnlineApplication.class, args);
	}

}