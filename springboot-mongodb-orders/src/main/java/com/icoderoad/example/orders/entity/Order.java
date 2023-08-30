package com.icoderoad.example.orders.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.Data;

@Data
@Document(collection = "orders")
public class Order implements Serializable {
	@MongoId
	private String id;
	
    private Long order_id;
    private Date order_date;
    @Transient
    private String formattedOrderDate;  // 用于存放格式化后的日期字符串
    private Long customer_id;
    private BigDecimal price;
    private Product product;
    private boolean order_status;

}