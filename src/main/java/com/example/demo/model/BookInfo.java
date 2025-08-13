package com.example.demo.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class BookInfo {
    private Integer id;
    private String bookName;
    private String author;
    private BigDecimal price;
    private String publish;
    private Integer donorId;
    private Integer status;
    private String statusCN; /* 后期加工 */
    private Date createTime;
    private Date updateTime;

    // 非数据库字段，仅用于前端归还按钮判断
    private Boolean isBorrowedByMe;
}
