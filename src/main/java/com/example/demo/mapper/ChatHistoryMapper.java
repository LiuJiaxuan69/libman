package com.example.demo.mapper;

import com.example.demo.model.ChatHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatHistoryMapper {
    ChatHistory selectByUserId(@Param("userId") Integer userId);
    int insert(ChatHistory chatHistory);
    int updateByUserId(ChatHistory chatHistory);
}
