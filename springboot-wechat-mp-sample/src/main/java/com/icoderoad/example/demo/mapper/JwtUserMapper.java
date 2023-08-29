package com.icoderoad.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icoderoad.example.demo.entity.JwtUser;

@Mapper
public interface JwtUserMapper extends BaseMapper<JwtUser> {
    // 可自定义查询方法
}