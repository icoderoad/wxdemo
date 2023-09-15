package com.icoderoad.example.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.icoderoad.example.user.entity.User;
import com.icoderoad.example.user.service.UserService;

@Controller
@SessionAttributes("loggedInUser")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model) {
        // 使用UserService进行用户验证
        if (userService.authenticateUser(username, password)) {
            User user = new User(username, null); // 不暴露密码到会话
            model.addAttribute("loggedInUser", user);
            return "redirect:/dashboard";
        } else {
            return "login";
        }
    }

    @GetMapping("/dashboard")
    public String showDashboard(@SessionAttribute("loggedInUser") User loggedInUser, Model model) {
        model.addAttribute("username", loggedInUser.getUsername());
        return "dashboard";
    }
}