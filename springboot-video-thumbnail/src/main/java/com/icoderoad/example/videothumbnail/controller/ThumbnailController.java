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