package com.icoderoad.example.seckill.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.icoderoad.example.seckill.entity.Product;
import com.icoderoad.example.seckill.repository.ProductRepository;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;
    
    // 查询秒杀商品列表
    public List<Product> listSecKillProducts() {
        LocalDateTime now = LocalDateTime.now();
        return productRepository.findByStartTimeBeforeAndEndTimeAfterAndStatus(now, now, 1);
    }
    
}