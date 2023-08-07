package com.icoderoad.example.demo.util;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.stereotype.Component;

import com.icoderoad.example.demo.conf.OtpConfig;

@Component
public class OtpUtil {
    private final OtpConfig otpConfig;
    private static final int OTP_LENGTH = 6;
    private SecureRandom secureRandom;

    public OtpUtil(OtpConfig otpConfig) {
        this.otpConfig = otpConfig;
    }

    @PostConstruct
    public void init() {
        secureRandom = new SecureRandom();
    }

    /**
     * 生成一个六位数的OTP（一次性密码）。
     *
     * @return 生成的OTP
     */
    public String generateOtp() {
        // 从应用配置中获取OTP密钥，并将其解码为字节数组
        byte[] secretKeyBytes = new Base32().decode(otpConfig.getOtpSecretKey());
        
        // 计算当前时间步长
        long counter = System.currentTimeMillis() / 30000; // 每30秒为一个时间步长
        byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();

        // 使用HMAC算法计算验证码
        byte[] hmacBytes = HmacUtils.hmacSha1(secretKeyBytes, counterBytes);
        int offset = hmacBytes[hmacBytes.length - 1] & 0xf;

        // 将计算得到的HMAC值的后4个字节转化为整数，并通过取模运算得到6位数的OTP值
        int otpValue = ((hmacBytes[offset] & 0x7f) << 24 |
                        (hmacBytes[offset + 1] & 0xff) << 16 |
                        (hmacBytes[offset + 2] & 0xff) << 8 |
                        (hmacBytes[offset + 3] & 0xff)) % (int) Math.pow(10, OTP_LENGTH);

        // 将OTP值格式化为6位数，并返回
        return String.format("%06d", otpValue);
    }

    /**
     * 验证用户输入的OTP是否正确。
     *
     * @param userOtp 用户输入的OTP
     * @return 验证结果，true表示验证成功，false表示验证失败
     */
    public boolean validateOtp(String userOtp) {
        // 生成一个新的OTP
        String generatedOtp = generateOtp();
        
        // 将用户输入的OTP与新生成的OTP进行比较，判断是否一致
        return userOtp.equals(generatedOtp);
    }
}
