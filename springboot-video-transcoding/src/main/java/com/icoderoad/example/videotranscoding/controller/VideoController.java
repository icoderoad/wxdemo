package com.icoderoad.example.videotranscoding.controller;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.icoderoad.example.videotranscoding.service.VideoTranscodingService;

@Controller
public class VideoController {

    @Value("${video.storage.path}")
    private String videoStoragePath;

    @Value("${transcoded.video.storage.path}")
    private String transcodedVideoStoragePath;

    private final VideoTranscodingService transcodingService;

    @Autowired
    public VideoController(VideoTranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }

    @GetMapping("/")
    public String home() {
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadVideo(@RequestParam("file") MultipartFile file, Model model) {
        if (!file.isEmpty()) {
            try {
                // 生成唯一的文件名
                String originalFilename = file.getOriginalFilename();
                String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFilename;

                // 保存上传的原始视频文件
                File videoFile = new File(videoStoragePath + File.separator + uniqueFileName);
                file.transferTo(videoFile);

                // 生成转码后的文件名
                String transcodedFileName = UUID.randomUUID().toString() + ".mp4";
                String transcodedVideoPath = transcodedVideoStoragePath + File.separator + transcodedFileName;

                // 调用VideoTranscodingService进行视频转码
                transcodingService.transcodeVideo(videoFile.getAbsolutePath(), transcodedVideoPath);

                // 设置模型属性，以在视图中显示播放器
                model.addAttribute("videoFileName", transcodedFileName);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return   "upload";
    }
}