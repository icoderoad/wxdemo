package com.icoderoad.example.compressfile.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.icoderoad.example.compressfile.service.FileUploadService;

@Controller
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @Value("${upload.dir}")
    private String uploadDir;

    @GetMapping("/")
    public String index(Model model) {
        List<String> compressedFiles = fileUploadService.listUploadedFiles();
        model.addAttribute("compressedFiles", compressedFiles);
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
          
            String contentType = file.getContentType();
            if (contentType != null && (contentType.equals("application/zip") || 	contentType.equals("application/x-rar-compressed"))) {
                fileUploadService.uploadAndExtract(file);
                redirectAttributes.addFlashAttribute("message", "文件上传成功并解压！");
            } else {
                redirectAttributes.addFlashAttribute("error", "只能上传.zip和.rar文件。");
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "文件上传失败：" + e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/download")
    public void downloadFile(@RequestParam("fileName") String fileName, HttpServletResponse response) {
        try {
            File file = new File(uploadDir  + File.separator + "prd" + File.separator + fileName);
            FileInputStream fis = new FileInputStream(file);

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            OutputStream os = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            fis.close();
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @PostMapping("/get-file-list")
    @ResponseBody
    public List<String> getFileList(@RequestParam String compressedFile) {
    	List<String> fileList = fileUploadService.getFileList(compressedFile);
    	return  fileList;
    }
}