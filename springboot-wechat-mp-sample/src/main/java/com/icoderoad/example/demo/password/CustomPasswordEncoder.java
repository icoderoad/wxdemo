package com.icoderoad.example.demo.password;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.icoderoad.example.demo.enums.PasswordType;

@Component
public class CustomPasswordEncoder implements PasswordEncoder {

    private final PasswordType passwordType;

    public CustomPasswordEncoder(@Value("${custom.password.type:BCRYPT}") String passwordType) {
        this.passwordType = PasswordType.valueOf(passwordType.toUpperCase()); // 默认为 BCRYPT
    }

    @Override
    public String encode(CharSequence rawPassword) {
        PasswordEncoder encoder = getEncoderForType(passwordType);
        String pwd = encoder.encode(rawPassword);
        System.out.println("pwd:"+ pwd);
        return pwd;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        PasswordEncoder encoder = getEncoderForType(passwordType);
        return encoder.matches(rawPassword, encodedPassword);
    }

    private PasswordEncoder getEncoderForType(PasswordType type) {
    	// 实例化相应的密码处理类并返回
        switch (type) {
            case PLAIN:
                return new NoOpPasswordEncoder();
            case BCRYPT:
            	return new BCryptPasswordEncoder();
            case PBKDF2:
            	return new Pbkdf2PasswordEncoder();
            case SCRYPT:
            	return new SCryptPasswordEncoder();
            case MD4:
            	return  new Md4PasswordEncoder();
            case MD5:
            	return  new MessageDigestPasswordEncoder("MD5");
            case SHA1:
                return new SHAPasswordEncoder("SHA-1","");
            case SHA256:
            	return new SHAPasswordEncoder();
            case SHA384:
            	return new SHAPasswordEncoder("SHA-384","");
            case SHA512:
            	return new SHAPasswordEncoder("SHA-512","");
            case SM3:
            	return new SM3PasswordEncoder();
            case LDAP:
            	return new LdapShaPasswordEncoder();
            default:
                throw new IllegalArgumentException("Unsupported password type: " + type);
        }
    }
}