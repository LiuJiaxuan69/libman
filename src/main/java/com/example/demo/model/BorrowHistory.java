package com.example.demo.model;

import lombok.Data;
import java.util.Date;

@Data
public class BorrowHistory {
    private Integer bookId;
    private Integer userId;
    private Date borrowTime;
}
