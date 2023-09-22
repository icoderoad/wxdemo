package com.icoderoad.example.numberplate.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Configuration
public class TesseractConfig {

    @Value("${tesseract.dataPath}")
    private String tessDataPath;

    @Bean
    public Tesseract tesseract() throws TesseractException {
    	
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("chi_sim"); // 设置语言

        return tesseract;
    }
}