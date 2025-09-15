package com.example.demo.mapper;

import java.sql.Date;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.example.demo.model.BorrowInfo;

import lombok.NonNull;


@Mapper
public interface BorrowInfoMapper {
    // 插入操作
    @Insert("insert into borrow_info (user_id, book_id) values (#{userId}, #{bookId})")
    Integer insertBorrowInfo(BorrowInfo borrowInfo);

    // 更新操作
    @Update("update borrow_info set due_time = #{dueTime} where book_id = #{bookId} and user_id = #{userId}")
    Integer updateDueDate(Integer bookId, Integer userId, Date dueTime);

    // 删除操作
    @Delete("delete from borrow_info where book_id = #{bookId} and user_id = #{userId}")
    Integer deleteBorrowInfo(Integer bookId, Integer userId);

    // 查询操作
    @Select("select * from borrow_info where book_id = #{bookId} and user_id = #{userId}")
    BorrowInfo queryBorrowInfoByUserIdAndBookId(Integer bookId, Integer userId);

    // 获取用户借阅了哪些书
    @Select({
        "<script>",
        "select book_id from borrow_info where user_id = #{userId} and book_id in",
        "<foreach item='item' index='index' collection='bookIdList' open='(' separator=',' close=')'>",
        "#{item}",
        "</foreach>",
        "</script>"
    })
    List<Integer> queryBorrowedBookIdsByUserAndBookList(@Param("userId") @NonNull Integer userId, @Param("bookIdList") @NonNull List<@NonNull Integer> bookIdList);
}
