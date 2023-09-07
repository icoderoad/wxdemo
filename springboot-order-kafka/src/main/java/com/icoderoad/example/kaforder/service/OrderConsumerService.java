package com.icoderoad.example.kaforder.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icoderoad.example.kaforder.entity.Order;
import com.icoderoad.example.kaforder.repository.OrderRepository;

@Component
public class OrderConsumerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OrderRepository orderRepository; // 你的OrderRepository

    @Autowired
    public OrderConsumerService(KafkaTemplate<String, String> kafkaTemplate, OrderRepository orderRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderRepository = orderRepository;
    }

    @KafkaListener(topics = "order-topic", containerFactory = "batchFactory")
    public void consumeOrders(List<String> orderJsonList) {
        for (String orderJson : orderJsonList) {
            Order order = convertJsonToOrder(orderJson);
            if (order != null) {
                // 处理订单的业务逻辑，例如保存到数据库
                saveOrderToDatabase(order);
            }
        }
    }

    private Order convertJsonToOrder(String orderJson) {
    	try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(orderJson, Order.class);
        } catch (Exception e) {
            // 处理异常情况，例如日志记录或抛出自定义异常
            e.printStackTrace();
            return null; // 或者抛出异常
        }
    }

    private void saveOrderToDatabase(Order order) {
        // 将订单保存到数据库
        orderRepository.save(order);
    }
}