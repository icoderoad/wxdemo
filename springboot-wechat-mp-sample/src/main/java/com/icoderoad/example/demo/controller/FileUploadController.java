package com.icoderoad.example.demo.controller;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileUploadController {
    // 存储文件上传的目录
    private static final String UPLOAD_DIR = "your/upload/directory";

    // 上传文件，这里支持分片上传
    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam("chunkNumber") int chunkNumber,
                                         @RequestParam("totalChunks") int totalChunks,
                                         @RequestParam("identifier") String identifier) {
        try {
            // 创建上传目录
            Path uploadPath = Paths.get(UPLOAD_DIR, identifier);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 获取当前分片的文件名
            String chunkFilename = chunkNumber + ".part";
            Path chunkFile = uploadPath.resolve(chunkFilename);

            // 保存分片文件
            Files.write(chunkFile, file.getBytes());

            // 判断是否所有分片都已上传，若是则合并分片文件
            if (chunkNumber == totalChunks - 1) {
                mergeChunks(uploadPath, identifier, file.getOriginalFilename(), totalChunks);
            }

            return ResponseEntity.ok("上传成功");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("上传失败");
        }
    }

    // 合并分片文件
    private void mergeChunks(Path uploadPath, String identifier, String filename, int totalChunks) throws IOException {
        Path mergedFile = Paths.get(UPLOAD_DIR, filename);
        try (FileChannel destChannel = FileChannel.open(mergedFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            for (int i = 0; i < totalChunks; i++) {
                String chunkFilename = i + ".part";
                Path chunkFile = uploadPath.resolve(chunkFilename);
                try (FileChannel srcChannel = FileChannel.open(chunkFile, StandardOpenOption.READ)) {
                    destChannel.transferFrom(srcChannel, destChannel.size(), srcChannel.size());
                }
                Files.delete(chunkFile);
            }
        }
        Files.delete(uploadPath);
    }

    // 获取已上传的文件位置
    @GetMapping("/resume")
    public ResponseEntity<Long> getResumePosition(@RequestParam("identifier") String identifier) {
        Path uploadPath = Paths.get(UPLOAD_DIR, identifier);
        if (Files.exists(uploadPath)) {
            try (Stream<Path> files = Files.list(uploadPath)) {
                long lastChunkNumber = files
                        .map(file -> Long.parseLong(file.getFileName().toString().replace(".part", "")))
                        .max(Long::compare)
                        .orElse(-1L);
                // 已上传分片数量+1即为下一个待上传分片的编号
                long resumePosition = lastChunkNumber + 1;
                return ResponseEntity.ok(resumePosition);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(-1L);
            }
        } else {
            // 上传目录不存在，从头开始上传
            return ResponseEntity.ok(0L);
        }
    }
}