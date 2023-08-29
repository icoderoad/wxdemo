package com.icoderoad.example.demo.init;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.icoderoad.example.demo.entity.Goods;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class InitGoodsRunner implements CommandLineRunner {

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

    @Value("${goods.quantity}")
    private int goodsQuantity;

    @Override
    public void run(String... args) throws Exception {
        log.info("初始化商品信息...");
        Map<String, Integer> goodsInfo = getGoodsInfoFromProperties();
        goodsInfo.forEach((name, quantity) -> {
            Goods goods = new Goods(generateGoodsId(name), name, quantity);
            redisTemplate.opsForValue().set(generateGoodsKey(goods.getId()), goods);
            log.info("id:{}, 商品: {}，数量: {}，已初始化.", goods.getId(),name, quantity);
        });
        log.info("商品信息初始化完成.");
    }

    private Map<String, Integer> getGoodsInfoFromProperties() {
        Map<String, Integer> goodsInfo = new HashMap<>();
        // 从配置文件中读取商品信息
        goodsInfo.put("商品1", goodsQuantity);
        goodsInfo.put("商品2", goodsQuantity);
        goodsInfo.put("商品3", goodsQuantity);
        goodsInfo.put("商品4", goodsQuantity);
        goodsInfo.put("商品5", goodsQuantity);
        return goodsInfo;
    }

    private Long generateGoodsId(String name) {
        // 这里可以使用一个算法来生成商品ID，此处为演示直接使用name的hashcode作为ID
        return (long) name.hashCode();
    }

    private String generateGoodsKey(Long id) {
        return "goods:" + id;
    }
}
