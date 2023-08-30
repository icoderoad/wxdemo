使用springboot + mybatis-plus + redis 实现优惠券生成及定时检测功能

下面是一个基于Spring Boot、MyBatis-Plus和Redis的示例项目，实现优惠券生成及定时检测功能涉及数据库操作、定时任务、缓存操作等。

创建数据库表

以下是一个简化的优惠券表的MySQL DDL语句：

```sql
CREATE TABLE `coupon` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL,
  `value` decimal(10,2) NOT NULL,
  `expiry_date` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8;
```

请将下面配置添加到项目的pom.xml文件中

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 项目基本信息 -->
    <groupId>com.example</groupId>
    <artifactId>coupon-system</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <properties>
        <java.version>1.8</java.version>
        <spring-boot.version>2.5.4</spring-boot.version>
        <mybatis-plus.version>3.5.3.1</mybatis-plus.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
    
        <!-- Spring Boot Starter Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    
        <!-- MyBatis Plus Starter -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
    
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    
        <!-- Spring Boot Starter Data Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
      
			<dependency>
	        <groupId>redis.clients</groupId>
	        <artifactId>jedis</artifactId>
	    </dependency>
	     
        <!-- Spring Boot Starter AOP -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
    
         <!-- jackson-datatype-jsr310 -->
    	<dependency>
    	    <groupId>com.fasterxml.jackson.datatype</groupId>
    	    <artifactId>jackson-datatype-jsr310</artifactId>
    	</dependency>
      
      <!-- MySQL Connector Java -->
		<dependency>
		    <groupId>mysql</groupId>
		    <artifactId>mysql-connector-java</artifactId>
		    <version>${mysql-connector-java.version}</version> 
		</dependency>
    
    </dependencies>
    
    <build>
        <plugins>
            <!-- Spring Boot Maven Plugin -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```


配置属性文件

在 application.properties 中配置数据库、Redis和定时任务相关的属性：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/wxdemo
spring.datasource.username=root
spring.datasource.password=123456

spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=123456

#MyBatis
mybatis-plus.mapper-locations=classpath:mapper/**/*.xml

#Scheduling
spring.task.scheduling.enabled=true
```

Coupon.java - 实体类

创建优惠券实体类，使用Lombok注解简化代码：

```java
package com.icoderoad.example.coupon.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.Data;

@Data
public class Coupon {
	
	@TableId(value = "id", type = IdType.AUTO)
    private Integer id;
	
    private String code;
    
    private BigDecimal value;
    
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime expiryDate;
}
```

CouponMapper.java

创建MyBatis的Mapper接口和XML映射文件，用于数据库操作。

```java
package com.icoderoad.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icoderoad.example.demo.entity.Coupon;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {
	
}
```

CouponService.java Service 层

创建Service层，实现优惠券生成和定时检测功能。

```java
package com.icoderoad.example.coupon.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.icoderoad.example.coupon.entity.Coupon;
import com.icoderoad.example.coupon.mapper.CouponMapper;

@Service
public class CouponService {
    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // 生成 coupon
    public void generateCoupon(String code, BigDecimal value,  LocalDateTime expiryDate) {
        // 入库
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setValue(value);
        coupon.setExpiryDate(expiryDate);
        couponMapper.insert(coupon);
    
        // 缓存数据
        redisTemplate.opsForList().leftPush("coupons", coupon);
    }
    
    // 检测过期数据
    public List<Coupon> getExpiredCoupons() {
        Date currentTime = new Date();
        List<Coupon> expiredCoupons = couponMapper.selectList(
            new QueryWrapper<Coupon>()
                .lt("expiry_date", currentTime)
        );
    
        redisTemplate.opsForList().remove("coupons", 0, expiredCoupons);
    
        return expiredCoupons;
    }
    
    public boolean isCouponExpired(String code) {
        Coupon coupon = couponMapper.selectOne(
            new QueryWrapper<Coupon>()
                .eq("code", code)
        );
    
        if (coupon == null) {
            // Handle the case where the coupon code doesn't exist
            return false;
        }
    
        LocalDateTime currentTime = LocalDateTime.now();
        return coupon.getExpiryDate().isBefore(currentTime);
    }

}
```

RedisConfig.java 配置类

