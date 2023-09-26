package com.icoderoad.example.capturevideoshot.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {

    @Value("${video.upload.dir}")
    private String uploadDir;
    
    @Value("${screenshot.storage.path}")
    private String screenshotStoragePath; // 从配置文件中读取视频截图存储路径

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/upload")
    public String uploadVideo(@RequestParam("videoFile") MultipartFile videoFile, Model model) {
        if (!videoFile.isEmpty()) {
            try {
                // 保存上传的视频文件
                String videoFileName = videoFile.getOriginalFilename();
                String videoFilePath = uploadDir + File.separator + videoFileName;
                videoFile.transferTo(new File(videoFilePath));

                // 截取视频截图
                List<String> screenshotUrls = captureVideoshots(videoFilePath, screenshotStoragePath);

                model.addAttribute("message", "视频上传成功！");
                model.addAttribute("screenshotUrls", screenshotUrls);
            } catch (IOException e) {
                model.addAttribute("message", "视频上传失败！");
                e.printStackTrace();
            }
        } else {
            model.addAttribute("message", "请选择一个视频文件上传！");
        }
        return "index";
    }

    public  List<String> captureVideoshots(String videoPath, String outputDirectory) throws FrameGrabber.Exception, IOException {
        Loader.load(avutil.class);
        Loader.load(avformat.class);
        Loader.load(avcodec.class);

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath);
        List<String> screenshotUrls = new ArrayList<>();
        try {
        	grabber.start();
            int totalFrames = grabber.getLengthInFrames();
            int frameInterval = totalFrames / 4; // 3 screenshots per video

            for (int i = 1; i <= 3; i++) {
                int frameNumber =   i * frameInterval;
                grabber.setFrameNumber(frameNumber);
                Frame frame = grabber.grabImage();
                String shotName =  "screenshot" + System.currentTimeMillis() + ".jpg";
                if (frame != null) {
                    String outputFilePath = outputDirectory + File.separator + shotName;
                    screenshotUrls.add("/thumbnail/"+shotName);
                    Java2DFrameConverter converter = new Java2DFrameConverter();
                    BufferedImage bufferedImage = converter.getBufferedImage(frame);
                    if (bufferedImage != null) {
                        ImageIO.write(bufferedImage, "jpg", new File(outputFilePath));
                    } else {
                        System.err.println("BufferedImage frame 为空 " + frameNumber);
                    }
                } else {
                    // Handle the case where the frame is null
                    System.err.println("Frame 为空 " + frameNumber);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                grabber.stop();
            } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
                e.printStackTrace();
            }
        }
        return screenshotUrls;
    }
    
}
