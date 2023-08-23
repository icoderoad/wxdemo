package com.icoderoad.example.demo.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthGiteeRequest;
import me.zhyd.oauth.request.AuthRequest;

@Controller
@RequestMapping("/oauth2")
public class OAuth2UserController {

	@Value("${justauth.type.gitee.client-id}")
	private String clientId;

	@Value("${justauth.type.gitee.client-secret}")
	private String clientSecret;

	@Value("${justauth.type.gitee.redirect-uri}")
	private String redirectUri;

	@GetMapping("/login")
	public String login() {

		return "/oauth2/gitee-login";
	}

	@PostMapping("/auth/gitee")
	public void authenticateWithGitee(HttpServletResponse response) throws IOException {
		AuthRequest authRequest = new AuthGiteeRequest(
				AuthConfig.builder().clientId(clientId).redirectUri(redirectUri).clientSecret(clientSecret).build());

		response.sendRedirect(authRequest.authorize());
	}

	@GetMapping("/auth/gitee/callback")
	public String giteeCallback(@RequestParam(name = "code") String code, Model model) {
		AuthRequest authRequest = new AuthGiteeRequest(AuthConfig.builder().clientId(clientId).redirectUri(redirectUri)
				.clientSecret(clientSecret).ignoreCheckState(true).build());

		AuthResponse<AuthUser> response = authRequest.login(AuthCallback.builder().code(code).build());

		if (response.ok()) {
			AuthUser user = (AuthUser) response.getData();
			
			// 你可以在此处创建或检索应用中的用户，然后使用Spring Security进行身份验证
			// ...
			model.addAttribute("user", user);
			return "/oauth2/gitee-user";
		} else {
			return "redirect:/oauth2/error";
		}
	}

	@GetMapping("/error")
	public String error(Model model) {
		return "/oauth2/error";
	}
}
