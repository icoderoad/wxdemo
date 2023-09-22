package com.icoderoad.example.numberplate.entity;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import com.icoderoad.example.numberplate.util.GenerateIdUtil;
import com.icoderoad.example.numberplate.util.ImageUtil;

import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 线段实体类
 * 也可以用来标记一条直线
 */
@Data
@NoArgsConstructor
public class Line {
    
    private Long id;

    private Point start;

    private Point end;

    
    public Line(Point start, Point end) {
        setId(GenerateIdUtil.getId());
        setStart(start);
        setEnd(end);
    }
    
    public boolean equals(Line l) {
        if(l.getId().equals(getId())) {
            return true;
        }
        return false;
    }
    

    /**
     * 
     * @param row 四通道，1cols
     */
    public Line(Mat row) {
        setStart(new Point(row.get(0, 0)[0], row.get(0, 0)[1]));
        setEnd(new Point(row.get(0, 0)[2], row.get(0, 0)[3]));
    }

    public Line(double startX, double startY, double endX, double endY) {
        setStart(new Point(startX, startY));
        setEnd(new Point(endX, endY));
    }
    
    /**
     * 线段长度
     * @return
     */
    public double getLength() {
        return Math.sqrt(Math.pow((start.x - end.x), 2) + Math.pow((start.y - end.y), 2));
    }
    
    /**
     * 计算线段的中间点
     * @return
     */
    public Point getMidPoint() {
        return new Point((start.x + end.x)/2, (start.y + end.y)/2);
    }
    
    /**
     * 计算原点(0,0)到当前线段所在直线的距离
     * @return
     */
    public double getDistanceToOrigin() {
        return ImageUtil.getDistance(new Point(0, 0), start, end);
    }
    
    
    /**
     * 计算线段的斜率
     * @return
     */
    public double getK() {
        if(end.x == start.x) {
            return Double.MAX_VALUE; // 跟Y轴平行
        }
        /*if(end.y == start.y) {
            return 0; // 跟X轴平行
        }*/
        return  (end.y - start.y) / (end.x - start.x);
    }

}