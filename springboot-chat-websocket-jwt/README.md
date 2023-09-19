使用Spring Boot、WebSocket、Thymeleaf和JWT实现即时通讯功能

对于创建一个使用Spring Boot、WebSocket和JWT的即时通讯模块，这是一个相对复杂的任务，需要多个文件和配置。以下是一个简要的步骤和代码示例，以帮助大家入门。

**创建一个Spring Boot项目：**

可以使用Spring Initializer（https://start.spring.io/）或者使用Spring Boot CLI 创建一个新的Spring Boot项目。

**添加依赖：**

在`pom.xml`中添加以下依赖：

```xml
<dependencies>
    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- Spring Boot Starter Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <!-- Spring Boot Starter Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <!-- MySQL Connector -->
    <dependency>
      <groupId>mysql</groupId>
      <artifactId>mysql-connector-java</artifactId>
      <version>${mysql-connector-java.version}</version>
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
    <!-- JSON Web Token (JWT) Support -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt</artifactId>
        <version>0.9.1</version>
    </dependency>
</dependencies>
```

**配置application.properties 属性：**

在`application.properties`中配置你的数据库连接信息：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

jwt.secret-key=ZxqataLnkjxnfc4Ew4J3uHlmJn0MqVkJLOECgiCFfQC
spring.main.allow-circular-references=true

spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false
```

**创建用户表（DDL语句）：**

根据需求创建用户表，例如：

```sql
CREATE TABLE socket_jwt_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
);
```

**创建实体类和Repository：**

创建一个用户实体类和对应的Repository来处理用户数据的访问。

```java
package com.icoderoad.example.chat.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name = "socket_jwt_users")
public class User {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
}
```

**创建Repository类：**

```java
package com.icoderoad.example.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.icoderoad.example.chat.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
```

**创建ChatMessage实体类**

```java
package com.icoderoad.example.chat.entity;

import lombok.Data;

@Data
public class ChatMessage {
    private String sender;
    private String recipient;
    private String message;
}
```

**创建JwtTokenUtil工具类** 

```java
package com.icoderoad.example.chat.util;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

@Component
public class JwtTokenUtil {

    @Value("${jwt.secret-key}")
    private String secretKey; // 从配置文件读取密钥

    private static final long EXPIRATION_TIME = 3600000; // 1小时
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HEADER_STRING = "Authorization";

    // 创建JWT令牌
    public String generateToken(String username) {
        Date expirationDate = new Date(System.currentTimeMillis() + EXPIRATION_TIME);

        return Jwts.builder()
                .setSubject(username)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .compact();
    }

    // 验证JWT令牌
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            // 无效的签名
        } catch (MalformedJwtException e) {
            // 无效的令牌
        } catch (ExpiredJwtException e) {
            // 令牌已过期
        } catch (UnsupportedJwtException e) {
            // 不支持的令牌
        } catch (IllegalArgumentException e) {
            // 无效的令牌
        }
        return false;
    }

    // 从令牌中获取用户名
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }
}
```

**创建WebSocket配置类：**

配置WebSocket支持，允许即时通讯。

```java
package com.icoderoad.example.chat.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.icoderoad.example.chat.util.JwtTokenUtil;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	@Autowired
	private JwtTokenUtil jwtTokenUtil;
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue", "/topic", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptorAdapter() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 在此处验证JWT令牌
                	 String jwtToken = accessor.getFirstNativeHeader("jwt-token");
                    if (jwtToken==null || !jwtTokenUtil.validateToken(jwtToken.toString())) {
                        throw new CredentialsExpiredException("Invalid JWT token");
                    }
                }
                return message;
            }
        });
    }
   
}
```

**创建认证和授权配置类：**

认证成功处理类 CustomAuthenticationSuccessHandler

```java
package com.icoderoad.example.chat.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${jwt.secret-key}") // 从配置文件读取密钥
    private String secretKey;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 生成JWT Token
        String token = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .compact();

        // 添加JWT Token到响应头
        response.addHeader("Authorization", "Bearer " + token);

        // 重定向到/chat页面
        response.sendRedirect("/chat");
    }
}
```

自定义的用户实体类 CustomUserDetails

```java
package com.icoderoad.example.chat.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

    private String username;
    private String password;
    private List<GrantedAuthority> authorities;

    public CustomUserDetails(String username, String password, List<String> roles) {
        this.username = username;
        this.password = password;
        this.authorities = new ArrayList<>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 帐户永不过期
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 帐户永不锁定
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 凭据永不过期
    }

    @Override
    public boolean isEnabled() {
        return true; // 帐户启用
    }
}
```

创建一个实现 `UserDetailsService` 接口的服务类 CustomUserDetailsService

```java
package com.icoderoad.example.chat.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.icoderoad.example.chat.entity.User;
import com.icoderoad.example.chat.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        // 创建 CustomUserDetails 对象并返回
        return new CustomUserDetails(user.getUsername(), user.getPassword(), Arrays.asList("admin"));
    }
}
```

创建一个认证和授权配置类，使用JWT进行用户认证。

```java
package com.icoderoad.example.chat.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.icoderoad.example.chat.security.CustomAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;
  
    @Autowired
    private CustomAuthenticationSuccessHandler authenticationSuccessHandler;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
                .and()
            .formLogin()
                .loginPage("/login")
                .loginProcessingUrl("/doLogin")
          		.successHandler(authenticationSuccessHandler)
          		.usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
                .and()
            .logout()
                .permitAll()
                .and()
            .csrf().disable();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
