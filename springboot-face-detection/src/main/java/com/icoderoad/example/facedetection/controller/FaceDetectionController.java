package com.icoderoad.example.facedetection.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import nu.pattern.OpenCV;

@Controller
public class FaceDetectionController {

    @Value("${upload.directory}")
    private String uploadDirectory;

    @Value("${output.directory}")
    private String outputDirectory;

    static {
    	OpenCV.loadLocally();
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/detect")
    public String detectFaces(@RequestParam("file") MultipartFile file, Model model) {
        if (!file.isEmpty()) {
            try {
                String fileName = file.getOriginalFilename();
                String filePath = uploadDirectory + "/" + fileName;
                File uploadDir = new File(uploadDirectory);
                if( !uploadDir.exists() ) {
                	uploadDir.mkdirs();
                }
                // 保存上传的文件
                byte[] bytes = file.getBytes();
                Path path = Paths.get(filePath);
                Files.write(path, bytes);

                // 加载OpenCV级联分类器
             // 使用ClassLoader加载级联分类器文件
                Resource resource = new ClassPathResource("model/haarcascade_frontalface_default.xml");
                File cascadeFile = resource.getFile();
                CascadeClassifier faceDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());


                // 读取上传的图像文件
                Mat image = Imgcodecs.imread(filePath);

                // 在图像中检测人脸
                MatOfRect faceDetections = new MatOfRect();
                faceDetector.detectMultiScale(image, faceDetections);

                // 在图像上绘制检测到的人脸
                for (Rect rect : faceDetections.toArray()) {
                    Imgproc.rectangle(image, rect.tl(), rect.br(), new Scalar(0, 255, 0), 3);
                }
                
                File outputDir = new File(outputDirectory);
                if( !outputDir.exists() ) {
                	outputDir.mkdirs();
                }
                
                // 保存带有人脸检测结果的图像
                String outputFilePath = outputDirectory + "/" + fileName;
                Imgcodecs.imwrite(outputFilePath, image);

                model.addAttribute("resultImage", "/images/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "result";
    }
}