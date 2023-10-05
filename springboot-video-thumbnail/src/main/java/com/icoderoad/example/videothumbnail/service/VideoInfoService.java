package com.icoderoad.example.videothumbnail.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VideoInfoService {

  @Value("${ffmpeg.path}")
    private String ffmpegPath;

    public int getVideoTotalFrames(String videoFilePath) throws IOException, InterruptedException {
       String ffmpegCommand = ffmpegPath;
 
      // 构建FFmpeg命令，用于获取视频信息
        String[] cmd = {
            ffmpegCommand,
            "-i", videoFilePath,
            "-vframes", "1",
            "-f", "null",
            "-"
        };

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        Process process = processBuilder.start();

        // 读取FFmpeg的输出流，其中包含视频信息
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String line;
        StringBuilder output = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        process.waitFor();

        // 从输出中解析总帧数
        String outputStr = output.toString();
        int totalFramesIndex = outputStr.indexOf("fps");
        if (totalFramesIndex != -1) {
            String framesInfo = outputStr.substring(totalFramesIndex - 10, totalFramesIndex).trim();
            String[] parts = framesInfo.split(" ");
            for (String part : parts) {
                if (part.matches("\\d+")) {
                    return Integer.parseInt(part);
                }
            }
        }

        throw new IOException("无法获取视频总帧数");
    }
}