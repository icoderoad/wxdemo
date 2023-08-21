package com.icoderoad.example.demo.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.icoderoad.example.demo.filter.JwtAuthenticationFilter;
import com.icoderoad.example.demo.service.SecUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Value("${jwt.secret-key}")
    private String jwtSecret;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() throws Exception {
        return new JwtAuthenticationFilter(jwtSecret);
    }
    
    @Bean
    public JwtAuthenticationProvider jwtAuthenticationProvider(){
        return new JwtAuthenticationProvider();
    }

    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
    	http.csrf().disable()
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .authorizeRequests()
        .antMatchers("/jwt/login").permitAll()
        .antMatchers("/view/jwt/**").permitAll()
        .antMatchers("/sec/admin/**").hasRole("ADMIN") // 需要ADMIN角色才能访问/admin路径
        .antMatchers("/sec/user/**").hasRole("USER") // 需要USER角色才能访问/user路径
        .and()
        .formLogin()
            .loginPage("/sec/login") // 登录页的URL
            .defaultSuccessUrl("/sec/default", true) // 登录成功后的默认跳转页（根据角色）
            .permitAll()
        .and()
        .logout()
            .permitAll();
    	 //禁用缓存
    	http.headers().cacheControl();
    	//使用自定义provider
    	http.authenticationProvider(jwtAuthenticationProvider());
        //将我们的JWT filter添加到UsernamePasswordAuthenticationFilter前面，因为这个Filter是authentication开始的filter，我们要早于它
    	http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new SecUserDetailsService();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}