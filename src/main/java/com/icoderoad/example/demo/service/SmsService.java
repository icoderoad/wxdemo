package com.icoderoad.example.demo.service;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.icoderoad.example.demo.conf.AliyunSmsConfig;

import cn.hutool.core.util.RandomUtil;

@Service
public class SmsService {

    @Autowired
    private IAcsClient iAcsClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private AliyunSmsConfig aliyunSmsConfig;

    // 发送短信验证码
    public boolean sendOtpCode(String phoneNumber) {
        String otpCode = generateOtpCode();
        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dysmsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        request.putQueryParameter("RegionId", aliyunSmsConfig.getRegionId());
        request.putQueryParameter("PhoneNumbers", phoneNumber);
        request.putQueryParameter("SignName", aliyunSmsConfig.getSignName());
        request.putQueryParameter("TemplateCode", aliyunSmsConfig.getTemplateCode());
        request.putQueryParameter("TemplateParam", "{\"code\":\"" + otpCode + "\"}");
        try {
            CommonResponse response = iAcsClient.getCommonResponse(request);
            if (response.getHttpResponse().isSuccess()) {
                // 发送成功，将验证码存储到Redis中，以便后续验证
                redisTemplate.opsForValue().set(getOtpKey(phoneNumber), otpCode, aliyunSmsConfig.getOtpExpireSeconds(), TimeUnit.SECONDS);
                return true;
            }
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 验证短信验证码
    public boolean verifyOtpCode(String phoneNumber, String otpCode) {
        String storedOtpCode = redisTemplate.opsForValue().get(getOtpKey(phoneNumber));
        return otpCode.equals(storedOtpCode);
    }

    // 生成6位随机数验证码
    private String generateOtpCode() {
        return RandomUtil.randomNumbers(6);
    }

    // 获取存储在Redis中的验证码Key
    private String getOtpKey(String phoneNumber) {
        return "otp:" + phoneNumber;
    }
}