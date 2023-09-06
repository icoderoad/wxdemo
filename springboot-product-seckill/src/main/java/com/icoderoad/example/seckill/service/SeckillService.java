package com.icoderoad.example.seckill.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.icoderoad.example.seckill.entity.Order;
import com.icoderoad.example.seckill.entity.Product;

@Service
public class SeckillService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    // 初始化秒杀商品信息到Redis缓存
    @PostConstruct
    public void initSeckillProductCache() {
        List<Product> seckillProducts = productService.listSecKillProducts();
        for (Product product : seckillProducts) {
            String key = "seckill_product:" + product.getId();
            redisTemplate.opsForValue().set(key, String.valueOf(product.getStock()));
        }
    }

    // 秒杀商品
    public boolean seckillProduct(Long userId, Long productId) {
        String key = "seckill_product:" + productId;
        String stockStr = redisTemplate.opsForValue().get(key);
        
        if (stockStr != null) {
            int stock = Integer.parseInt(stockStr);
            if (stock > 0) {
                // 在Redis中原子减少库存
                Long updatedStock = redisTemplate.opsForValue().decrement(key);
                if (updatedStock >= 0) {
                	
                    // 创建订单
                    Order order = orderService.createOrder(userId, productId);
                    if (order != null) {
                        return true;
                    } else {
                        // 订单创建失败，恢复Redis中的库存
                        redisTemplate.opsForValue().increment(key);
                    }
                }
            }
        }

        return false;
    }
}