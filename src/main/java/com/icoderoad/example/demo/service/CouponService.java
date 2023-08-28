package com.icoderoad.example.demo.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.icoderoad.example.demo.entity.Coupon;
import com.icoderoad.example.demo.mapper.CouponMapper;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class CouponService {
    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 生成 coupon
    public void generateCoupon(String code, BigDecimal value, LocalDateTime expiryDate) {
        // 入库
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setValue(value);
        coupon.setExpiryDate(expiryDate);
        couponMapper.insert(coupon);

        // 缓存数据
        redisTemplate.opsForList().leftPush("coupons", coupon);
    }

    // 检测过期数据
    public List<Coupon> getExpiredCoupons() {
        Date currentTime = new Date();
        List<Coupon> expiredCoupons = couponMapper.selectList(
            new QueryWrapper<Coupon>()
                .lt("expiry_date", currentTime)
        );

        redisTemplate.opsForList().remove("coupons", 0, expiredCoupons);

        return expiredCoupons;
    }
    
    //导出csv优惠券
    public void exportCouponsToCSV(HttpServletResponse response) {
        try (PrintWriter writer = response.getWriter()) {
            writer.println("ID,优惠券代码,优惠券值,过期时间");

            List<Coupon> coupons = couponMapper.selectList(new QueryWrapper<>());
            for (Coupon coupon : coupons) {
                writer.println(
                    coupon.getId() + ","
                    + coupon.getCode() + ","
                    + coupon.getValue() + ","
                    + coupon.getExpiryDate()
                );
            }

            System.out.println("优惠券导出 CSV 文件成功!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //导出pdf优惠券
    public void exportCouponsToPDF(HttpServletResponse response) throws IOException, DocumentException {
        try (Document document = new Document(PageSize.A4)) {
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();
            
            //支持导出文件中包含中文
            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED); // 使用中文字体
            Font fontChinese = new Font(bfChinese, 12, Font.NORMAL);
            
            List<Coupon> coupons = couponMapper.selectList(new QueryWrapper<>());
            for (Coupon coupon : coupons) {
                String couponInfo = "ID: " + coupon.getId() + "\n"
                        + "优惠券代码: " + coupon.getCode() + "\n"
                        + "优惠券值: " + coupon.getValue() + "\n"
                        + "过期时间: " + coupon.getExpiryDate() + "\n\n";
                Paragraph paragraph = new Paragraph(couponInfo, fontChinese); // 使用中文字体
                document.add(paragraph);
            }
        }

        System.out.println("优惠券导出 PDF 文件成功!");
    }
    
    //导出excel优惠券
    public void exportCouponsToExcel(HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Coupons");

        List<Coupon> coupons = couponMapper.selectList(new QueryWrapper<>());
        // 添加标题行
        String[] columns = {"ID", "优惠券代码", "优惠券值", "过期时间"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
        	 org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
        }

        int rowNum = 1; // 从第二行开始，略过标题行

        for (Coupon coupon : coupons) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;

            org.apache.poi.ss.usermodel.Cell cellId = row.createCell(colNum++);
            cellId.setCellValue(coupon.getId());

            org.apache.poi.ss.usermodel. Cell cellCode = row.createCell(colNum++);
            cellCode.setCellValue(coupon.getCode());

            org.apache.poi.ss.usermodel.Cell cellValue = row.createCell(colNum++);
            cellValue.setCellValue(coupon.getValue().doubleValue());

            org.apache.poi.ss.usermodel.Cell cellExpiryDate = row.createCell(colNum++);
            cellExpiryDate.setCellValue(coupon.getExpiryDate().toString());
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=coupons.xlsx");
        response.setStatus(HttpServletResponse.SC_OK);

        workbook.write(response.getOutputStream());
        workbook.close();

        System.out.println("优惠券导出 Excel 文件成功!");
    }
    
    public boolean isCouponExpired(String code) {
        Coupon coupon = couponMapper.selectOne(
            new QueryWrapper<Coupon>()
                .eq("code", code)
        );

        if (coupon == null) {
            return false;
        }

        LocalDateTime currentTime = LocalDateTime.now();
        return coupon.getExpiryDate().isBefore(currentTime);
    }
}