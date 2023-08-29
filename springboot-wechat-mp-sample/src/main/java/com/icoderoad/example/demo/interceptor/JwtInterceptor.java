package com.icoderoad.example.demo.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.icoderoad.example.demo.service.AuthService;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Autowired
    public JwtInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取 Authorization 头部信息，该头部应包含 JWT Token
        String token = request.getHeader("Authorization");

        // 验证 JWT Token 是否有效
        if (token != null && authService.verifyJWT(token).isPresent()) {
            return true; // 验证通过，继续处理请求
        }

        // 验证失败，返回 401 未授权状态码
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}
