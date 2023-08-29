package com.icoderoad.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icoderoad.example.demo.entity.Coupon;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {
	
}
