package com.example.demo.common;

import lombok.Data;

@Data
public class OffsetRequest {
    private int offset = 0;  // 起始偏移量
    private int count = 8;   // 请求数量
}
