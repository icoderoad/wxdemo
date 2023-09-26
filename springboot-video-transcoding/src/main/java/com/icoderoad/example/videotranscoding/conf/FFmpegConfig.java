package com.icoderoad.example.videotranscoding.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FFmpegConfig {

    @Value("${ffmpeg.command.path}")
    private String ffmpegCommandPath;

    @Bean
    public String ffmpegCommandPath() {
        return ffmpegCommandPath;
    }
}