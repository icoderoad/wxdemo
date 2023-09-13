package com.icoderoad.example.chat.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Value("${spring.rabbitmq.host}") // 获取rabbitmq.host配置属性的值
    private String rabbitmqHost;
	@Value("${spring.rabbitmq.username}") 
	private String rabbitmqUser;
	@Value("${spring.rabbitmq.password}")
	private String rabbitmqPassword;
	
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableStompBrokerRelay("/topic")
        .setRelayHost(rabbitmqHost) // 使用配置属性中的RabbitMQ主机名
        .setRelayPort(61613) // 设置RabbitMQ STOMP端口
        .setClientLogin(rabbitmqUser)
        .setClientPasscode(rabbitmqPassword)
        .setSystemLogin(rabbitmqUser)
        .setSystemPasscode(rabbitmqPassword);;
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chat").withSockJS();
    }
}