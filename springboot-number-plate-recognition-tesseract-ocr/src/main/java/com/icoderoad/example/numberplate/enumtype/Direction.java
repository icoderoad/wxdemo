package com.icoderoad.example.numberplate.enumtype;


public enum Direction {

    VERTICAL("VERTICAL","垂直"), 
    HORIZONTAL("HORIZONTAL","水平"), 
    UNKNOWN("UNKNOWN","未知");

    public final String code;
    public final String desc;

    Direction(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDesc(String code) {
        Direction[] enums = values();
        for (Direction type : enums) {
            if (type.code().equals(code)) {
                return type.desc();
            }
        }
        return null;
    }

    public static String getCode(String desc) {
        Direction[] enums = values();
        for (Direction type : enums) {
            if (type.desc().equals(desc)) {
                return type.code();
            }
        }
        return null;
    }
    
    
    public String code() {
        return this.code;
    }

    public String desc() {
        return this.desc;
    }
    
}