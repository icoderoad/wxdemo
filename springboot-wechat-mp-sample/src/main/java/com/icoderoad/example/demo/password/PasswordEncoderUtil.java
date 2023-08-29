package com.icoderoad.example.demo.password;

import org.springframework.security.crypto.codec.Utf8;

import java.security.MessageDigest;


public class PasswordEncoderUtil {

   
    static boolean equals(String expected, String actual) {
        byte[] expectedBytes = bytesUtf8(expected);
        byte[] actualBytes = bytesUtf8(actual);

        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private static byte[] bytesUtf8(String s) {
        if (s == null) {
            return null;
        }

        return Utf8.encode(s); 
    }

    private PasswordEncoderUtil() {
    }
}
