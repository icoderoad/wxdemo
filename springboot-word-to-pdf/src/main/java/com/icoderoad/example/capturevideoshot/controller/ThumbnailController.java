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
