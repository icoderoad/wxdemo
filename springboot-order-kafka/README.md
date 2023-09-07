Spring Boot 与 Kafka 结合可以轻松实现千万级订单数据的异步处理

主包括订单表的 DDL 定义、初始化数据、以及核心代码方法和注释说明

订单表的 DDL 定义：

```sql
CREATE TABLE kafka_orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    order_date TIMESTAMP,
    customer_id INT,
    total_amount DECIMAL(10, 2)
);
```

初始化一些订单数据：

```sql
INSERT INTO kafka_orders (order_date, customer_id, total_amount)
VALUES
    ('2023-09-06 10:00:00', 1, 100.00),
    ('2023-09-06 11:00:00', 2, 150.00),
    ('2023-09-06 12:00:00', 1, 75.00);
```

在 Spring Boot 项目，在 pom.xml 中添加相关依赖

```xml
<!-- Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Thymeleaf for HTML rendering -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- Jackson for JSON serialization -->
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
</dependency>
```

上述`pom.xml`文件包含了Spring Boot Web、Kafka、Thymeleaf和Jackson的依赖配置。

配置信息添加到`application.properties`文件中

```properties
# Spring Boot应用配置
server.port=8080

# Kafka配置
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=order-consumer-group
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer

# Kafka消费配置
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.enable-auto-commit=false
spring.kafka.consumer.auto-commit-interval=1000
spring.kafka.consumer.fetch-max-wait=5000
spring.kafka.consumer.fetch-min-size=1
spring.kafka.consumer.max-poll-records=500
```

创建Spring Boot应用程序的主类`KafkaOrderServiceApplication`：

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KafkaOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaOrderServiceApplication.class, args);
    }
}
```

创建`Order`实体类：

```java
package com.icoderoad.example.kaforder.entity;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "kafka_orders")
public class Order {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Date orderDate;
	private Long customerId;
	private BigDecimal totalAmount;
}
```

Kafka生产者`OrderProducerService：

```java
package com.icoderoad.example.kaforder.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icoderoad.example.kaforder.entity.Order;

@Component
public class OrderProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic = "order-topic";

    @Autowired
    public OrderProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void produceOrder(Order order) {
        String orderJson = convertOrderToJson(order);
        kafkaTemplate.send(topic, orderJson);
    }

    private String convertOrderToJson(Order order) {
    	  try {
              ObjectMapper objectMapper = new ObjectMapper();
              return objectMapper.writeValueAsString(order);
          } catch (JsonProcessingException e) {
              // 处理异常情况，例如日志记录或抛出自定义异常
              e.printStackTrace();
              return null; // 或者抛出异常
          }
    }
}
```

Kafka消费者`OrderConsumerService`，并使用批量消费方式：

```jade
package com.icoderoad.example.kaforder.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icoderoad.example.kaforder.entity.Order;
import com.icoderoad.example.kaforder.repository.OrderRepository;

@Component
public class OrderConsumerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OrderRepository orderRepository; // 你的OrderRepository

    @Autowired
    public OrderConsumerService(KafkaTemplate<String, String> kafkaTemplate, OrderRepository orderRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderRepository = orderRepository;
    }

    @KafkaListener(topics = "order-topic", containerFactory = "batchFactory")
    public void consumeOrders(List<String> orderJsonList) {
        for (String orderJson : orderJsonList) {
            Order order = convertJsonToOrder(orderJson);
            if (order != null) {
                // 处理订单的业务逻辑，例如保存到数据库
                saveOrderToDatabase(order);
            }
        }
    }

    private Order convertJsonToOrder(String orderJson) {
    	try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(orderJson, Order.class);
        } catch (Exception e) {
            // 处理异常情况，例如日志记录或抛出自定义异常
            e.printStackTrace();
            return null; // 或者抛出异常
        }
    }

    private void saveOrderToDatabase(Order order) {
        // 将订单保存到数据库
        orderRepository.save(order);
    }
}
```

`TestDataProducerService`类来生成测试订单数据：

