package com.icoderoad.example.chat.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源路径，例如将所有以 "/**" 开头的请求映射到 classpath:/static/ 目录下的静态资源
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}
