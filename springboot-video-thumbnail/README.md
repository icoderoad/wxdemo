实现Spring Boot + FFmpeg + Thymeleaf + Bootstrap + jquery 的拖动选择视频并生成缩略图功能

这个示例包括前端的HTML页面和后端的Spring Boot应用程序，其中前端实现了拖动选择视频帧号并显示缩略图，后端通过FFmpeg生成缩略图和获取视频总帧数等功能。

首先，创建一个Spring Boot项目并添加所需的依赖，然后按以下方式组织代码：

** 项目结构：**

```
springboot-video-thumbnail/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.icoderoad./
│   │   │       └── videothumbnail/
│   │   │           └── controller/
│   │   │               ├── ThumbnailController.java
│   │   │           └── service/								
│   │   │               ├── VideoInfoService.java
│   │   │        └── VideoThumbnailApplication.java
│   │   ├── resources/
│   │   │   └── static/
│   │   │       ├── index.html
│   │   │       └── scripts.js
│   │   └── application.properties
│   └── test/
└── pom.xml
```

**pom.xml：** 在项目的`pom.xml`文件中添加所需的依赖：

```xml
<dependencies>
    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Thymeleaf Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>

    <!-- FFmpeg Wrapper for Java -->
    <dependency>
        <groupId>net.bramp.ffmpeg</groupId>
        <artifactId>ffmpeg</artifactId>
        <version>0.6.2</version>
    </dependency>
</dependencies>
```

**application.properties：** 在`src/main/resources`目录下的`application.properties`文件中配置视频上传路径和FFmpeg可执行文件路径：

```properties
# 上传文件保存路径
upload.path=/path/to/your/upload/directory

# FFmpeg可执行文件路径
ffmpeg.path=/path/to/your/ffmpeg/executable

# 文件上传配置
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB

# Spring Boot Thymeleaf配置
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false
```

**`ThumbnailResponse.java` 实体类**: 来封装生成的缩略图的URL

```java
package com.icoderoad.example.videothumbnail.entity;

public class ThumbnailResponse {

    private String thumbnailURL;

    public ThumbnailResponse(String thumbnailURL) {
        this.thumbnailURL = thumbnailURL;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public void setThumbnailURL(String thumbnailURL) {
        this.thumbnailURL = thumbnailURL;
    }
}
```

**ThumbnailController.java：** 创建一个Controller来处理请求，并调用`VideoInfoService`服务来生成缩略图和获取视频总帧数。

```java
package com.icoderoad.example.videothumbnail.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.icoderoad.example.videothumbnail.entity.ThumbnailResponse;
import com.icoderoad.example.videothumbnail.service.VideoInfoService;

@Controller
public class ThumbnailController {

    @Autowired
    private VideoInfoService videoInfoService;

    @Value("${upload.path}")
    private String uploadPath;
    
    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    // 处理视频上传
    @PostMapping("/uploadVideo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadVideo(@RequestParam("file") MultipartFile videoFile) {
    	 Map<String, Object> response = new HashMap<>();
    	if (videoFile.isEmpty()) {
    		response.put("msg", "未选择任何视频文件。");
    		response.put("success", false);
            return ResponseEntity.badRequest().body(response);
        }
				
				 // 检查文件扩展名是否为mp4
        if (!checkFileExtension(videoFile, "mp4")) {
        	response.put("msg", "请上传MP4格式的视频文件。");
        	response.put("success", false);
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // 保存视频文件到服务器
            String videoFilePath = uploadPath + "/" + videoFile.getOriginalFilename();
            videoFile.transferTo(new File(videoFilePath));
            // 返回 videoFilePath
            response.put("videoFilePath", videoFilePath);
            response.put("success", true);
            return ResponseEntity.ok().body(response);
        } catch (IOException e) {
        	response.put("msg", "视频上传失败。");
        	response.put("success", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 获取视频总帧数的接口
    @GetMapping("/getTotalFrames")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTotalFrames(@RequestParam("videoFilePath") String videoFilePath) throws IOException, InterruptedException {
        int totalFrams =  videoInfoService.getVideoTotalFrames(videoFilePath);
        Map<String, Object> response = new HashMap<>();
        response.put("totalFrames", String.valueOf(totalFrams));
        return ResponseEntity.ok().body(response);
    }
    
     // 生成指定帧缩略图的接口
    @PostMapping("/generateThumbnail")
    @ResponseBody
    public ResponseEntity<?> generateThumbnail(
            @RequestParam("frameNumber") int frameNumber,
            @RequestParam("videoFile") String videoFileName) {

        try {
            String videoFilePath = uploadPath + "/" + videoFileName;
            String thumbnailFilePath = uploadPath + "/thumbnails" ;// 保存缩略图的文件路径
            File thumbnailDir = new File(thumbnailFilePath);
            if( !thumbnailDir.exists() ) {
            	thumbnailDir.mkdirs();
            }
            String thumbnailName = "/"+ FilenameUtils.getBaseName(videoFileName)+"_output_%03d.png";
         // 构建FFmpeg命令来生成图像序列，使用%03d命名模式
            String ffmpegCommand = ffmpegPath +
                    " -i " + videoFilePath +
                    " -vf \"select=eq(n\\," + frameNumber + ")\" -vframes 1 " +
                    thumbnailFilePath + thumbnailName;

            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand.split(" "));
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
          

            if (exitCode == 1) {
                // 缩略图生成成功，返回缩略图文件路径
            	thumbnailName = thumbnailName.replace("%03d", "001");
                return ResponseEntity.ok().body(new ThumbnailResponse("/thumbnails" +thumbnailName));
            } else {
                return ResponseEntity.badRequest().body("生成缩略图失败。");
            }
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.badRequest().body("生成缩略图失败。");
        }
    }
    
  
    
    @GetMapping("/thumbnails/{filename}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String filename) {
        try {
        	  String thumbnailFolderPath = uploadPath + "/thumbnails"; // 缩略图文件夹路径
            Path thumbnailPath = Paths.get(thumbnailFolderPath).resolve(filename);

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
    
    // 检查文件扩展名的函数
    private boolean checkFileExtension(MultipartFile file, String validExtension) {
        String fileExtension = FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase();
        return fileExtension.equals(validExtension);
    }

}
```

