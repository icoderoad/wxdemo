package com.icoderoad.example.kaforder.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icoderoad.example.kaforder.entity.Order;

@Component
public class TestDataProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic = "order-topic";

    @Autowired
    public TestDataProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void produceTestData(int numberOfOrders) {
        ExecutorService executorService = Executors.newFixedThreadPool(10); // 使用多线程模拟并行推送

        for (int i = 0; i < numberOfOrders; i++) {
            executorService.submit(() -> {
                Order order = generateRandomOrder();
                String orderJson = convertOrderToJson(order);
                kafkaTemplate.send(topic, orderJson);
            });
        }

        executorService.shutdown();
        // 等待所有任务完成
        while (!executorService.isTerminated()) {
            // 空循环
        }
    }

    private Order generateRandomOrder() {
        // 生成随机订单数据
        Order order = new Order();
        order.setOrderDate(new Date());
        order.setCustomerId((long) (Math.random() * 1000)); // 随机客户ID
        order.setTotalAmount(new BigDecimal(Math.random() * 1000));
        return order;
    }

    public String convertOrderToJson(Order order) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            // 处理异常情况，例如日志记录或抛出自定义异常
            e.printStackTrace();
            return null; // 或者抛出异常
        }
		}	
}