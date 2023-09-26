使用Spring Boot、JavaCV、Thymeleaf 和 Bootstrap 来实现视频自动截图功能

要使用Spring Boot和JavaCV来实现自动截取视频的截图功能，您需要遵循以下步骤：

创建项目

创建一个Spring Boot应用程序： 如果还没有创建Spring Boot应用程序，请创建一个新的Spring Boot项目。

添加依赖项： 

首先，需要在Spring Boot项目中添依赖项。

```xml
		<!-- Spring Boot Starter Thymeleaf -->
		<dependency>
		    <groupId>org.springframework.boot</groupId>
		    <artifactId>spring-boot-starter-thymeleaf</artifactId>
		</dependency>
	
	    <dependency>
		    <groupId>org.bytedeco</groupId>
		    <artifactId>javacv-platform</artifactId>
		    <version>1.5.9</version> 
		</dependency>
```

**配置application.properties**

在`src/main/resources/application.properties`文件中添加以下配置，以指定视频文件的上传路径：

```properties
# 上传视频文件的存储路径
video.upload.dir=/path/to/your/upload/directory
# 视频截图存储路径
screenshot.storage.path=/path/to/screenshot/storage
```

**创建控制器**

创建一个Spring Boot控制器来处理上传视频和生成截图的请求。以下是一个示例控制器：

```java
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
```

创建一个Controller来处理生成和显示缩略图的请求：

```java
package com.icoderoad.example.capturevideoshot.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class ThumbnailController {

	 @Value("${screenshot.storage.path}")
	 private String screenshotStoragePath; // 从配置文件中读取视频截图存储路径

    @GetMapping("/thumbnail/{filename}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String filename) {
        try {
            Path thumbnailPath = Paths.get(screenshotStoragePath).resolve(filename);

            Resource resource = new UrlResource(thumbnailPath.toUri());

            if (resource.exists() && resource.isReadable()) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.IMAGE_JPEG); // 设置响应的媒体类型为图像

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
```

通过GET请求，您可以通过访问`/thumbnail/{filename}`来获取和显示生成的缩略图，其中`{filename}`是缩略图文件的名称。例如，`/thumbnail/mythumbnail.jpg`将显示名为`mythumbnail.jpg`的缩略图

**创建Thymeleaf模板**

在`src/main/resources/templates`目录下创建一个名为`index.html`的Thymeleaf模板文件，用于显示上传表单和截图结果：

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>视频截图工具</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
</head>
<body>
<div class="container mt-5">
    <h2>上传视频并生成截图</h2>
    <form method="POST" action="/upload" enctype="multipart/form-data">
        <div class="form-group">
            <input type="file" class="form-control-file" id="videoFile" name="videoFile">
        </div>
        <button type="submit" class="btn btn-primary">上传视频</button>
    </form>
    <br>
    <div th:if="${message}">
        <div class="alert alert-info" th:text="${message}"></div>
    </div>
    <div th:if="${screenshotUrls}">
        <h3>生成的截图：</h3>
        <div th:each="url : ${screenshotUrls}">
            <img th:src="${url}" alt="截图">
        </div>
    </div>
</div>
</body>
</html>
```

**启动应用程序：**

运行Spring Boot应用程序，并访问http://localhost:8080/来上传视频文件并生成视频截图，然后显示生成的3张视频截图。