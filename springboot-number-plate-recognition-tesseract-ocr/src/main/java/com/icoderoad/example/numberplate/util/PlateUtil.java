package com.icoderoad.example.numberplate.util;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.ANN_MLP;
import org.opencv.ml.SVM;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.icoderoad.example.numberplate.entity.PlateRecoResult;
import com.icoderoad.example.numberplate.enumtype.Direction;
import com.icoderoad.example.numberplate.enumtype.PlateColor;
import com.icoderoad.example.numberplate.enumtype.PlateHSV;
import com.icoderoad.example.numberplate.train.SVMTrain;

import nu.pattern.OpenCV;

/**
 * 车牌处理工具类 车牌切图按字符分割 字符识别
 */
public class PlateUtil {

    private static SVM svm = null;
    // 简单测试了一下，发现绿牌跟蓝牌分开识别，准确率更高
    private static ANN_MLP ann_blue = null;
    private static ANN_MLP ann_green = null;
    private static ANN_MLP ann_cn = null;
    public static final String DEFAULT_SVM_PATH ="model/1602828039163_svm.xml";
    public static final String DEFAULT_ANN_PATH ="model/ann.xml";
    public static final String DEFAULT_ANN_CN_PATH = "model/ann_cn.xml";
    public static final String DEFAULT_ANN_GREEN_PATH = "model/ann_green.xml";
    public static final String plateReg = "([京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领A-Z]{1}[A-Z]{1}(([0-9]{5}[DF])|([DF]([A-HJ-NP-Z0-9])[0-9]{4})))|([京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领A-Z]{1}[A-Z]{1}[A-HJ-NP-Z0-9]{4}[A-HJ-NP-Z0-9挂学警港澳]{1})";

    static {
    	OpenCV.loadLocally();
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        svm = SVM.create();
        ann_blue = ANN_MLP.create();
        ann_green = ANN_MLP.create();
        ann_cn = ANN_MLP.create();
        loadSvmModel(DEFAULT_SVM_PATH);
        loadAnnBlueModel(DEFAULT_ANN_PATH);
        loadAnnGreenModel(DEFAULT_ANN_GREEN_PATH);
        loadAnnCnModel(DEFAULT_ANN_CN_PATH);
    }

    public static void loadSvmModel(String path) {
        svm.clear();
        svm = SVM.load(path);
    }

    public static void loadAnnBlueModel(String path) {
        ann_blue.clear();
        ann_blue = ANN_MLP.load(path);
    }

    public static void loadAnnGreenModel(String path) {
        ann_green.clear();
        ann_green = ANN_MLP.load(path);
    }

    public static void loadAnnCnModel(String path) {
        ann_cn.clear();
        ann_cn = ANN_MLP.load(path);
    }

    /**
     * 根据正则表达式判断字符串是否是车牌
     *
     * @param str
     * @return
     */
    public static Boolean isPlate(String str) {
        Pattern p = Pattern.compile(plateReg);
        Boolean bl = false;
        Matcher m = p.matcher(str);
        while (m.find()) {
            bl = true;
            break;
        }
        return bl;
    }

    /**
     *
     * @param imagePath
     * @param dst
     * @param debug
     * @param tempPath
     * @return
     */
    public static Vector<Mat> findPlateByContours(String imagePath, Vector<Mat> dst, Boolean debug, String tempPath) {
        Mat src = Imgcodecs.imread(imagePath);
        final Mat resized = ImageUtil.narrow(src, 600, debug, tempPath); // 调整大小,加快后续步骤的计算效率
        return findPlateByContours(src, resized, dst, debug, tempPath);
    }

