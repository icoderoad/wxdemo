package com.icoderoad.example.demo.controller;

import java.security.Principal;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.icoderoad.example.demo.entity.SecUser;
import com.icoderoad.example.demo.service.SecUserService;

@Controller
@RequestMapping("/sec/")
public class SecUserController {

    @Autowired
    private SecUserService userService;

    @GetMapping("/login")
    public String login() {
        return "sec/login";
    }

    @GetMapping("/admin")
    public String adminPage(Model model, Principal principal) {
    	 String username = principal.getName();
         SecUser user = userService.findByUsername(username);
         model.addAttribute("user", user);
        return "sec/admin";// 返回管理员页面视图
    }

    @GetMapping("/user")
    public String userPage(Model model, Principal principal) {
        String username = principal.getName();
        SecUser user = userService.findByUsername(username);
        model.addAttribute("user", user);
        return "sec/user";// 返回用户页面视图
    }
    
    @GetMapping("/default")
    public String defaultPage(Authentication authentication) {
        if (authentication != null) {
        	 // 获取用户的所有角色
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            if (authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                return "redirect:/sec/admin";// 如果用户是管理员角色，重定向到管理员页面
            } else if (authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"))) {
                return "redirect:/sec/user";// 如果用户是普通用户角色，重定向到用户页面
            }
        }
        return "redirect:/login"; // 如果没有匹配的角色，重定向到登录页面
    }
}