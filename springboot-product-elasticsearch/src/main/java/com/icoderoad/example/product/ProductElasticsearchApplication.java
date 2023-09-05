package com.icoderoad.example.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.icoderoad.example.product.mapper")
public class ProductElasticsearchApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductElasticsearchApplication.class, args);
	}

}