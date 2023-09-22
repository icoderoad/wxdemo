package com.icoderoad.example.numberplate.util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.google.common.collect.Lists;
import com.icoderoad.example.numberplate.entity.Line;

import nu.pattern.OpenCV;

/**
 * 图片处理工具类
 * 将原图，经过算法处理，得到车牌的图块
 * @date 2020-05-18 12:07
 */
public class ImageUtil {

    static {
        // 加载本地安装的opencv库
    	OpenCV.loadLocally();
    }


    /**
     * 解决中文路径下读取图片异常的问题，不在需要先复制图片到temp目录下了
     * 将图像转换为Mat
     * 处理 Imgcodecs.imread(filename) 直接读取被编解码处理过的图片，提示的图片损坏问题：
     * Corrupt JPEG data: 14 extraneous bytes before marker 0xdb
     * 经过编解码、或者图片压缩的图片，直接用opencv读取可能会报错
     * @param path
     * @return
     */
    public static Mat imread(String path, Integer cvType) {
        // 海康设备的图片, 可能做了压缩、编解码等
        File f = new File(path);
        FileInputStream fi = null;
        try{
            fi = new FileInputStream(f);
            BufferedImage sourceImg = ImageIO.read(fi);//判断图片是否损坏
            return matify(sourceImg, cvType);
        }catch (Exception e) {
            try {
                fi.close();//关闭IO流才能操作图片
            } catch (IOException ioException) {
            }
        } finally{
            try {
                fi.close();
            } catch (IOException e) {
            }
        }
        return null;
    }


    /**
     * 将图像转换为Mat
     * 处理 Imgcodecs.imread(filename) 直接读取被编解码处理过的图片，提示的图片损坏问题：
     * Corrupt JPEG data: 14 extraneous bytes before marker 0xdb
     * 经过编解码、或者图片压缩的图片，直接用opencv读取会报错
     * @param im
     * @return
     */
    public static Mat matify(BufferedImage im, Integer cvType){
        //将bufferedimage转换为字节数组
        byte [] pixels =((DataBufferByte)im.getRaster().getDataBuffer()).getData();
        if(null == cvType){
            cvType = CvType.CV_8UC3;
        }
        //创建一个与图像大小相同的矩阵
        Mat image = new Mat(im.getHeight(), im.getWidth(), cvType);
        //用图像值填充矩阵
        image.put(0,0, pixels);
        return image;
    }


    /***
     * 保存算法过程每个步骤处理结果，输出结果jpg图像
     * @param debug 缓存目录
     * @param tempPath
     * @param methodName
     * @param inMat
     */
    public static void debugImg(Boolean debug, String tempPath, String methodName, Mat inMat) {
        if (debug) {
            // 通过getId生成文件名称，使得每个步骤生成的图片能够按照执行时间进行排序
            Imgcodecs.imwrite(tempPath + GenerateIdUtil.getStrId() +"_" +methodName + ".jpg", inMat);
        }
    }

    /**
     * 图像灰度化
     * @param inMat rgbMat/原图
     * @param debug 是否输出结果图片
     * @param tempPath 结果图片输出路径
     * @return greyMat
     */
    public static void gray(Mat inMat, Mat dst, Boolean debug, String tempPath) {
        Imgproc.cvtColor(inMat, dst, Imgproc.COLOR_BGR2GRAY);
        debugImg(debug, tempPath, "gray", dst);
    }


    /**
     * 高斯滤波，用于 抑制噪声，平滑图像， 防止把噪点也检测为边缘
     * 高斯滤波器相比于均值滤波器对图像个模糊程度较小
     * https://blog.csdn.net/qinchao315/article/details/81269328
     * https://blog.csdn.net/qq_35294564/article/details/81142524
     * @param inMat 原图
     * @param debug 是否输出结果图片
     * @param tempPath 结果图片输出路径
     * @return
     */
    public static int GS_BLUR_KERNEL = 3;  // 滤波内核大小必须是 正奇数
    public static void gaussianBlur(Mat inMat, Mat dst, Boolean debug, String tempPath) {
        Size ksize = new Size(GS_BLUR_KERNEL, GS_BLUR_KERNEL);
        Imgproc.GaussianBlur(inMat, dst, ksize, 0, 0, Core.BORDER_DEFAULT);
        debugImg(debug, tempPath, "gaussianBlur", dst);
    }


    /**
     * 均值滤波
     * @param inMat
     * @param debug 是否输出结果图片
     * @param tempPath 结果图片输出路径
     * @return
     */
    public static int BLUR_KERNEL = 12;  // 滤波内核大小必须是 正奇数
    public static void blur(Mat inMat, Mat dst, Boolean debug, String tempPath) {
        Point anchor = new Point(-1,-1);
        Size ksize = new Size(BLUR_KERNEL, BLUR_KERNEL);
        Imgproc.blur(inMat, dst, ksize, anchor, Core.BORDER_DEFAULT);
        debugImg(debug, tempPath, "blur", dst);
    }



