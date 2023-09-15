package com.icoderoad.example.user.controller;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.icoderoad.example.user.entity.User;
import com.icoderoad.example.user.repository.UserRepository;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HttpSession httpSession;
  
    @Autowired
    private PasswordEncoder passwordEncoder; // 注入密码加密器

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String username, @RequestParam String password, Model model) {
        // 从数据库中查找用户
        User user = userRepository.findByUsername(username);
        
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            // 验证成功，将用户信息保存到Session中
            httpSession.setAttribute("user", user);
            return "redirect:/dashboard"; // 重定向到用户仪表板
        } else {
            // 验证失败，返回登录页面并显示错误消息
            model.addAttribute("error", "用户名或密码错误");
            return "login";
        }
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        User user = (User) httpSession.getAttribute("user");
        if (user != null) {
            model.addAttribute("user", user);
            return "dashboard";
        } else {
            return "redirect:/login"; // 如果用户未登录，重定向到登录页面
        }
    }

    @GetMapping("/logout")
    public String logoutUser() {
        httpSession.invalidate(); // 使Session失效
        return "redirect:/login"; // 重定向到登录页面
    }
}