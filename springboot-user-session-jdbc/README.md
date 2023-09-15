Spring Boot中使用JDBC类型的会话存储实现用户登陆功能

在Spring Boot中，`spring.session.store-type`属性用于配置会话（session）的存储方式。会话存储方式决定了如何管理和存储用户会话的信息，例如用户登录状态、会话数据等。以下是可能的`spring.session.store-type`属性的值以及它们的用法说明：

1. **none**（默认值）：
   - 这个选项表示不使用持久化存储会话信息，会话数据将仅保存在内存中。
   - 这是最简单和最轻量级的选项，但会话数据在应用重新启动时会丢失。
2. **jdbc**：
   - 使用JDBC（Java Database Connectivity）来将会话数据存储在关系型数据库中。
   - 您需要配置数据源和相应的数据库表来存储会话信息。Spring Boot提供了默认的表结构，您可以选择使用或自定义。
   - 这种方式适合需要持久性和跨应用服务器的会话存储需求。
3. **redis**：
   - 使用Redis作为会话存储后端。
   - 您需要配置Redis连接信息，并可以选择配置一些其他属性，如超时策略等。
   - 这种方式适合需要跨多个应用实例共享会话数据的情况，也适用于具有高可用性和分布式特性的场景。
4. **mongodb**：
   - 使用MongoDB作为会话存储后端。
   - 您需要配置MongoDB连接信息。
   - 这种方式适用于需要在文档数据库中存储会话数据的情况。

现在我们实现 JDBC 类型的会话存储，在Spring Boot中使用JDBC类型的会话存储，需要配置一个数据源（DataSource）以及相应的数据库表来存储会话信息。以下是一个完整的示例，包括用户表的DDL定义、用户登录验证、Thymeleaf视图等。

用户表的DDL定义，可以在MySQL数据库中运行：

```sql
CREATE TABLE session_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
);
```

`pom.xml`依赖

```xml
			  <!-- Spring Boot Web Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
				<dependency>
          <groupId>org.projectlombok</groupId>
          <artifactId>lombok</artifactId>
          <optional>true</optional>
        </dependency>
        <!-- Spring Boot Thymeleaf Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
			  <!-- Spring Security Crypto -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>
				<!-- Spring Session JDBC -->
        <dependency>
            <groupId>org.springframework.session</groupId>
            <artifactId>spring-session-jdbc</artifactId>
        </dependency>

        <!-- Spring Boot JDBC Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <!-- MySQL JDBC Driver -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.33</version>
        </dependency>
```

在`application.properties`中配置数据库连接信息

```properties
# Spring Boot应用配置
spring.application.name=session-demo
server.port=8080

# 数据库连接配置
spring.datasource.url=jdbc:mysql://localhost:3306/your_database_name
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# 使用mysql数据库初始化会话表
spring.session.store-type=jdbc
spring.session.jdbc.initialize-schema=always

# Spring Boot Thymeleaf配置
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false
```

实体类 `User.java`：

```java
package com.icoderoad.example.user.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class User implements Serializable {
    private String username;
    private String password;

    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
```

服务类 `UserService.java`：

```java
package com.icoderoad.example.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.icoderoad.example.user.entity.User;

@Service
public class UserService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    // 用户验证方法，检查用户名和密码是否匹配数据库中的记录
    public boolean authenticateUser(String username, String password) {
        // 查询数据库中的用户信息
        String sql = "SELECT username, password FROM users WHERE username = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, new Object[]{username}, (rs, rowNum) ->
                    new User(rs.getString("username"), rs.getString("password")));
            if (user != null && passwordEncoder.matches(password, user.getPassword())) {
                return true; // 用户验证成功
            }
        } catch (Exception e) {
            // 用户不存在或发生其他异常
        }
        return false; // 用户验证失败
    }
}
```

控制类 `UserController.java`：

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@SessionAttributes("loggedInUser")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model) {
        // 使用UserService进行用户验证
        if (userService.authenticateUser(username, password)) {
            User user = new User(username, null); // 不暴露密码到会话
            model.addAttribute("loggedInUser", user);
            return "redirect:/dashboard";
        } else {
            return "login";
        }
    }

    @GetMapping("/dashboard")
    public String showDashboard(@SessionAttribute("loggedInUser") User loggedInUser, Model model) {
        model.addAttribute("username", loggedInUser.getUsername());
        return "dashboard";
    }
}
```

创建一个类来执行初始化操作。这个类将实现 `CommandLineRunner` 接口，并在 `run` 方法中执行初始化操作

```java
package com.icoderoad.example.user.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitUser implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public InitUser(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 在此处创建初始用户
        String username = "admin";
        String password = "admin";
        String encodedPassword = passwordEncoder.encode(password);

        // 检查数据库中是否已存在用户，如果不存在，则插入初始用户
        String sql = "SELECT COUNT(*) FROM session_users WHERE username = ?";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, username);

        if (count == 0) {
            jdbcTemplate.update("INSERT INTO session_users (username, password) VALUES (?, ?)", username, encodedPassword);
        }
    }
}
```

Spring Security 配置

```java
package com.icoderoad.example.user.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
```

定义了一个名为 `passwordEncoder` 的 `PasswordEncoder` bean，使用了 BCrypt 算法

在目录`src/main/resources/templates/`（登录视图）login.html：

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>用户登录</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
</head>
<body>
<div class="container">
    <h2>用户登录</h2>
    <form action="/login" method="post">
        <div class="form-group">
            <label for="username">用户名：</label>
            <input type="text" class="form-control" id="username" name="username" required>
        </div>
        <div class="form-group">
            <label for="password">密码：</label>
            <input type="password" class="form-control" id="password" name="password" required>
        </div>
        <button type="submit" class="btn btn-primary">登录</button>
    </form>
</div>
</body>
</html>
```

在目录`src/main/resources/templates/`（仪表盘视图）dashboard.html：

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>仪表盘</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
</head>
<body>
<div class="container">
    <h2>欢迎, <span th:text="${username}"></span>!</h2>
    <a href="/logout">退出登录</a>
</div>
</body>
</html>
```

启动 Spring Boot 应用后，访问 http://localhost:8080/login ，显示登陆页面，输入用户名密码，用户名密码均输入 admin ,点击登陆，验证成功后转向仪表盘页面  http://localhost:8080/dashboard  ，可以看到登陆的用户为 admin 。查看数据库中 SPRING_SESSION 表，是否已经存在 session 数据。