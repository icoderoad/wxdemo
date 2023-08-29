package com.icoderoad.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.icoderoad.example.demo.entity.SecRole;
import com.icoderoad.example.demo.repos.SecRoleRepository;

@Service
public class SecRoleService {
    @Autowired
    private SecRoleRepository roleRepository;
    
    public SecRole findByName(String name) {
        return roleRepository.findByName(name);
    }
}