```java
package com.icoderoad.example.coupon.conf;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String redisHost;
    @Value("${spring.redis.password}")
    private String password;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisHost, redisPort);
        redisStandaloneConfiguration.setPassword(password);
        return new JedisConnectionFactory(redisStandaloneConfiguration);
    }
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory());

        // 设置key的序列化器为StringRedisSerializer
        template.setKeySerializer(new StringRedisSerializer());
        // 设置hash key的序列化器为StringRedisSerializer
        template.setHashKeySerializer(new StringRedisSerializer());

        // 设置value的序列化器为Jackson2JsonRedisSerializer
        template.setValueSerializer(jackson2JsonRedisSerializer());
        // 设置hash value的序列化器为Jackson2JsonRedisSerializer
        template.setHashValueSerializer(jackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisSerializer<Object> jackson2JsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        return new Jackson2JsonRedisSerializer<>(Object.class);
    }
  
}
```

定时任务

创建定时任务来检测过期优惠券并执行相应操作。

```java
package com.icoderoad.example.coupon.job;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.icoderoad.example.coupon.entity.Coupon;
import com.icoderoad.example.coupon.service.CouponService;

@Component
public class CouponExpirationJob {
    @Autowired
    private CouponService couponService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Scheduled(cron = "0 0 * * * *") // 每小时执行一次
    public void checkExpiredCoupons() {
        List<Coupon> expiredCoupons = couponService.getExpiredCoupons();
    
        // 从缓存中移除过期数据
        for (Coupon expiredCoupon : expiredCoupons) {
            redisTemplate.opsForList().remove("coupons", 0, expiredCoupon);
        }
    }

}
```

注意，这个定时任务会根据定时表达式（cron表达式）每小时运行一次，检查和清理过期的优惠券。

创建 CouponRequestDTO 类

```java
package com.icoderoad.example.coupon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CouponRequestDTO {
	private String code;
	private BigDecimal value;
	private LocalDateTime expiryDate;
}
```

创建 CouponController 类

创建REST API接口，用于测试优惠券生成功能和定时检测功能。

```java
package com.icoderoad.example.coupon.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.icoderoad.example.coupon.dto.CouponRequestDTO;
import com.icoderoad.example.coupon.service.CouponService;

@RestController
public class CouponController {
    @Autowired
    private CouponService couponService;

    @PostMapping("/generate-coupon")
    public void generateCoupon(@RequestBody CouponRequestDTO request) {
        couponService.generateCoupon(request.getCode(), request.getValue(), request.getExpiryDate());
    }
    
    @GetMapping("/check-coupon/{code}")
    public String checkCouponExpiration(@PathVariable String code) {
        boolean isExpired = couponService.isCouponExpired(code);
    
        if (isExpired) {
            return "优惠卷 " + code + " 已过期.";
        } else {
            return "优惠卷 " + code + " 未过期.";
        }
    }

}
```

系统启动时生成20条初始优惠券，代码中使用了Random类来生成随机数，同时使用java.time包来处理日期时间

```java
package com.icoderoad.example.coupon.init;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.icoderoad.example.coupon.service.CouponService;

@Component
public class InitCouponRunner implements CommandLineRunner {
    private static final int NUM_INITIAL_COUPONS = 20;
    private static final int MAX_EXPIRY_DAYS = 20;

    @Autowired
    private CouponService couponService;
    
    @Override
    public void run(String... args) throws Exception {
        generateInitialCoupons();
    }
    
    private void generateInitialCoupons() {
        Random random = new Random();
        LocalDateTime currentTime = LocalDateTime.now();
    
        for (int i = 0; i < NUM_INITIAL_COUPONS; i++) {
            String code = generateRandomCode(18);
            BigDecimal value = BigDecimal.valueOf(random.nextDouble() * 100);
            int expiryDays = random.nextInt(MAX_EXPIRY_DAYS) + 1;
            LocalDateTime expiryDate = currentTime.plusDays(expiryDays);
    
            couponService.generateCoupon(code, value, expiryDate);
        }
    }
    
    private String generateRandomCode(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
    
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            code.append(chars.charAt(index));
        }
    
        return code.toString();
    }

}
```

以上代码创建了一个 InitCouponRunner 类，实现了CommandLineRunner接口，用于在系统启动时生成20条初始优惠券。在generateInitialCoupons方法中，它会生成随机的优惠券代码、金额和过期时间，并通过couponService将它们插入到数据库中。

这个示例提供了一个基本框架，可以根据实际需求进行扩展和优化。我们可以通过 `/check-coupon/{code}` 的GET请求接口，其中{code}是要检查的优惠券的代码，判断优惠券是否已过期，也可以通过 `/generate-coupon` 生成新的优惠券。