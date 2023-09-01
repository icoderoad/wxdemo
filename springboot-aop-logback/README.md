使用 Spring Boot + AOP + Logback 用切面来拦截商品操作日志功能实现

在 Spring Boot 中使用切面来拦截操作日志，以及配合使用 MyBatis-Plus 框架进行操作，并使用 Thymeleaf 视图显示商品列表，同时配置 Logback 日志输出到文件，以下是一个示例的代码和详细说明：

**数据库表结构的 DDL 语句10条初始化商品数据**：

```sql
CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    description TEXT
);

INSERT INTO product (name, price, description) VALUES
    ('商品 1', 100.00, '商品描述   1'),
    ('商品 2', 150.00, '商品描述   2'),
    ('商品 3', 200.00, '商品描述   3'),
    ('商品 4', 50.00, '商品描述   4'),
    ('商品 5', 300.00, '商品描述   5'),
    ('商品 6', 120.00, '商品描述   6'),
    ('商品 7', 80.00, '商品描述   7'),
    ('商品 8', 250.00, '商品描述   8'),
    ('商品 9', 180.00, '商品描述   9'),
    ('商品 10', 90.00, '商品描述   10');
```

**`pom.xml` 依赖：**

在你的 `pom.xml` 文件中添加如下依赖，包括 Spring Boot Starter Web、MyBatis-Plus、Thymeleaf、Jackson、Logback 等。如果有其他需要的依赖，也可以根据需要进行添加。

```xml
<dependencies>
    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- MyBatis-Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>最新版本</version>
    </dependency>
    
      <!-- MySQL Driver -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>最新版本</version>
    </dependency>

    <!-- Thymeleaf -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>

    <!-- Jackson for JSON -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>

    <!-- Logback -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
    </dependency>
</dependencies>
```

请将 `<version>最新版本</version>` 替换为所需的版本号。

**`application.properties` 属性配置：**

在 `src/main/resources` 目录下创建 `application.properties` 文件，设置属性配置信息。以下是一个示例：

```properties
# 数据源配置
spring.datasource.url=jdbc:mysql://localhost:3306/your_database_name
spring.datasource.username=your_database_username
spring.datasource.password=your_database_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# MyBatis-Plus 配置
mybatis-plus.mapper-locations=classpath:mapper/*.xml
mybatis-plus.global-config.id-type=auto

# Thymeleaf 配置
spring.thymeleaf.mode=HTML
spring.thymeleaf.cache=false

# 日志配置
logging.level.root=INFO
logging.level.com.icoderoad.example=DEBUG
logging.file=logs/application.log
logging.pattern.console=%msg%n
```

请将上述配置根据实际情况进行修改，特别是数据库配置、日志文件路径等信息。

**创建商品实体类 `Product`：**

```java
package com.icoderoad.example.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private double price;
    private String description;
}
```
**创建商品Mapper `ProductMapper` ：**

```java
package com.icoderoad.example.product.mapper
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icoderoad.example.model.Product;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductMapper extends BaseMapper<Product> {
}
```

**创建商品Service `ProductService` 和其实现类 `ProductServiceImpl`：**

```java
package com.icoderoad.example.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.icoderoad.example.product.entity.Product;

public interface ProductService extends IService<Product> {
}

package com.icoderoad.example.product.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icoderoad.example.mapper.ProductMapper;
import com.icoderoad.example.model.Product;
import com.icoderoad.example.service.ProductService;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
}
```

** 创建商品Controller `ProductController`：**

```java
package com.icoderoad.example.product.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.icoderoad.example.product.service.ProductService;

@Controller
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productService.list());
        return "product/list";
    }
}
```

** 日志配置信息：**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration>
<configuration>
	<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{yyyy-mm-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>logs/application.log</file>
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
		      	<pattern>%d{yyyy-mm-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
    	 <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE" />
    </root>
</configuration>

```

** 创建 WebLog 类：**

```java
package com.icoderoad.example.product.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Controller层的日志封装类
 */
@Data
@EqualsAndHashCode
public class WebLog {
    /**
     * 操作描述
     */
    private String description;

    /**
     * 操作用户
     */
    private String username;

    /**
     * 操作时间
     */
    private Long startTime;

    /**
     * 消耗时间
     */
    private Integer spendTime;

    /**
     * 根路径
     */
    private String basePath;

    /**
     * URI
     */
    private String uri;

    /**
     * URL
     */
    private String url;

    /**
     * 请求类型
     */
    private String method;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 请求参数
     */
    private Object parameter;

    /**
     * 返回结果
     */
    private Object result;

}

```

** 创建切面类：**
然后，创建切面类来拦截操作日志并将其转化为 JSON 格式，输出转换后的 JSON 数据。

```java
package com.icoderoad.example.product.aspect;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icoderoad.example.product.entity.WebLog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Aspect
@Component
public class LogAspect {
	 private static final Logger LOGGER = LoggerFactory.getLogger(LogAspect.class);
    private final ObjectMapper objectMapper;

    @Autowired
    public LogAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // 切入点定义：拦截所有Controller的方法
    @Pointcut("execution(* com.icoderoad.example.product.controller.*.*(..))")
    public void webLog() {}

    // 在方法返回后执行，记录日志
    @AfterReturning(returning = "result", pointcut = "webLog()")
    public void doAfterReturning(JoinPoint joinPoint, Object result) throws Throwable {
        // 获取当前请求的属性
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 创建 WebLog 对象，并填充相关信息
        WebLog webLog = new WebLog();
        webLog.setStartTime(System.currentTimeMillis());
        webLog.setBasePath(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort());
        webLog.setUri(request.getRequestURI());
        webLog.setUrl(request.getRequestURL().toString());
        webLog.setMethod(request.getMethod());
        webLog.setIp(getClientIp(request));  // 获取客户端真实 IP 地址
        webLog.setParameter(Arrays.toString(joinPoint.getArgs()));
        webLog.setResult(result);

        // 将 WebLog 对象转换为 JSON 格式，并输出到控制台（实际应该输出到日志文件）
        String logJson = objectMapper.writeValueAsString(webLog);
        LOGGER.info(logJson);
    }

    // 获取客户端真实 IP 地址
    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }
}
```

** 项目启动类：**

```java
package com.icoderoad.example.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.icoderoad.example.product.mapper")
public class AopLogbackProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(AopLogbackProductApplication.class, args);
	}

}
```

** 创建Thymeleaf视图 `list.html`：**

位于 `src/main/resources/templates/product/list.html`。

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
     <meta charset="UTF-8">
    <title>商品列表</title>
    <!-- 引入 Bootstrap 的 CSS 文件 -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
	<div  class="container">
	    <h1>商品列表</h1>
	    <table class="table table-striped">
	        <thead>
	            <tr>
	                <th>ID</th>
	                <th>名称</th>
	                <th>价格</th>
	                <th>描述</th>
	            </tr>
	        </thead>
	        <tbody>
	            <tr th:each="product : ${products}">
	                <td th:text="${product.id}"></td>
	                <td th:text="${product.name}"></td>
	                <td th:text="${product.price}"></td>
	                <td th:text="${product.description}"></td>
	            </tr>
	        </tbody>
	    </table>
	 </div>
</body>
</html>
```

启动 Spring Boot 应用后，访问 http://localhost:8080/products，显示商品列表，在指定日志文件中查看日志输出格式。