** VideoInfoService.java：** 创建一个服务类，用于获取视频总帧数和生成缩略图。

```java
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
```

** index.html：** 创建一个HTML页面，包含拖动选择视频帧号的功能和显示缩略图的区域。

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>视频拖动生成视频缩略图示例</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://code.jquery.com/ui/1.12.1/themes/base/jquery-ui.css">
</head>
<body>

<div class="container mt-5">
    <h1>视频处理示例</h1>

    <!-- 文件上传表单 -->
    <form action="/uploadVideo" method="post" enctype="multipart/form-data">
        <div class="form-group">
            <input type="file" id="videoFile" name="file" class="form-control-file">
        </div>
    </form>

    <!-- 拖动帧号选择区域 -->
    <div class="mt-4">
        <label for="frameNumber">选择帧号：</label>
        <input type="number" id="frameNumber" min="1" value="1">
    </div>

    <!-- 显示视频缩略图区域 -->
    <div class="mt-4">
        <div id="thumbnailContainer" style="width: 100%; height: 200px; border: 1px solid #ccc; overflow: hidden;">
            <img id="thumbnailImage" src="#" alt="缩略图" style="width: 100%; height: auto; display: none">
        </div>
    </div>
		
  	<!-- 生成指定帧缩略图按钮 -->
    <div class="mt-4">
        <button id="generateThumbnailBtn" class="btn btn-primary">生成指定帧缩略图</button>
    </div>
    <!-- 显示上传成功消息 -->
    <div th:if="${message}" class="alert alert-success mt-4" th:text="${message}"></div>
</div>

