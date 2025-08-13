package com.example.demo.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private int total;      // 所有记录数
    private List<T> records; // 当前页数据

    public PageResult(Integer total, List<T> records) {
        this.total = total;
        this.records = records;
    }
}
