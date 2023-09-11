package com.icoderoad.example.useronline.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.icoderoad.example.useronline.entity.User;
import com.icoderoad.example.useronline.repository.UserRepository;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
}