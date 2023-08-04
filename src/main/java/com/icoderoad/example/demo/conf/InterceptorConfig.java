package com.icoderoad.example.demo.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.icoderoad.example.demo.interceptor.HMACOTPInterceptor;
import com.icoderoad.example.demo.interceptor.JwtInterceptor;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    
    @Autowired
    private HMACOTPInterceptor hmacotpInterceptor;

    @Autowired
    public InterceptorConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加 JwtInterceptor 拦截器，并指定拦截的路径
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/jwt/user/**"); // 可根据实际路径配置
        
        registry.addInterceptor(hmacotpInterceptor)
        .addPathPatterns("/secure/**"); // 在此处添加需要进行身份验证的URL路径
    }
}