```java
package com.icoderoad.example.kaforder.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icoderoad.example.kaforder.entity.Order;

@Component
public class TestDataProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic = "order-topic";

    @Autowired
    public TestDataProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void produceTestData(int numberOfOrders) {
        ExecutorService executorService = Executors.newFixedThreadPool(10); // 使用多线程模拟并行推送

        for (int i = 0; i < numberOfOrders; i++) {
            executorService.submit(() -> {
                Order order = generateRandomOrder();
                String orderJson = convertOrderToJson(order);
                kafkaTemplate.send(topic, orderJson);
            });
        }

        executorService.shutdown();
        // 等待所有任务完成
        while (!executorService.isTerminated()) {
            // 空循环
        }
    }

    private Order generateRandomOrder() {
        // 生成随机订单数据
        Order order = new Order();
        order.setOrderDate(new Date());
        order.setCustomerId((long) (Math.random() * 1000)); // 随机客户ID
        order.setTotalAmount(new BigDecimal(Math.random() * 1000));
        return order;
    }

    public String convertOrderToJson(Order order) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            // 处理异常情况，例如日志记录或抛出自定义异常
            e.printStackTrace();
            return null; // 或者抛出异常
        }
		}	
}
```

在Spring Boot应用程序启动后调用`TestDataProducer`类的生成测试订单数据方法

```
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TestDataGenerationRunner implements ApplicationRunner {

    private final TestDataProducer testDataProducer;

    public TestDataGenerationRunner(TestDataProducer testDataProducer) {
        this.testDataProducer = testDataProducer;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 在应用程序启动后调用生成测试订单数据方法
        testDataProducer.produceTestData(10000); // 生成1万条测试数据
    }
}
```

KafkaConfig 配置类

```java
package com.icoderoad.example.kaforder.conf;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id}")
    private String consumerGroupId;

    @Value("${kafka.consumer.key-deserializer}")
    private String keyDeserializer;

    @Value("${kafka.consumer.value-deserializer}")
    private String valueDeserializer;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> batchFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // 设置ConsumerFactory
        factory.setConsumerFactory(consumerFactory());

        // 设置批量消费模式
        factory.setBatchListener(true);

        // 设置并发消费者数量
        factory.setConcurrency(3); // 你可以根据需要调整并发度

        // 设置AckMode为手动确认
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);

        return new DefaultKafkaConsumerFactory<>(consumerProps);
    }
}

```

OrderController

```java
package com.icoderoad.example.kaforder.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.icoderoad.example.kaforder.entity.Order;
import com.icoderoad.example.kaforder.service.OrderProducerService;

@Controller
public class OrderController {

    private final OrderProducerService orderProducer;

    @Autowired
    public OrderController(OrderProducerService orderProducer) {
        this.orderProducer = orderProducer;
    }

    @GetMapping("/createOrder")
    public String createOrderForm(Model model) {
        return "orders/create-order"; // 返回名为create-order的HTML视图
    }

    @PostMapping("/createOrder")
    public String createOrder(@RequestBody Order order, RedirectAttributes redirectAttributes) {
        orderProducer.produceOrder(order);
        redirectAttributes.addFlashAttribute("successMessage", "订单已创建成功！"); // 重定向时传递成功消息
        return "redirect:/createOrder";
    }
}
```

`create-order.html`的HTML模板文件，用于输入订单信息

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>创建订单</title>
</head>
<body>
    <h1>创建订单</h1>

    <!-- 显示成功消息 -->
    <div th:if="${successMessage}" class="alert alert-success">
        <p th:text="${successMessage}"></p>
    </div>

    <!-- 订单创建表单 -->
    <form method="post" action="/createOrder">
        <label for="orderDate">订单日期：</label>
        <input type="text" id="orderDate" name="orderDate" placeholder="请输入订单日期"><br>

        <label for="customerId">客户ID：</label>
        <input type="text" id="customerId" name="customerId" placeholder="请输入客户ID"><br>

        <label for="totalAmount">总金额：</label>
        <input type="text" id="totalAmount" name="totalAmount" placeholder="请输入总金额"><br>

        <button type="submit">创建订单</button>
    </form>
</body>
</html>
```