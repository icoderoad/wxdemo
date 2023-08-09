package com.icoderoad.example.demo.password;

import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Base64;

public class MessageDigestPasswordEncoder implements PasswordEncoder {
    private static final String PREFIX = "{";
    private static final String SUFFIX = "}";
    private StringKeyGenerator saltGenerator = new Base64StringKeyGenerator();
    private boolean encodeHashAsBase64;

    private Digester digester;

    
    public MessageDigestPasswordEncoder(String algorithm) {
        this.digester = new Digester(algorithm, 1);
    }

    public void setEncodeHashAsBase64(boolean encodeHashAsBase64) {
        this.encodeHashAsBase64 = encodeHashAsBase64;
    }

    
    public String encode(CharSequence rawPassword) {
        String salt = PREFIX + this.saltGenerator.generateKey() + SUFFIX;
        return digest(salt, rawPassword);
    }

    private String digest(String salt, CharSequence rawPassword) {
        String saltedPassword = rawPassword + salt;

        byte[] digest = this.digester.digest(Utf8.encode(saltedPassword));
        String encoded = encode(digest);
        return salt + encoded;
    }

    private String encode(byte[] digest) {
        if (this.encodeHashAsBase64) {
            return Utf8.decode(Base64.getEncoder().encode(digest));
        }
        else {
            return new String(Hex.encode(digest));
        }
    }

    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        String salt = extractSalt(encodedPassword);
        String rawPasswordEncoded = digest(salt, rawPassword);
        return PasswordEncoderUtil.equals(encodedPassword.toString(), rawPasswordEncoded);
    }

   
    public void setIterations(int iterations) {
        this.digester.setIterations(iterations);
    }

    private String extractSalt(String prefixEncodedPassword) {
        int start = prefixEncodedPassword.indexOf(PREFIX);
        if (start != 0) {
            return "";
        }
        int end = prefixEncodedPassword.indexOf(SUFFIX, start);
        if (end < 0) {
            return "";
        }
        return prefixEncodedPassword.substring(start, end + 1);
    }
}