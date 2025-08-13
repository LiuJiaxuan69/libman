package com.example.demo.model;

import lombok.Data;
import java.util.Date;

@Data
public class BorrowInfo {
    private Integer bookId;
    private Integer userId;
    private Date borrowTime;
    private Date dueTime; // 应还日期
}
