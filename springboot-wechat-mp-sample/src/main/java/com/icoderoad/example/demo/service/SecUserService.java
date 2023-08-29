package com.icoderoad.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.icoderoad.example.demo.entity.SecUser;
import com.icoderoad.example.demo.repos.SecUserRepository;

@Service
public class SecUserService {
    @Autowired
    private SecUserRepository userRepository;
    
    public SecUser findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}