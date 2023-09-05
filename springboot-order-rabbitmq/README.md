使用Springboot+JPA+RabbitMQ让订单指定时间后失效功能

实现订单功能涉及多个组件和模块，包括MySQL数据库、Spring Boot、RabbitMQ以及前端Bootstrap页面。下面我将分步骤为您提供相关代码和注释。

Cenos7下Docker Compose的安装

```sh
sudo curl -L "https://github.com/docker/compose/releases/download/1.23.1/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
```

编写docker-compose.yml

```yaml
version: '3'
services:
  rabbitmq:
    image: rabbitmq:3.8.3-management
    container_name: rabbitmq
    restart: always
    hostname: devRabbitmq
    ports:
      - 15672:15672
      - 5672:5672
    volumes:
      - ./data:/var/lib/rabbitmq
    environment:
      - RABBITMQ_DEFAULT_USER=root
      - RABBITMQ_DEFAULT_PASS=123456
```

运行容器

```sh
docker-compose up -d
```

**创建订单数据库表**

创建一个数据库表来存储订单信息：

```sql
CREATE TABLE orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(255) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_expired BOOLEAN DEFAULT FALSE
);
```

这个表包含了订单的基本信息，包括订单号、总金额、订单日期和一个标志字段用于表示订单是否过期。

**添加 pom.xml 依赖**

创建一个Spring Boot项目，添加以下依赖项：Spring Web、Spring Data JPA、Mysql、RabbitMQ。

```xml
 			<!-- Spring Boot核心依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data JPA依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- MySQL数据库驱动依赖 -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
        </dependency>

        <!-- RabbitMQ依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <!-- Thymeleaf模板引擎依赖（可选，如果您要使用Thymeleaf模板） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
```

**配置 `application.properties`** 

在`application.properties`文件中配置数据库和RabbitMQ连接信息：

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password

# RabbitMQ配置
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=your_rabbitmq_username
spring.rabbitmq.password=your_rabbitmq_password
```

** 创建订单实体**

创建一个Java实体类来映射订单数据表。这里使用Spring Data JPA来简化数据库操作。

```java
package com.icoderoad.example.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name="orders")
@Data
public class Order {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderNumber;
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;
    
    @Column(name = "is_expired")
    private boolean expired;

}
```

**创建订单仓库**

创建一个订单仓库接口，继承自Spring Data JPA的`JpaRepository`。

```java
package com.icoderoad.example.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.icoderoad.example.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
```

**创建 RabbitMQ 配置类**

```java
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
```

在上述代码中，我们创建了一个名为`order-exchange`的Fanout交换机，并定义了1个队列`order-expiration-queue`。然后，我们使用`Binding`将这个队列绑定到交换机上，这样交换机会将消息广播给绑定的队列。

**创建RabbitMQ消息发送者**

创建一个消息发送者，用于在订单指定时间后将订单标记为过期。

```java
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
```

** 创建RabbitMQ消息监听器**

创建一个消息监听器，用于接收订单过期的消息并更新订单状态。

```java
package com.icoderoad.example.order.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.icoderoad.example.order.entity.Order;
import com.icoderoad.example.order.repository.OrderRepository;

@Component
public class OrderExpirationListener {
    @Autowired
    private OrderRepository orderRepository;

    @RabbitListener(queues = "order-expiration-queue")
    public void handleExpiredOrder(Long orderId) {
        // 根据orderId更新订单状态为过期
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setExpired(true);
            orderRepository.save(order);
        }
    }
}
```

** 创建Spring Boot Controller**

创建一个Spring Boot Controller来处理页面请求和订单操作。

```java
package com.icoderoad.example.order.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.icoderoad.example.order.entity.Order;
import com.icoderoad.example.order.repository.OrderRepository;
import com.icoderoad.example.order.service.OrderSenderService;

@Controller
public class OrderController {
	
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private OrderSenderService orderSenderService;

	@GetMapping("/")
	public String index(Model model) {
		List<Order> orders = orderRepository.findAll();
		model.addAttribute("orders", orders);
		return "/orders/order-list";
	}

	@GetMapping("/createOrderPage")
	public String createOrderPage() {
		return "create-order"; // 返回创建订单的页面
	}

	@PostMapping("/createOrder")
	public String createOrder(Order order) {
		// 创建订单并保存到数据库
		orderRepository.save(order);

		// 发送订单过期消息，设置延迟时间（示例设置为5分钟）
		orderSenderService.sendOrderExpirationMessage(order.getId(), 5 * 60 * 1000);

		return "redirect:/";
	}
}
```

这个Controller包含了订单列表展示和创建订单的功能。

**创建Bootstrap前端页面**

在src/main/resources/templates/orders/目录下创建一个名为`order-list.html`的HTML文件，用于展示订单列表和操作按钮,使用Bootstrap来美化页面。

```html
<!DOCTYPE html>
<html>
<head>
    <title>订单管理</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
</head>
<body>
    <div class="container mt-5">
        <h2>订单列表</h2>
      	<div class="container mt-3">
          <a href="/createOrderPage" class="btn btn-success">创建新订单</a>
      	</div>
        <table class="table table-bordered">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>订单号</th>
                    <th>总金额</th>
                    <th>下单日期</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <!-- 使用Thymeleaf语法动态填充订单数据 -->
                <tr th:each="order : ${orders}">
                    <td th:text="${order.id}"></td>
                    <td th:text="${order.orderNumber}"></td>
                    <td th:text="${order.totalAmount}"></td>
                    <td th:text="${#dates.format(order.orderDate, 'yyyy-MM-dd HH:mm:ss')}"></td>
                    <td>
                        <a th:href="@{'/editOrder/' + ${order.id}}" class="btn btn-primary btn-sm">编辑</a>
                        <a th:href="@{'/deleteOrder/' + ${order.id}}" class="btn btn-danger btn-sm">删除</a>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</body>
</html>
```

**创建一个新的HTML表单页面**

在您的`src/main/resources/templates/orders/`目录下创建一个名为`create-order.html`的HTML文件，用于显示订单创建表单。

```html
<!DOCTYPE html>
<html>
<head>
    <title>创建订单</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
</head>
<body>
    <div class="container mt-5">
        <h2>创建新订单</h2>
        <form method="post" action="/createOrder">
            <div class="form-group">
                <label for="orderNumber">订单号：</label>
                <input type="text" class="form-control" id="orderNumber" name="orderNumber" required>
            </div>
            <div class="form-group">
                <label for="totalAmount">总金额：</label>
                <input type="number" class="form-control" id="totalAmount" name="totalAmount" step="0.01" required>
            </div>
            <button type="submit" class="btn btn-primary">创建订单</button>
        </form>
    </div>
</body>
</html>
```

这个表单包含了订单号和总金额字段，以及一个提交按钮。

以上是一个简单的Spring Boot + JPA + RabbitMQ实现订单功能的示例。