    /**
     * 根据图片，获取可能是车牌的图块集合
     *
     * @param src 输入原图
     * @param inMat 调整尺寸后的图
     * @param dst 可能是车牌的图块集合
     * @param debug 是否保留图片的处理过程
     * @param tempPath 图片处理过程的缓存目录
     */
    public static Vector<Mat> findPlateByContours(Mat src, Mat inMat, Vector<Mat> dst, Boolean debug, String tempPath) {
        // 灰度图
        Mat gray = new Mat();
        ImageUtil.gray(inMat, gray, debug, tempPath);

        // 高斯模糊
        Mat gsMat = new Mat();
        ImageUtil.gaussianBlur(gray, gsMat, debug, tempPath);

        // Sobel 运算，得到图像的一阶水平方向导数
        Mat sobel = new Mat();
        ImageUtil.sobel(gsMat, sobel, debug, tempPath);

        // 图像进行二值化
        Mat threshold = new Mat();
        ImageUtil.threshold(sobel, threshold, debug, tempPath);

        // 使用闭操作 同时处理一些干扰元素
        Mat morphology = threshold.clone();
        ImageUtil.morphologyClose(threshold, morphology, debug, tempPath); // 闭操作

        // 边缘腐蚀，边缘膨胀，可以多执行两次
        morphology = ImageUtil.erode(morphology, debug, tempPath, 4, 4);
        morphology = ImageUtil.dilate(morphology, debug, tempPath, 4, 4, true);

        // 将二值图像，resize到原图的尺寸； 如果使用缩小后的图片提取图块，可能会出现变形，影响后续识别结果
        ImageUtil.enlarge(morphology, morphology, src.size(), debug, tempPath);

        // 获取图中所有的轮廓
        List<MatOfPoint> contours = ImageUtil.contours(src, morphology, debug, tempPath);
        // 根据轮廓， 筛选出可能是车牌的图块
        Vector<Mat> blockMat = ImageUtil.screenBlock(src, contours, false, debug, tempPath);

        // 找出可能是车牌的图块，存到dst中， 返回结果
        hasPlate(blockMat, dst, debug, tempPath);

        return dst;
    }

    /**
     *
     * @param imagePath
     * @param dst
     * @param plateHSV
     * @param debug
     * @param tempPath
     * @return
     */
    public static Vector<Mat> findPlateByHsvFilter(String imagePath, Vector<Mat> dst, PlateHSV plateHSV, Boolean debug, String tempPath) {
        Mat src = Imgcodecs.imread(imagePath);
        final Mat resized = ImageUtil.narrow(src, 600, debug, tempPath); // 调整大小,加快后续步骤的计算效率
        return findPlateByHsvFilter(src, resized, dst, plateHSV, debug, tempPath);
    }

    /**
     *
     * @param src 输入原图
     * @param inMat 调整尺寸后的图
     * @param dst 可能是车牌的图块集合
     * @param debug 是否保留图片的处理过程
     * @param tempPath 图片处理过程的缓存目录
     * @return
     */
    public static Vector<Mat> findPlateByHsvFilter(Mat src, Mat inMat, Vector<Mat> dst, PlateHSV plateHSV, Boolean debug, String tempPath) {
        // hsv取值范围过滤
        Mat hsvMat = ImageUtil.hsvFilter(inMat, debug, tempPath, plateHSV.minH, plateHSV.maxH);
        // 图像均衡化
        Imgproc.cvtColor(hsvMat, hsvMat, Imgproc.COLOR_HSV2BGR);
        Mat equalizeMat = ImageUtil.equalizeHist(hsvMat, debug, tempPath);
        hsvMat.release();

        // 二次hsv过滤，二值化
        Mat threshold = ImageUtil.hsvThreshold(equalizeMat, debug, tempPath, plateHSV.equalizeMinH, plateHSV.equalizeMaxH);
        Mat morphology = threshold.clone();
        ImageUtil.morphologyClose(threshold, morphology, debug, tempPath); // 闭操作
        threshold.release();

        Mat rgb = new Mat();
        Imgproc.cvtColor(morphology, rgb, Imgproc.COLOR_BGR2GRAY);

        // 将二值图像，resize到原图的尺寸； 如果使用缩小后的图片提取图块，可能会出现变形，影响后续识别结果
        ImageUtil.enlarge(rgb, rgb, src.size(), debug, tempPath);
        // 提取轮廓
        List<MatOfPoint> contours = ImageUtil.contours(src, rgb, debug, tempPath);
        // 根据轮廓， 筛选出可能是车牌的图块 // 切图的时候， 处理绿牌，需要往上方扩展一定比例像素
        Vector<Mat> blockMat = ImageUtil.screenBlock(src, contours, plateHSV.equals(PlateHSV.GREEN), debug, tempPath);

        // 找出可能是车牌的图块，存到dst中， 返回结果
        hasPlate(blockMat, dst, debug, tempPath);
        return dst;
    }

    /**
     * 输入车牌切图集合，判断是否包含车牌
     *
     * @param inMat
     * @param dst
     *            包含车牌的图块
     */
    public static void hasPlate(Vector<Mat> inMat, Vector<Mat> dst, Boolean debug, String tempPath) {
        for (Mat src : inMat) {
            if (src.rows() == Constant.DEFAULT_HEIGHT && src.cols() == Constant.DEFAULT_WIDTH) { // 尺寸限制; 已经结果resize了，此处判断一下
                Mat samples = SVMTrain.getFeature(src);
                float flag = svm.predict(samples);
                if (flag == 0) { // 目标符合
                    dst.add(src);
                    ImageUtil.debugImg(true, tempPath, "platePredict", src);
                }
            }
        }
        return;
    }

