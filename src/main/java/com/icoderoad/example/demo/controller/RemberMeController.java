package com.icoderoad.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/remberme")
public class RemberMeController {

	@GetMapping("/login")
	public String showLoginPage() {
		return "/rember-me/login";
	}

	@GetMapping("/profile")
	public String showUserProfilePage() {
		return "/rember-me/profile";
	}

}
