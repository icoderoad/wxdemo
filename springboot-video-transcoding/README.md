使用Spring Boot、FFmpeg、Thymeleaf 和 Bootstrap 来实现视频自动转码并通过播放器播放功能

要实现视频自动转码并通过播放器播放，可以使用Spring Boot、FFmpeg命令的、Thymeleaf和Bootstrap。下面是一些关键步骤和示例代码来实现这个功能。

首先，需要在`pom.xml`文件中添加所需的依赖项：

```xaml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
</dependencies>
```

接下来，需要配置`application.properties`文件，设置视频文件和转码后的文件的存储路径：

```properties
# 视频文件存储路径
video.storage.path=/path/to/video/storage

# 转码后的视频文件存储路径
transcoded.video.storage.path=/path/to/transcoded/video/storage

# FFmpeg命令的路径
ffmpeg.command.path=/path/to/ffmpeg

# 文件上传配置
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB

# Spring Boot Thymeleaf配置
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false
```

创建一个配置类 FFmpegConfig

```java
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
```

创建服务类 `VideoTranscodingService`

`VideoTranscodingService` 类以从配置中获取FFmpeg命令路径并执行转码：

```java
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
```



创建一个控制器，用于上传和处理视频文件以及生成播放页面：

```java
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
```

在上述代码中，上传视频后，使用 FFmpeg 库进行视频转码。

设置一个处理视频文件的控制器方法，以便通过URL访问转码后的视频：

```java
package com.icoderoad.example.videotranscoding.controller;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class VideoStreamingController {

    @Value("${transcoded.video.storage.path}")
    private String transcodedVideoStoragePath;

    @GetMapping("/videos/{videoFileName}")
    @ResponseBody
    public FileSystemResource streamVideo(@PathVariable String videoFileName) {
        File videoFile = new File(transcodedVideoStoragePath + File.separator + videoFileName);
        return new FileSystemResource(videoFile);
    }
}
```

这将允许在网页上播放转码后的视频文件。确保在 FFmpeg 中设置正确的视频转码逻辑，并将转码后的文件保存在`transcodedVideoStoragePath`中。

接下来，创建Thymeleaf模板以显示播放器和上传表单。在resources/templates文件夹下创建一个名为"upload.html"的文件：

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>视频转码</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
<div class="container mt-5">
    <h1>视频转码和播放</h1>
    <form method="post" action="/upload" enctype="multipart/form-data">
        <div class="form-group">
            <label for="file">选择视频文件</label>
            <input type="file" name="file" id="file" class="form-control-file">
        </div>
        <button type="submit" class="btn btn-primary">上传并转码</button>
    </form>

    <hr>

    <div th:if="${videoFileName != null}">
        <h2>播放视频</h2>
        <video controls th:src="@{'/videos/' + ${videoFileName}}" width="640" height="360"></video>
    </div>
</div>
</body>
</html>
```

然后，创建一个Spring Boot应用程序并设置控制器和视图模板。

```java
package com.icoderoad.example.videotranscoding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VideoTranscodingApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(VideoTranscodingApplication.class, args);
	}

}
```

**启动应用程序：**

运行Spring Boot应用程序，并访问http://localhost:8080/来上传视频文件并对视频进行转码，然后显示转码后的视频播放页面。