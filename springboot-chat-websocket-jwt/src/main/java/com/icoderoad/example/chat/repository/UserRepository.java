package com.icoderoad.example.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.icoderoad.example.chat.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    
    @Query("SELECT u FROM User u WHERE u.username <> ?1")
    List<User> findAllExceptCurrentUser(String username);
}