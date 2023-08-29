package com.icoderoad.example.demo.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import cn.hutool.jwt.JWT;

@Controller
@RequestMapping("/view/jwt")
public class JwtViewController {
	
	@Value("${jwt.secret-key}")
    private String jwtSecret;

	@Autowired
    AuthenticationManager authenticationManager;
	
    @GetMapping("/login")
    public String loginPage() {
        return "/jwt/jLogin";
    }
    
    @PostMapping("/login")
    public String login(String username, String password, RedirectAttributes redirectAttributes) {

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);
        authenticationManager.authenticate(authenticationToken);

         //上一步没有抛出异常说明认证成功，我们向用户颁发jwt令牌
        String jwtToken = JWT.create()
                .setPayload("username", username)
                .setKey(jwtSecret.getBytes(StandardCharsets.UTF_8))
                .sign();

        redirectAttributes.addFlashAttribute("jwtToken", jwtToken); // 将 Token 添加到 Flash 属性

        return "redirect:/view/jwt/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "/jwt/dashboard";
    }

    @GetMapping("/public")
    public String publicPage() {
        return "jwt/public_page";
    }
}