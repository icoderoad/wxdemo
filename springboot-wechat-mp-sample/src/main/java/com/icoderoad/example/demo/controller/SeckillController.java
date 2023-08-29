package com.icoderoad.example.demo.controller;


import com.icoderoad.example.demo.entity.Goods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seckill")
@Slf4j
public class SeckillController {

    private final StringRedisTemplate redisTemplate;

    public SeckillController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @RequestMapping("/{id}")
    public String seckill(@PathVariable Long id) {
        String key = generateGoodsKey(id);
        String value = redisTemplate.opsForValue().get(key);

        if (value != null) {
            Goods goods = Goods.fromJson(value);
            if (goods.getQuantity() > 0) {
                goods.setQuantity(goods.getQuantity() - 1);
                String newValue = goods.toJson();
                redisTemplate.opsForValue().set(key, newValue);
                return "秒杀成功！商品名：" + goods.getName() + "，剩余数量：" + goods.getQuantity();
            }
        }
        return "秒杀失败，商品不存在或已售罄！";
    }

    private String generateGoodsKey(Long id) {
        return "goods:" + id;
    }
}
