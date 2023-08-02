package com.icoderoad.example.demo.conf;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KaptchaConfig {

    @Value("${kaptcha.border:no}")
    private String border;

    @Value("${kaptcha.border.color:black}")
    private String borderColor;

    @Value("${kaptcha.textproducer.font.color:black}")
    private String fontColor;

    @Value("${kaptcha.image.width:120}")
    private String imageWidth;

    @Value("${kaptcha.image.height:40}")
    private String imageHeight;

    @Value("${kaptcha.textproducer.char.length:4}")
    private String charLength;

    @Value("${kaptcha.textproducer.char.string:0123456789}")
    private String charString;

    @Value("${kaptcha.session.key:code}")
    private String sessionKey;

    @Value("${kaptcha.textproducer.font.size:30}")
    private String fontSize;

    @Value("${kaptcha.noise.impl:com.google.code.kaptcha.impl.DefaultNoise}")
    private String noiseImpl;

    @Value("${kaptcha.word.impl:com.google.code.kaptcha.text.impl.DefaultWordRenderer}")
    private String wordImpl;

    @Bean
    public DefaultKaptcha captchaProducer() {
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        Properties properties = new Properties();

        properties.setProperty("kaptcha.border", border);
        properties.setProperty("kaptcha.border.color", borderColor);
        properties.setProperty("kaptcha.textproducer.font.color", fontColor);
        properties.setProperty("kaptcha.image.width", imageWidth);
        properties.setProperty("kaptcha.image.height", imageHeight);
        properties.setProperty("kaptcha.textproducer.char.length", charLength);
        properties.setProperty("kaptcha.textproducer.char.string", charString);
        properties.setProperty("kaptcha.session.key", sessionKey);
        properties.setProperty("kaptcha.textproducer.font.size", fontSize);
        properties.setProperty("kaptcha.noise.impl", noiseImpl);
        properties.setProperty("kaptcha.word.impl", wordImpl);

        Config config = new Config(properties);
        kaptcha.setConfig(config);

        return kaptcha;
    }
}
