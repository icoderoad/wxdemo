package com.icoderoad.example.order.conf;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	// 定义一个Fanout交换机，用于广播消息给多个队列
    @Bean
    public FanoutExchange orderExchange() {
        return new FanoutExchange("order-exchange");
    }

    // 定义1个队列，并将它们绑定到orderExchange交换机上
    @Bean
    public Queue orderExpirationQueue() {
        return new Queue("order-expiration-queue");
    }

    @Bean
    public Binding bindingOrderExpiration(Queue orderExpirationQueue, FanoutExchange orderExchange) {
        return BindingBuilder.bind(orderExpirationQueue).to(orderExchange);
    }
 
}