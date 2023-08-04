package com.icoderoad.example.demo.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

public class HMACOTPUtil {

    // 有效的HMACOTP验证时间窗口（秒）
    private static final int VALID_WINDOW_SECONDS = 300;

    public static boolean isValidHMACOTP(String otp, String secretKey) {
        try {
            // 获取当前时间的时间戳（秒）
            long currentTimestamp = Instant.now().getEpochSecond();

            // 尝试5分钟内验证时间窗口内的HMACOTP
            for (int i = -1; i <= VALID_WINDOW_SECONDS; i++) {
                long timestamp = currentTimestamp + i;

                // 计算HMACOTP
                String generatedOTP = generateHMACOTP(secretKey, timestamp);

                // 比较生成的HMACOTP和传入的HMACOTP是否相同
                if (otp.equals(generatedOTP)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String generateHMACOTP(String secretKey, long timestamp) throws NoSuchAlgorithmException, InvalidKeyException {
        // 将时间戳转换为字节数组
        byte[] timestampBytes = String.valueOf(timestamp).getBytes();

        // 创建HMAC算法实例，这里使用HmacSHA256
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");

        // 使用秘钥初始化HMAC算法
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
        hmacSha256.init(secretKeySpec);

        // 计算HMAC
        byte[] hmacBytes = hmacSha256.doFinal(timestampBytes);

        // 对HMAC进行Base64编码
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
}