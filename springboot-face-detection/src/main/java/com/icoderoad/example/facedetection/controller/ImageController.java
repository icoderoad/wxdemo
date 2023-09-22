package com.icoderoad.example.facedetection.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/images")
public class ImageController {

	@Value("${output.directory}")
	private String outputDirectory;

	@GetMapping("/{imageName:.+}")
	public ResponseEntity<Resource> getImage(@PathVariable String imageName) throws IOException {
		Path imagePath = Paths.get(outputDirectory).resolve(imageName);
		Resource resource = new FileSystemResource(imagePath.toFile());

		if (resource.exists() && resource.isReadable()) {
			return ResponseEntity.ok().contentLength(resource.contentLength()).contentType(MediaType.IMAGE_JPEG) // 适配图像类型
					.body(resource);
		} else {
			// 处理资源不存在的情况，例如返回404
			return ResponseEntity.notFound().build();
		}
	}
}
