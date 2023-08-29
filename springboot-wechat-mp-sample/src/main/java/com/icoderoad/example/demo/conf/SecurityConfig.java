package com.icoderoad.example.demo.conf;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;

import com.icoderoad.example.demo.filter.JwtAuthenticationFilter;
import com.icoderoad.example.demo.service.RemberMeDetailsService;
import com.icoderoad.example.demo.service.SecUserDetailsService;

import me.zhyd.oauth.config.AuthDefaultSource;
import me.zhyd.oauth.request.AuthGiteeRequest;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private DataSource dataSource;

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
	public JwtAuthenticationProvider jwtAuthenticationProvider() {
		return new JwtAuthenticationProvider();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf().disable().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
				.authorizeRequests().antMatchers("/jwt/login").permitAll()
				.antMatchers("/view/jwt/**").permitAll()
				.antMatchers("/oauth2/login").permitAll()
				.antMatchers("/remberme/login").permitAll()
				.antMatchers("/sec/admin/**").hasRole("ADMIN") // 需要ADMIN角色才能访问/admin路径
				.antMatchers("/sec/user/**").hasRole("USER") // 需要USER角色才能访问/user路径
//				.and().formLogin().loginPage("/sec/login") // 登录页的URL
//				.and().formLogin().loginPage("/remberme/login") // 登录页的URL
//				.defaultSuccessUrl("/sec/default", true) // 登录成功后的默认跳转页（根据角色）
//				.defaultSuccessUrl("/remberme/profile") 
				.and()
	            .rememberMe()
	                .tokenRepository(tokenRepository())
	                .tokenValiditySeconds(604800) // 设置记住登录的有效时间，这里是一周 一周（7 天 * 24 小时 * 60 分钟 * 60 秒）
	                .userDetailsService(userDetailsService())
				.and().logout().permitAll()
				.and().oauth2Login()
				 .loginPage("/oauth2/login") // 指向自定义的登录页面
		         .defaultSuccessUrl("/auth/gitee/callback", true) // 认证成功后的重定向
				;
		
		// jwt 配置
		// 禁用缓存
		/*http.headers().cacheControl();
		// 使用自定义provider
		http.authenticationProvider(jwtAuthenticationProvider());
		// 将我们的JWT
		// filter添加到UsernamePasswordAuthenticationFilter前面，因为这个Filter是authentication开始的filter，我们要早于它
		http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);*/
		
		return http.build();
	}

	/*@Bean
	public UserDetailsService userDetailsService() {
		return new SecUserDetailsService();
	}*/
	
	@Bean
	public UserDetailsService userDetailsService() {
		return new RemberMeDetailsService();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public JdbcTokenRepositoryImpl tokenRepository() {
		JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
		tokenRepository.setDataSource(dataSource);
		tokenRepository.setCreateTableOnStartup(false);
		return tokenRepository;
	}
}