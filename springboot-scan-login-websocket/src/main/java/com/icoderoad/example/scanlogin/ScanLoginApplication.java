package com.icoderoad.example.scanlogin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
public class ScanLoginApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScanLoginApplication.class, args);
	}

}