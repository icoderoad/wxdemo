package com.icoderoad.example.videotranscoding.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VideoTranscodingService {

    private final String ffmpegCommandPath;

    @Autowired
    public VideoTranscodingService(String ffmpegCommandPath) {
        this.ffmpegCommandPath = ffmpegCommandPath;
    }

    public void transcodeVideo(String inputVideoPath, String outputVideoPath) {
        try {
            // 构建FFmpeg命令
            String ffmpegCommand = ffmpegCommandPath + " -i " + inputVideoPath + " -c:v libx264 -c:a aac " + outputVideoPath;

            // 执行FFmpeg命令
            Process process = Runtime.getRuntime().exec(ffmpegCommand);

            // 获取命令执行的输出流
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;

            // 读取FFmpeg命令输出，以便查看进度或错误信息
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // 等待命令执行完成
            int exitCode = process.waitFor();

            // 检查命令是否成功执行
            if (exitCode == 0) {
                System.out.println("视频转码成功！");
            } else {
                System.err.println("视频转码失败！");
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}