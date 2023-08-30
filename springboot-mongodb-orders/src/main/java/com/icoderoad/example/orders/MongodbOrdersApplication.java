package com.icoderoad.example.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.icoderoad.example.orders.repository")
public class MongodbOrdersApplication {

	public static void main(String[] args) {
		SpringApplication.run(MongodbOrdersApplication.class, args);
	}

}