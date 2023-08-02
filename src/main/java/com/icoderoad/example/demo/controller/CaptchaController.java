package com.icoderoad.example.demo.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.code.kaptcha.impl.DefaultKaptcha;


@Controller
public class CaptchaController {

    @Autowired
    private DefaultKaptcha captchaProducer;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${captcha.type}")
    private String captchaType;

    	
    @GetMapping("/captcha")
    public void getCaptcha(@RequestParam("uuid") String uuid, HttpServletResponse response) throws IOException {
        String code = generateCode();

        // Store the code in Redis
        stringRedisTemplate.opsForValue().set(uuid, code, 5, TimeUnit.MINUTES);

        BufferedImage image = captchaProducer.createImage(code);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", outputStream);

        response.setContentType("image/jpeg");
        response.getOutputStream().write(outputStream.toByteArray());
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }

    @ResponseBody
	@PostMapping("/verifyCaptcha")
    public ResponseEntity<String> verifyCaptcha(@RequestParam("uuid") String uuid, @RequestParam("code") String code) {
        String storedCode = stringRedisTemplate.opsForValue().get(uuid);

        if (storedCode == null) {
            // 验证码已过期
            return ResponseEntity.badRequest().body("验证码已过期，请刷新页面重新获取验证码。");
        }

        if ("arithmetic".equalsIgnoreCase(captchaType)) {
            // 进行算术运算验证码的验证
            String expectedCode = generateArithmeticCodeResult(storedCode);
            if (expectedCode.equals(code)) {
                // 验证码正确
                return ResponseEntity.ok("验证码正确！");
            } else {
                // 验证码错误
                return ResponseEntity.badRequest().body("验证码错误，请重新输入。");
            }
        } else {
            // 进行随机数验证码的验证
            if (storedCode.equals(code)) {
                // 验证码正确
                return ResponseEntity.ok("验证码正确！");
            } else {
                // 验证码错误
                return ResponseEntity.badRequest().body("验证码错误，请重新输入。");
            }
        }
    }
    
    @GetMapping("/testCaptcha")
    public String showTestCaptchaPage() {
        return "test_captcha";
    }

    private String generateArithmeticCodeResult(String code) {
        String[] parts = code.split(" ");
        int num1 = Integer.parseInt(parts[0]);
        char operator = parts[1].charAt(0);
        int num2 = Integer.parseInt(parts[2]);

        int result;
        switch (operator) {
            case '+':
                result = num1 + num2;
                break;
            case '-':
                result = num1 - num2;
                break;
            case '*':
                result = num1 * num2;
                break;
            default:
                result = 0;
        }

        return String.valueOf(result);
    }    

    private String generateCode() {
        if ("arithmetic".equalsIgnoreCase(captchaType)) {
            return generateArithmeticCode();
        } else {
            return captchaProducer.createText();
        }
    }

    private String generateArithmeticCode() {
        int num1 = (int) (Math.random() * 10);
        int num2 = (int) (Math.random() * 10);
        int operator = (int) (Math.random() * 3); // 0: addition, 1: subtraction, 2: multiplication

        int result;
        char operatorChar;

        switch (operator) {
            case 0:
                result = num1 + num2;
                operatorChar = '+';
                break;
            case 1:
                result = num1 - num2;
                operatorChar = '-';
                break;
            default:
                result = num1 * num2;
                operatorChar = '*';
        }

        return num1 + " " + operatorChar + " " + num2 + " = ?";
    }
}