    /**
     * 判断车牌切图颜色
     *
     * @param inMat
     * @return
     */
    public static PlateColor getPlateColor(Mat inMat, Boolean adaptive_minsv, Boolean debug, String tempPath) {
        // 判断阈值
        final float thresh = 0.5f;
        // 转到HSV空间，对H均衡化之后的结果
        Mat hsvMat = ImageUtil.equalizeHist(inMat, debug, tempPath);

        if (colorMatch(hsvMat, PlateColor.GREEN, adaptive_minsv, debug, tempPath) > thresh) {
            return PlateColor.GREEN;
        }
        if (colorMatch(hsvMat, PlateColor.YELLOW, adaptive_minsv, debug, tempPath) > thresh) {
            return PlateColor.YELLOW;
        }
        if (colorMatch(hsvMat, PlateColor.BLUE, adaptive_minsv, debug, tempPath) > thresh) {
            return PlateColor.BLUE;
        }
        return PlateColor.UNKNOWN;
    }

    /**
     * 颜色匹配计算
     *
     * @param inMat
     * @param r
     * @param adaptive_minsv
     * @param debug
     * @param tempPath
     * @return
     */
    public static Float colorMatch(Mat hsvMat, PlateColor r, Boolean adaptive_minsv, Boolean debug, String tempPath) {
        final float max_sv = 255;
        final float minref_sv = 64;
        final float minabs_sv = 95;

        Integer countTotal = hsvMat.rows() * hsvMat.cols();
        Integer countMatched = 0;

        // 匹配模板基色,切换以查找想要的基色
        int min_h = r.minH;
        int max_h = r.maxH;
        float diff_h = (float) ((max_h - min_h) / 2);
        int avg_h = (int) (min_h + diff_h);

        for (int i = 0; i < hsvMat.rows(); i++) {
            for (int j = 0; j < hsvMat.cols(); j++) {
                int H = (int) hsvMat.get(i, j)[0];
                int S = (int) hsvMat.get(i, j)[1];
                int V = (int) hsvMat.get(i, j)[2];

                boolean colorMatched = false;
                if (min_h < H && H <= max_h) {
                    int Hdiff = Math.abs(H - avg_h);
                    float Hdiff_p = Hdiff / diff_h;
                    float min_sv = 0;
                    if (adaptive_minsv) {
                        min_sv = minref_sv - minref_sv / 2 * (1 - Hdiff_p);
                    } else {
                        min_sv = minabs_sv;
                    }
                    if ((min_sv < S && S <= max_sv) && (min_sv < V && V <= max_sv)) {
                        colorMatched = true;
                    }
                }
                if (colorMatched) {
                    countMatched++;
                }
            }
        }
        return countMatched * 1F / countTotal;
    }

