package com.icoderoad.example.numberplate.conf;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import nu.pattern.OpenCV;

public class ImagePreprocessing {
	static {
        try {
            NativeLoader.loader();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	
    public static void main(String[] args) {
        // 加载图像
        Mat image = Imgcodecs.imread("/Users/zjp/Desktop/33.png", Imgcodecs.IMREAD_GRAYSCALE);
        // 图像二值化
        Mat binaryImage = new Mat();
        Imgproc.threshold(image, binaryImage, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        
        // 去噪
        Mat denoisedImage = new Mat();
        Imgproc.GaussianBlur(binaryImage, denoisedImage, new Size(3, 3), 0);
        
        // 保存图像
        Imgcodecs.imwrite("/Users/zjp/Desktop/preprocessed_image.jpg", denoisedImage);
    }
}
