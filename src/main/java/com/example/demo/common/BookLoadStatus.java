package com.example.demo.common;

import lombok.Data;

@Data
public class BookLoadStatus {
    public boolean isEnd;    // 是否加载完毕
    public int remaining;    // 剩余未加载数量

    // 构造器
    public BookLoadStatus(boolean isEnd, int remaining) {
        this.isEnd = isEnd;
        this.remaining = remaining;
    }
}
