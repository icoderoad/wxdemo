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