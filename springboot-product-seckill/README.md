SpringBoot + Redis +JPA + Mysql 实现大并发商品秒杀功能

实现商品秒杀功能涉及多个技术栈组件，下面将为大家提供一些核心的代码示例和注释，包括MySQL表DDL语句、初始化商品SQL、Java代码逻辑和Thymeleaf + Bootstrap视图的代码逻辑。

**创建MySQL表**

我们需要创建一个表来存储商品信息和秒杀活动信息。

```sql
CREATE TABLE IF NOT EXISTS seckill_products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS seckill_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    order_time DATETIME NOT NULL,
    UNIQUE KEY (user_id, product_id)
);
```

初始化语句

```sql
-- 商品初始化 SQL 语句
INSERT INTO seckill_products (name, price, stock, start_time, end_time, status) VALUES
    ('商品1', 50.00, 100, '2023-09-06 08:00:00', '2023-09-06 10:00:00', 1),
    ('商品2', 30.00, 200, '2023-09-06 08:00:00', '2023-09-06 10:00:00', 1),
    ('商品3', 20.00, 150, '2023-09-06 08:00:00', '2023-09-06 10:00:00', 1),
    ('商品4', 100.00, 50, '2023-09-06 08:00:00', '2023-09-06 10:00:00', 1),
    ('商品5', 10.00, 300, '2023-09-06 08:00:00', '2023-09-06 10:00:00', 1),
    ('商品6', 60.00, 120, '2023-09-06 08:00:00', '2023-09-06 10:00:00', 1),
    ('商品7', 40.00, 80, '2023-09-06 08:00:00', '2023-09-06 10:00:00', 1),
    ('商品8', 25.00, 250, '2023-09-06 08:00:00', '2023-09-06 10:00:00', 1),
    ('商品9', 15.00, 180, '2023-09-06 08:00:00', '2023-09-06 10:00:00', 1),
    ('商品10', 70.00, 90, '2023-09-06 08:00:00', '2023-09-06 10:00:00', 1);
```

`pom.xml`文件依赖

```xml
	<!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Starter Data JPA (for MySQL integration) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- MySQL Connector -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
    </dependency>

    <!-- Spring Boot Starter Redis (for Redis integration) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

		<dependency>
	        <groupId>redis.clients</groupId>
	        <artifactId>jedis</artifactId>
	    </dependency>
	    
    <!-- RabbitMQ Client -->
    <dependency>
        <groupId>com.rabbitmq</groupId>
        <artifactId>amqp-client</artifactId>
    </dependency>

    <!-- Spring Boot Starter Thymeleaf (for Thymeleaf and Bootstrap integration) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
```

`application.properties`文件配置

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Hibernate配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Redis配置
spring.redis.host=localhost
spring.redis.port=6379

# RabbitMQ配置
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=your_username
spring.rabbitmq.password=your_password

# Spring Boot Thymeleaf配置
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false

# 日志级别配置
logging.level.root=INFO
```

 商品实体类

```java
package com.icoderoad.example.seckill.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name="seckill_products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
}
```

 订单实体类

```java
package com.icoderoad.example.seckill.entity;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name="seckill_orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long productId;
    private LocalDateTime orderTime;
}
```

ProductRepository 类

```java
package com.icoderoad.example.seckill.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.icoderoad.example.seckill.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // 查询秒杀商品列表
    List<Product> findByStartTimeBeforeAndEndTimeAfterAndStatus(LocalDateTime startTime, LocalDateTime endTime, int status);
    
    // 根据商品ID获取商品信息
    Product findByIdAndStatus(Long id, int status);
  
}
```

这个 `ProductRepository` 接口扩展了 `JpaRepository`，它提供了一些常见的CRUD（创建、读取、更新、删除）操作。除此之外，添加了两个自定义查询方法：

1. `findByStartTimeBeforeAndEndTimeAfterAndStatus`: 用于查询秒杀活动期间的商品列表。
2. `findByIdAndStatus`: 用于根据商品ID获取商品信息。

OrderRepository 类

```java
package com.icoderoad.example.seckill.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.icoderoad.example.seckill.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 根据用户ID和商品ID查询订单
    Order findByUserIdAndProductId(Long userId, Long productId);
    
    // 查询某个用户的所有订单
    List<Order> findByUserId(Long userId);
    
    // 查询某个商品的所有订单
    List<Order> findByProductId(Long productId);
}
```

这个 `OrderRepository` 接口扩展了 `JpaRepository`，它提供了一些常见的CRUD（创建、读取、更新、删除）操作。除此之外，添加了一些自定义查询方法，用于根据不同的条件查询订单信息，包括根据用户ID、商品ID等。

商品Service

```java
package com.icoderoad.example.seckill.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.icoderoad.example.seckill.entity.Product;
import com.icoderoad.example.seckill.repository.ProductRepository;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;
    
    // 查询秒杀商品列表
    public List<Product> listSecKillProducts() {
        LocalDateTime now = LocalDateTime.now();
        return productRepository.findByStartTimeBeforeAndEndTimeAfterAndStatus(now, now, 1);
    }
    
}
```

订单Service

```java
package com.icoderoad.example.seckill.service;

import java.time.LocalDateTime;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.icoderoad.example.seckill.entity.Order;
import com.icoderoad.example.seckill.entity.Product;
import com.icoderoad.example.seckill.repository.OrderRepository;
import com.icoderoad.example.seckill.repository.ProductRepository;

