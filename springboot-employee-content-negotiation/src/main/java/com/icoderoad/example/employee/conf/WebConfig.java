package com.icoderoad.example.employee.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {
	
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .defaultContentType(MediaType.APPLICATION_JSON) // 设置默认的媒体类型为JSON
            .favorParameter(true) // 启用通过请求参数来进行内容协商，例如"?format=json"
            .parameterName("format") // 设置请求参数的名称，默认是"format"
            .ignoreAcceptHeader(true) // 忽略请求头中的Accept字段
            .useJaf(false) // 不使用Java Activation Framework来确定媒体类型
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType("xml", MediaType.APPLICATION_XML)
            .mediaType("json", MediaType.APPLICATION_JSON);
    }
}