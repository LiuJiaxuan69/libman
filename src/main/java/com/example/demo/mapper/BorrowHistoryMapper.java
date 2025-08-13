package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.example.demo.model.BorrowInfo;

@Mapper
public interface BorrowHistoryMapper {
    // 查询操作
    // 查询指定用户借阅历史
    @Select("select * from borrow_info where user_id = #{userId}")
    List<BorrowInfo> queryBorrowHistoryByUserId(Integer userId);
    // 查询指定书籍被借阅历史
    @Select("select * from borrow_info where book_id = #{bookId}")
    List<BorrowInfo> queryBorrowHistoryByBookId(Integer bookId);

    // 插入操作
    @Insert("insert into borrow_history (user_id, book_id, borrow_time) values (#{userId}, #{bookId}, #{borrowTime})")
    Integer insertBorrowInfo(BorrowInfo borrowInfo);
}
