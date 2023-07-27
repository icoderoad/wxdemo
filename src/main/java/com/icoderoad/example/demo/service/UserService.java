package com.icoderoad.example.demo.service;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.icoderoad.example.demo.entity.User;
import com.icoderoad.example.demo.mapper.UserMapper;
import com.icoderoad.example.demo.util.SessionContext;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserMapper userMapper, RedisTemplate<String, String> redisTemplate) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public boolean login(String userName, String password, HttpSession session) {

        // 验证用户名和密码是否匹配
        boolean loginSuccess = this.validateUser(userName, password);


        // 在登录成功后，判断是否已经有用户登录了
        if( loginSuccess ){
	        String previousSessionId = redisTemplate.opsForValue().get(userName);
	        if (previousSessionId != null) {
	            // 如果存在已登录用户，使其Session失效，实现踢出功能
	            HttpSession previousSession = SessionContext.getSession(previousSessionId);
	            if (previousSession != null) {
	                previousSession.invalidate();
	            }
	        }

	         User user = getUserByUsername(userName);
	        session.setAttribute("loggedInUser", user);
	        // 更新Redis中的用户Session信息
	        redisTemplate.opsForValue().set(userName, session.getId());
    	}

        return true;
    }

    public boolean validateUser(String userName, String password) {
        // 根据用户名查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        User user = userMapper.selectOne(queryWrapper);

        // 判断用户是否存在并且密码是否匹配
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return true; // 用户名和密码匹配
        }

        return false; // 用户名或密码错误
    }

 	public User getUserByUsername(String userName) {
        // 根据用户名查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        return userMapper.selectOne(queryWrapper);
    }

    public void registerUser(User user) {
        // 对密码进行BCrypt哈希加密
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);

        // 插入用户信息到数据库
        userMapper.insert(user);
    }
}