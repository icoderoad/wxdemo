package com.icoderoad.example.datasource.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.icoderoad.example.datasource.entity.primary.User;
import com.icoderoad.example.datasource.entity.secondary.SecUser;
import com.icoderoad.example.datasource.repository.primary.PrimaryUserRepository;
import com.icoderoad.example.datasource.repository.secondary.SecondaryUserRepository;

@Controller
public class UserController {
    @Autowired
    private PrimaryUserRepository primaryUserRepository;

    @Autowired
    private SecondaryUserRepository secondaryUserRepository;

    @GetMapping("/")
    public String showUsers(Model model) {
        List<User> primaryUsers = primaryUserRepository.findAll();
        List<SecUser> secondaryUsers = secondaryUserRepository.findAll();

        model.addAttribute("primaryUsers", primaryUsers);
        model.addAttribute("secondaryUsers", secondaryUsers);

        return "user-list"; // 返回Thymeleaf模板名称
    }
}