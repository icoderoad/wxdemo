package com.icoderoad.example.datasource.repository.secondary;

import org.springframework.data.jpa.repository.JpaRepository;

import com.icoderoad.example.datasource.entity.secondary.SecUser;

//第二个数据源Repository
public interface SecondaryUserRepository extends JpaRepository<SecUser, Long> {
}