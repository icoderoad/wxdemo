package com.icoderoad.example.demo.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.icoderoad.example.demo.service.CouponService;
import com.lowagie.text.DocumentException;

@Controller
public class ExportController {
	
    @Autowired
    private CouponService couponService;

    @GetMapping("/export-coupons/csv")
    public void exportCoupons(HttpServletResponse response) {
    	
    	String fileName = "coupons.csv";
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("text/csv; charset=UTF-8");
        //生成导出 CSV 文件
        couponService.exportCouponsToCSV(response);

    }
    
    @GetMapping("/export-coupons/pdf")
    public void exportCouponsPdf(HttpServletResponse response) throws IOException, DocumentException {
        String fileName = "coupons.pdf"; 
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/pdf; charset=UTF-8");

        couponService.exportCouponsToPDF(response);
    }
    
    @GetMapping("/export-test")
    public String exportTestPage(Model model) {
        return "/coupon/export-test"; 
    }
    
}