    /**
     * 对图像进行Sobel 运算，得到图像的一阶水平方向导数
     * 边缘检测算子，是一阶的梯度算法
     * 所谓梯度运算就是对图像中的像素点进行就导数运算，从而得到相邻两个像素点的差异值
     * 对噪声具有平滑作用，提供较为精确的边缘方向信息，边缘定位精度不够高。当对精度要求不是很高时，是一种较为常用的边缘检测方法
     * @param inMat 灰度图
     * @param debug
     * @param tempPath
     * @return
     */
    public static int SOBEL_SCALE = 1;
    public static int SOBEL_DELTA = 0;
    public static int SOBEL_X_WEIGHT = 1;
    public static int SOBEL_Y_WEIGHT = 0;
    public static int SOBEL_KERNEL = 3;// 内核大小必须为奇数且不大于31
    public static double alpha = 1.5; // 乘数因子
    public static double beta = 10.0; // 偏移量
    public static void sobel(Mat inMat, Mat dst, Boolean debug, String tempPath) {
        Mat grad_x = new Mat();
        Mat grad_y = new Mat();
        Mat abs_grad_x = new Mat();
        Mat abs_grad_y = new Mat();

        // Sobel滤波 计算水平方向灰度梯度的绝对值
        Imgproc.Sobel(inMat, grad_x, CvType.CV_8U, 1, 0, SOBEL_KERNEL, SOBEL_SCALE, SOBEL_DELTA, Core.BORDER_DEFAULT);
        Core.convertScaleAbs(grad_x, abs_grad_x, alpha, beta);   // 增强对比度

        // Sobel滤波 计算垂直方向灰度梯度的绝对值
        Imgproc.Sobel(inMat, grad_y, CvType.CV_8U, 0, 1, SOBEL_KERNEL, SOBEL_SCALE, SOBEL_DELTA, Core.BORDER_DEFAULT);
        Core.convertScaleAbs(grad_y, abs_grad_y, alpha, beta);
        grad_x.release();
        grad_y.release();

        // 计算结果梯度
        Core.addWeighted(abs_grad_x, SOBEL_X_WEIGHT, abs_grad_y, SOBEL_Y_WEIGHT, 0, dst);
        abs_grad_x.release();
        abs_grad_y.release();
        debugImg(debug, tempPath, "sobel", dst);
    }


    /**
     * 对图像进行scharr 运算，得到图像的一阶水平方向导数
     * 增强对比度，边缘检测
     * 所谓梯度运算就是对图像中的像素点进行就导数运算，从而得到相邻两个像素点的差异值
     * @param inMat
     * @param debug
     * @param tempPath
     * @return
     */
    public static void scharr(Mat inMat, Mat dst, Boolean debug, String tempPath) {
        Mat grad_x = new Mat();
        Mat grad_y = new Mat();
        Mat abs_grad_x = new Mat();
        Mat abs_grad_y = new Mat();

        //注意求梯度的时候我们使用的是Scharr算法，sofia算法容易受到图像细节的干扰
        Imgproc.Scharr(inMat, grad_x, CvType.CV_32F, 1, 0);
        Imgproc.Scharr(inMat, grad_y, CvType.CV_32F, 0, 1);
        //openCV中有32位浮点数的CvType用于保存可能是负值的像素数据值
        Core.convertScaleAbs(grad_x, abs_grad_x);
        Core.convertScaleAbs(grad_y, abs_grad_y);
        //openCV中使用release()释放Mat类图像，使用recycle()释放BitMap类图像
        grad_x.release();
        grad_y.release();

        Core.addWeighted(abs_grad_x, 0.5, abs_grad_y, 0.5, 0, dst);
        abs_grad_x.release();
        abs_grad_y.release();
        debugImg(debug, tempPath, "scharr", dst);
    }


    /**
     *
     * @param inMat
     * @param dst
     * @param debug
     * @param tempPath
     */
    public static void canny(Mat inMat, Mat dst, Boolean debug, String tempPath) {
        // 低于阈值1的像素点会被认为不是边缘；
        // 高于阈值2的像素点会被认为是边缘；
        Imgproc.Canny(inMat, dst, 100, 150);
        debugImg(debug, tempPath, "canny", dst);
    }


    /**
     * 对图像进行二值化。将灰度图像（每个像素点有256个取值可能， 0代表黑色，255代表白色）
     * 转化为二值图像（每个像素点仅有1和0两个取值可能）
     * @param inMat
     * @param debug
     * @param tempPath
     * @return
     */
    public static void threshold(Mat inMat, Mat dst, Boolean debug, String tempPath) {
        Imgproc.threshold(inMat, dst, 100, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);
        debugImg(debug, tempPath, "threshold", dst);
        inMat.release();
    }