    /**
     * 车牌切图，分割成单个字符切图
     * @param inMat 输入原始图像
     * @param charMat 返回字符切图vector
     * @param debug
     * @param tempPath
     */
    public static String charsSegment(Mat inMat, PlateColor color, Boolean debug, String tempPath) {

        int charCount = 7; // 车牌字符个数
        if (color.equals(PlateColor.GREEN)) {
            charCount = 8;
        }

        // 切换到灰度图
        Mat gray = new Mat();
        Imgproc.cvtColor(inMat, gray, Imgproc.COLOR_BGR2GRAY);
        ImageUtil.gaussianBlur(gray, gray, debug, tempPath);

        // 图像进行二值化 // 图像二值化阈值选取--未完成yuxue
        Mat threshold = new Mat();
        switch (color) {
        case BLUE:
            Imgproc.threshold(gray, threshold, 10, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);
            break;
        default: // GREEN YELLOW
            Imgproc.threshold(gray, threshold, 10, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY_INV);
            break;
        }
        ImageUtil.debugImg(debug, tempPath, "plateThreshold", threshold); // 输出二值图

        // 边缘腐蚀
        threshold = ImageUtil.erode(threshold, debug, tempPath, 2, 2);

        // 垂直方向投影，错切校正 // 理论上，还可以用于分割字符
        Integer px = getShearPx(threshold);
        ImageUtil.shearCorrection(threshold, threshold, px, debug, tempPath);

        // 前面已经结果错切校正了，可以按照垂直、水平方向投影进行精确定位
        // 垂直投影 + 垂直分割线，分割字符 // 水平投影，去掉上下边框、铆钉干扰
        threshold = sepAndClear(threshold, px, charCount, debug, tempPath);

        // 边缘膨胀 // 还原腐蚀操作产生的影响 // 会影响中文字符的精确度
        threshold = ImageUtil.dilate(threshold, debug, tempPath, 2, 2, true);

        // 提取外部轮廓
        List<MatOfPoint> contours = Lists.newArrayList();

        Imgproc.findContours(threshold, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        Vector<Rect> charRect = new Vector<Rect>(); // 字符轮廓集合

        Mat dst = null;
        if (debug) {
            dst = inMat.clone();
            Imgproc.cvtColor(threshold, dst, Imgproc.COLOR_GRAY2BGR);
        }
        for (int i = 0; i < contours.size(); i++) { // 遍历轮廓
            MatOfPoint contour = contours.get(i);
            Rect mr = Imgproc.boundingRect(contour); // 得到包覆此轮廓的最小正矩形
            if (checkCharSizes(mr)) { // 验证尺寸，主要验证高度是否满足要求，去掉不符合规格的字符，中文字符后续处理
                charRect.add(mr);
                if (debug) {
                    ImageUtil.drawRectangle(dst, mr);
                    ImageUtil.debugImg(debug, tempPath, "boundingRect", dst);
                }
            }
        }
        if (null == charRect || charRect.size() <= 0) { // 未识别到字符
            return null;
        }
        // 字符个数不足，要按照分割的区域补齐 // 同时处理中文字符
        // System.out.println("字符个数===>" + charRect.size());

        // 遍历轮廓，修正超高超宽的字符，去掉铆钉的干扰, 并排序
        Vector<Rect> sorted = new Vector<Rect>();
        sortRect(charRect, sorted);

        // 定位省份字母位置
        Integer posi = getSpecificRect(sorted, color);
        Integer prev = posi - 1 <= 0 ? 0 : posi - 1;

        // 定位中文字符 // 中文字符可能不是连续的轮廓，需要特殊处理
        Rect chineseRect = getChineseRect(sorted.get(posi), sorted.get(prev));

        Mat chineseMat = new Mat(threshold, chineseRect);
        chineseMat = preprocessChar(chineseMat);
        ImageUtil.debugImg(debug, tempPath, "chineseMat", chineseMat);

        // 识别字符，计算置信度
        List<PlateRecoResult> result = Lists.newArrayList();

        // 一般来说，中文字符预测比较不准确，所以参考业界的做法，设置默认所在的省份字符，如果置信度较低， 则默认该字符
        PlateRecoResult chinese = new PlateRecoResult();
        chinese.setSort(0);
        chinese.setRect(chineseRect);
        predictChinese(chineseMat, chinese); // 预测中文字符
        result.add(chinese);
        charCount--;

        for (int i = posi; i < sorted.size() && charCount > 0; i++, charCount--) { // 预测中文之外的字符
            Mat img_crop = new Mat(threshold, sorted.get(i));
            img_crop = preprocessChar(img_crop);
            PlateRecoResult chars = new PlateRecoResult();
            chars.setSort(i+1);
            chars.setRect(chineseRect);
            predict(img_crop, color, chars); // 预测数字、字符
            result.add(chars);
            ImageUtil.debugImg(debug, tempPath, "charMat", img_crop);
        }
        String plate = "";  // 车牌识别结果
        Double fonfidence = 0.0D; // 置信度
        for (PlateRecoResult p : result) {
            plate += p.getChars();
            fonfidence += p.getConfi();
        }
        System.out.println(plate + "===>" + fonfidence);
        return plate;
    }

    /**
     *
     * @param threshold
     * @param dst
     * @param px
     * @param sep
     * @param debug
     * @param tempPath
     */
    public static Mat sepAndClear(Mat threshold, Integer px, Integer charCount, Boolean debug, String tempPath) {
        Mat dst = threshold.clone();
        Set<Integer> rows = Sets.newHashSet();
        int ignore = 10;
        // 水平方向投影 // 按rows清除干扰 // 去掉上下边框干扰像素
        // 垂直方向投影; 按cols清楚干扰; 意义不大; 直接分割，提取字符更简单
        for (int i = 0; i < threshold.rows(); i++) {
            int count = Core.countNonZero(threshold.row(i));
            if (count <= 15) {
                rows.add(i);
                if (i < ignore) {
                    for (int j = 0; j < i; j++) {
                        rows.add(j);
                    }
                }
                if (i > threshold.rows() - ignore) {
                    for (int j = i + 1; j < threshold.rows(); j++) {
                        rows.add(j);
                    }
                }
            }
        }

        Integer minY = 0;
        for (int i = 0; i < threshold.rows(); i++) {
            if (rows.contains(i)) {
                if (i <= threshold.rows() / 2) {
                    minY = i;
                }
                for (int j = 0; j < threshold.cols(); j++) {
                    dst.put(i, j, 0);
                }
            }
        }

        threshold.release();
        ImageUtil.debugImg(debug, tempPath, "sepAndClear", dst);

        // 分割字符，返回所有字符的边框，Rect(x, y, width, height)
        // 在这里提取，估计比轮廓提取方式更准确，尤其在提取中文字符方面
        /*Integer height = dst.rows() - rows.size();
        Integer y = minY + 1;   // 修正一个像素
        Integer x = 0;
        Integer width = 0;
        Boolean bl = false; // 是否是全0的列
        Vector<Rect> rects = new Vector<Rect>();    // 提取到的轮廓集合，可用于后续的字符识别，也可用于去除垂直方向的干扰
        for (int i = 0; i < dst.cols(); i++) {
            int count = Core.countNonZero(dst.col(i));
            if(count <= 0) { // 黑色的列; 因为前面就行了边缘腐蚀，这里选择全黑色的列作为分割
                bl = true;
            } else {
                if(bl) {
                    x = i;
                }
                bl =false;
                width++;
            }
            if(bl && width > 0) {   // 切割图块
                Rect r = new Rect(x, y, width, height); // 提取到的轮廓
                // 按轮廓切图
                Mat img_crop = new Mat(dst, r);
                ImageUtil.debugImg(debug, tempPath, "sepAndClear-crop", img_crop);
                rects.add(r);
                width = 0;
            }
        }*/
        return dst;
    }

    /**
     * 基于字符垂直方向投影，计算错切值，用于错切校正 上下均去掉6个像素；
     * 去掉上下边框的干扰 最大处理25px的错切校；
     * 左右去掉25像素，保证每列都能取到完整的数据 相当于实现任意角度的投影计算；
     * @param threshold 二值图像， 0黑色 255白色； 136 * 36
     * @return sep 可以用于字符分割的列
     * @return 错切像素值
     */
    public static Integer getShearPx(Mat threshold) {
        int px = 25; // 最大处理25像素的错切
        int ignore = 6; // 去掉上下边框干扰像素

        int maxCount = 0; // 取count值最大
        int minPx = px; // 取绝对值最小
        Integer result = 0;

        for (int i = -px; i <= px; i++) {
            int colCount = 0; // 计数满足条件的列
            // 计算按像素值倾斜的投影
            for (int j = px; j < threshold.cols() - px; j++) {
                int cellCount = 0; // 计数每列为0的值
                float c = i * 1F / threshold.rows();
                for (int k = ignore; k < threshold.rows() - ignore; k++) {
                    double d = threshold.get(k, Math.round(j + k * c))[0];
                    if (d <= 10) {
                        cellCount++;
                    }
                }
                if (cellCount >= 24) {
                    colCount++;
                }
            }
            // System.out.println(i + "===>" + minPx + "===>" + colCount + "===>" + maxCount);
            if (colCount == maxCount) {
                if (Math.abs(i) <= minPx) {
                    minPx = Math.abs(i);
                    result = i;
                }
            }
            if (colCount > maxCount) {
                maxCount = colCount;
                minPx = Math.abs(i);
                result = i;
            }
        }
        System.err.println("错切校正像素值===>" + result);
        return result;
    }

    /**
     * 根据字符的外接矩形倾斜角度计算错切值，
     * 用于错切校正 提取最小正矩形的时候，同时提取最小外接斜矩形；
     * 计算错切像素值 在切割字符之后，进行预测之前，对每个字符进行校正
     * @param angleRect 字符最小外接矩形 集合
     * @return
     */
    public static Integer getShearPx(Vector<RotatedRect> angleRect) {
        Integer posCount = 0;
        Integer negCount = 0;
        Float posAngle = 0F;
        Float negAngle = 0F;
        for (RotatedRect r : angleRect) {
            if (Math.abs(r.angle) >= 45) { // 向右倾斜 需要向左校正
                posCount++;
                posAngle = posAngle + 90F + (float) r.angle;
            } else {
                negCount++;
                negAngle = negAngle - (float) r.angle;
            }
        }
        Integer px = 0;
        if (posCount > negCount) {
            px = -posAngle.intValue() / posCount / 2;
        } else {
            px = negAngle.intValue() / negCount / 2;
        }
        if (Math.abs(px) > 10) {
            px = px % 10;
        }
        return px;
    }

    /**
     * 预测数字、字母 字符
     *
     * @param img
     * @return
     */
    public static void predict(Mat img, PlateColor color, PlateRecoResult chars) {
        Mat f = PlateUtil.features(img, Constant.predictSize);

        int index = 0;
        Double maxVal = -2D;
        Mat output = new Mat(1, Constant.strCharacters.length, CvType.CV_32F);
        if(color.equals(PlateColor.GREEN)) {
            ann_green.predict(f, output); // 预测结果
        } else {
            ann_blue.predict(f, output); // 预测结果
        }
        for (int j = 0; j < Constant.strCharacters.length; j++) {
            double val = output.get(0, j)[0];
            if (val > maxVal) {
                maxVal = val;
                index = j;
            }
        }
        String result = String.valueOf(Constant.strCharacters[index]);
        chars.setChars(result);
        chars.setConfi(maxVal);
    }

    /**
     * 预测中文字符
     *
     * @param img
     * @return
     */
    public static void predictChinese(Mat img, PlateRecoResult chinese) {
        Mat f = PlateUtil.features(img, Constant.predictSize);
        int index = 0;
        Double maxVal = -2D;

        Mat output = new Mat(1, Constant.strChinese.length, CvType.CV_32F);
        ann_cn.predict(f, output); // 预测结果
        for (int j = 0; j < Constant.strChinese.length; j++) {
            double val = output.get(0, j)[0];
            if (val > maxVal) {
                maxVal = val;
                index = j;
            }
        }
        String result = Constant.strChinese[index];
        chinese.setChars(Constant.KEY_CHINESE_MAP.get(result));
        chinese.setConfi(maxVal);
    }

    /**
     * 找出指示城市的字符的Rect，
     * 例如 苏A7003X，就是A的位置
     * 之所以选择城市的字符位置，是因为该位置不管什么字母，占用的宽度跟高度的差不多，而且字符笔画是连续的，能大大提高位置的准确性
     * @param vecRect
     * @return
     */
    public static Integer getSpecificRect(Vector<Rect> vecRect, PlateColor color) {
        List<Integer> xpositions = Lists.newArrayList();

        int maxHeight = 0;
        int maxWidth = 0;
        for (int i = 0; i < vecRect.size(); i++) {
            xpositions.add(vecRect.get(i).x);
            if (vecRect.get(i).height > maxHeight) {
                maxHeight = vecRect.get(i).height;
            }
            if (vecRect.get(i).width > maxWidth) {
                maxWidth = vecRect.get(i).width;
            }
        }
        int specIndex = 0;
        for (int i = 0; i < vecRect.size(); i++) {
            Rect mr = vecRect.get(i);
            int midx = mr.x + mr.width / 2;

            if (PlateColor.GREEN.equals(color)) {
                if ((mr.width > maxWidth * 0.8 || mr.height > maxHeight * 0.8) && (midx < Constant.DEFAULT_WIDTH * 2 / 8 && midx > Constant.DEFAULT_WIDTH / 8)) {
                    specIndex = i;
                }
            } else {
                // 如果一个字符有一定的大小，并且在整个车牌的1/7到2/7之间，则是我们要找的特殊车牌
                if ((mr.width > maxWidth * 0.8 || mr.height > maxHeight * 0.8) && (midx < Constant.DEFAULT_WIDTH * 2 / 7 && midx > Constant.DEFAULT_WIDTH / 7)) {
                    specIndex = i;
                }
            }
        }
        return specIndex;
    }

    /**
     * 根据特殊车牌来构造猜测中文字符的位置和大小
     *
     * @param rectSpe
     * @return
     */
    public static Rect getChineseRect(Rect rectSpe, Rect rectPrev) {
        int height = rectSpe.height;
        float newwidth = rectSpe.width * 1.15f;
        int x = rectSpe.x;
        int y = rectSpe.y;

        // 判断省份字符前面的位置，是否有宽度符合要求的中文字符
        if (rectPrev.width >= rectSpe.width && rectPrev.x <= rectSpe.x - rectSpe.width) {
            return rectPrev;
        }
        // 如果没有，则按照车牌尺寸来切割
        int newx = x - (int) (newwidth * 1.15);
        newx = Math.max(newx, 0);
        Rect a = new Rect(newx, y, (int) newwidth, height);
        return a;
    }

    /**
     * 字符预处理: 统一每个字符的大小
     *
     * @param in
     * @return
     */
    final static int CHAR_SIZE = 20;

    private static Mat preprocessChar(Mat in) {
        int h = in.rows();
        int w = in.cols();
        // 生成输出对角矩阵(2, 3)
        // 1 0 0
        // 0 1 0
        Mat transformMat = Mat.eye(2, 3, CvType.CV_32F);
        int m = Math.max(w, h);
        transformMat.put(0, 2, (m - w) / 2f);
        transformMat.put(1, 2, (m - h) / 2f);

        Mat warpImage = new Mat(m, m, in.type());
        Imgproc.warpAffine(in, warpImage, transformMat, warpImage.size(), Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, new Scalar(0));
        Mat resized = new Mat(CHAR_SIZE, CHAR_SIZE, CvType.CV_8UC3);
        Imgproc.resize(warpImage, resized, resized.size(), 0, 0, Imgproc.INTER_CUBIC);
        return resized;
    }

    /**
     * 字符尺寸验证；
     * 去掉尺寸不符合的图块 此处计算宽高比意义不大，因为字符 1 的宽高比干扰就已经很大了
     * @param r
     * @return
     */
    public static Boolean checkCharSizes(Rect r) {
        float minHeight = 15f;
        float maxHeight = 35f;
        double charAspect = r.size().width / r.size().height;
        return charAspect < 1 && minHeight <= r.size().height && r.size().height < maxHeight;
    }

    /**
     * 将Rect按位置从左到右进行排序 遍历轮廓，修正超高的字符，去掉铆钉的干扰, 并排序
     *
     * @param vecRect
     * @param out
     * @return
     */
    public static void sortRect(Vector<Rect> vecRect, Vector<Rect> out) {
        Map<Integer, Integer> map = Maps.newHashMap();
        Integer avgY = 0; // 所有字符的平均起点Y值 // 小于平均值的，削脑袋
        Integer avgHeight = 0; // 所有字符的平均身高 //大于平均值的，剁脚
        Integer avgWidth = 0; // 计算所有大于8像素(去掉【1】字符的干扰)轮廓的均值 // 大于平均值的，进行瘦身操作
        Integer wCount = 0;

        for (int i = 0; i < vecRect.size(); ++i) {
            map.put(vecRect.get(i).x, vecRect.indexOf(vecRect.get(i)));
            avgY += vecRect.get(i).y;
            avgHeight += vecRect.get(i).height;
            if (vecRect.get(i).width >= 10) {
                wCount++;
                avgWidth += vecRect.get(i).width;
            }
        }
        avgY = avgY / vecRect.size();
        avgHeight = avgHeight / vecRect.size();
        avgWidth = avgWidth / wCount;
        Set<Integer> set = map.keySet();
        Object[] arr = set.toArray();
        Arrays.sort(arr);
        for (Object key : arr) {
            Rect r = vecRect.get(map.get(key));
            if (Math.abs(avgY - r.y) >= 2 || Math.abs(r.height - avgHeight) >= 2) {
                r = new Rect(r.x, avgY - 1, r.width, avgHeight); // 身材超高或者超矮的，修正一下
            }
            if (r.width > avgWidth) { // 轮廓偏宽
                r = new Rect(r.x, r.y, avgWidth, r.height); // 瘦身
            }
            out.add(r);
        }
        return;
    }

    public static float[] projectedHistogram(final Mat img, Direction direction) {
        int sz = img.rows();
        if (direction.equals(Direction.VERTICAL)) {
            sz = img.cols();
        }

        // 统计这一行或一列中，非零元素的个数，并保存到nonZeroMat中
        float[] nonZeroMat = new float[sz];
        Core.extractChannel(img, img, 0);
        for (int j = 0; j < sz; j++) {
            Mat data = direction.equals(Direction.VERTICAL) ? img.row(j) : img.col(j);
            int count = Core.countNonZero(data);
            nonZeroMat[j] = count;
        }
        float max = 0;
        for (int j = 0; j < nonZeroMat.length; ++j) {
            max = Math.max(max, nonZeroMat[j]);
        }
        if (max > 0) {
            for (int j = 0; j < nonZeroMat.length; ++j) {
                nonZeroMat[j] /= max;
            }
        }
        return nonZeroMat;
    }

    public static Mat features(Mat in, int sizeData) {
        float[] vhist = projectedHistogram(in, Direction.VERTICAL);
        float[] hhist = projectedHistogram(in, Direction.HORIZONTAL);
        Mat lowData = new Mat();
        if (sizeData > 0) {
            Imgproc.resize(in, lowData, new Size(sizeData, sizeData));
        }
        int numCols = vhist.length + hhist.length + lowData.cols() * lowData.rows();
        Mat out = new Mat(1, numCols, CvType.CV_32F);

        int j = 0;
        for (int i = 0; i < vhist.length; ++i, ++j) {
            out.put(0, j, vhist[i]);
        }
        for (int i = 0; i < hhist.length; ++i, ++j) {
            out.put(0, j, hhist[i]);
        }
        for (int x = 0; x < lowData.cols(); x++) {
            for (int y = 0; y < lowData.rows(); y++, ++j) {
                double[] val = lowData.get(x, y);
                out.put(0, j, val[0]);
            }
        }
        return out;
    }

    /**
     * 根据图片，获取可能是车牌的图块集合 多种方法实现：
     *   1、网上常见的轮廓提取车牌算法
     *   2、hsv色彩分割算法
     *   3、 参考人脸识别算法，实现特征识别算法 --参考 PlateCascadeTrain
     * @param dst 可能是车牌的图块集合
     * @param debug 是否保留图片的处理过程
     * @param tempPath 图片处理过程的缓存目录
     */
    public static Vector<Mat> getPlateMat(String imagePath, Vector<Mat> dst, Boolean debug, String tempPath) {
        Mat src = ImageUtil.imread(imagePath, CvType.CV_8UC3);

        final Mat resized = ImageUtil.narrow(src, 600, debug, tempPath); // 调整大小,加快后续步骤的计算效率

        CompletableFuture<Vector<Mat>> f1 = CompletableFuture.supplyAsync(() -> {
            Vector<Mat> r = findPlateByContours(src, resized, dst, debug, tempPath);
            return r;
        });
        CompletableFuture<Vector<Mat>> f2 = CompletableFuture.supplyAsync(() -> {
            Vector<Mat> r = findPlateByHsvFilter(src, resized, dst, PlateHSV.BLUE, debug, tempPath);
            return r;
        });
        CompletableFuture<Vector<Mat>> f3 = CompletableFuture.supplyAsync(() -> {
            Vector<Mat> r = findPlateByHsvFilter(src, resized, dst, PlateHSV.GREEN, debug, tempPath);
            return r;
        });
        CompletableFuture<Vector<Mat>> f4 = CompletableFuture.supplyAsync(() -> {
            Vector<Mat> r = findPlateByHsvFilter(src, resized, dst, PlateHSV.YELLOW, debug, tempPath);
            return r;
        });
        CompletableFuture<Vector<Mat>> f5 = CompletableFuture.supplyAsync(() -> {
            Vector<Mat> r = new Vector<Mat>(); // 参考人脸识别算法，实现特征识别算法，--未完成； 参考PlateCascadeTrain
            return r;
        });

        // 这里的 join() 将阻塞，直到所有的任务执行结束
        CompletableFuture.allOf(f1, f2, f3, f4, f5).join();
        try {
            Vector<Mat> result = f1.get();
            result.addAll(f2.get());
            result.addAll(f3.get());
            result.addAll(f4.get());
            result.addAll(f5.get());
            return result;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        Instant start = Instant.now();

        String tempPath = Constant.DEFAULT_TEMP_DIR;
        String filename = Constant.DEFAULT_TEMP_DIR + "33.png";
        File f = new File(filename);
        if (!f.exists()) {
            File f1 = new File(filename.replace("jpg", "png"));
            File f2 = new File(filename.replace("png", "bmp"));
            filename = f1.exists() ? f1.getPath() : f2.getPath();
        }

        Boolean debug = true;
        Vector<Mat> dst = new Vector<Mat>();
        // 提取车牌图块
        // getPlateMat(filename, dst, debug, tempPath);
        // findPlateByContours(filename, dst, debug, tempPath);
        // findPlateByHsvFilter(filename, dst, PlateHSV.BLUE, debug, tempPath);
        findPlateByHsvFilter(filename, dst, PlateHSV.BLUE, debug, tempPath);

        Set<String> result = Sets.newHashSet();
        dst.stream().forEach(inMat -> {
            // 识别车牌颜色
            PlateColor color = PlateUtil.getPlateColor(inMat, true, debug, tempPath);
            // 识别车牌字符
            String plateNo = PlateUtil.charsSegment(inMat, color, debug, tempPath);
            result.add(plateNo + "\t" + color.desc);
        });
        System.out.println(result.toString());

        Instant end = Instant.now();
        System.err.println("总耗时：" + Duration.between(start, end).toMillis());
    }

}