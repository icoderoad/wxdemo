package com.icoderoad.example.user.repository;

import org.springframework.data.repository.CrudRepository;

import com.icoderoad.example.user.entity.User;

public interface UserRepository extends CrudRepository<User, Long> {
}