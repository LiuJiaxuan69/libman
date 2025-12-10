package com.example.demo.mapper;

import com.example.demo.model.Feedback;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FeedbackMapper {

    @Insert("INSERT INTO feedback(user_id, content, rating, created_at) VALUES(#{userId}, #{content}, #{rating}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Feedback feedback);

    @Select("SELECT id, user_id AS userId, content, rating, created_at AS createdAt FROM feedback ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Feedback> list(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT id, user_id AS userId, content, rating, created_at AS createdAt FROM feedback WHERE id = #{id}")
    Feedback findById(@Param("id") Integer id);
}