```

创建一个初始化类，实现 `CommandLineRunner` 接口，用于添加 admin 和 test 用户到数据库中

```java
package com.icoderoad.example.chat.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.icoderoad.example.chat.entity.User;
import com.icoderoad.example.chat.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 初始化 admin 用户
    	if( userRepository.findByUsername("admin") == null ) {
	        User adminUser = new User();
	        adminUser.setUsername("admin");
	        adminUser.setPassword(passwordEncoder.encode("admin")); // 使用密码编码器加密密码
	//        adminUser.setRoles("ADMIN");
	        userRepository.save(adminUser);
    	}

        // 初始化 test 用户
    	if( userRepository.findByUsername("test") == null ) {
	        User testUser = new User();
	        testUser.setUsername("test");
	        testUser.setPassword(passwordEncoder.encode("test")); // 使用密码编码器加密密码
	//        testUser.setRoles("USER");
	        userRepository.save(testUser);
    	}
    }
}
```

**创建ChatController 类：**

创建WebSocket处理器，处理WebSocket连接和消息。

```java
package com.icoderoad.example.chat.controller;

import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.icoderoad.example.chat.entity.ChatMessage;
import com.icoderoad.example.chat.entity.User;
import com.icoderoad.example.chat.repository.UserRepository;
import com.icoderoad.example.chat.util.JwtTokenUtil;

@Controller
public class ChatController {
  
	@Autowired
	private JwtTokenUtil jwtTokenUtil;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
    private SimpMessagingTemplate messagingTemplate;

  	@GetMapping("/login")
    public String loginPage() {
        return "login";
    }
  
    @GetMapping("/chat")
    public String chatPage(HttpServletRequest request, Model model) {
        // 从请求中获取 JWT Token
        String jwtToken = extractJwtTokenFromRequest(request);
			
        // 检查 JWT Token 是否有效
        if (jwtTokenUtil.validateToken(jwtToken)) {
            // 用户已登录，显示消息发送页面
        	String currentUsername = jwtTokenUtil.getUsernameFromToken(jwtToken);
        	model.addAttribute("currentUsername", currentUsername);
        	 List<User> users = userRepository.findAllExceptCurrentUser(currentUsername);

             model.addAttribute("users", users);
             model.addAttribute("jwtToken", jwtToken);
            return "chat";
        } else {
            // 用户未登录，重定向到登录页面
            return "redirect:/login";
        }
    }

    @MessageMapping("/sendSystemMessage")
    public void handleMessage(@Payload ChatMessage message) {
       
            // 在这里处理消息
            String content = message.getMessage();
            String sender = message.getSender();

            // 构建响应消息
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMessage("系统回复：" + content);
            chatMessage.setSender(sender);

            // 发送系统消息给所有用户
            messagingTemplate.convertAndSend("/user/queue/system", chatMessage);
       
    }
  
    @MessageMapping("/chat/{username}")
    public void handlePrivateMessage(@DestinationVariable("username") String username,Principal principal, ChatMessage message) {
    	// 获取当前认证的用户
        String sender = principal.getName();
        String recipient =username;

        // 构建私密消息
        ChatMessage privateMessage = new ChatMessage();
        privateMessage.setSender(sender);
        privateMessage.setRecipient(recipient);
        privateMessage.setMessage(message.getMessage());

        // 发送私密消息给目标用户
        messagingTemplate.convertAndSendToUser(recipient, "/queue/private", privateMessage);
    }
    
    // 从请求中提取 JWT Token 的方法
    private String extractJwtTokenFromRequest(HttpServletRequest request) {
        // 从请求头或参数中提取 JWT Token
        Object jwtToken = request.getSession().getAttribute("jwt-token");
        if (jwtToken != null && jwtToken.toString().startsWith("Bearer ")) {
            return jwtToken.toString().substring(7); // 去掉 "Bearer " 前缀
        }
        return null;
    }
}
```

**创建登录页面（login.html）**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>登录页面</title>
    <!-- 引入 Bootstrap CSS 文件 -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
</head>
<body>
    <div class="container mt-5">
        <div class="row justify-content-center">
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header">用户登录</div>
                    <div class="card-body">
                        <form action="/doLogin" method="post">
                            <div class="form-group">
                                <label for="username">用户名</label>
                                <input type="text" class="form-control" id="username" name="username" placeholder="请输入用户名">
                            </div>
                            <div class="form-group">
                                <label for="password">密码</label>
                                <input type="password" class="form-control" id="password" name="password" placeholder="请输入密码">
                            </div>
                            <button type="submit" class="btn btn-primary">登录</button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
```

