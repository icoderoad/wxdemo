package com.icoderoad.example.demo.controller;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.icoderoad.example.demo.dto.LoginResponseDTO;
import com.icoderoad.example.demo.entity.OtpUser;
import com.icoderoad.example.demo.service.OtpUserService;
import com.icoderoad.example.demo.util.HMACOTPUtil;

@RestController
public class OtpLoginController {

    private final OtpUserService userService;

    @Autowired
    public OtpLoginController(OtpUserService userService) {
        this.userService = userService;
    }

    @PostMapping("/secure/login")
    public LoginResponseDTO login(@RequestParam String userName, @RequestParam String password, @RequestParam String otp) {

        LoginResponseDTO responseDTO = new LoginResponseDTO();

        // 检查用户名和密码是否正确
        OtpUser user = userService.getOne(Wrappers.<OtpUser>lambdaQuery().eq(OtpUser::getUserName, userName));
        if (user == null || !user.getPassword().equals(password)) {
            responseDTO.setMessage("用户名或密码错误");
            return responseDTO;
        }

        // 检查HMACOTP验证码是否正确
        String secretKey = user.getSecretKey();
        if (!HMACOTPUtil.isValidHMACOTP(otp, secretKey)) {
            responseDTO.setMessage("HMACOTP验证码错误");
            return responseDTO;
        }

        responseDTO.setMessage("登录成功");
        return responseDTO;
    }
    
    @GetMapping("/otp/generate-otp")
    public String generateOTP(@RequestParam String userName) {
        OtpUser user = userService.getOne(Wrappers.<OtpUser>lambdaQuery().eq(OtpUser::getUserName, userName));
        if (user == null) {
            return "用户不存在";
        }

        // 获取当前时间的时间戳（秒）
        long currentTimestamp = Instant.now().plusSeconds(300).getEpochSecond();

        // 生成HMACOTP
        String otp="";
		try {
			otp = HMACOTPUtil.generateHMACOTP(user.getSecretKey(), currentTimestamp);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return "生成的OTP：" + otp;
    }
  
}