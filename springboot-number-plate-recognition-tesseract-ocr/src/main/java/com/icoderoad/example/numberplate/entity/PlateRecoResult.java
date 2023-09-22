package com.icoderoad.example.numberplate.entity;

import org.opencv.core.Rect;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车牌字符识别结果
 */

@Data
@NoArgsConstructor
public class PlateRecoResult {

    /**
     * 字符序列
     */
    private Integer sort;
    
    /**
     * 字符
     */
    private String chars;
    
    /**
     * 识别置信度
     */
    private Double confi;
    
    /**
     * 字符所在轮廓，最小正矩形
     */
    private Rect rect;
    
    
}