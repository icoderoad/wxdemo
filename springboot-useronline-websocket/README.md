Springboot + websocket  实现用户实时在线统计功能

以下是一个使用Spring Boot和WebSocket实现用户实时在线统计功能的示例，包括相关代码和注释说明。请注意，由于篇幅有限，我将提供关键代码和说明，大家需要根据自己的项目需求进一步完善。

**用户表DDL定义**（可以根据实际需求进行修改）：

```sql
CREATE TABLE online_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
);
```

初始化用户SQL语句

```sql
INSERT INTO `online_users` (`id`, `username`, `password`)
VALUES
	(1000, 'admin', 'admin');
```

**pom.xml**（添加Spring Boot、WebSocket、Thymeleaf和JPA的依赖配置）：

```xml
<!-- Spring Boot Starter Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Boot Starter WebSocket -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- Spring Boot Starter Thymeleaf -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- Spring Boot Starter Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
 <!-- MySQL数据库驱动依赖 -->
<dependency>
  <groupId>mysql</groupId>
  <artifactId>mysql-connector-java</artifactId>
  <version>8.0.33</version>
</dependency>
```

**application.properties**（数据库和WebSocket配置）：

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# WebSocket配置
spring.websocket.path=/websocket

spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false
```

**User实体类**（用于JPA持久化用户信息）：

```java
package com.icoderoad.example.useronline.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name="online_users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    
}
```

**UserRepository**（JPA仓库接口）：

```java
package com.icoderoad.example.useronline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.icoderoad.example.useronline.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
```

**UserService**（用于处理用户相关的逻辑）：

```java
package com.icoderoad.example.useronline.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.icoderoad.example.useronline.entity.User;
import com.icoderoad.example.useronline.repository.UserRepository;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
}
```

**WebSocket配置**：

```java
package com.icoderoad.example.useronline.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/websocket").withSockJS();
    }
}
```

**UserController**（处理登录和WebSocket相关逻辑）：

```java
package com.icoderoad.example.useronline.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.icoderoad.example.useronline.entity.User;
import com.icoderoad.example.useronline.service.UserService;

@Controller
public class UserController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserService userService;

    @MessageMapping("/hello")
    public void greeting(String message) throws Exception {
        // 处理WebSocket消息
        // 发送消息到/topic/onlineUsers
        messagingTemplate.convertAndSend("/topic/onlineUsers", message);
    }

    @GetMapping("/login")
    public String loginForm() {
        return "user/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model) {
        // 根据用户名和密码验证登录
        User user = userService.getUserByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            // 登录成功
            // 发送WebSocket消息通知其他用户
            messagingTemplate.convertAndSend("/topic/onlineUsers", username + " 已登录");
            // 登录成功
            return "redirect:/login?success=true"; // 重定向到登录页并传递成功提示参数
        } else {
            // 登录失败
            return "redirect:/login?error";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 获取在线用户列表，显示在Thymeleaf视图中
        return "user/dashboard";
    }
}
```

**Thymeleaf视图**（dashboard.html和login.html，使用Bootstrap美化页面）：

```html
<!-- dashboard.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container mt-4">
    	 <div class="row justify-content-center">
            <div class="col-md-6"> <!-- 控制表单的宽度为屏幕的60% -->
		        <h1 class="mb-4">在线用户列表</h1>
		        <ul class="list-group" id="userList">
		            <!-- 用户列表项 -->
		        </ul>
		    </div>
		  </div>
    </div>

    
    <script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>
	<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.1.4/dist/sockjs.min.js"></script>

    <script th:inline="javascript">
	    var stompClient = null;
	
	    function connect() {
	        var socket = new SockJS('/websocket');
	        stompClient = Stomp.over(socket);
	        stompClient.connect({}, function(frame) {
	            stompClient.subscribe('/topic/onlineUsers', function(message) {
	                // 处理在线用户消息
	                var userList = document.getElementById('userList');
	                var listItem = document.createElement('li');
	                listItem.className = 'list-group-item';
	                listItem.textContent = message.body;
	                userList.appendChild(listItem);
	            });
	        });
	    }
	
	    connect();
    </script>
</body>
</html>

<!-- login.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
	 <div class="container mt-5">
        <div class="row justify-content-center">
            <div class="col-md-6"> <!-- 控制表单的宽度为屏幕的60% -->
			    <h1>用户登陆页</h1>
			    <form th:action="@{/login}" method="post">
			        <div class="form-group">
			            <label for="username">用户名:</label>
			            <input type="text" class="form-control" id="username" name="username" required>
			        </div>
			        <div class="form-group">
			            <label for="password">密码:</label>
			            <input type="password" class="form-control" id="password" name="password" required>
			        </div>
			        <button type="submit" class="btn btn-primary">登陆</button>
			    </form>
			     <!-- 显示登录成功提示（如果有） -->
			    <div th:if="${param.success}" class="alert alert-success">用户登陆成功!</div>
			    <!-- 显示登录错误消息（如果有） -->
			    <div th:if="${param.error}" class="alert alert-danger">用户名或密码验证失败</div>
		   </div>
		 </div>
	</div>
</body>
</html>
```

启动 Spring Boot 应用后，访问 http://localhost:8080/dashboard ，显示在线用户列表，访问http://localhost:8080/login 显示登陆页面，用户名密码均输入 admin ,点击登陆，提示 "用户登陆成功!".切换到  http://localhost:8080/dashboard  页面，可以看到登陆的用户 admin 显示在列表中。
