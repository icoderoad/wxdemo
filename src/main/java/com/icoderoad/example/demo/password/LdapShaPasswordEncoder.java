package com.icoderoad.example.demo.password;

import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.MessageDigest;
import java.util.Base64;


public class LdapShaPasswordEncoder implements PasswordEncoder {

    private static final int SHA_LENGTH = 20;
    private static final String SSHA_PREFIX = "{SSHA}";
    private static final String SSHA_PREFIX_LC = SSHA_PREFIX.toLowerCase();
    private static final String SHA_PREFIX = "{SHA}";
    private static final String SHA_PREFIX_LC = SHA_PREFIX.toLowerCase();

    private BytesKeyGenerator saltGenerator;

    private boolean forceLowerCasePrefix;

    public LdapShaPasswordEncoder() {
        this(KeyGenerators.secureRandom());
    }

    public LdapShaPasswordEncoder(BytesKeyGenerator saltGenerator) {
        if (saltGenerator == null) {
            throw new IllegalArgumentException("saltGenerator cannot be null");
        }
        this.saltGenerator = saltGenerator;
    }

   
    private byte[] combineHashAndSalt(byte[] hash, byte[] salt) {
        if (salt == null) {
            return hash;
        }

        byte[] hashAndSalt = new byte[hash.length + salt.length];
        System.arraycopy(hash, 0, hashAndSalt, 0, hash.length);
        System.arraycopy(salt, 0, hashAndSalt, hash.length, salt.length);

        return hashAndSalt;
    }

    
    public String encode(CharSequence rawPass) {
        byte[] salt = this.saltGenerator.generateKey();
        return encode(rawPass, salt);
    }


    private String encode(CharSequence rawPassword, byte[] salt) {
        MessageDigest sha;

        try {
            sha = MessageDigest.getInstance("SHA");
            sha.update(Utf8.encode(rawPassword));
        }
        catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("No SHA implementation available!");
        }

        if (salt != null) {
            sha.update(salt);
        }

        byte[] hash = combineHashAndSalt(sha.digest(), salt);

        String prefix;

        if (salt == null || salt.length == 0) {
            prefix = forceLowerCasePrefix ? SHA_PREFIX_LC : SHA_PREFIX;
        }
        else {
            prefix = forceLowerCasePrefix ? SSHA_PREFIX_LC : SSHA_PREFIX;
        }

        return prefix + Utf8.decode(Base64.getEncoder().encode(hash));
    }

    private byte[] extractSalt(String encPass) {
        String encPassNoLabel = encPass.substring(6);

        byte[] hashAndSalt = Base64.getDecoder().decode(encPassNoLabel.getBytes());
        int saltLength = hashAndSalt.length - SHA_LENGTH;
        byte[] salt = new byte[saltLength];
        System.arraycopy(hashAndSalt, SHA_LENGTH, salt, 0, saltLength);

        return salt;
    }

   
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return matches(rawPassword == null ? null : rawPassword.toString(), encodedPassword);
    }

    private boolean matches(String rawPassword, String encodedPassword) {
        String prefix = extractPrefix(encodedPassword);

        if (prefix == null) {
            return PasswordEncoderUtil.equals(encodedPassword, rawPassword);
        }

        byte[] salt;
        if (prefix.equals(SSHA_PREFIX) || prefix.equals(SSHA_PREFIX_LC)) {
            salt = extractSalt(encodedPassword);
        }
        else if (!prefix.equals(SHA_PREFIX) && !prefix.equals(SHA_PREFIX_LC)) {
            throw new IllegalArgumentException("Unsupported password prefix '" + prefix
                    + "'");
        }
        else {
            // Standard SHA
            salt = null;
        }

        int startOfHash = prefix.length();

        String encodedRawPass = encode(rawPassword, salt).substring(startOfHash);

        return PasswordEncoderUtil
                .equals(encodedRawPass, encodedPassword.substring(startOfHash));
    }

    private String extractPrefix(String encPass) {
        if (!encPass.startsWith("{")) {
            return null;
        }

        int secondBrace = encPass.lastIndexOf('}');

        if (secondBrace < 0) {
            throw new IllegalArgumentException(
                    "Couldn't find closing brace for SHA prefix");
        }

        return encPass.substring(0, secondBrace + 1);
    }

    public void setForceLowerCasePrefix(boolean forceLowerCasePrefix) {
        this.forceLowerCasePrefix = forceLowerCasePrefix;
    }
}