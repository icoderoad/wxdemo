package com.icoderoad.example.demo.init;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.icoderoad.example.demo.service.CouponService;

@Component
public class InitCouponRunner implements CommandLineRunner {
    private static final int NUM_INITIAL_COUPONS = 20;
    private static final int MAX_EXPIRY_DAYS = 20;

    @Autowired
    private CouponService couponService;

    @Override
    public void run(String... args) throws Exception {
        generateInitialCoupons();
    }

    private void generateInitialCoupons() {
        Random random = new Random();
        LocalDateTime currentTime = LocalDateTime.now();

        for (int i = 0; i < NUM_INITIAL_COUPONS; i++) {
            String code = generateRandomCode(18);
            BigDecimal value = BigDecimal.valueOf(random.nextDouble() * 100);
            int expiryDays = random.nextInt(MAX_EXPIRY_DAYS) + 1;
            LocalDateTime expiryDate = currentTime.plusDays(expiryDays);

            couponService.generateCoupon(code, value, expiryDate);
        }
    }

    private String generateRandomCode(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();

        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            code.append(chars.charAt(index));
        }

        return code.toString();
    }
}