    /**
     * 闭操作
     * 对图像进行闭操作以后，可以看到车牌区域被连接成一个矩形的区域
     * @param inMat  二值图像
     * @param debug
     * @param tempPath
     * @return
     */
    public static int DEFAULT_MORPH_SIZE_WIDTH = 10;
    public static int DEFAULT_MORPH_SIZE_HEIGHT = 10; // 大于1
    public static Mat morphologyClose(Mat inMat, Mat dst, Boolean debug, String tempPath) {
        Size size = new Size(DEFAULT_MORPH_SIZE_WIDTH, DEFAULT_MORPH_SIZE_HEIGHT);
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size);
        Imgproc.morphologyEx(inMat, dst, Imgproc.MORPH_CLOSE, kernel);
        debugImg(debug, tempPath, "morphologyClose", dst);
        return dst;
    }


    /**
     * 开操作
     * 干掉一些细小的白点
     * @param inMat 二值图像
     * @param debug
     * @param tempPath
     * @return
     */
    public static Mat morphologyOpen(Mat inMat, Mat dst, Boolean debug, String tempPath) {
        Size size = new Size(2, 2);
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size);
        Imgproc.morphologyEx(inMat, dst, Imgproc.MORPH_OPEN, kernel);
        debugImg(debug, tempPath, "morphologyOpen", dst);
        return dst;
    }



    /**
     * 提取外部轮廓
     * 这个算法会把全图的轮廓都计算出来，因此要进行筛选。
     * @param src 原图
     * @param inMat morphology Mat
     * @param debug
     * @param tempPath
     * @return
     */
    public static List<MatOfPoint> contours(Mat src, Mat inMat, Boolean debug, String tempPath) {
        List<MatOfPoint> contours = Lists.newArrayList();
        Mat hierarchy = new Mat();
        Point offset = new Point(0, 0); // 偏移量
        /*if(inMat.width() > 600) {
            offset = new Point(-5, -10); // 偏移量 // 对应sobel的偏移量
        }*/
        // RETR_EXTERNAL只检测最外围轮廓， // RETR_LIST   检测所有的轮廓
        // CHAIN_APPROX_NONE 保存物体边界上所有连续的轮廓点到contours向量内
        Imgproc.findContours(inMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE, offset);
        if (debug) {
            Mat result = new Mat();
            src.copyTo(result); //  复制一张图，不在原图上进行操作，防止后续需要使用原图
            // 将轮廓用红色描绘到原图
            Imgproc.drawContours(result, contours, -1, new Scalar(0, 0, 255, 255));
            // 输出带轮廓的原图
            debugImg(debug, tempPath, "contours", result);
        }
        return contours;
    }


    /**
     * 根据轮廓， 筛选出可能是车牌的图块
     * @param src
     * @param matVector
     * @param debug
     * @param tempPath
     * @return
     */
    public static Vector<Mat> screenBlock(Mat src, List<MatOfPoint> contours, Boolean isGreen,Boolean debug, String tempPath){
        Vector<Mat> dst = new Vector<Mat>();

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint m1 = contours.get(i);
            MatOfPoint2f m2 = new MatOfPoint2f(m1.toArray());

            // RotatedRect 该类表示平面上的旋转矩形，有三个属性： 矩形中心点(质心); 边长(长和宽); 旋转角度
            // boundingRect()得到包覆此轮廓的最小正矩形， minAreaRect()得到包覆轮廓的最小斜矩形
            RotatedRect mr = Imgproc.minAreaRect(m2);

            // 以图片左上角为原点，上边为x轴建立坐标系；
            // x轴逆时针旋转，首次平行的边为mr.size.width，x轴跟该条边组成的角度，即angle，  角度取值范围：[-90° ~ 0°]
            double angle = mr.angle;
            if (checkPlateSize(mr)) {  //排除不合法的图块
                if (debug) {
                    Mat result = src.clone();
                    // 将斜矩形轮廓描绘到原图
                    drawRectangle(result, mr);
                    // 将轮廓描绘到原图
                    Imgproc.drawContours(result, Lists.newArrayList(m1), -1, new Scalar(0, 0, 255, 255));
                    // 输出带轮廓的原图
                    debugImg(debug, tempPath, "crop", result);
                }

                Size rect_size = new Size((int) mr.size.width, (int) mr.size.height);
                if (mr.size.width < mr.size.height) {
                    angle = 90 + angle; // 处理车牌相对水平位置，旋转角度不超过90°的图片，超过之后，车牌相当于倒置，不予处理
                    rect_size = new Size(rect_size.height, rect_size.width);
                }
                /*System.err.println("外接矩形倾斜角度：" +  mr.angle);
                System.err.println("校正角度：" +  angle);*/

                // 旋转角度，根据需要是否进行角度旋转
                Mat img_rotated = new Mat();
                Mat rotmat = Imgproc.getRotationMatrix2D(mr.center, angle, 1); // 旋转对象；angle>0则 逆时针
                Imgproc.warpAffine(src, img_rotated, rotmat, src.size()); // 仿射变换  对原图进行旋转校正 // 处理倾斜的图片
                debugImg(debug, tempPath, "img_rotated", img_rotated);

                // 如果相机在车牌正前方，拍摄角度较小，不管相机是否保持水平，使用仿射变换，减少照片倾斜影响即可
                // 如果相机在车牌的左前、右前、上方等，拍摄角度较大时，则需要考虑使用投影变换
                // 仿射变换  对原图进行错切校正
                // 轮廓的提取，直接影响校正的效果
                // Mat shear = img_rotated.clone();
                // rect_size = shearCorrection(m2, mr, img_rotated, shear, rect_size, debug, tempPath);

                // 切图   按给定的尺寸、给定的中心点
                Mat img_crop = new Mat();

                if(isGreen) {
                    // 如果是新能源牌照，需要向上扩展一定的尺寸--未完成yuxue
                    Size s = new Size(rect_size.width, rect_size.height + (rect_size.height/8));
                    Point c = new Point(mr.center.x, mr.center.y - (rect_size.height/16) -8);   // 偏移量修正
                    Imgproc.getRectSubPix(img_rotated, s, c, img_crop);
                } else {
                    Point c = new Point(mr.center.x, mr.center.y -4);   // 偏移量修正
                    Imgproc.getRectSubPix(img_rotated, rect_size, c, img_crop);
                }

                // 处理切图，调整为指定大小
                Mat resized = new Mat(Constant.DEFAULT_HEIGHT, Constant.DEFAULT_WIDTH, CvType.CV_8UC3);
                Imgproc.resize(img_crop, resized, resized.size(), 0, 0, Imgproc.INTER_CUBIC); // INTER_AREA 缩小图像的时候使用 ; INTER_CUBIC 放大图像的时候使用
                debugImg(debug, tempPath, "crop_resize", resized);
                dst.add(resized);
            }
        }
        return  dst;
    }



    /**
     * 外接斜矩形 描绘到原图
     * @param inMat
     * @param dst
     */
    public static void drawRectangle(Mat inMat, RotatedRect mr) {
        Mat points = new Mat();
        Imgproc.boxPoints(mr, points);
        Scalar scalar = new Scalar(0, 255, 0, 255); //绿色
        if(points.rows() == 4) {
            Point start = new Point(points.get(0, 0)[0], points.get(0, 1)[0]);
            Point end = new Point(points.get(1, 0)[0], points.get(1, 1)[0]);
            Imgproc.line(inMat, start, end, scalar);

            start = new Point(points.get(1, 0)[0], points.get(1, 1)[0]);
            end = new Point(points.get(2, 0)[0], points.get(2, 1)[0]);
            Imgproc.line(inMat, start, end, scalar);

            start = new Point(points.get(2, 0)[0], points.get(2, 1)[0]);
            end = new Point(points.get(3, 0)[0], points.get(3, 1)[0]);
            Imgproc.line(inMat, start, end, scalar);

            start = new Point(points.get(3, 0)[0], points.get(3, 1)[0]);
            end = new Point(points.get(0, 0)[0], points.get(0, 1)[0]);
            Imgproc.line(inMat, start, end, scalar);
        }
    }


    /**
     *
     * @param inMat
     * @param dst
     * @param rect 正矩形
     */
    public static void drawRectangle(Mat inMat, Rect rect) {
        Imgproc.rectangle(inMat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0));
    }


    /**
     * 图块错切校正
     * 根据轮廓、以及最小斜矩形矩形错切校正，用于处理变形(不是倾斜)的车牌图片  即【平行四边形】的车牌校正为【长方形】
     * 该算法，容易受到到轮廓的影响，要求轮廓定位得比较精确
     * 其他方案:
     *  1、在处理字符的时候，进行错切校正，
     *      a、根据字符的外接矩形倾斜角度来校正字符 --已经实现
     *      b、计算字符垂直方向投影，分别左右倾斜一定角度，获取最优错切校正像素值(推荐)--已经实现
     *  2、在训练字符识别模型的时候，加入错切的样本数据
     * @param m2 轮廓
     * @param mr 包覆轮廓的最小斜矩形
     * @param inMat 原图
     * @param shear 错切校正后的图
     * @param rect_size 斜矩形的size
     * @param debug
     * @param tempPath
     * @return
     */
    @SuppressWarnings("unused")
    private static Size shearCorrection(MatOfPoint2f m2, RotatedRect mr, Mat inMat, Mat dst, Size rect_size, Boolean debug, String tempPath){
        Mat vertex = new Mat();
        Imgproc.boxPoints(mr, vertex);  // 最小外接矩形，四个顶点 Mat(4, 2)
        // 提取短边的两个顶点， 命名为上、下顶点
        Point p0 = new Point(vertex.get(0, 0)[0], vertex.get(0, 1)[0]);
        Point p1 = new Point(vertex.get(1, 0)[0], vertex.get(1, 1)[0]);
        Point p2 = new Point(vertex.get(2, 0)[0], vertex.get(2, 1)[0]);
        Point p3 = new Point(vertex.get(3, 0)[0], vertex.get(3, 1)[0]);
        Point[] shortLine0 = {p0, p1}; // 短边
        Point[] shortLine1 = {p2, p3}; // 短边
        Point[] longLine0 = {p0, p3}; // 长边
        Point[] longLine1 = {p2, p1}; // 长边
        if(getDistance(p0, p1) > getDistance(p0, p3)) {
            shortLine0[1] = p3;
            shortLine1[1] = p1;
            longLine0[1] = p1;
            longLine1[1] = p3;
        }

        Point[] leftShortLine = shortLine0;
        if(shortLine0[0].x + shortLine0[1].x > shortLine1[0].x + shortLine1[1].x ) { // 只要有错切，就一定不相等
            leftShortLine = shortLine1;
        }

        double height = mr.size.height;
        double width = mr.size.width;
        if(width < height) {
            height = mr.size.width;
            width = mr.size.height;
        }
        // 最小外接矩形，两条长的边，一定是跟轮廓的长的边保持平行的
        // 根据轮廓计算校正像素值； 错切像素取值范围：[5,30]以内，否则不予处理，防止动作较大，影响结果
        // 计算，轮廓里面，离短边最近的点，获取其距离短边的距离
        List<Point> points = m2.toList();
        if(null == points || points.size() <= 0) {
            return rect_size;
        }

        List<Point> result = Lists.newArrayList(); // 按坐标有序
        double distanceSum = 0;
        // 遍历，获取离短边最近轮廓短边的点集合; 计算错切值
        for (Point p : points) {
            // 排除离两条长边较近的点
            if(getDistance(p, longLine0[0], longLine0[1]) <= height/5) {
                continue;
            }
            if(getDistance(p, longLine1[0], longLine1[1]) <= height/5) {
                continue;
            }
            // 提取剩下点中，离左短边较近的点
            double distance = getDistance(p, leftShortLine[0], leftShortLine[1]);
            if(distance <= width/4) {
                result.add(p);
                distanceSum += distance;
            }
        }
        // 计算到的错切值，大于0，则上边线向右，下边线向左拉伸； 小于0，则上边线向左，下边线向右拉伸
        double shearPX = 2 * distanceSum / result.size();
        if( Constant.DEFAULT_MIN_SHEAR_PX > shearPX || shearPX > Constant.DEFAULT_MAX_SHEAR_PX) {
            return rect_size;
        }
        // 距离跟坐标排序，判断需要错切的方向
        // 任取两个点得到一条直线，得到跟短边的交点，
        // 交点距up点较近，则向右拉伸，交点距离down点较近，则向左拉伸
        Point up = leftShortLine[0];
        Point down = leftShortLine[1];
        if(up.y > down.y) {
            up = leftShortLine[1];
            down = leftShortLine[0];
        }

        Integer c1 = 0, c2 = 0; // c1>c2,则向右拉伸
        for (int i = 0; i < result.size() / 2; i++) {
            Point s = result.get(i);
            for (int j = result.size() / 2; j < result.size(); j++) {
                Point e = result.get(j);
                if(getDistance(up, s, e) < getDistance(down, s, e) ) {
                    c1++;
                } else {
                    c2++;
                }
            }
        }
        // 错切校正之后，要减掉校正的像素值；校正前轮廓外接矩形较大，校正后，需要调整矩形的width
        if(rect_size.width - shearPX > 0) {
            rect_size = new Size(rect_size.width - shearPX, rect_size.height);
        }
        if(c1<c2) {
            shearPX = -shearPX;
        }
        // System.err.println("错切方向： " + shearPX);
        // 计算错切比例
        double top = shearPX / height *  down.y;
        double bottom = shearPX / height * (inMat.height() - up.y);

        // 提取图片左上、左下、右上 三个顶点，根据角度，计算偏移量
        MatOfPoint2f srcPoints = new MatOfPoint2f();
        srcPoints.fromArray(new Point(0, 0), new Point(0, inMat.rows()), new Point(inMat.cols(), 0));
        MatOfPoint2f dstPoints = new MatOfPoint2f();
        dstPoints.fromArray(new Point(0 + top, 0), new Point(0 - bottom, inMat.rows()), new Point(inMat.cols() + top, 0));

        Mat trans_mat = Imgproc.getAffineTransform(srcPoints, dstPoints);
        Imgproc.warpAffine(inMat, dst, trans_mat, inMat.size()); // 对整张图进行错切校正

        debugImg(debug, tempPath, "shearCorrection", dst);
        return rect_size;
    }

    /**
     * 错切校正
     * @param inMat
     * @param dst
     * @param px  校正像素值   >0上边向右  <0上边向左
     * @param debug
     * @param tempPath
     */
    public static void shearCorrection(Mat inMat, Mat dst, Integer px, Boolean debug, String tempPath){
        MatOfPoint2f srcPoints = new MatOfPoint2f();
        srcPoints.fromArray(new Point(0, 0), new Point(0, inMat.rows()), new Point(inMat.cols(), 0));
        MatOfPoint2f dstPoints = new MatOfPoint2f();
        dstPoints.fromArray(new Point(0 + px/2.0, 0), new Point(0 - px/2.0, inMat.rows()), new Point(inMat.cols() + px/2.0, 0));
        Mat trans_mat = Imgproc.getAffineTransform(srcPoints, dstPoints);
        Imgproc.warpAffine(inMat, dst, trans_mat, inMat.size());
        ImageUtil.debugImg(debug, tempPath, "shear", dst);
    }

    /**
     * 投影变换 举例
     * @param inMat
     * @param dst
     * @param debug
     * @param tempPath
     */
    public static void warpPerspective(Mat inMat, Mat dst, Boolean debug, String tempPath){
        // 原图四个顶点
        MatOfPoint2f srcPoints = new MatOfPoint2f();
        srcPoints.fromArray(new Point(0, 0), new Point(0, inMat.rows()), new Point(inMat.cols(), 0), new Point(inMat.cols(), inMat.rows()));
        // 目标图四个顶点
        MatOfPoint2f dstPoints = new MatOfPoint2f();
        dstPoints.fromArray(new Point(0 + 80, 0), new Point(0 - 80, inMat.rows()), new Point(inMat.cols() + 80, 0) , new Point(inMat.cols() - 80, inMat.rows()));

        Mat trans_mat  = Imgproc.getPerspectiveTransform(srcPoints, dstPoints);
        Imgproc.warpPerspective(inMat, dst, trans_mat, inMat.size());
        ImageUtil.debugImg(debug, tempPath, "warpPerspective", dst);
    }


    /**
     * 计算两个点之间的距离
     * @param p1
     * @param p2
     * @return
     */
    public static double getDistance(Point p1, Point p2) {
        double distance = 0;
        distance = Math.pow((p1.x - p2.x), 2) + Math.pow((p1.y - p2.y), 2);
        distance = Math.sqrt(distance);
        return distance;
    }

    /**
     * 计算两条线段之间的距离; 两条线段近乎平行，夹角<=5°
     * @param l1
     * @param l2
     * @return 线段的4个端点，到其近似平行线的垂直距离最小值
     */
    public static double getMinDistance(Line l1, Line l2) {
        double distance0 = getDistance(l1.getStart(), l2.getStart(), l2.getEnd());
        double distance1 = getDistance(l1.getEnd(), l2.getStart(), l2.getEnd());;
        double distance2 = getDistance(l2.getEnd(), l1.getStart(), l1.getEnd());;
        double distance3 = getDistance(l2.getEnd(), l1.getStart(), l1.getEnd());;
        double distance = 10000;
        if(distance > distance0){
            distance = distance0;
        }
        if(distance > distance1){
            distance = distance1;
        }
        if(distance > distance2){
            distance = distance2;
        }
        if(distance > distance3){
            distance = distance3;
        }
        return distance;
    }


    /**
     * 计算点到AB点连线的距离
     * 即，计算点到线的垂直距离
     * @param p
     * @param a
     * @param b
     * @return
     */
    public static double getDistance(Point p, Point a, Point b) {
        double distance = 0, A = 0, B = 0, C = 0;
        A = a.y - b.y;
        B = b.x - a.x;
        C = a.x * b.y - a.y * b.x;
        // 代入点到直线距离公式
        distance = (Math.abs(A * p.x + B * p.y + C)) / (Math.sqrt(A * A + B * B));
        return distance;
    }


    /**
     * 计算两条线段的角度
     * 返回值包含正负数
     * @param k1 斜率
     * @param k2 斜率
     * @return
     */
    public static double getAngle(double k1, double k2) {
        double k = (k2 - k1) / (1 + k1*k2);
        return Math.toDegrees(Math.atan(k));
    }


    /**
     * 计算两条线的交点
     * @param a
     * @param b
     * @return
     */
    public static Point getCrossPoint(Line a, Line b) {
        double ka = a.getK();
        double kb = b.getK();
        double x = (ka*a.getStart().x - a.getStart().y - kb*b.getStart().x + b.getStart().y) / (ka - kb);
        double y = (ka*kb*(a.getStart().x - b.getStart().x) + ka*b.getStart().y - kb*a.getStart().y) / (ka - kb);
        return new Point(x, y);
    }


    /**
     * 根据一个点、直线的斜率、距离，计算另外一个点的坐标
     * @param p 已知点
     * @param distance 目标点跟已知点的距离
     * @param a 目标点跟已知点所在直线的斜率
     * @return
     */
    public static Point getDestPoint(Point p, Double distance, Double a) {
        // y = ax + b 表示直线； 计算b的值
        double b = p.y - a * p.x;
        // 计算直线跟x轴的交点 0 = ax +b
        Point c = new Point(- b / a, 0);
        // 计算p点跟c点的距离
        double dis = getDistance(p, c);
        // 根据三个点之间的距离比例，计算目标点的坐标
        Point dest = new Point((p.x - c.x) * (dis-distance) / dis, p.y * (dis-distance) / dis);
        return dest;
    }



    /**
     * 对minAreaRect获得的最小外接矩形
     * 判断面积以及宽高比是否在制定的范围内
     * 黄牌、蓝牌、绿牌
     * 国内车牌大小: 440mm*140mm，宽高比 3.142857
     * @param mr
     * @return
     */
    private static boolean checkPlateSize(RotatedRect mr) {
        // 切图面积取值范围
        int min = Constant.DEFAULT_MIN_SIZE;
        int max = Constant.DEFAULT_MAX_SIZE;
        // 计算切图面积
        int area = (int) (mr.size.height * mr.size.width);
        // 计算切图宽高比
        double r = mr.size.width / mr.size.height;
        if (r < 1) {  // 特殊情况下，获取到的width  height 值是相反的
            r = mr.size.height / mr.size.width;
        }
        return min <= area && area <= max && Constant.DEFAULT_MIN_RATIO <= r && r <= Constant.DEFAULT_MAX_RATIO;
    }


    /**
     * 进行膨胀操作
     * 也可以理解为字体加粗操作
     * @param inMat 二值图像
     * @return
     */
    public static Mat dilate(Mat inMat, Boolean debug, String tempPath, int row, int col, Boolean correct) {
        Mat result = inMat.clone();
        // 返回指定形状和尺寸的结构元素  矩形：MORPH_RECT;交叉形：MORPH_CROSS; 椭圆形：MORPH_ELLIPSE
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(row, col));
        Imgproc.dilate(inMat, result, element);

        // 先腐蚀 后扩张，会存在一定的偏移； 这里校正偏移量
        if(correct) {
            Mat transformMat = Mat.eye(2, 3, CvType.CV_32F);
            transformMat.put(0, 2, -col/2);
            transformMat.put(1, 2, -row/2);
            Imgproc.warpAffine(result, result, transformMat, inMat.size());
        }
        debugImg(debug, tempPath, "dilate", result);
        return result;
    }

    /**
     * 边缘腐蚀，
     * @param inMat 二值图像
     * @return
     */
    public static Mat erode(Mat inMat, Boolean debug, String tempPath, int row, int col) {
        Mat result = inMat.clone();
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(row, col));
        Imgproc.erode(inMat, result, element);
        debugImg(debug, tempPath, "erode", result);
        return result;
    }



    /**
     * 直方图均衡化   用于提高图像的质量
     * 直方图均衡化是一种常见的增强图像对比度的方法，使用该方法可以增强局部图像的对比度，尤其在数据较为相似的图像中作用更加明显
     * 直方图是图像中像素强度分布的图形表达方式.
     * 它统计了每一个强度值所具有的像素个数
     * https://blog.csdn.net/zqx951102/article/details/84201936
     * @param inMat
     * @param debug
     * @param tempPath
     * @return
     */
    public static Mat equalizeHist(Mat inMat, Boolean debug, String tempPath) {
        Mat dst = new Mat();
        // 灰度图均衡化
        // Imgproc.cvtColor(inMat, dst, Imgproc.COLOR_BGR2GREY);
        // Imgproc.equalizeHist(inMat, dst);

        // 转到HSV空间进行均衡化     (H色相     S饱和度     V亮度)
        Imgproc.cvtColor(inMat, dst, Imgproc.COLOR_BGR2HSV);
        List<Mat> hsvSplit = Lists.newArrayList();
        Core.split(dst, hsvSplit); // 通道分离  三个通道是反过来的  0:V 1:S 2:H
        Imgproc.equalizeHist(hsvSplit.get(2), hsvSplit.get(2)); // 对H(亮度)空间进行均衡化
        Core.merge(hsvSplit, dst);
        debugImg(debug, tempPath, "equalizeHist", dst);
        return dst;
    }

    /**
     * 直方图均衡化
     * @param inMat
     * @param dst
     * @param debug
     * @param tempPath
     * @return
     */
    public static Mat equalizeHist1(Mat inMat, Mat dst,Boolean debug, String tempPath) {
        List<Mat> matList = Lists.newArrayList();
        Core.split(inMat, matList);
        for (Mat mat : matList) {
            Imgproc.equalizeHist(mat, mat);
        }
        Core.merge(matList, dst);
        debugImg(debug, tempPath, "equalizeHist", dst);
        return dst;
    }




    /**
     * HSV色彩空间过滤  返回过滤后的hsvMat
     * @param inMat rgb图像
     * @param debug
     * @param tempPath
     * @param hsv 变长参数，依次为： minH,maxH,minS,maxS,minV,maxV
     * @return 返回过滤后的hsvMat; 不满足range的像素点，替换为黑色
     */
    public static Mat hsvFilter(Mat inMat, Boolean debug, String tempPath, Integer...range) {
        Mat hsvMat = new Mat();    // 转换为hsv图像
        Imgproc.cvtColor(inMat, hsvMat, Imgproc.COLOR_BGR2HSV);
        Mat dst = hsvMat.clone();
        // 从数据库中读取配置参数
        for (int i = 0; i < hsvMat.rows(); i++) {
            for (int j = 0; j < hsvMat.cols(); j++) {
                double[] hsv = hsvMat.get(i, j);
                Integer h = (int) hsv[0];
                Integer s = (int) hsv[1];
                Integer v = (int) hsv[2];
                Integer c = 0;
                if (range.length >= 1 && range[0] <= h) {
                    c++;
                }
                if (range.length >= 2 && h <= range[1]) {
                    c++;
                }
                if (range.length >= 3 && range[2] <= s) {
                    c++;
                }
                if (range.length >= 4 && s <= range[3]) {
                    c++;
                }
                if (range.length >= 5 && range[4] <= v) {
                    c++;
                }
                if (range.length >= 6 && v <= range[5]) {
                    c++;
                }
                if (c == range.length) {   // 所有条件都满足，不处理
                    continue;
                } else {
                    hsv[0] = 0.0;
                    hsv[1] = 0.0;
                    hsv[2] = 0.0;   // 黑色
                    dst.put(i, j, hsv);
                }
            }
        }
        debugImg(debug, tempPath, "hsvFilter", dst);
        return dst;
    }


    /**
     * HSV色彩空间过滤; 返回二值图像
     * @param inMat rgb图像
     * @param debug
     * @param tempPath
     * @param hsv 变长参数，依次为： minH,maxH,minS,maxS,minV,maxV
     * @return 返回二值图像
     */
    public static Mat hsvThreshold(Mat inMat, Boolean debug, String tempPath, Integer...range) {
        Mat hsvMat = new Mat();    // 转换为hsv图像
        Imgproc.cvtColor(inMat, hsvMat, Imgproc.COLOR_BGR2HSV);
        Mat threshold = new Mat(hsvMat.size(), hsvMat.type());
        for (int i = 0; i < hsvMat.rows(); i++) {
            for (int j = 0; j < hsvMat.cols(); j++) {
                double[] hsv = hsvMat.get(i, j);
                Integer h = (int) hsv[0];
                Integer s = (int) hsv[1];
                Integer v = (int) hsv[2];
                Integer c = 0;
                if (range.length >= 1 && range[0] <= h) {
                    c++;
                }
                if (range.length >= 2 && h <= range[1]) {
                    c++;
                }
                if (range.length >= 3 && range[2] <= s) {
                    c++;
                }
                if (range.length >= 4 && s <= range[3]) {
                    c++;
                }
                if (range.length >= 5 && range[4] <= v) {
                    c++;
                }
                if (range.length >= 6 && v <= range[5]) {
                    c++;
                }
                if (c == range.length) {   // 所有条件都满足，不处理
                    hsv[0] = 255.0;
                    hsv[1] = 255.0;
                    hsv[2] = 255.0; // 白色
                } else {
                    hsv[0] = 0.0;
                    hsv[1] = 0.0;
                    hsv[2] = 0.0;   // 黑色 二值算法
                }
                threshold.put(i, j, hsv);
            }
        }
        debugImg(debug, tempPath, "hsvThreshold", threshold);
        return threshold;
    }




    /**
     * 缩小图片，锁定横纵比
     * 防止图片像素太大，后续的计算太费时
     * 但是这样处理之后，图片可能会失真，影响车牌文字识别效果
     * 可以考虑，定位出车牌位置之后，计算出原图的车牌位置，从原图中区图块进行车牌文字识别
     * @param inMat
     * @param maxRows
     * @return
     */
    public static Mat narrow(Mat inMat, Integer maxCols, Boolean debug, String tempPath) {
        if(null == maxCols || maxCols <= 0) {
            maxCols = 600;
        }
        if(maxCols >= inMat.cols()) {   // 图片尺寸小于指定大小，则不处理
            return inMat;
        }
        float r = inMat.rows() * 1.0f / inMat.cols();
        Integer rows = Math.round(maxCols * r);
        Mat resized = new Mat(rows, maxCols, inMat.type());

        // INTER_AREA 缩小图像的时候使用 // INTER_CUBIC 放大图像的时候使用
        double fx = (double)resized.cols()/inMat.cols(); // 水平缩放比例，输入为0时，则默认当前计算方式
        double fy = (double)resized.rows()/inMat.rows(); // 垂直缩放比例，输入为0时，则默认当前计算方式
        Imgproc.resize(inMat, resized, resized.size(), fx, fy, Imgproc.INTER_LINEAR);
        // debugImg(debug, tempPath, "narrow", resized); // 不再生成debug图片
        return resized;
    }


    /**
     * 放大图片尺寸
     * 放大二值图像到原始图片的尺寸，然后提取轮廓，再从原图裁剪图块
     * 防止直接在缩放后的图片上提取图块，因图片变形导致图块识别结果异常
     * @param inMat
     * @param size
     * @param debug
     * @param tempPath
     * @return
     */
    public static void enlarge(Mat inMat, Mat dst, Size size, Boolean debug, String tempPath) {
        if(inMat.width() >= size.width) {
            dst = inMat;
            return;
        }
        Imgproc.resize(inMat, dst, size, 0, 0, Imgproc.INTER_CUBIC);
        debugImg(debug, tempPath, "enlarge", dst);
    }


    /**
     * 按最大宽度，计算放大/缩小比例
     * 锁定纵横比
     * @param inMat
     * @param dst
     * @param maxWidth
     * @param debug
     * @param tempPath
     */
    public static Mat zoom(Mat inMat, Integer maxWidth, Boolean debug, String tempPath){
        Double ratio = maxWidth * 1.0 / inMat.width();
        Integer maxHeight = (int)Math.round(ratio * inMat.height());
        Mat dst = new Mat(maxHeight, maxWidth, inMat.type());
        zoom(inMat, dst, ratio, ratio, debug, tempPath);
        return dst;
    }


    /**
     * 放大、缩小
     * 不锁定纵横比
     * @param inMat
     * @param dst
     * @param x 水平方向变换比例
     * @param y 垂直方向变换比例
     */
    public static void zoom(Mat inMat, Mat dst, Double x, Double y, Boolean debug, String tempPath){
        Mat trans_mat = Mat.zeros(2, 3, CvType.CV_32FC1);
        trans_mat.put(0, 0, x);
        trans_mat.put(1, 1, y);
        Imgproc.warpAffine(inMat, dst, trans_mat, dst.size()); // 仿射变换
        debugImg(debug, tempPath, "zoom", dst);
    }


    /**
     * 平移
     * @param img
     * @param offsetx
     * @param offsety
     * @return
     */
    public static void translateImg(Mat inMat, Mat dst, int offsetx, int offsety){
        //定义平移矩阵
        Mat trans_mat = Mat.zeros(2, 3, CvType.CV_32FC1);
        trans_mat.put(0, 0, 1);
        trans_mat.put(0, 2, offsetx);
        trans_mat.put(1, 1, 1);
        trans_mat.put(1, 2, offsety);
        Imgproc.warpAffine(inMat, dst, trans_mat, inMat.size());
    }


    /**
     * 旋转角度，angle>0顺时针旋转
     * @param inMat
     * @param dst
     * @param angle
     * @return
     */
    public static void rotateImg(Mat inMat, Mat dst, double angle, Boolean debug, String tempPath){
        Point src_center = new Point(inMat.cols() / 2.0F, inMat.rows() / 2.0F);
        rotateImg(inMat, dst, angle, src_center, debug, tempPath);
    }


    /**
     * 旋转角度  ，angle>0顺时针旋转
     * @param inMat
     * @param dst
     * @param angle
     * @param center
     * @param debug
     * @param tempPath
     */
    public static void rotateImg(Mat inMat, Mat dst, double angle, Point center, Boolean debug, String tempPath){
        Mat img_rotated = Imgproc.getRotationMatrix2D(center, angle, 1);
        Imgproc.warpAffine(inMat, dst, img_rotated, inMat.size());
        debugImg(debug, tempPath, "img_rotated", img_rotated);
    }


    /**
     * 边缘增强
     * 拉普拉斯算子增强
     * @param inMat
     * @param dst
     * @param debug
     * @param tempPath
     */
    public static void edgeEnhance(Mat inMat, Mat dst, Boolean debug, String tempPath) {
        List<Mat> matList = Lists.newArrayList();
        Core.split(inMat, matList);
        for (Mat mat : matList) {
            Mat sharpMat8U = new Mat();
            Mat sharpMat = new Mat();
            Imgproc.Laplacian(mat, sharpMat, CvType.CV_16S);    // 拉普拉斯算子增强
            sharpMat.convertTo(sharpMat8U, CvType.CV_8U);
            Core.add(mat, sharpMat8U, mat);
        }
        Core.merge(matList, dst);
        debugImg(debug, tempPath, "edgeEnhance", dst);
    }


    /**
     * https://blog.csdn.net/u011276025/article/details/89790190
     * https://blog.csdn.net/qq_29540745/article/details/74681853
     * @param inMat
     * @param dst
     * @param debug
     * @param tempPath
     */
    public static void unevenLightCompensate(Mat grey, Mat dst, Integer blockSize, Boolean debug, String tempPath) {
        double average = Core.mean(grey).val[0];
        Integer rows_new = (int) Math.ceil(grey.rows() * 1.0 / blockSize);
        Integer cols_new = (int) Math.ceil(grey.cols() * 1.0 / blockSize);
        Mat blockImage = Mat.zeros(rows_new, cols_new, CvType.CV_32FC1);

        // 均值算法？？
        for (int i = 0; i < rows_new; i++) {
            for (int j = 0; j < cols_new; j++) {
                int rowmin = i * blockSize;
                int rowmax = (i + 1) * blockSize;
                if (rowmax > grey.rows()) {
                    rowmax = grey.rows();
                }

                int colmin = j * blockSize;
                int colmax = (j + 1) * blockSize;
                if (colmax > grey.cols()) {
                    colmax = grey.cols();
                }

                Mat imageROI = grey.rowRange(rowmin, rowmax);
                double temaver =  Core.mean(imageROI.colRange(colmin, colmax)).val[0];

                blockImage.put(i, j, temaver - average);
            }
        }
        Mat grey2 = new Mat();
        grey.convertTo(grey2, CvType.CV_32FC1);


        Mat blockImage2 = new Mat();
        Imgproc.resize(blockImage, blockImage2, grey.size(), 0, 0, Imgproc.INTER_CUBIC);
        Core.subtract(grey2, blockImage2, dst);
        dst.convertTo(dst, CvType.CV_8UC1);
        debugImg(debug, tempPath, "unevenLightCompensate", dst);

    }


    public static void main(String[] args) {
        String tempPath = Constant.DEFAULT_TEST_DIR;
        String filename = tempPath + "26.jpg";
        File f = new File(filename);
        if(!f.exists()) {
            File f1 = new File(filename.replace("jpg", "png"));
            File f2 = new File(filename.replace("png", "bmp"));
            filename = f1.exists() ? f1.getPath() : f2.getPath();
        }
        Mat inMat = Imgcodecs.imread(filename);
        Mat dst = inMat.clone();
        morphologyOpen(inMat, dst, true, Constant.DEFAULT_TEMP_DIR);
    }



}