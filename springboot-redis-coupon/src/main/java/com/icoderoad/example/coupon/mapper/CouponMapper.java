package com.icoderoad.example.coupon.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icoderoad.example.coupon.entity.Coupon;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {
	
}