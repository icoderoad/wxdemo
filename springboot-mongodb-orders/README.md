使用Spring Boot集成MongoDB和Thymeleaf实现分页查询订单信息功能

首先我们需要 安装docker-compose，不同系统请参考官方文档：https://docs.docker.com/compose/install/

centos7安装命令

```sh
sudo curl -L "https://github.com/docker/compose/releases/download/1.23.1/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

sudo chmod +x /usr/local/bin/docker-compose
docker-compose --version
```

创建 docker-compose.yml 文件

```yaml
version: '2.1'
services:
  mongo:
    image: "mongo:4.0-xenial"
    command: --replSet rs0 --smallfiles --oplogSize 128
    ports:
      - "27017:27017"
    environment:
      - MONGO_INITDB_ROOT_USERNAME=mongouser
      - MONGO_INITDB_ROOT_PASSWORD=mongopw
```

在docker-compose.yml 文件相同目录执行 `docker-compose up -d`以后台模式启动docker-compose。

执行命令 docker-compose exec mongo /usr/bin/mongo -u mongouser -p mongopw 进行 mongodb 命令行，执行以下命令，创建登陆用户。

```sh
rs.initiate();
rs.status();
use mgdb
db.createUser({    
user: "mongouser",
pwd: "mongopw",
roles:[{role: "dbOwner" , db:"mgdb"}]
})
```

初始化订单数据

```sh
db.orders.insertMany([
  {
    order_id: 101,
    order_date: ISODate("2023-07-30T10:08:22.001Z"),
    customer_id: 1001,
    price: NumberDecimal("50.50"),
    product: {
      name: '小型摩托车',
      description: '小型二轮摩托车'
    },
    order_status: false
  },
  {
    order_id: 102, 
    order_date: ISODate("2023-07-30T10:11:09.001Z"),
    customer_id: 1002,
    price: NumberDecimal("15.00"),
    product: {
      name: '汽车电池',
      description: '12V汽车电池'
    },
    order_status: false
  },
  {
    order_id: 103,
    order_date: ISODate("2023-07-30T12:00:30.001Z"),
    customer_id: 1003,
    price: NumberDecimal("25.25"),
    product: {
      name: '锤子',
      description: '16盎司木工锤'
    },
    order_status: false
  }
]);
```

以下是一个使用Spring Boot集成MongoDB并实现分页查询订单信息的示例代码，包括pom.xml中的依赖配置、Thymeleaf分页显示页面的详细代码。

**pom.xml 依赖配置：**

```xml
<!-- Spring Boot Starter Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Boot Starter Data MongoDB -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>

<!-- Thymeleaf Template Engine -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

`application.properties` 配置文件，包含了 MongoDB 的连接信息配置：

```properties
# MongoDB连接信息
spring.data.mongodb.uri=mongodb://username:password@localhost:27017/mgdb

# 数据库名称
spring.data.mongodb.database=mgdb

# 是否启用MongoDB的JPA存储库支持
spring.data.mongodb.repositories.enabled=true

# 日志级别
logging.level.org.springframework.data.mongodb.core=DEBUG
```

在上面的示例中，`username` 和 `password` 分别代表你的 MongoDB 数据库的用户名和密码。将它们替换为实际使用的用户名和密码。连接字符串的格式是 `mongodb://username:password@host:port/database`。

Order.java - 订单实体类

```java
package com.icoderoad.example.orders.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.Data;

@Data
@Document(collection = "orders")
public class Order implements Serializable {
	@MongoId
	private String id;
	
    private Long order_id;
    private Date order_date;
    @Transient
    private String formattedOrderDate;  // 用于存放格式化后的日期字符串
    private Long customer_id;
    private BigDecimal price;
    private Product product;
    private boolean order_status;

}
```

Product.java - 产品实体类

```java
package com.icoderoad.example.orders.entity;

import lombok.Data;

@Data
public class Product {
    private String name;
    private String description;
}
```

OrderRepository.java - 订单数据访问接口

```java
package com.icoderoad.example.orders.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.icoderoad.example.orders.entity.Order;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
}
```

OrderService.java - 订单业务逻辑

```java
package com.icoderoad.example.orders.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.icoderoad.example.orders.entity.Order;

public interface OrderService {
    Page<Order> getOrdersByStatus(boolean orderStatus, Pageable pageable);
}
```

 OrderServiceImpl.java - 订单业务逻辑实现

