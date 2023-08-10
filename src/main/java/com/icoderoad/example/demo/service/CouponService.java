package com.icoderoad.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.icoderoad.example.demo.entity.Coupon;
import com.icoderoad.example.demo.mapper.CouponMapper;

@Service
public class CouponService {
    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 生成 coupon
    public void generateCoupon(String code, BigDecimal value, LocalDateTime expiryDate) {
        // 入库
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setValue(value);
        coupon.setExpiryDate(expiryDate);
        couponMapper.insert(coupon);

        // 缓存数据
        redisTemplate.opsForList().leftPush("coupons", coupon);
    }

    // 检测过期数据
    public List<Coupon> getExpiredCoupons() {
        Date currentTime = new Date();
        List<Coupon> expiredCoupons = couponMapper.selectList(
            new QueryWrapper<Coupon>()
                .lt("expiry_date", currentTime)
        );

        redisTemplate.opsForList().remove("coupons", 0, expiredCoupons);

        return expiredCoupons;
    }
    
    public boolean isCouponExpired(String code) {
        Coupon coupon = couponMapper.selectOne(
            new QueryWrapper<Coupon>()
                .eq("code", code)
        );

        if (coupon == null) {
            // Handle the case where the coupon code doesn't exist
            return false;
        }

        LocalDateTime currentTime = LocalDateTime.now();
        return coupon.getExpiryDate().isBefore(currentTime);
    }
}