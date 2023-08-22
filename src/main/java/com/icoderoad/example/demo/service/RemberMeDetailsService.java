package com.icoderoad.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.icoderoad.example.demo.entity.RmUser;
import com.icoderoad.example.demo.mapper.RmUserMapper;

@Service
public class RemberMeDetailsService implements UserDetailsService {
	
	@Autowired
	private RmUserMapper userMapper;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		RmUser user = userMapper.findByUsername(username);
		if (user == null) {
			throw new UsernameNotFoundException("User not found");
		}
		return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
				.password(user.getPassword()).roles("USER").build();
	}
}
