package com.example.demo.common;

import lombok.Data;

@Data
public class PageRequest {
    private int currentPage = 1; // 当前页
    private int pageSize = 8;   // 每页中的记录数

    public int getOffset() {
        return (currentPage - 1) * pageSize;
    }
}
