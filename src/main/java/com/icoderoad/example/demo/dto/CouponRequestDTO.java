package com.icoderoad.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CouponRequestDTO {
	private String code;
	private BigDecimal value;
	private LocalDateTime expiryDate;
}