<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script src="https://code.jquery.com/ui/1.12.1/jquery-ui.js"></script>
<script src="/js/scripts.js"></script>
</body>
</html>
```

** scripts.js：** 在 /src/main/resources/static/js 目录创建一个JavaScript文件 scripts.js，包含前端的交互逻辑，包括拖动选择帧号和显示缩略图。

```javascript
$(document).ready(function () {
	    const frameNumberInput = $('#frameNumber');
	    const thumbnailContainer = $('#thumbnailContainer');
	    const thumbnailImage = $('#thumbnailImage');
	    const videoFileInput = $('#videoFile'); // 添加视频文件输入
			
	   	const generateThumbnailBtn = $('#generateThumbnailBtn');

        // 当用户点击生成指定帧缩略图按钮时触发
        generateThumbnailBtn.on('click', function () {
            const frameNumber = parseInt($('#frameNumber').val());
            const videoFile = $('#videoFile')[0].files[0];
            
            // 检查文件扩展名是否为mp4
            if (checkFileExtension(videoFile, 'mp4')) {
                generateThumbnail(frameNumber, videoFile);
            } else {
                alert('请上传MP4格式的视频文件。');
            }
        });

        // 生成指定帧缩略图的函数
        function generateThumbnail(frameNumber, videoFile) {
            // 向后端发送请求，生成指定帧的缩略图
            $.ajax({
                url: '/generateThumbnail', // 根据实际的后端处理路径修改
                type: 'POST',
                data: {
                    frameNumber: frameNumber,
                    videoFile: videoFile.name
                },
                success: function (data) {
                    if (data && data.thumbnailURL) {
						console.log("thumbnailURL:", data.thumbnailURL);
                        // 显示生成的缩略图
                        $('#thumbnailImage').attr('src', data.thumbnailURL);
                         $('#thumbnailImage').show();
                        alert('缩略图生成成功！');
                    } else {
                        alert('缩略图生成失败。');
                    }
                },
                error: function () {
                    alert('缩略图生成失败。');
                }
            });
        }
	  
	  	// 检查文件扩展名的函数
	    function checkFileExtension(file, validExtension) {
	        const fileExtension = file.name.split('.').pop().toLowerCase();
	        return fileExtension === validExtension;
	    }
	  
	    //  初始化拖动功能，开始时禁用它
	    frameNumberInput.slider({
	        min: 1,
	        step: 1,
	        slide: function (event, ui) {
	            // 更新输入框的值
	            frameNumberInput.val(ui.value);
	        },
	      	disabled: true // 初始禁用
	    });

		// 当用户输入帧号时，更新拖动条
		frameNumberInput.on('input', function() {
			const frameNumber = parseInt(frameNumberInput.val());
			frameNumberInput.slider('value', frameNumber);
		});

	    // 当视频文件选择发生变化时
	    videoFileInput.on('change', function () {
	        const videoFile = videoFileInput[0].files[0];
	        if (videoFile) {
	            // 上传视频文件
	            uploadVideoFile(videoFile);
	        }
	    });

	    // 上传视频文件的函数
	    function uploadVideoFile(videoFile) {
	      	// 检查文件扩展名是否为mp4
	      	if (!checkFileExtension(videoFile, 'mp4')) {
	          alert('请上传MP4格式的视频文件。');
	          return;
	      	}
	
	        const formData = new FormData();
	        formData.append('file', videoFile);
	
	        $.ajax({
	            url: '/uploadVideo', // 根据实际接口地址修改
	            type: 'POST',
	            data: formData,
	            contentType: false,
	            processData: false,
	            success: function (data) {
	                if (data && data.success) {
	                    console.log('视频上传成功');
	                     // 从上传视频接口返回的data中获取videoFilePath
	                    const videoFilePath = data.videoFilePath;
	
	                    // 调用getTotalFrames并传递videoFilePath参数
	                    getTotalFrames(videoFilePath);
	                } else {
						alert(data.msg);
	                    console.error('视频上传失败');
	                }
	            },
	            error: function () {
	                console.error('视频上传失败');
	            }
	        });
	    }
		var totalFrames = 0;
	  	// 获取视频总帧数的函数
		function getTotalFrames(videoFilePath) {
		    $.ajax({
		        url: '/getTotalFrames',
		        type: 'GET',
		        data: { videoFilePath: videoFilePath }, // 传递videoFilePath参数
		        dataType: 'json',
		        success: function (data) {
		            if (data && data.totalFrames) {
		                totalFrames = data.totalFrames;
		                frameNumberInput.slider('option', 'max', totalFrames);
		                frameNumberInput.val(1);
		                // 在成功获取视频总帧数后启用输入框
		                frameNumberInput.slider('enable');
		            } else {
		                console.error('无法获取视频总帧数');
		            }
		        },
		        error: function () {
		            console.error('无法从服务端获取视频总帧数');
		        }
		    });
		}
	    
});
```

请注意，在实际应用中，需要根据你的需求实现视频上传和缩略图生成的逻辑，这个示例提供了一个基本框架。

**启动应用程序：**

运行Spring Boot应用程序，并访问http://localhost:8080/来上传视频文件并对视频进行选择要截取缩略图的帧，点击 “生成指定帧缩略图” 按钮，生成视图缩略图。