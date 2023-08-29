Spring Boot 项目通过 AOP 结合 Redis 实现分布式限流

在 Spring Boot 项目中通过 AOP 结合 Redis 实现分布式限流，具体的步骤如下：

添加依赖：在 pom.xml 文件中添加 Redis 和 Spring AOP 的依赖。

```xml
		<dependency>
		    <groupId>org.springframework.boot</groupId>
		    <artifactId>spring-boot-starter-data-redis</artifactId>
		</dependency>
		
		<dependency>
	        <groupId>redis.clients</groupId>
	        <artifactId>jedis</artifactId>
	    </dependency>
	    
	    <dependency>
		    <groupId>org.springframework.boot</groupId>
		    <artifactId>spring-boot-starter-aop</artifactId>
		</dependency>
 		<dependency>
	        <groupId>org.springframework.boot</groupId>
	        <artifactId>spring-boot-starter-thymeleaf</artifactId>
	    </dependency>
```

配置 Redis：在 application.properties 中配置 Redis 连接信息。

```properties
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=123456
```

创建 Redis 配置类 RedisConfig：

```java
package com.icoderoad.example.ratelimit.conf;

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

创建自定义注解：创建一个自定义注解来标注需要进行限流的方法。

```java
package com.icoderoad.example.ratelimit.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimit {
    String key() default "";
    long limit() default 10;
    long period() default 60;
}
```

创建自定义异常类：

```java
package com.icoderoad.example.ratelimit.exception;

public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
  
}
```

创建异常处理器类：

```java
package com.icoderoad.example.ratelimit.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitException.class)
    public String handleRateLimitException(RateLimitException ex) {
        return ex.getMessage(); // 返回自定义错误信息
    }

}
```

实现 AOP 限流逻辑：创建一个切面类来实现限流逻辑。

package com.icoderoad.example.demo.aspect;

```java
package com.icoderoad.example.ratelimit.aspect;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.icoderoad.example.ratelimit.aop.RateLimit;
import com.icoderoad.example.ratelimit.exception.RateLimitException;

@Aspect
@Component
@Order(1) // 设置切面优先级，确保在事务切面之前执行
public class RateLimitAspect {

    private final RedisTemplate<String, String> redisTemplate;
    private final HttpServletRequest request; // 注入 HttpServletRequest
    
    public RateLimitAspect(RedisTemplate<String, String> redisTemplate, HttpServletRequest request) {
        this.redisTemplate = redisTemplate;
        this.request = request;
    }
    //获取请求用户IP地址
    public String getClientIpAddress() {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
    
    @Around("@annotation(rateLimit)") // 拦截自定义注解的方法
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String ipAddress = getClientIpAddress(); // 获取请求的 IP 地址
        String key = rateLimit.key() + ipAddress; // 构建存储在 Redis 中的键
    
        long currentTime = System.currentTimeMillis() / 1000; // 转换为秒
        long expirationTime = currentTime - rateLimit.period(); // 按秒计算
    
        // 清理过期的记录
        redisTemplate.opsForZSet().removeRangeByScore(key, Double.MIN_VALUE, expirationTime);
    
        long currentCount = redisTemplate.opsForZSet().count(key, Double.MIN_VALUE, Double.MAX_VALUE);
    
        // 判断请求是否超过限流阈值
        if (currentCount < rateLimit.limit()) {
            // 添加当前请求到 Redis 中
            redisTemplate.opsForZSet().add(key, String.valueOf(currentTime), currentTime);
    
            // 设置过期时间，避免 Redis 中的数据无限增长
            redisTemplate.expire(key, rateLimit.period(), TimeUnit.SECONDS);
    
            // 执行原方法
            return joinPoint.proceed();
        } else {
        	System.out.println("已经超过最大限制");
        	throw new RateLimitException("已经超过最大限制"); // 抛出自定义异常
        }
    }

}
```

在需要限流的方法上使用注解：在需要进行限流的方法上使用自定义的 @RateLimit 注解。

通过以上步骤，当请求超过限流阈值时，RateLimitAspect 中的代码将会抛出自定义的 RateLimitException 异常。然后，异常处理器类 GlobalExceptionHandler 会捕获这个异常，并返回预定义的自定义错误信息。这种方式既能阻止请求继续执行，又能返回自定义的错误信息。

package com.icoderoad.example.demo.controller;

```java
package com.icoderoad.example.ratelimit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.icoderoad.example.ratelimit.aop.RateLimit;

@Controller
public class LimitController {

    @RateLimit(key = "limitApi", limit = 2, period = 300)
    @GetMapping("/limitApi")
    @ResponseBody
    public String limitApi() {
        return "API 数据正常响应";
    }
    
    @GetMapping("/limitTest")
    public String limitTest() {
        return "limit/rate-limit";
    }

}
```

创建一个简单的视图用于测试：

在src/main/resources/templates/limit 目录下，创建 rate-limit.html 的HTML页面内容为：


```html
<!DOCTYPE html>
<html>
<head>
    <title>限流测试页</title>
</head>
<body>
    <h1>限流测试页</h1>
    <button onclick="sendRequest()">发送请求</button>
    <p id="response"></p>

    <script>
        function sendRequest() {
            var xhr = new XMLHttpRequest();
            xhr.open("GET", "/limitApi", true);
            xhr.onreadystatechange = function() {
                if (xhr.readyState === 4) {
                    document.getElementById("response").textContent = xhr.responseText;
                }
            };
            xhr.send();
        }
    </script>
</body>
</html>
```
创建并发测试类：为了测试并发请求，可以使用 JUnit 来创建一个并发测试类。

```java
package com.icoderoad.example.ratelimit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = "server.port=8080")
public class ConcurrentTest {

    private final RestTemplate restTemplate = new RestTemplate();
    
    @Test
    public void testConcurrentRequests() throws InterruptedException {
        int numThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    
        for (int i = 0; i < numThreads; i++) {
            executorService.execute(() -> {
                String response = restTemplate.getForObject("http://localhost:8080/limitApi", String.class);
                System.out.println(response);
            });
        }
    
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

}
```

启动 Spring Boot 应用后，访问 http://localhost:8080/limitTest，点击“发送请求”按钮，按钮下面将显示是否正常执行API的文字描述信息。
