package com.icoderoad.example.demo.aspect;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.icoderoad.example.demo.aop.RateLimit;
import com.icoderoad.example.demo.exception.RateLimitException;

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