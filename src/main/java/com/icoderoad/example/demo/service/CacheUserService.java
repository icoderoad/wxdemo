package com.icoderoad.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.icoderoad.example.demo.entity.CacheUser;
import com.icoderoad.example.demo.mapper.CacheUserMapper;

@Service
public class CacheUserService {

    @Autowired
    private CacheUserMapper userMapper;

    @Cacheable(value = "users", key = "#id")
    public CacheUser getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @CachePut(value = "users", key = "#user.id")
    public CacheUser updateUser(CacheUser user) {
        userMapper.updateById(user);
        return user;
    }

    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
}