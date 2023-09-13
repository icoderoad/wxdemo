package com.icoderoad.example.compressfile.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService {

    @Value("${upload.dir}")
    private String uploadDir;
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".zip", ".rar");

    public void uploadAndExtract(MultipartFile file) throws IOException {
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 保存上传的文件到指定目录
        String uploadedFilePath = uploadDir + File.separator + file.getOriginalFilename();
        file.transferTo(new File(uploadedFilePath));

        // 解压文件并保存到指定目录
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(uploadedFilePath))) {
            ZipEntry entry;
            byte[] buffer = new byte[1024];
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String extractedFilePath = uploadDir + File.separator + "prd" + File.separator + entry.getName();
                    File extractedFile = new File(extractedFilePath);
                    extractedFile.getParentFile().mkdirs();
                    try (OutputStream outputStream = new FileOutputStream(extractedFile)) {
                        int length;
                        while ((length = zipInputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }
                }
            }
        }
    }

    public List<String> listUploadedFiles() {
        File directory = new File(uploadDir);
        String[] files = directory.list();

        if (files != null) {
            // 使用Java 8 Stream过滤文件列表，只返回.zip和.rar文件
            return Arrays.stream(files)
                    .filter(fileName -> ALLOWED_EXTENSIONS.stream().anyMatch(fileName::endsWith))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
    
    public List<String> getFileList(String compressedFile) {
        List<String> fileList = new ArrayList<>();

        // 压缩文件完整路径
        String compressedFilePath = uploadDir + File.separator + compressedFile;

        // 使用 ZipFile 类打开压缩文件
        try  {
        	ZipFile zipFile = new ZipFile(compressedFilePath);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                fileList.add(entry.getName());
            }
            zipFile.close();
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return fileList;
    }
}