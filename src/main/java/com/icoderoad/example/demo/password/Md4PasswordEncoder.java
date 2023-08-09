package com.icoderoad.example.demo.password;

import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Base64;

public class Md4PasswordEncoder implements PasswordEncoder {
    private static final String PREFIX = "{";
    private static final String SUFFIX = "}";
    private StringKeyGenerator saltGenerator = new Base64StringKeyGenerator();
    private boolean encodeHashAsBase64;


    public void setEncodeHashAsBase64(boolean encodeHashAsBase64) {
        this.encodeHashAsBase64 = encodeHashAsBase64;
    }

    /**
     * Encodes the rawPass using a MessageDigest. If a salt is specified it will be merged
     * with the password before encoding.
     *
     * @param rawPassword The plain text password
     * @return Hex string of password digest (or base64 encoded string if
     * encodeHashAsBase64 is enabled.
     */
    public String encode(CharSequence rawPassword) {
        String salt = PREFIX + this.saltGenerator.generateKey() + SUFFIX;
        return digest(salt, rawPassword);
    }

    private String digest(String salt, CharSequence rawPassword) {
        if (rawPassword == null) {
            rawPassword = "";
        }
        String saltedPassword = rawPassword + salt;
        byte[] saltedPasswordBytes = Utf8.encode(saltedPassword);

        Md4 md4 = new Md4();
        md4.update(saltedPasswordBytes, 0, saltedPasswordBytes.length);

        byte[] digest = md4.digest();
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