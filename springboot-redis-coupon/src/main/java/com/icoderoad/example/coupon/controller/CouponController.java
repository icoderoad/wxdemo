package com.icoderoad.example.coupon.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.icoderoad.example.coupon.dto.CouponRequestDTO;
import com.icoderoad.example.coupon.service.CouponService;

@RestController
public class CouponController {
    @Autowired
    private CouponService couponService;

    @PostMapping("/generate-coupon")
    public void generateCoupon(@RequestBody CouponRequestDTO request) {
        couponService.generateCoupon(request.getCode(), request.getValue(), request.getExpiryDate());
    }
    
    @GetMapping("/check-coupon/{code}")
    public String checkCouponExpiration(@PathVariable String code) {
        boolean isExpired = couponService.isCouponExpired(code);
    
        if (isExpired) {
            return "优惠卷 " + code + " 已过期.";
        } else {
            return "优惠卷 " + code + " 未过期.";
        }
    }

}