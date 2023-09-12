package com.icoderoad.example.minio.controller;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;

@Controller
public class FileController {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @GetMapping("/")
    public String index(Model model) {
        List<Item> items = listFiles();
        model.addAttribute("items", items);
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(file.getOriginalFilename())
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
        } catch (Exception e) {
            // 处理异常
        }
        return "redirect:/";
    }

    @GetMapping("/download/{filename}")
    public void downloadFile(@PathVariable("filename") String filename,
                              HttpServletResponse response) {
        try {
            GetObjectArgs args = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(filename)
                .build();
            InputStream stream = minioClient.getObject(args);

            // 设置响应头，告诉浏览器下载文件
            response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(filename, "UTF-8") + "\"");
            response.setContentType("application/octet-stream");

            // 将文件内容复制到响应输出流
            IOUtils.copy(stream, response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            // 处理异常
        }
    }

    private List<Item> listFiles() {
        try {
        	Iterable<Result<Item>> results=  minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build());
        	// 将结果转换为List<Item>
            List<Item> itemList = new ArrayList<>();
            for (Result<Item> result : results) {
                Item item = result.get();
                itemList.add(item);
            }
            return itemList;
        } catch (Exception e) {
            // 处理异常
            return Collections.emptyList();
        }
    }
}