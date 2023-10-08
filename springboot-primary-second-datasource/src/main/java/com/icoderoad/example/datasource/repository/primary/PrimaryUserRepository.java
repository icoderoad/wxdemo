package com.icoderoad.example.datasource.repository.primary;

import org.springframework.data.jpa.repository.JpaRepository;

import com.icoderoad.example.datasource.entity.primary.User;

//主数据源Repository
public interface PrimaryUserRepository extends JpaRepository<User, Long> {
}