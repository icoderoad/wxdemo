package com.icoderoad.example.demo.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.icoderoad.example.demo.entity.SecUser;

@Repository
public interface SecUserRepository extends JpaRepository<SecUser, Long> {
    SecUser findByUsername(String username);
}