@Service
public class OrderService {
	
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    // 创建订单
    @Transactional
    public Order createOrder(Long userId, Long productId) {
    	Order order = orderRepository.findByUserIdAndProductId(userId, productId);
    	//检查用户是否秒杀过此商品
    	if( order!=null ) {
    		return  null;
    	}
        // 检查库存是否足够
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || product.getStock() <= 0) {
            return null;
        }
        
        // 扣减库存
        product.setStock(product.getStock() - 1);
        productRepository.save(product);
        
        // 创建订单
        order = new Order();
        order.setUserId(userId);
        order.setProductId(productId);
        order.setOrderTime(LocalDateTime.now());
        return orderRepository.save(order);
    }
    
}
```

集成Redis

```java
package com.icoderoad.example.seckill.conf;

import java.time.Duration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableCaching
public class RedisConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(60)))
                .transactionAware()
                .build();
    }
}
```

在高并发场景中，将秒杀商品信息存储在Redis缓存中是一种有效的方式，以减轻数据库的负载并提高响应速度。以下是核心秒杀算法逻辑，包括将秒杀商品信息存储到Redis中：

SeckillService

```java
package com.icoderoad.example.seckill.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.icoderoad.example.seckill.entity.Order;
import com.icoderoad.example.seckill.entity.Product;

@Service
public class SeckillService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    // 初始化秒杀商品信息到Redis缓存
    @PostConstruct
    public void initSeckillProductCache() {
        List<Product> seckillProducts = productService.listSecKillProducts();
        for (Product product : seckillProducts) {
            String key = "seckill_product:" + product.getId();
            redisTemplate.opsForValue().set(key, String.valueOf(product.getStock()));
        }
    }

    // 秒杀商品
    public boolean seckillProduct(Long userId, Long productId) {
        String key = "seckill_product:" + productId;
        String stockStr = redisTemplate.opsForValue().get(key);
        
        if (stockStr != null) {
            int stock = Integer.parseInt(stockStr);
            if (stock > 0) {
                // 在Redis中原子减少库存
                Long updatedStock = redisTemplate.opsForValue().decrement(key);
                if (updatedStock >= 0) {
                    // 创建订单
                    Order order = orderService.createOrder(userId, productId);
                    if (order != null) {
                        return true;
                    } else {
                        // 订单创建失败，恢复Redis中的库存
                        redisTemplate.opsForValue().increment(key);
                    }
                }
            }
        }

        return false;
    }
}
```



**SeckillController 类，用于处理商品秒杀的请求和页面渲染**

```java
package com.icoderoad.example.seckill.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.icoderoad.example.seckill.entity.Product;
import com.icoderoad.example.seckill.service.ProductService;
import com.icoderoad.example.seckill.service.SeckillService;

@Controller
@RequestMapping("/seckill")
public class SeckillController {
    @Autowired
    private SeckillService seckillService;
    
    @Autowired
    private ProductService productService;
    
    @GetMapping("/list")
    public String list(Model model) {
        // 查询秒杀商品列表
        List<Product> products = productService.listSecKillProducts();
        model.addAttribute("products", products);
        return "seckill/list";
    }
    
    @PostMapping("/{productId}")
    @ResponseBody
    public ResponseEntity<String> seckill(@PathVariable Long productId) {
        // 模拟用户ID，实际中应从认证/登录信息中获取
        Long userId = 12345L;
        
        if (seckillService.seckillProduct(userId, productId)) {
            return ResponseEntity.ok("秒杀成功！");
        } else {
            return ResponseEntity.ok("秒杀失败，秒杀过此商品或商品已售罄。");
        }
    }
}
```

**Thymeleaf + Bootstrap视图**

创建Thymeleaf模板和Bootstrap样式的前端视图，以允许用户浏览商品并进行秒杀操作。这里只提供简单示例，你可以根据实际需求进行扩展。

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>秒杀商品列表</title>
    <!-- 引入Bootstrap样式 -->
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css">
    <!-- 引入jQuery库 -->
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
</head>
<body>
    <div class="container">
        <h1>秒杀商品列表</h1>
        <table class="table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>名称</th>
                    <th>价格</th>
                    <th>库存</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="product : ${products}">
                    <td th:text="${product.id}"></td>
                    <td th:text="${product.name}"></td>
                    <td th:text="${product.price}"></td>
                    <td th:text="${product.stock}"></td>
                    <td>
                        <button class="btn btn-primary seckill-button" th:attr="data-product-id=${product.getId()}" >秒杀</button>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <!-- 添加JavaScript代码 -->
    <script type="text/javascript">
        $(document).ready(function () {
            // 点击秒杀按钮触发事件
            $(".seckill-button").click(function () {
                var productId = $(this).data("product-id");
                var button = $(this); // 保存按钮的引用，以便在回调函数中禁用按钮
                // 向后端发送秒杀请求
                $.post("/seckill/" + productId, function (data) {
                    if (data === "秒杀成功！") {
                        alert("秒杀成功！");
                        // 秒杀成功后，禁用按钮
                        button.prop("disabled", true);
                        // 刷新商品列表
                        refreshProductList();
                    } else {
                        alert("秒杀失败，请稍后重试或商品已售罄。");
                    }
                });
            });
        	 // 刷新商品列表的函数
            function refreshProductList() {
                $.get("/seckill/list", function (html) {
                    // 用获取到的新商品列表HTML替换原来的列表
                    $(".container").html(html);
                });
            }
        });
    </script>
</body>
</html>
```

以上是一个简单的示例，实际的秒杀系统需要更复杂的逻辑和安全措施，例如防止恶意请求、限流、验证码等。