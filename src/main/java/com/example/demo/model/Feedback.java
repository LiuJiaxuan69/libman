package com.example.demo.model;

import lombok.Data;

@Data
public class Feedback {
    private Integer id;
    private Integer userId; // 可为空，匿名反馈时为 null
    private String content; // 反馈内容
    private Integer rating; // 1-5 评分，可为空
    private java.time.LocalDateTime createdAt;
}
