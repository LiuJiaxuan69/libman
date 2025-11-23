package com.example.demo.model;

import lombok.Data;
import java.util.Date;

@Data
public class ChatHistory {
    private Long id;
    private Integer userId;
    private String contextJson;
    private Integer totalChars;
    private Date createTime;
    private Date updateTime;
}
