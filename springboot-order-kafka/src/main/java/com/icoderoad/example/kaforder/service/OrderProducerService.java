package com.icoderoad.example.kaforder.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icoderoad.example.kaforder.entity.Order;

@Component
public class OrderProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic = "order-topic";

    @Autowired
    public OrderProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void produceOrder(Order order) {
        String orderJson = convertOrderToJson(order);
        kafkaTemplate.send(topic, orderJson);
    }

    private String convertOrderToJson(Order order) {
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