**创建Thymeleaf模板(chat.html)：**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>即时通讯</title>
    
    <!-- 引入 Bootstrap CSS 文件 -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap/dist/css/bootstrap.min.css">

    <!-- 自定义样式 -->
    <style>
        /* 添加自定义样式 */
        /* 例如：修改背景颜色、字体样式等 */
    </style>
</head>
<body>
    <div class="container">
        <h1 class="mt-5 mb-4">即时通讯</h1>

        <!-- 聊天窗口 -->
        <div class="row">
            <div class="col-md-8">
                <!-- 显示聊天消息的区域 -->
                <div class="card">
                    <div class="card-body">
                        <div id="chatArea" class="overflow-auto" style="height: 400px;"></div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 消息发送表单 -->
        <div class="mt-4">
            <form id="messageForm">
                <div class="form-group">
                    <label for="recipient">选择用户：</label>
                    <select class="form-control" id="recipient" name="recipient">
                    	 <option value="system">系统消息</option>
                          <option th:each="user : ${users}" th:value="${user.username}" th:text="${user.username}"></option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="message">消息内容：</label>
                    <input type="text" class="form-control" id="message" name="message">
                </div>
                <button type="submit" class="btn btn-primary">发送消息</button>
            </form>
        </div>
    </div>
	
	<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.5.0/dist/sockjs.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>
    <script th:inline="javascript">
        var stompClient = null;
     	// 获取前面注入的currentUsername变量
        var username = [[${currentUsername}]];

     
        function connectWebSocket() {
            var socket = new SockJS('/ws');
            var jwtToken = [[${jwtToken}]];
            var headers = {
            		  "jwt-token": jwtToken
            		};
            stompClient = Stomp.over(socket);

            stompClient.connect(headers, function (frame) {
                console.log('Connected: ' + frame);

                // 构建订阅路径，替换{username}为实际的用户名
                var subscriptionPath = '/user/' + username + '/queue/private';

                // 订阅私有消息队列
                stompClient.subscribe(subscriptionPath, function (message) {
                    // 处理接收到的私有消息
                    console.log("message:", message);
                    var privateMessage = JSON.parse(message.body);
                    displayPrivateMessage(privateMessage);
                });

                // 订阅系统消息队列
                var systemSubscriptionPath = '/user/queue/system';
                stompClient.subscribe(systemSubscriptionPath, function (message) {
                    // 处理接收到的系统消息
                    var systemMessage = JSON.parse(message.body);
                    displaySystemMessage(systemMessage);
                });
            });
        }

        // WebSocket连接
        connectWebSocket();

        // 处理并显示私有消息
        function displayPrivateMessage(privateMessage) {
            // 在用户界面上显示私有消息，可以是聊天窗口或其他UI组件
            var sender = privateMessage.sender;
            var content = privateMessage.message;

            // 在chatArea中追加消息显示
            var messageDiv = document.createElement("div");
            messageDiv.textContent = sender + ": " + content;
            document.getElementById("chatArea").appendChild(messageDiv);
        }

        // 处理并显示系统消息
        function displaySystemMessage(systemMessage) {
            // 在用户界面上显示系统消息，可以是通知、提示或其他UI组件
            var content = systemMessage.message;

            // 在chatArea中追加系统消息显示
            var messageDiv = document.createElement("div");
            messageDiv.textContent = "系统消息: " + content;
            document.getElementById("chatArea").appendChild(messageDiv);
        }

        // 处理消息发送表单的提交
        document.getElementById("messageForm").addEventListener("submit", function (event) {
            event.preventDefault();

            var messageInput = document.getElementById("message");
            var recipientSelect = document.getElementById("recipient");
            var message = messageInput.value;
            var recipient = recipientSelect.value;
			console.log("recipient:", recipient);
			var sendUrl = "/app/chat/" + recipient;
			if( "system" == recipient ){
				sendUrl = "/app/sendSystemMessage" ;
			}

            // 发送消息到服务器
            stompClient.send(sendUrl , {}, JSON.stringify({
                'recipient': recipient,
                'message': message
            }));

           // messageInput.value = "";
        });
    </script>
</body>
</html>
```

启动 Spring Boot 应用后，访问 http://localhost:8080/login，在两个不同的浏览器打开此登陆页，分别使用 admin及test 登陆系统，两个用户密码与用户名相同。登陆成功以后转到 http://localhost:8080/chat 页面，选择用户有系统消息及其他的系统用户，选择相应类型，输入消息内容，发送消息。可以在即时通讯下面看到发送的即时消息。