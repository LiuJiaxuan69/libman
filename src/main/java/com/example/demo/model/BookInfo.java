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
    private String categoryIds; // JSON 数组字符串，存储分类 ID 列表
    private String categoryNames; // 非数据库字段，存储分类名称列表，便于前端显示
    private String tags; // JSON 数组字符串，存储标签列表
    private String description; // 书籍描述
    private String coverUrl; // 书籍封面相对路径或完整URL（默认 default.png）

    // 非数据库字段，仅用于前端归还按钮判断
    private Boolean isBorrowedByMe;
}
