package com.icoderoad.example.chat.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.icoderoad.example.chat.entity.User;
import com.icoderoad.example.chat.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 初始化 admin 用户
    	if( userRepository.findByUsername("admin") == null ) {
	        User adminUser = new User();
	        adminUser.setUsername("admin");
	        adminUser.setPassword(passwordEncoder.encode("admin")); // 使用密码编码器加密密码
	//        adminUser.setRoles("ADMIN");
	        userRepository.save(adminUser);
    	}

        // 初始化 test 用户
    	if( userRepository.findByUsername("test") == null ) {
	        User testUser = new User();
	        testUser.setUsername("test");
	        testUser.setPassword(passwordEncoder.encode("test")); // 使用密码编码器加密密码
	//        testUser.setRoles("USER");
	        userRepository.save(testUser);
    	}
    }
}