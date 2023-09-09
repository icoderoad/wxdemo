package com.icoderoad.example.userversion.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.icoderoad.example.userversion.entity.User;

public interface UserRepository extends MongoRepository<User, String> {
	 // 按userName查找用户的方法
    User findByUsername(String userName);
}

