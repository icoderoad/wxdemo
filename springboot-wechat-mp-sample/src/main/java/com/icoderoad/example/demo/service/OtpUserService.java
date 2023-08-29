package com.icoderoad.example.demo.service;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icoderoad.example.demo.entity.OtpUser;
import com.icoderoad.example.demo.mapper.OtpUserMapper;

@Service
public class OtpUserService extends ServiceImpl<OtpUserMapper, OtpUser> {
}