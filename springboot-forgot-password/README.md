通过 SpringBoot 、Mybatis-plus、Thymeleaf、JavaMailSender实现忘记密码功能

实现忘记密码功能涉及多个步骤，包括生成重置密码 Token、发送邮件、验证 Token、更新密码等。以下是一个简化的代码示例，展示了如何使用SpringBoot、MyBatis-Plus、Thymeleaf 等技术来实现忘记密码功能。

数据库表 - 用户表

```sql
CREATE TABLE `fpassword_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_name` varchar(50) NOT NULL,
  `password` varchar(80) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_name` (`user_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1684474548507967490 DEFAULT CHARSET=utf8mb4;

INSERT INTO `fpassword_user` (`id`, `user_name`, `password`, `name`, `phone`, `email`, `create_time`)
VALUES
    (1000, 'admin', '$2a$10$sjHpHNI1Hvbak99aFkBxxeNElhFjHGUs/AMXP0P2kEp3zr6C97Coe', '超管', '19876543210', 'admin@icoderoad.com', '2023-08-25 09:52:03');
```

数据库表 - 忘记密码 Token 存储

```sql
CREATE TABLE `password_reset_token` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `token` varchar(255) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `expiry_date` timestamp NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_token` (`token`),
  KEY `fk_user_id` (`user_id`),
  CONSTRAINT `fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `fpassword_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

maven pom.xml 文件


```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	 <parent>
        <groupId>com.icoderoad.example</groupId>
        <artifactId>demo</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
	<artifactId>springboot-forgot-password</artifactId>
	<version>1.0.0-SNAPSHOT</version>
	<packaging>jar</packaging>
	<name>ForgotPassword</name>
	<description>Demo ForgotPassword project for Spring Boot</description>
	<properties>
		<java.version>1.8</java.version>
	</properties>
	<dependencies>
	
		 <dependency>
	        <groupId>org.springframework.boot</groupId>
	        <artifactId>spring-boot-starter-thymeleaf</artifactId>
	    </dependency>
	    
	     <!-- MyBatis-Plus -->
	    <dependency>
	        <groupId>com.baomidou</groupId>
	        <artifactId>mybatis-plus-boot-starter</artifactId>
	        <version>${mybatis-plus-boot-starter.version}</version>
	    </dependency>
	
	    <!-- JavaMailSender -->
	    <dependency>
	        <groupId>org.springframework.boot</groupId>
	        <artifactId>spring-boot-starter-mail</artifactId>
	    </dependency>
	    
	    <dependency>
	        <groupId>javax.mail</groupId>
	        <artifactId>javax.mail-api</artifactId>
	    </dependency>
	    
	    <dependency>
	        <groupId>com.sun.mail</groupId>
	        <artifactId>javax.mail</artifactId>
	        <version>${javax.mail.version}</version>
	    </dependency>
	
	    <!-- MySQL Driver -->
	    <dependency>
	        <groupId>mysql</groupId>
	        <artifactId>mysql-connector-java</artifactId>
	        <version>${mysql-connector-java.version}</version>
	    </dependency>
    
    <dependency>
		    <groupId>org.springframework.security</groupId>
		    <artifactId>spring-security-crypto</artifactId>
		</dependency>

	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

</project>
```
邮件配置属性文件 - application.properties

```properties
#邮件配置
spring.mail.host=smtp.qq.com
spring.mail.port=587
spring.mail.username=67642742@qq.com
spring.mail.password=xxx
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true

#项目配置
app.email.from=67642742@qq.com
app.email.reset-link=http://localhost:8080/forgot-password/reset-password?token=

spring.datasource.url=jdbc:mysql://127.0.0.1:3306/wxdemo
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

密码配置类 - PasswordConfig.java

```java
package com.icoderoad.example.forgotpassword.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
```

实体类 - FPUser.java

```java
package com.icoderoad.example.forgotpassword.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("fpassword_user")
public class FPUser {

    private Long id;
    private String userName;
    private String password;
    private String name;
    private String phone;
    private String email;
    private LocalDateTime createTime;

}
```

实体类 - PasswordResetToken.java

```java
package com.icoderoad.example.forgotpassword.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("password_reset_token")
public class PasswordResetToken {
    
    private Long id;
    private String token;
    private Long userId;
    private LocalDateTime expiryDate;
  
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

}
```


Mapper接口 - FPUserMapper.java

```java
package com.icoderoad.example.forgotpassword.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icoderoad.example.forgotpassword.entity.FPUser;

public interface FPUserMapper extends BaseMapper<FPUser> {
    
}
```

Mapper接口 - PasswordResetTokenMapper.java

```java
package com.icoderoad.example.forgotpassword.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icoderoad.example.forgotpassword.entity.PasswordResetToken;

public interface PasswordResetTokenMapper extends BaseMapper<PasswordResetToken> {

}
```

Service类 - FPUserService.java

```java
package com.icoderoad.example.forgotpassword.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icoderoad.example.forgotpassword.entity.FPUser;
import com.icoderoad.example.forgotpassword.mapper.FPUserMapper;

@Service
public class FPUserService extends ServiceImpl<FPUserMapper, FPUser> {
    private final FPUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public FPUserService(FPUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }
    
    public FPUser findByUserName(String userName) {
          // 根据用户名查询用户
        QueryWrapper<FPUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        return userMapper.selectOne(queryWrapper);
    }
    
    public FPUser findByEmail(String email) {
        // 根据用户名查询用户
        QueryWrapper<FPUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        return userMapper.selectOne(queryWrapper);
    }
    
    public void updateUserPassword(Long userId, String newPassword) {
        FPUser user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userMapper.updateById(user);
        }
    }

}
```

Service类 - PasswordResetTokenService.java

```java
package com.icoderoad.example.forgotpassword.service;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icoderoad.example.forgotpassword.entity.PasswordResetToken;
import com.icoderoad.example.forgotpassword.mapper.PasswordResetTokenMapper;