```java
package com.icoderoad.example.orders.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.icoderoad.example.orders.entity.Order;
import com.icoderoad.example.orders.repository.OrderRepository;
import com.icoderoad.example.orders.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Page<Order> getOrdersByStatus(boolean orderStatus, Pageable pageable) {
    	// 获取所有订单数据
        List<Order> allOrders = orderRepository.findAll();

        // 使用 stream 和 filter 进行过滤
        List<Order> filteredOrders = allOrders.stream()
                .filter(order -> order.isOrder_status() == orderStatus)
                .collect(Collectors.toList());

        // 创建分页结果
        int startIndex = pageable.getPageNumber() * pageable.getPageSize();
        int endIndex = Math.min(startIndex + pageable.getPageSize(), filteredOrders.size());
        List<Order> pageOrders = filteredOrders.subList(startIndex, endIndex);
        pageOrders.forEach(order -> {
            Date date = order.getOrder_date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate =  sdf.format(date);
            order.setFormattedOrderDate(formattedDate);
        });;
        return new PageImpl<>(pageOrders, pageable, filteredOrders.size());
    }
}
```

OrderController.java - 控制器

```java
package com.icoderoad.example.orders.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.icoderoad.example.orders.entity.Order;
import com.icoderoad.example.orders.service.OrderService;

@Controller
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public String getOrderList(
            @RequestParam(name = "status", required = false, defaultValue = "false") boolean status,
            @PageableDefault(size = 10)  Pageable pageable,
            Model model) {
        Page<Order> orders = orderService.getOrdersByStatus(status, pageable);
        model.addAttribute("orders", orders);
        return "order/order-list"; // 返回Thymeleaf模板名称
    }
}
```

**order-list.html - 分页显示页面详细代码（使用Thymeleaf）：**

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>订单列表</title>
    <!-- 引入 Bootstrap 的 CSS 文件 -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
	<div  class="container">
	    <h1>订单列表</h1>
	    <table class="table table-striped">
	        <thead>
	            <tr>
	                <th>订单 ID</th>
	                <th>订单日期</th>
	                <th>客户 ID</th>
	                <th>价格</th>
	                <th>产品名</th>
	                <th>产品描述</th>
	                <th>订单状态</th>
	            </tr>
	        </thead>
	        <tbody>
	            <tr th:each="order : ${orders.content}">
	                <td th:text="${order.order_id}"></td>
	                <td th:text="${order.formattedOrderDate}"></td>
	                <td th:text="${order.customer_id}"></td>
	                <td th:text="${order.price}"></td>
	                <td th:text="${order.product.name}"></td>
	                <td th:text="${order.product.description}"></td>
	                <td th:text="${order.order_status} ? '已完成' : '进行中'"></td>
	            </tr>
	        </tbody>
	    </table>
	    <div th:replace="order/pagination :: pagination"></div>
    </div>
</body>
</html>
```

**pagination.html - 分页代码（使用Thymeleaf）：**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>分页</title>
    <!-- 引入 Bootstrap 的 CSS 文件 -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div th:fragment="pagination">
    	<div class="container mt-4">
	        <ul class="pagination">
	            <li th:class="${orders.first ? 'page-item  disabled' : 'page-item'}">
	                <a th:class="page-link" th:href="@{/orders(page=1, size=${orders.size})}">&laquo;</a>
	            </li>
	            <li th:class="${orders.first ? 'page-item disabled' : 'page-item'}">
	                <a th:class="page-link" th:href="@{/orders(page=${orders.number - 1}, size=${orders.size})}">&lsaquo;</a>
	            </li>
	            <li th:class="${orders.last ? 'page-item disabled' : 'page-item'}">
	                <a th:class="page-link" th:href="@{/orders(page=${orders.number + 1}, size=${orders.size})}">&rsaquo;</a>
	            </li>
	            <li th:class="${orders.last ? 'page-item disabled' : 'page-item'}">
	                <a th:class="page-link" th:href="@{/orders(page=${orders.totalPages}, size=${orders.size})}">&raquo;</a>
	            </li>
	        </ul>
	      </div>
    </div>
</body>
</html>
```

将Thymeleaf模板放置在`src/main/resources/templates/order`目录下

启动 Spring Boot 应用后，访问 http://localhost:8080/orders，显示查询订单分页列表。