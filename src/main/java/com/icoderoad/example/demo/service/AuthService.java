package com.icoderoad.example.demo.service;

import java.text.ParseException;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.icoderoad.example.demo.conf.JwtConfig;
import com.icoderoad.example.demo.entity.JwtUser;
import com.icoderoad.example.demo.mapper.JwtUserMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Service
public class AuthService {

	@Autowired
    private JwtUserMapper jwtUserMapper;

    @Autowired
    private JwtConfig jwtConfig;
    
    @Autowired
    private  BCryptPasswordEncoder bCryptPasswordEncoder;;

    public Optional<JwtUser> verifyUser(String userName, String password) {
        // 根据用户名查询数据库中的用户信息
        QueryWrapper<JwtUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        JwtUser user = jwtUserMapper.selectOne(queryWrapper);

        // 验证用户密码是否正确
        if (user != null && bCryptPasswordEncoder.matches(password, user.getPassword())) {
            return Optional.of(user);
        } else {
            return Optional.empty();
        }
    }

    public String generateJWT(JwtUser user) throws JOSEException {
        // JWT 配置
        JWSSigner signer = new MACSigner(jwtConfig.getSecretKey());
        Date now = new Date();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUserName())
                .issueTime(now)
                .expirationTime(new Date(now.getTime() + 3600 * 1000)) // 设置过期时间为1小时
                .build();

        // 创建 JWT 并签名
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    public Optional<JwtUser> verifyJWT(String token) throws ParseException, JOSEException {
        // 解析 JWT
        SignedJWT signedJWT = SignedJWT.parse(token);

        // 验证 JWT
        JWSVerifier verifier = new MACVerifier(jwtConfig.getSecretKey());
        if (!signedJWT.verify(verifier)) {
            return Optional.empty();
        }

        // 获取用户信息
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
        String userName = claimsSet.getSubject();

        JwtUser user = jwtUserMapper.selectOne(new QueryWrapper<JwtUser>().eq("user_name", userName));
        return Optional.of(user);
    }
}