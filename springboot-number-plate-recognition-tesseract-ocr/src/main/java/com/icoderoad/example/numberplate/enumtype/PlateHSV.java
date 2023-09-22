package com.icoderoad.example.numberplate.enumtype;

public enum PlateHSV {

    BLUE(105, 125, 5, 35), 
    GREEN(60, 100, 5, 35), 
    YELLOW(15, 35, 5, 35), 
    UNKNOWN(0, 0, 0, 0);

    public final int minH;
    public final int maxH;

    public final int equalizeMinH;
    public final int equalizeMaxH;

    PlateHSV(int minH, int maxH, int equalizeMinH, int equalizeMaxH) {
        this.minH = minH;
        this.maxH = maxH;
        this.equalizeMinH = equalizeMinH;
        this.equalizeMaxH = equalizeMaxH;
    }

}