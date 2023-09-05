package com.icoderoad.example.order.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderSenderService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendOrderExpirationMessage(Long orderId, int delayInMilliseconds) {
        // 发送延迟消息
        rabbitTemplate.convertAndSend("order-exchange", "order-expiration", orderId, message -> {
            message.getMessageProperties().setExpiration(String.valueOf(delayInMilliseconds));
            return message;
        });
    }
}