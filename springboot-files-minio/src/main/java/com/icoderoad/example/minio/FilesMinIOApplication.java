package com.icoderoad.example.minio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
public class FilesMinIOApplication {

	public static void main(String[] args) {
		SpringApplication.run(FilesMinIOApplication.class, args);
	}

}