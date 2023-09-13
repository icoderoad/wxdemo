Springboot + websocket +RabbitMQ + thymeleaf + bootstrap 实现用户聊天互动功能

为了实现Spring Boot中的用户聊天互动功能，我们可以使用WebSocket来处理实时通信，RabbitMQ来处理消息传递，Thymeleaf和Bootstrap来创建用户界面。以下是详细的步骤和相应的配置。

**创建Spring Boot项目**

首先，创建一个Spring Boot项目，并确保在项目的pom.xml文件中添加以下依赖：

```xml
<!-- Spring Boot Starter Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Boot Starter Thymeleaf -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- Spring Boot Starter WebSocket -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- RabbitMQ -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-reactor-netty</artifactId>
</dependency>
```

**配置RabbitMQ**

在`application.properties`中添加RabbitMQ的配置：

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=your-username
spring.rabbitmq.password=your-password

# Thymeleaf模板配置
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false
```

**创建静态资源配置类**

```java
package com.icoderoad.example.chat.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源路径，例如将所有以 "/**" 开头的请求映射到 classpath:/static/ 目录下的静态资源
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}
```

**创建WebSocket配置类**

创建一个WebSocket配置类，用于配置WebSocket的相关内容：

```java
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
```

** 创建聊天控制器**

创建一个控制器类，处理WebSocket消息：

```java
package com.icoderoad.example.chat.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.icoderoad.example.chat.entity.ChatMessage;

@Controller
public class ChatController {
	
	@GetMapping("/")
    public String chat(Model model) {
        return "chat"; 
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(ChatMessage message) {
        return message;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(ChatMessage message) {
        return message;
    }
}
```

**创建聊天消息实体**

创建一个表示聊天消息的实体类：

```java
package com.icoderoad.example.chat.entity;

import lombok.Data;

@Data
public class ChatMessage {

    private String content;
    private String sender;

}
```

**创建Thymeleaf模板**

创建一个Thymeleaf模板来显示聊天界面。在`src/main/resources/templates`目录下创建一个名为`chat.html`的文件：

```html
<!DOCTYPE html>
<html>
<head>
    <title>聊天应用</title>
    <!-- 使用 jsdelivr.net 上的 Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-alpha1/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container">
        <div class="row">
            <div class="col-md-4 offset-md-4">
                <h3>聊天应用</h3>
                <div id="chat" class="border p-3" style="height: 300px; overflow-y: scroll;"></div>
                <form id="messageForm" class="mt-3">
                	<input type="text" id="sender" class="form-control" placeholder="你的名字" />
                    <input type="text" id="message" class="form-control" placeholder="输入你的消息..." />
                    <button type="submit" class="btn btn-primary mt-2">发送</button>
                </form>
            </div>
        </div>
    </div>

    <!-- 使用 jsdelivr.net 上的 jQuery 和 Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/jquery@3.6.0/dist/jquery.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0/dist/js/bootstrap.min.js"></script>
    
    <!-- 使用 jsDelivr CDN 引入 sockjs-client 和 stomp.js -->
	<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js"></script>
	<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>

    
    <!-- 使用本地的 app.js -->
    <script src="/js/app.js"></script>
</body>
</html>
```

**创建WebSocket JavaScript客户端**

在`src/main/resources/static/js`目录下创建一个名为`app.js`的文件，用于处理WebSocket客户端：

```js
var stompClient = null;

// 假设你的表单中有两个输入字段，一个是发送者的名称，另一个是消息内容
var senderInput = document.getElementById('sender');
var messageInput = document.getElementById('message');
var messageForm = document.getElementById('messageForm');

messageForm.addEventListener('submit', function (event) {
    // 阻止表单的默认提交行为
    event.preventDefault();

    // 获取发送者名称和消息内容
    var sender = senderInput.value.trim();
    var message = messageInput.value.trim();

    // 验证发送者和消息内容是否为空
    if (sender === '' || message === '') {
        // 如果有任何一个字段为空，显示错误消息
        alert('发送者和消息内容不能为空！');
        return; // 阻止继续执行下面的代码
    }

});

function connect() {
    var socket = new SockJS('/chat');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/public', function (response) {
            showMessage(JSON.parse(response.body));
        });
    });
}

function showMessage(message) {
    $("#chat").append("<p>" + message.sender + ": " + message.content + "</p>");
}

function sendMessage() {
    var message = $("#message").val();
    var sender = $("#sender").val();
    stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({ sender: sender, content: message }));
    $("#message").val("");
}

$(function () {
    connect();
    $("#messageForm").on('submit', function (e) {
        e.preventDefault();
        sendMessage();
    });
});
```

**启动应用**

现在，你可以启动你的Spring Boot应用。用户可以访问`http://localhost:8080/来使用聊天功能。

这就是一个基本的Spring Boot应用，使用WebSocket、RabbitMQ、Thymeleaf和Bootstrap来实现用户聊天互动功能。