@Service
public class PasswordResetTokenService extends ServiceImpl<PasswordResetTokenMapper, PasswordResetToken> {

    public void createPasswordResetToken(Long userId, String token, LocalDateTime expiryDate) {
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setUserId(userId);
        passwordResetToken.setToken(token);
        passwordResetToken.setExpiryDate(expiryDate);
        this.save(passwordResetToken);
    }
    
    public PasswordResetToken findByToken(String token) {
        QueryWrapper<PasswordResetToken> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("token", token);
        return this.getOne(queryWrapper);
    }
    
    public void deleteToken(Long tokenId) {
        this.removeById(tokenId);
    }

}
```


邮件发送服务 - EmailService.java

```java
package com.icoderoad.example.forgotpassword.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final Environment environment;

    @Autowired
    public EmailService(JavaMailSender mailSender, Environment environment) {
        this.mailSender = mailSender;
        this.environment = environment;
    }
    
    public void sendPasswordResetEmail(String recipientEmail, String resetLink) {
        String fromEmail = environment.getProperty("app.email.from");
    
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(recipientEmail);
        message.setSubject("重置密码链接");
        message.setText("请点击下面的链接重置您的密码：" + resetLink);
    
        mailSender.send(message);
    }

}
```

项目启动类

```java
package com.icoderoad.example.forgotpassword;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.icoderoad.example.forgotpassword.mapper")
public class ForgotPasswordApplication {

	public static void main(String[] args) {
		SpringApplication.run(ForgotPasswordApplication.class, args);
	}

}
```

注意：一定增加 @MapperScan 注解，不然项目启动时会报错。

Controller - PasswordResetController.java

```java
package com.icoderoad.example.forgotpassword.controller;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.icoderoad.example.forgotpassword.entity.FPUser;
import com.icoderoad.example.forgotpassword.entity.PasswordResetToken;
import com.icoderoad.example.forgotpassword.service.EmailService;
import com.icoderoad.example.forgotpassword.service.FPUserService;
import com.icoderoad.example.forgotpassword.service.PasswordResetTokenService;

