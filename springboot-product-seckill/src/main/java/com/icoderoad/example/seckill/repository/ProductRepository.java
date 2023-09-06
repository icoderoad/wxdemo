package com.icoderoad.example.seckill.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.icoderoad.example.seckill.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // 查询秒杀商品列表
    List<Product> findByStartTimeBeforeAndEndTimeAfterAndStatus(LocalDateTime startTime, LocalDateTime endTime, int status);
    
    // 根据商品ID获取商品信息
    Product findByIdAndStatus(Long id, int status);
  
}