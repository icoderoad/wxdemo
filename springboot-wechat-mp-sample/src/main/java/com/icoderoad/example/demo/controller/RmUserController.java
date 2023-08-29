package com.icoderoad.example.demo.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.icoderoad.example.demo.entity.RmUser;
import com.icoderoad.example.demo.service.RmUserService;

@Controller
@RequestMapping("/rm")
public class RmUserController {

    @Autowired
    private RmUserService userService;

    @GetMapping("/login")
    public String loginPage( HttpServletRequest request ) {
    	RmUser user = (RmUser) request.getSession().getAttribute("user");
        if (user == null) {
             user = userService.getUserFromRememberMeCookie(request);
        }
    	if( user !=null ) {
    		return "redirect:/rm/dashboard";
    	}
        return "rm/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        @RequestParam(required = false) boolean rememberMe,
                        HttpServletRequest request, HttpServletResponse response, Model model) {
        boolean loginResult = userService.login(username, password, rememberMe, request, response);
        if (loginResult) {
            return "redirect:/rm/dashboard";
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        userService.logout(request, response);
        return "redirect:/rm/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        RmUser user = (RmUser) request.getSession().getAttribute("user");
        if (user == null) {
            user = userService.getUserFromRememberMeCookie(request);
        }
        if (user != null) {
            model.addAttribute("user", user);
            return "rm/dashboard";
        } else {
            return "redirect:/rm/login";
        }
    }
}