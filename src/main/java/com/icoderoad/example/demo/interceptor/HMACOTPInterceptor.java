package com.icoderoad.example.demo.interceptor;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.icoderoad.example.demo.entity.OtpUser;
import com.icoderoad.example.demo.service.OtpUserService;
import com.icoderoad.example.demo.util.HMACOTPUtil;


@Component
public class HMACOTPInterceptor implements HandlerInterceptor {

	@Autowired
	private OtpUserService otpUserService;
	
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
    	String userName = request.getParameter("userName");
        String password = request.getParameter("password");
        String otp = request.getParameter("otp"); // 第二因素：HMACOTP验证码

        // 检查用户名和密码是否正确
        OtpUser user = otpUserService.getOne(Wrappers.<OtpUser>lambdaQuery().eq(OtpUser::getUserName, userName));
        if (user == null || !user.getPassword().equals(password)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name()); // 设置响应字符编码为UTF-8
            response.getWriter().write("用户名或密码错误");
            return false;
        }

        // 检查HMACOTP验证码是否正确
        String secretKey = user.getSecretKey();
        if (!HMACOTPUtil.isValidHMACOTP(otp, secretKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name()); // 设置响应字符编码为UTF-8
            response.getWriter().write("HMACOTP验证码错误");
            return false;
        }

        // 用户身份验证通过
        return true;
    }
  
}