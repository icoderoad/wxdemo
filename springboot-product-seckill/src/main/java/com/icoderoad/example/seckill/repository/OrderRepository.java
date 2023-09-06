package com.icoderoad.example.seckill.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.icoderoad.example.seckill.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 根据用户ID和商品ID查询订单
    Order findByUserIdAndProductId(Long userId, Long productId);
    
    // 查询某个用户的所有订单
    List<Order> findByUserId(Long userId);
    
    // 查询某个商品的所有订单
    List<Order> findByProductId(Long productId);
}