@Controller
@RequestMapping("/forgot-password")
public class PasswordResetController {
    

    @Autowired
    private  FPUserService userService;
    
    @Autowired
    private  PasswordResetTokenService tokenService;
    
    @Autowired
    private  EmailService emailService;
    
    @Autowired
    private  PasswordEncoder passwordEncoder;
    
    @Value("${app.email.reset-link}")
    private String resetLink;


    @GetMapping
    public String showForgotPasswordForm() {
        return "forgot-pwd/forgot_password_form";
    }
    
    private static final int TOKEN_LENGTH = 32;
    
    @PostMapping
    public String processForgotPassword(@RequestParam("email") String email) {
        FPUser user = userService.findByEmail(email);
        if (user != null) {
            String token = generateToken();
            LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);
            tokenService.createPasswordResetToken(user.getId(), token, expiryDate);
    
            String resetUrl = resetLink + token;
    
            emailService.sendPasswordResetEmail(email, resetUrl);
        }
        return "redirect:/forgot-password?success";
    }
    
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token) {
        PasswordResetToken passwordResetToken = tokenService.findByToken(token);
        if (passwordResetToken != null && !passwordResetToken.isExpired()) {
            return "forgot-pwd/reset_password_form";
        } else {
            return "redirect:/forgot-password?error";
        }
    }
    
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("password") String password) {
        PasswordResetToken passwordResetToken = tokenService.findByToken(token);
        if (passwordResetToken != null && !passwordResetToken.isExpired()) {
            FPUser user = userService.getById(passwordResetToken.getUserId());
            user.setPassword(passwordEncoder.encode(password));
            userService.updateById(user);
            tokenService.deleteToken(passwordResetToken.getId());
            return "forgot-pwd/reset_password_succ";
        } else {
            return "redirect:/forgot-password?error";
        }
    }
    
    public  String generateToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

}
```

Thymeleaf模板视图

Thymeleaf模板的存放目录为 Spring Boot项目的src/main/resources/templates/forgot-pwd/目录下

forgot_password_form.html - 忘记密码页面模板


```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>忘记密码</title>
</head>
<body>
    <h1>忘记密码</h1>
    <form th:action="@{/forgot-password}" method="post">
        <label for="email">请输入您的注册邮箱：</label>
        <input type="email" id="email" name="email" required>
        <button type="submit">提交</button>
    </form>

    <p th:if="${param.success}">重置密码链接已发送至您的邮箱。</p>
    <p th:if="${param.error}">找不到与该邮箱相关的用户。</p>
 </body>
</html>
```
reset_password_form.html - 重置密码页面模板


```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>重置密码</title>
</head>
<body>
    <h1>重置密码</h1>
    <form th:action="@{/forgot-password/reset-password}" method="post">
        <input type="hidden" name="token" th:value="${param.token}">

        <label for="password">请输入新密码：</label>
        <input type="password" id="password" name="password" required>

        <button type="submit">提交</button>
    </form>

	<p th:if="${param.error}">重置链接无效或已过期。</p>
</body>
</html>
```
reset_password_succ.html - 重置密码成功页模板

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>重置密码</title>
</head>
<body>
    <h1>重置密码成功，请使用新密码进行登陆</h1>
  </body>
</html>
```

确保这些HTML模板文件与 Spring Boot 项目的Thymeleaf视图解析器配置相匹配，存放在src/main/resources/templates/forgot-pwd目录下。

现在，启动 Spring Boot 应用后，访问 http://localhost:8080/forgot-password 将忘记页，输入用户密码用户注册邮箱，点击 提交按钮，发送忘记密码邮件。在邮件中点击连接显示重置密码页面，输入新密码，点击提交，更新用户密码为新密码。

